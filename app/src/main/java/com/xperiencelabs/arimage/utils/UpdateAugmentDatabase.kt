package com.xperiencelabs.arimage.utils

import android.content.Context
import android.widget.Toast
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Session
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xperiencelabs.arimage.domain.model.Image
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import javax.inject.Inject

class UpdateAugmentDatabase (
    private val appContext:Context,
    private val params:WorkerParameters
): CoroutineWorker(appContext,params) {

    private var session: Session? = null
    private  var database: AugmentedImageDatabase? = null
    override suspend fun doWork(): Result {

        val imagesByteArray = params.inputData.getByteArray("imageData")

        val imageJson = imagesByteArray?.let { String(it,Charsets.UTF_8) }
        val imagesType = object : TypeToken<List<Image>>() {}.type
        val images = Gson().fromJson<List<Image>>(imageJson, imagesType)

//        val images = withContext(Dispatchers.IO) {
//            ObjectInputStream(ByteArrayInputStream(imagesByteArray)).use { it.readObject() as List<Image> }
//        }

        val sharedPreferences = applicationContext.getSharedPreferences("MyPrefs",Context.MODE_PRIVATE)

        session = Session(applicationContext)
        createNewImageDb(session!!,images)

        //Reset the delete count
        sharedPreferences.edit().putInt("delete_count",0).apply()

        return  Result.success()
    }

    private suspend fun createNewImageDb(session: Session,images:List<Image>){
        val path = applicationContext.getExternalFilesDir(null)
        val file = File(path, "arimage.imgdb")

        try {
            if(file.exists()){
                CoroutineScope(Dispatchers.IO).launch {
//                    Toast.makeText(appContext,"File Exists",Toast.LENGTH_SHORT).show()
                    file.delete()
                }

            }
            val newDatabase = AugmentedImageDatabase(session)
            for (image in images){
                val bitmap = uriToBitmap(image.image.toUri(),appContext,false)
                newDatabase.addImage(image.name,bitmap)
            }
            //Serializes the new database to the file
            val outputStream = withContext(Dispatchers.IO) {
                FileOutputStream(file)
            }
            newDatabase.serialize(outputStream)
            withContext(Dispatchers.IO) {
                outputStream.close()
            }

        }catch (e:IOException){
            e.printStackTrace()
        }
    }


}
package com.xperiencelabs.arimage.screens.upload

import android.animation.LayoutTransition
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Session
import com.google.gson.Gson
import com.xperiencelabs.arimage.R
import com.xperiencelabs.arimage.databinding.FragmentAddImageBinding
import com.xperiencelabs.arimage.domain.model.Image
import com.xperiencelabs.arimage.utils.compressBitmap
import com.xperiencelabs.arimage.utils.getFileName
import com.xperiencelabs.arimage.utils.makeToast
import com.xperiencelabs.arimage.utils.uriToBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class AddImageFragment : Fragment() {
    private var _binding:FragmentAddImageBinding? = null
    private val binding get() = _binding!!
    private val bitmapArgs:AddImageFragmentArgs by navArgs()
    private var cBitmap:Bitmap? = null
    private var session: Session? = null
    private  var database: AugmentedImageDatabase? = null
    private lateinit var uname:String
    private lateinit var uvideo:String
    private lateinit var bitmap:Bitmap
    private var isImageSelected = false
    private var isVideoSelected = false
    private lateinit var viewModel: SharedViewModel
    private lateinit var fragmentContext:Context
    private var remainingImages:Int = 15
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]
        bitmap = BitmapFactory.decodeResource(requireContext().resources,R.drawable.default_image)



    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
       _binding = FragmentAddImageBinding.inflate(layoutInflater)

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
       val  sharedPref = requireActivity().getSharedPreferences("isAppOpenedFirstTime",Context.MODE_PRIVATE)
        val isAppOpenedBefore = sharedPref?.getBoolean("isAppOpenedFirstTime",true)
        lifecycleScope.launch {
           val configureDbJob = launch { configureDb() }
            configureDbJob.join()
            val dbExists = checkDatabase()
            if(dbExists){
                return@launch
            }else{
                createDB()
            }

        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.waitlist_dialog,null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val waitListButton = dialogView.findViewById<Button>(R.id.joinWaitList)
        waitListButton.setOnClickListener {
            val websiteUrl = "https://xperiencelabs.co.in/photostories"
            val openUrlIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(websiteUrl)
            }
            startActivity(openUrlIntent)
        }



      CoroutineScope(Dispatchers.Main).launch {
          viewModel.remainingImage.collect {
              binding.remaining.text = it.toString()
              remainingImages = 10 -it
              if (it > 9){
                  dialog.show()
              }
          }
      }


        if (bitmapArgs.bitmap != null){
            cBitmap = bitmapArgs.bitmap
            binding.image.setImageBitmap(cBitmap)
            bitmap = compressBitmap(cBitmap!!)
            uname = generateUniqueFileName()
            isImageSelected = true
            checkMediaSelection()
        }

        val pickVideo = registerForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
            if(uri != null){
                CoroutineScope(Dispatchers.Main).launch {

                    uvideo = uri.toString()
                    Glide.with(fragmentContext).load(uri).into(binding.videoPreview)
                    binding.videoTitle.text = "Video Added"
                    isVideoSelected = true
                    checkMediaSelection()
                }
            }
        }



        binding.save.setOnClickListener {
            if (remainingImages < 1){
                dialog.show()
                return@setOnClickListener
            }
            viewModel.imageExists(uname) { imageExists ->
                if (imageExists) {
                    makeToast("Image Already Added",fragmentContext)
                    return@imageExists
                }

                binding.apply {
                    overlay.visibility = View.VISIBLE
                    save.isEnabled = false
                    save.text = "Saving..."
                    cardView.isEnabled = false
                    video.isEnabled = false
                }

                uploadImage(bitmap, uname) { isUploaded ->
                    binding.apply {
                        overlay.visibility = View.GONE
                        save.isEnabled = true
                        save.text = "Save"
                        cardView.isEnabled = true
                        video.isEnabled = true
                    }

                    lifecycleScope.launch {
                        val imageUri = saveImageToInternalStorage(bitmap, uname)
                        val image = Image(uname, imageUri.toString(), uvideo)
                        if (isUploaded) {
                            viewModel.addImage(image)
                        }
                    }
                    
                }
            }
        }



        binding.cardView.setOnClickListener {
//            pickMedia(){
//                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
//            }
            findNavController().navigate(R.id.action_addImageFragment2_to_cameraFragment)

        }
        binding.video.setOnClickListener {
            pickMedia {
                pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            }
        }
        binding.back.setOnClickListener {
           findNavController().popBackStack()
        }

        // enable layout transitions when width of image changes
        binding.cardView.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)


        //tapTargets when app is opened for the first time
        val tapTarget = TapTargetSequence(requireActivity()).targets(
            TapTarget.forView(
                binding.cardView,"Pick Image",
            "Pick cropped image you want to scan")
                .outerCircleColor(R.color.yellow)
                .outerCircleAlpha(0.9f)
                .dimColor(R.color.background)
                .targetCircleColor(R.color.white)
                .targetRadius(160)
                .titleTextSize(30)
                .tintTarget(false)
            ,

        TapTarget.forView(
            binding.video,
            "Pick Video",
            "Pick Video you want to play on the image \nKeep orientation of Image and Video same for better experience")
            .outerCircleColor(R.color.yellow)
            .outerCircleAlpha(0.9f)
            .dimColor(R.color.background)
            .targetCircleColor(R.color.white)
            .targetRadius(60)
            .titleTextSize(30)
            .tintTarget(false)
        ).listener(object:TapTargetSequence.Listener{
            override fun onSequenceFinish() {
                sharedPref.edit().apply{
                    putBoolean("isAppOpenedFirstTime",false)
                    apply()
                }
            }

            override fun onSequenceStep(lastTarget: TapTarget?, targetClicked: Boolean) {

            }

            override fun onSequenceCanceled(lastTarget: TapTarget?) {
            }

        })

        //start tap target
        if(isAppOpenedBefore==true){
            tapTarget.start()
        }




    }



    /**
    *Pick Media from local storage
    * @params: lambda function which will execute in coroutine
    also used in tap target
    */
    private fun pickMedia(pickMedia:()->Unit){
        CoroutineScope(Dispatchers.IO).launch {
            pickMedia()
        }
    }

    /**
     * Save image to internal storage in new file to avoid getting uri of deleted item in gallery,
     * this will keep track of image required for the app
     * @param bitmap stores bitmap
     * @param fileName name associated with it
     * @return uri
     */
    private suspend fun saveImageToInternalStorage(bitmap: Bitmap, fileName: String): Uri? {
        return withContext(Dispatchers.IO) {
            val directory = File(fragmentContext.filesDir, "Images")
            if (!directory.exists()) {
                directory.mkdir()
            }
            val file = File(directory, fileName)
            try {
                FileOutputStream(file).use { out ->
                    bitmap.compress( Bitmap.CompressFormat.JPEG, 50, out)
                }
                return@withContext Uri.fromFile(file)
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }
    }

    /*
    Serializes Db
    */
    private fun createDB(){
//        if(ActivityCompat.checkSelfPermission(fragmentContext,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
//            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),1)
//            return
//        }
        serialize()
    }

    /*
      Creates Augmented database in storage
      used to store scanned images
      used to store scanned image
     */
    private fun serialize() {
        val path = fragmentContext.getExternalFilesDir(null)
        val file = File(path,"arimage.imgdb")
        try {
            val outputStream = FileOutputStream(file)
//            database!!.addImage("matrix",bitmap)
            database!!.serialize(outputStream)
            outputStream.close()

        } catch (e: IOException){
            e.printStackTrace()
        }

    }


    /**
     * save scanned image to already created Augmented Database
     *
     * @param bitmap Bitmap we have to store in imgdb
     * @param name  Name associated with that bitmap
     * @param (boolean)->Unit  when image is uploaded

     */
    private fun uploadImage(bitmap: Bitmap,name: String,callback:(Boolean)->Unit){
    binding.scanning.visibility = View.VISIBLE
    binding.scanning.playAnimation()
      CoroutineScope(Dispatchers.IO).launch {
          addImageToExistingImgDbFile(bitmap,name){
              callback(it)
          }
      }

    }

    private fun checkMediaSelection(){
        binding.save.isEnabled = isImageSelected && isVideoSelected
    }

    private fun checkDatabase():Boolean{
        val path = fragmentContext.getExternalFilesDir(null)
        val file = File(path,"arimage.imgdb")
        return file.exists()
    }

    private suspend fun configureDb(){
        withContext(Dispatchers.IO){
            session =  Session(fragmentContext)
        }
        database = AugmentedImageDatabase(session)
    }

    /**
     * save scanned image to already created Augmented Database
     *
     * @param bitmap Bitmap we have to store in imgdb
     * @param name  Name associated with that bitmap
     * @param (boolean)->Unit  when image is uploaded

     */
    private suspend fun addImageToExistingImgDbFile(bitmap: Bitmap,name:String,callback:(Boolean)->Unit) {
        val path = fragmentContext.getExternalFilesDir(null)
        val file = File(path, "arimage.imgdb")

        try {
            val inputStream = withContext(Dispatchers.IO) {
                FileInputStream(file)
            }
            val database = AugmentedImageDatabase.deserialize(session,inputStream)
            database.addImage(name, bitmap)
            Log.e("Database",database.numImages.toString())

            val outputStream = withContext(Dispatchers.IO) {
                FileOutputStream(file)
            }
            database.serialize(outputStream)
            withContext(Dispatchers.IO) {
                outputStream.close()
            }
            withContext(Dispatchers.Main){
                delay(2000)
                callback(true)
                makeToast("Image Uploaded",fragmentContext)
                with(binding){
                    scanning.visibility = View.GONE
                    overlay.visibility = View.GONE
                }

            }
        } catch (e: IOException) {
            // Handle the IOException here.
        }
    }

    private fun generateUniqueFileName():String{
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return timeStamp.toString()
    }



}
package com.xperiencelabs.arimage.screens.upload

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencelabs.arimage.domain.model.Image
import com.xperiencelabs.arimage.domain.repository.ImageRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.security.auth.callback.Callback

@HiltViewModel
class SharedViewModel @Inject constructor(
    private val imageRepo: ImageRepo
) : ViewModel() {

    private val _images = MutableStateFlow<List<Image>>(emptyList())
    val images:StateFlow<List<Image>> = _images

    private val _remainingImage = MutableStateFlow<Int>(0)
    val remainingImage:StateFlow<Int> = _remainingImage

    init {
        getAllImages()
    }

    fun addImage(image:Image){
        viewModelScope.launch {
                imageRepo.addImage(image)

        }
    }

    fun imageExists(imageName: String,callback: (Boolean) -> Unit){
        viewModelScope.launch {
            val existingImage = imageRepo.getImage(imageName)
            callback(existingImage!=null)
        }
    }

    fun updateVideo(video:String,id:Int,callback: (String) -> Unit){
        viewModelScope.launch {
        try {
            imageRepo.updateVideo(video, id)
            callback("Video Updated")
        } catch (e:SecurityException){
            callback("Something went wrong")
        }

        }
    }

    private fun getAllImages(){
        viewModelScope.launch {
            imageRepo.getImages().collect()
            {
                _remainingImage.value = it.size
                _images.value = it
            }
        }
    }

     fun deleteImage(image: Image){
         viewModelScope.launch {
             imageRepo.deleteImage(image)
         }
     }


}
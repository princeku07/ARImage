package com.xperiencelabs.arimage.domain.repository

import com.xperiencelabs.arimage.domain.model.Image
import kotlinx.coroutines.flow.Flow

interface ImageRepo {
    suspend fun addImage(image: Image)
    suspend fun getImages():Flow<List<Image>>
    suspend fun getImage(name:String):Image?
    suspend fun updateVideo(video:String,id:Int)
    suspend fun deleteImage(image: Image)
}
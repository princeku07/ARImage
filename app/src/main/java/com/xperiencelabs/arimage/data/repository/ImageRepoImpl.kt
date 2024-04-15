package com.xperiencelabs.arimage.data.repository

import com.xperiencelabs.arimage.data.dao.ImageDao
import com.xperiencelabs.arimage.domain.model.Image
import com.xperiencelabs.arimage.domain.repository.ImageRepo
import kotlinx.coroutines.flow.Flow

class ImageRepoImpl(private val imageDao: ImageDao) : ImageRepo {
    override suspend fun addImage(image: Image) {
        imageDao.addImage(image)
    }

    override suspend fun getImages(): Flow<List<Image>> {
       return imageDao.getAllImages()
    }

    override suspend fun getImage(name: String): Image? {
        return imageDao.getImage(name)
    }

    override suspend fun updateVideo(video: String, id: Int) {
        return imageDao.updateVideo(video,id)
    }

    override suspend fun deleteImage(image: Image) {
        imageDao.deleteImage(image)
    }
}
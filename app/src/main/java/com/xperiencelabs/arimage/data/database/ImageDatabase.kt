package com.xperiencelabs.arimage.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.xperiencelabs.arimage.data.dao.ImageDao
import com.xperiencelabs.arimage.domain.model.Image

@Database(entities = [Image::class], version = 1, exportSchema = false)

abstract class ImageDatabase :RoomDatabase() {
    abstract val imageDao:ImageDao
}
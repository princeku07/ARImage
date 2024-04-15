package com.xperiencelabs.arimage.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xperiencelabs.arimage.domain.model.Image
import kotlinx.coroutines.flow.Flow


@Dao
interface ImageDao {

    @Insert()
    suspend fun addImage(image: Image)

    @Query("SELECT * FROM images ORDER BY id DESC")
    fun getAllImages():Flow<List<Image>>

    @Query("SELECT * FROM images WHERE name= :name")
    suspend fun getImage(name:String):Image?

    @Query("UPDATE images SET video = :video where id = :id")
    suspend fun updateVideo(video: String,id:Int)

    @Delete
    suspend fun deleteImage(image: Image)

}
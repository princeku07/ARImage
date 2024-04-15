package com.xperiencelabs.arimage.domain.model

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "images")
data class Image(
    val  name:String,
    val image:String,
    val video:String,
    @PrimaryKey(autoGenerate = true)
    var id:Int = 0
):Serializable

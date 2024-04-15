package com.xperiencelabs.arimage.di

import android.app.Application
import androidx.room.Room
import com.xperiencelabs.arimage.data.database.ImageDatabase
import com.xperiencelabs.arimage.data.repository.ImageRepoImpl
import com.xperiencelabs.arimage.domain.repository.ImageRepo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object ARImageModule {

    @Provides
    @Singleton
    fun providesDatabase(app:Application):ImageDatabase{
        return  Room.databaseBuilder(
            app,
            ImageDatabase::class.java,
            "image_db"
        ).build()
    }

    @Provides
    @Singleton
    fun providesImageRepo(db:ImageDatabase):ImageRepo {
        return  ImageRepoImpl(db.imageDao)
    }
}
package com.interviewprep.core.data.di

import android.content.Context
import androidx.room.Room
import com.interviewprep.core.data.db.ContentDao
import com.interviewprep.core.data.db.ContentDatabase
import com.interviewprep.core.data.db.UserDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun contentDb(@ApplicationContext context: Context): ContentDatabase =
        Room.databaseBuilder(context, ContentDatabase::class.java, "content.db")
            // content is rebuilt from bundled JSON, so a schema change can simply recreate it
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun userDb(@ApplicationContext context: Context): UserDatabase =
        Room.databaseBuilder(context, UserDatabase::class.java, "progress.db")
            // add real Migrations here when the user schema changes; progress must not be dropped
            .build()

    @Provides
    fun contentDao(db: ContentDatabase): ContentDao = db.contentDao()
}

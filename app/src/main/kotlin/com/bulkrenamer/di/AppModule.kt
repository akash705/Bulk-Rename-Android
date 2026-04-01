package com.bulkrenamer.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import com.bulkrenamer.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "bulk_renamer.db"
        ).build()

    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver
}

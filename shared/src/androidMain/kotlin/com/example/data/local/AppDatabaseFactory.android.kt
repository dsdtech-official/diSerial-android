package com.example.data.local

import android.content.Context
import androidx.room.Room

object AndroidDatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: run {
                val appContext = context.applicationContext
                val dbFile = appContext.getDatabasePath(DISERIAL_DATABASE_NAME)
                val builder = Room.databaseBuilder<AppDatabase>(
                    context = appContext,
                    name = dbFile.absolutePath,
                )
                AppDatabaseFactory.build(builder, useBundledDriver = false)
            }.also { instance = it }
        }
    }
}

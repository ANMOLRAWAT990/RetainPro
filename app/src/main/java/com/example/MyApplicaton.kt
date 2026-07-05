package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.WallDesignRepository

class MyApplicaton : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { WallDesignRepository(database.wallDesignDao()) }
}

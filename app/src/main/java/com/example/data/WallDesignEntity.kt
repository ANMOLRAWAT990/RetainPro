package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.WallParams

@Entity(tableName = "wall_designs")
data class WallDesignEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectName: String,
    val chainage: String,
    val paramsJson: String,
    val timestamp: Long = System.currentTimeMillis()
)

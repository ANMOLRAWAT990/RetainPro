package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WallDesignDao {
    @Query("SELECT * FROM wall_designs ORDER BY timestamp DESC")
    fun getAllDesigns(): Flow<List<WallDesignEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDesign(design: WallDesignEntity)

    @Query("DELETE FROM wall_designs WHERE id = :id")
    suspend fun deleteDesignById(id: Int)
}

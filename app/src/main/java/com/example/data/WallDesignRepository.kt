package com.example.data

import kotlinx.coroutines.flow.Flow

class WallDesignRepository(private val dao: WallDesignDao) {
    val allDesigns: Flow<List<WallDesignEntity>> = dao.getAllDesigns()

    suspend fun insert(design: WallDesignEntity) {
        dao.insertDesign(design)
    }

    suspend fun deleteById(id: Int) {
        dao.deleteDesignById(id)
    }
}

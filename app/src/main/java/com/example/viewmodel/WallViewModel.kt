package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MyApplicaton
import com.example.model.AnalysisResult
import com.example.model.WallParams
import com.example.engineering.StructuralCalculator
import com.example.data.WallDesignEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WallViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as MyApplicaton).repository

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(WallParams::class.java)

    private val _wallParams = MutableStateFlow(WallParams())
    val wallParams: StateFlow<WallParams> = _wallParams.asStateFlow()

    private val _analysisResult = MutableStateFlow<AnalysisResult?>(null)
    val analysisResult: StateFlow<AnalysisResult?> = _analysisResult.asStateFlow()

    val savedLogs = repository.allDesigns

    init {
        analyze()
    }

    fun updateParam(updater: (WallParams) -> WallParams) {
        _wallParams.update(updater)
    }

    fun analyze() {
        val result = StructuralCalculator.calculate(_wallParams.value)
        _analysisResult.value = result
    }

    fun reset() {
        _wallParams.value = WallParams()
        analyze()
    }

    fun saveCurrentDesign() {
        val params = _wallParams.value
        val entity = WallDesignEntity(
            projectName = params.projectName,
            chainage = params.chainage,
            paramsJson = adapter.toJson(params)
        )
        viewModelScope.launch {
            repository.insert(entity)
        }
    }

    fun loadDesign(entity: WallDesignEntity) {
        try {
            val params = adapter.fromJson(entity.paramsJson)
            if (params != null) {
                _wallParams.value = params
                analyze()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteDesign(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }
}

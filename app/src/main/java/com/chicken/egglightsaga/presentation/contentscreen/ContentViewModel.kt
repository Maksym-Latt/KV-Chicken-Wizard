package com.chicken.egglightsaga.presentation.contentscreen

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ContentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val initialUrl: String = savedStateHandle.get<String>("url")?.let { encoded ->
        Uri.decode(encoded)
    }.orEmpty()
}

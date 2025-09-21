package com.project.drmplayer

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PlayerViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
            val playerController: IPlayerController = ExoPlayerControllerImpl(application)
            @Suppress("UNCHECKED_CAST")
            return PlayerViewModel(playerController) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.project.drmplayer.controller

import androidx.lifecycle.LiveData
import com.project.drmplayer.viewmodel.Event

interface IPlayerController {
    val playerInstance: LiveData<Any?> // Generic type for the player object
    val availableResolutions: LiveData<List<String>>
    val toastMessage: LiveData<Event<String>>

    fun initializePlayer()
    fun loadVideo(videoUrl: String, licenseUrl: String)
    fun playDemoVideo()
    fun selectResolution(resolutionName: String)
    fun releasePlayer()
}
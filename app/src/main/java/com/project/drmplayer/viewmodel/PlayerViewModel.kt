package com.project.drmplayer.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.project.drmplayer.controller.IPlayerController

// Event wrapper for single-fire LiveData events
open class Event<out T>(private val content: T) {
    var hasBeenHandled = false
        private set

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    fun peekContent(): T = content
}

class PlayerViewModel(private val playerController: IPlayerController) : ViewModel() {

    val playerInstance: LiveData<Any?> = playerController.playerInstance
    val availableResolutions: LiveData<List<String>> = playerController.availableResolutions
    val toastMessage: LiveData<Event<String>> = playerController.toastMessage

    // Demo URLs are not directly player controller concerns, can stay or move to a config/repository if complex
    val demoMpdUrl = "https://bitmovin-a.akamaihd.net/content/art-of-motion_drm/mpds/11331.mpd"
    val demoLicenseUrl = "https://cwip-shaka-proxy.appspot.com/no_auth"

    fun initializePlayer() {
        playerController.initializePlayer()
    }

    fun loadVideo(videoUrl: String, licenseUrl: String) {
        playerController.loadVideo(videoUrl, licenseUrl)
    }

    fun playDemoVideo() {
        // The ViewModel can still decide which demo content to play, 
        // or this logic could also be moved to the controller if it's purely about playback
        initializePlayer() // Ensure player is initialized via controller
        playerController.loadVideo(demoMpdUrl, demoLicenseUrl)
        // Toasting can be handled by observing controller's toastMessage LiveData in Activity
        // If a specific toast is needed from VM for this action, it can have its own LiveData event
        // For now, assuming controller's loadVideo/playDemoVideo will trigger a toast via its own LiveData
    }

    fun selectResolution(resolutionName: String) {
        playerController.selectResolution(resolutionName)
    }

    fun releasePlayer() {
        playerController.releasePlayer()
    }

    override fun onCleared() {
        super.onCleared()
        // The PlayerController might have its own lifecycle needs, 
        // but a simple releasePlayer call is common.
        // If IPlayerController were a more complex component (e.g., managing its own coroutine scopes),
        // it might need its own lifecycle method like onCleared().
        playerController.releasePlayer()
    }
}

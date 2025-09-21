package com.project.drmplayer.controller

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.util.MimeTypes
import com.project.drmplayer.controller.IPlayerController
import com.project.drmplayer.viewmodel.Event

class ExoPlayerControllerImpl(private val application: Application) : IPlayerController {

    private var _exoPlayer: ExoPlayer? = null

    private val _playerInstanceLiveData = MutableLiveData<ExoPlayer?>()
    override val playerInstance: LiveData<Any?> = _playerInstanceLiveData as LiveData<Any?> // Cast to Any? as per interface

    private lateinit var trackSelector: DefaultTrackSelector

    private val _availableResolutionsLiveData =
        MutableLiveData<List<String>>(listOf("Select Resolution"))
    override val availableResolutions: LiveData<List<String>> = _availableResolutionsLiveData

    private val _toastMessageLiveData = MutableLiveData<Event<String>>()
    override val toastMessage: LiveData<Event<String>> = _toastMessageLiveData

    private val demoMpdUrl = "https://bitmovin-a.akamaihd.net/content/art-of-motion_drm/mpds/11331.mpd"
    private val demoLicenseUrl = "https://cwip-shaka-proxy.appspot.com/no_auth"

    init {
        trackSelector = DefaultTrackSelector(application)
    }

    override fun initializePlayer() {
        if (_exoPlayer == null) {
            _exoPlayer = ExoPlayer.Builder(application)
                .setTrackSelector(trackSelector)
                .build()
            _playerInstanceLiveData.postValue(_exoPlayer)

            _exoPlayer?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        updateAvailableResolutions()
                    }
                }
            })
        }
    }

    override fun loadVideo(videoUrl: String, licenseUrl: String) {
        if (videoUrl.isEmpty()) {
            _toastMessageLiveData.value = Event("Please enter a video URL")
            return
        }
        if (_exoPlayer == null) initializePlayer() // Ensure player is initialized

        _exoPlayer?.let { exoPlayer ->
            val mediaItemBuilder = MediaItem.Builder()
                .setUri(videoUrl)

            if (licenseUrl.isNotEmpty()) {
                val drmConfig = MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                    .setLicenseUri(licenseUrl)
                    .build()
                mediaItemBuilder.setDrmConfiguration(drmConfig)
            }

            val mimeType = when {
                videoUrl.contains(".mpd") -> MimeTypes.APPLICATION_MPD
                videoUrl.contains(".m3u8") -> MimeTypes.APPLICATION_M3U8
                else -> MimeTypes.APPLICATION_MPD // Default to DASH
            }
            mediaItemBuilder.setMimeType(mimeType)

            val mediaItem = mediaItemBuilder.build()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    override fun playDemoVideo() {
        initializePlayer()
        loadVideo(demoMpdUrl, demoLicenseUrl)
        _toastMessageLiveData.value = Event("Playing demo DRM content")
    }

    override fun releasePlayer() {
        _exoPlayer?.release()
        _exoPlayer = null
        _playerInstanceLiveData.postValue(null)
    }

    private fun updateAvailableResolutions() {
        val currentResolutions = mutableListOf("Select Resolution")
        val trackGroups = trackSelector.currentMappedTrackInfo

        if (trackGroups != null) {
            val rendererIndex = 0 // Video renderer
            val trackGroupArray = trackGroups.getTrackGroups(rendererIndex)
            for (groupIndex in 0 until trackGroupArray.length) {
                val trackGroup = trackGroupArray.get(groupIndex)
                for (trackIndex in 0 until trackGroup.length) {
                    val format = trackGroup.getFormat(trackIndex)
                    if (format.height > 0) {
                        val resolution = "${format.width}x${format.height}"
                        if (!currentResolutions.contains(resolution)) {
                            currentResolutions.add(resolution)
                        }
                    }
                }
            }
        }
        _availableResolutionsLiveData.postValue(currentResolutions)
    }

    override fun selectResolution(resolutionName: String) {
        val trackGroups = trackSelector.currentMappedTrackInfo ?: return
        val rendererIndex = 0 // Video renderer
        val trackGroupArray = trackGroups.getTrackGroups(rendererIndex)

        for (groupIndex in 0 until trackGroupArray.length) {
            val trackGroup = trackGroupArray.get(groupIndex)
            for (trackIndex in 0 until trackGroup.length) {
                val format = trackGroup.getFormat(trackIndex)
                if (format.height > 0) {
                    val resolution = "${format.width}x${format.height}"
                    if (resolution == resolutionName) {
                        val selectionOverride = DefaultTrackSelector.SelectionOverride(groupIndex, trackIndex)
                        val parameters = trackSelector.parameters.buildUpon()
                            .setSelectionOverride(rendererIndex, trackGroupArray, selectionOverride)
                            .build()
                        trackSelector.setParameters(parameters)
                        _toastMessageLiveData.postValue(Event("Resolution changed to $resolution"))
                        return
                    }
                }
            }
        }
    }
}
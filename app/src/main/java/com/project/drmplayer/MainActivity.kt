package com.project.drmplayer

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaItem.DrmConfiguration
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.util.MimeTypes

class MainActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var spinnerResolutions: Spinner
    private lateinit var trackSelector: DefaultTrackSelector
    private var resolutionAdapter: ArrayAdapter<String>? = null
    private var availableResolutions = mutableListOf<String>()

    // UI Elements
    private lateinit var etVideoUrl: EditText
    private lateinit var etLicenseUrl: EditText
    private lateinit var btnPlayCustom: Button
    private lateinit var btnPlayDemo: Button

    private val demoMpdUrl =
        "https://bitmovin-a.akamaihd.net/content/art-of-motion_drm/mpds/11331.mpd"
    private val demoLicenseUrl = "https://cwip-shaka-proxy.appspot.com/no_auth"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // Initialize UI elements
        playerView = findViewById(R.id.player_view)
        spinnerResolutions = findViewById(R.id.spinner_resolutions)
        etVideoUrl = findViewById(R.id.etVideoUrl)
        etLicenseUrl = findViewById(R.id.etLicenseUrl)
        btnPlayCustom = findViewById(R.id.btnPlayCustom)
        btnPlayDemo = findViewById(R.id.btnPlayDemo)

        trackSelector = DefaultTrackSelector(this)

        // Initialize resolution spinner
        resolutionAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, availableResolutions)
        resolutionAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerResolutions.adapter = resolutionAdapter

        // Set up resolution selection listener
        spinnerResolutions.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (position > 0) { // Skip "Select Resolution" option
                    selectResolution(position - 1)
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Set up button click listeners
        btnPlayCustom.setOnClickListener {
            playCustomVideo()
        }

        btnPlayDemo.setOnClickListener {
            playDemoVideo()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }


    private fun initializePlayer() {
        if (player == null) {
            player = ExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .build()
            playerView.player = player

            // Add player listener to detect when tracks are available
            player?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    if (playbackState == Player.STATE_READY) {
                        updateAvailableResolutions()
                    }
                }
            })
        }
    }

    private fun playCustomVideo() {
        val videoUrl = etVideoUrl.text.toString().trim()
        val licenseUrl = etLicenseUrl.text.toString().trim()

        if (videoUrl.isEmpty()) {
            Toast.makeText(this, "Please enter a video URL", Toast.LENGTH_SHORT).show()
            return
        }

        initializePlayer()
        loadVideo(videoUrl, licenseUrl)
    }

    private fun playDemoVideo() {
        initializePlayer()
        loadVideo(demoMpdUrl, demoLicenseUrl)
        Toast.makeText(this, "Playing demo DRM content", Toast.LENGTH_SHORT).show()
    }

    private fun loadVideo(videoUrl: String, licenseUrl: String) {
        player?.let { exoPlayer ->
            val mediaItemBuilder = MediaItem.Builder()
                .setUri(videoUrl)

            // Add DRM configuration if license URL is provided
            if (licenseUrl.isNotEmpty()) {
                val drmConfig = DrmConfiguration.Builder(C.WIDEVINE_UUID)
                    .setLicenseUri(licenseUrl)
                    .build()
                mediaItemBuilder.setDrmConfiguration(drmConfig)
            }

            // Set MIME type based on URL
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

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onResume() {
        super.onResume()
        initializePlayer()
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }

    private fun updateAvailableResolutions() {
        val trackGroups = trackSelector.currentMappedTrackInfo
        availableResolutions.clear()
        availableResolutions.add("Select Resolution") // Default option

        if (trackGroups != null) {
            val rendererIndex = 0 // Video renderer is typically at index 0
            val trackGroupArray = trackGroups.getTrackGroups(rendererIndex)
            
            for (groupIndex in 0 until trackGroupArray.length) {
                val trackGroup = trackGroupArray.get(groupIndex)
                for (trackIndex in 0 until trackGroup.length) {
                    val format = trackGroup.getFormat(trackIndex)
                    if (format.height > 0) {
                        val resolution = "${format.width}x${format.height}"
                        if (!availableResolutions.contains(resolution)) {
                            availableResolutions.add(resolution)
                        }
                    }
                }
            }
        }

        runOnUiThread {
            resolutionAdapter?.notifyDataSetChanged()
        }
    }

    private fun selectResolution(resolutionIndex: Int) {
        if (resolutionIndex >= availableResolutions.size - 1) return // Skip "Select Resolution" option
        
        val trackGroups = trackSelector.currentMappedTrackInfo
        if (trackGroups == null) return

        val rendererIndex = 0 // Video renderer
        val trackGroupArray = trackGroups.getTrackGroups(rendererIndex)
        val resolutionToFind = availableResolutions[resolutionIndex + 1] // +1 to skip default option
        
        for (groupIndex in 0 until trackGroupArray.length) {
            val trackGroup = trackGroupArray.get(groupIndex)
            for (trackIndex in 0 until trackGroup.length) {
                val format = trackGroup.getFormat(trackIndex)
                if (format.height > 0) {
                    val resolution = "${format.width}x${format.height}"
                    if (resolution == resolutionToFind) {
                        val selectionOverride = DefaultTrackSelector.SelectionOverride(groupIndex, trackIndex)
                        val parameters = trackSelector.parameters.buildUpon()
                            .setSelectionOverride(rendererIndex, trackGroupArray, selectionOverride)
                            .build()
                        trackSelector.setParameters(parameters)
                        
                        runOnUiThread {
                            Toast.makeText(this, "Resolution changed to $resolution", Toast.LENGTH_SHORT).show()
                        }
                        return
                    }
                }
            }
        }
    }
}

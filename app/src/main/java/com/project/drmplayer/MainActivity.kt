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
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.ExoPlayer // Import ExoPlayer for casting
import com.google.android.exoplayer2.ui.PlayerView
import com.project.drmplayer.viewmodel.PlayerViewModel
import com.project.drmplayer.viewmodel.PlayerViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var spinnerResolutions: Spinner
    private var resolutionAdapter: ArrayAdapter<String>? = null

    // UI Elements
    private lateinit var etVideoUrl: EditText
    private lateinit var etLicenseUrl: EditText
    private lateinit var btnPlayCustom: Button
    private lateinit var btnPlayDemo: Button

    private lateinit var viewModel: PlayerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize ViewModel using the factory
        val factory = PlayerViewModelFactory(application)
        viewModel = ViewModelProvider(this, factory)[PlayerViewModel::class.java]

        // Initialize UI elements
        playerView = findViewById(R.id.player_view)
        spinnerResolutions = findViewById(R.id.spinner_resolutions)
        etVideoUrl = findViewById(R.id.etVideoUrl)
        etLicenseUrl = findViewById(R.id.etLicenseUrl)
        btnPlayCustom = findViewById(R.id.btnPlayCustom)
        btnPlayDemo = findViewById(R.id.btnPlayDemo)

        // Initialize resolution spinner
        resolutionAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf<String>())
        resolutionAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerResolutions.adapter = resolutionAdapter

        // Set up observers
        viewModel.playerInstance.observe(this) { player ->
            // Cast the player instance to ExoPlayer. PlayerView expects ExoPlayer.
            playerView.player = player as? ExoPlayer
        }

        viewModel.availableResolutions.observe(this) { resolutions ->
            resolutionAdapter?.clear()
            resolutionAdapter?.addAll(resolutions)
            resolutionAdapter?.notifyDataSetChanged()
        }

        viewModel.toastMessage.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        // Set up resolution selection listener
        spinnerResolutions.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedResolution = resolutionAdapter?.getItem(position)
                if (selectedResolution != null && selectedResolution != "Select Resolution") {
                     viewModel.selectResolution(selectedResolution)
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Set up button click listeners
        btnPlayCustom.setOnClickListener {
            playCustomVideo()
        }

        btnPlayDemo.setOnClickListener {
            // Call playDemoVideo on ViewModel, which now uses the controller
            viewModel.playDemoVideo()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun playCustomVideo() {
        val videoUrl = etVideoUrl.text.toString().trim()
        val licenseUrl = etLicenseUrl.text.toString().trim()

        // ViewModel now handles empty URL check via controller, but immediate UI feedback can stay if desired
        if (videoUrl.isEmpty()) {
            Toast.makeText(this, "Please enter a video URL", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.initializePlayer() // Ensure player is initialized via ViewModel and Controller
        viewModel.loadVideo(videoUrl, licenseUrl)
    }

    override fun onStart() {
        super.onStart()
        viewModel.initializePlayer()
    }

    override fun onResume() {
        super.onResume()
        viewModel.initializePlayer()
    }

    override fun onPause() {
        super.onPause()
        viewModel.releasePlayer()
    }

    override fun onStop() {
        super.onStop()
        viewModel.releasePlayer()
    }
}

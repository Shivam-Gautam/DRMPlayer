# DRMPlayer Android App

## Overview

DRMPlayer is an Android application designed to demonstrate video playback using ExoPlayer. It includes functionality for initializing a player, loading video content from URLs, handling different MimeTypes (MPD, M3U8, MP4), and managing basic DRM configuration if a license URL is provided. The app features a demo player that streams a sample MP4 video from a remote HTTPS source.

## Features

*   **ExoPlayer Integration:** Utilizes ExoPlayer for robust video playback.
*   **Dynamic Video Loading:** Capable of loading videos from user-provided URLs.
*   **DRM Support (Basic):** Includes logic to configure DRM (Widevine) if a license URL is supplied with the video content.
*   **MimeType Handling:** Detects MimeTypes for `.mpd` (DASH), `.m3u8` (HLS), and `.mp4` streams. Allows ExoPlayer to infer other types.
*   **Demo Playback:** Includes a function to play a pre-configured sample MP4 video (`ForBiggerFun.mp4` from `commondatastorage.googleapis.com`).
*   **Resolution Selection:** Contains logic to detect available video resolutions from the track selector and allows selection (more relevant for adaptive streaming formats like DASH/HLS).
*   **LiveData Integration:** Exposes player instance, available resolutions, and toast messages via LiveData for UI observation.

## Core Logic & Implementation Details

### 1. Player Setup (`ExoPlayerControllerImpl.kt`)

*   **ExoPlayer Initialization:** An `ExoPlayer` instance is created and managed within `ExoPlayerControllerImpl.kt`. It uses a `DefaultTrackSelector` for managing track selection (e.g., video resolution).
*   **`initializePlayer()`:** Sets up the ExoPlayer instance if it hasn't been created.
*   **`releasePlayer()`:** Releases the ExoPlayer instance to free up resources.

### 2. Video Loading (`loadVideo` function)

*   Takes a `videoUrl` and an optional `licenseUrl` as input.
*   **MediaItem Creation:** A `MediaItem` is built using the provided `videoUrl`.
*   **DRM Configuration:** If a `licenseUrl` is provided, a `MediaItem.DrmConfiguration` is built (currently defaults to `C.WIDEVINE_UUID`) and attached to the `MediaItem`.
*   **MimeType Handling:**
    *   Explicitly sets MimeTypes for URLs containing `.mpd` (MimeTypes.APPLICATION_MPD), `.m3u8` (MimeTypes.APPLICATION_M3U8), or `.mp4` (MimeTypes.VIDEO_MP4).
    *   If the URL doesn't match these extensions, the MimeType is set to `null`, allowing ExoPlayer to attempt to infer the correct type from the content itself.
*   **Preparation & Playback:** The `MediaItem` is set to the player, which is then prepared and starts playing when ready.

### 3. Demo Playback (`playDemoVideo` function)

*   Initializes the player if needed.
*   Calls `loadVideo()` with a hardcoded HTTPS URL for a sample MP4 video: `https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4`.
*   The `demoLicenseUrl` is set to an empty string as this sample MP4 does not use DRM.

### 4. Resolution Management

*   `updateAvailableResolutions()`: When a video is ready (`Player.STATE_READY`), this function inspects the track information from `trackSelector.currentMappedTrackInfo` to find available video resolutions (width x height) and updates `_availableResolutionsLiveData`.
*   `selectResolution()`: Allows changing the video resolution by creating a `DefaultTrackSelector.SelectionOverride` and applying it to the track selector's parameters.

## How to Use / Test

1.  **Build and Run:** Open the project in Android Studio, build, and run on an Android device or emulator.
2.  **Demo Video:** The app should have a mechanism (e.g., a button in the UI, or it might play on startup depending on your Activity/Fragment implementation) that calls the `playDemoVideo()` method in `ExoPlayerControllerImpl`. This will start playing the sample MP4 video.
3.  **Custom URLs (if UI exists):** If the UI provides input fields for video URL and license URL, you can test custom content by entering the respective URLs and triggering the `loadVideo()` method.

## Key Files

*   `app/src/main/java/com/project/drmplayer/controller/ExoPlayerControllerImpl.kt`: Contains the core ExoPlayer setup, video loading logic, DRM handling, and resolution management.
*   **(Likely) UI Files:** Your Activity/Fragment files (e.g., `MainActivity.kt`, `PlayerFragment.kt`) that instantiate and interact with `ExoPlayerControllerImpl` and display the video using a `PlayerView`.
*   **(Likely) ViewModel Files:** Any ViewModel classes that use `ExoPlayerControllerImpl` to provide data to the UI.

## Setup and Build

1.  Clone the repository (if applicable).
2.  Open the project in Android Studio.
3.  Let Gradle sync the dependencies (ExoPlayer libraries should be listed in `app/build.gradle`).
4.  Ensure your device/emulator has internet access.
5.  Build and run the application.

## Known Limitations & Considerations

*   **DRM Provisioning:** For actual DRM-protected content, the device might need to be provisioned. This setup assumes the device is already capable of playing Widevine DRM content.
*   **License Server Compatibility:** DRM license server interactions can be complex and vary. The current DRM setup is basic.
*   **Resolution Selection for MP4:** The resolution selection feature is most effective for adaptive bitrate streams (DASH/HLS). For a single-resolution MP4 like the demo, it will likely only show one resolution.
*   **Error Handling:** The current controller posts toast messages for some events. A production app would require more comprehensive error handling and user feedback mechanisms.


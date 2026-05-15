# Sharing (Screenshots) & Video Recording

## Key files

- [`app/src/main/java/com/music42/swiftyprotein/ui/proteinview/ProteinViewScreen.kt`](../app/src/main/java/com/music42/swiftyprotein/ui/proteinview/ProteinViewScreen.kt)
- [`app/src/main/java/com/music42/swiftyprotein/ui/proteinview/MediaProjectionForegroundService.kt`](../app/src/main/java/com/music42/swiftyprotein/ui/proteinview/MediaProjectionForegroundService.kt)
- [`app/src/main/res/xml/file_paths.xml`](../app/src/main/res/xml/file_paths.xml)

## Screenshot sharing

- Uses `PixelCopy` to capture the Activity window.
- Writes an image file into `cacheDir/shared_images/`.
- Shares via an `ACTION_SEND` intent using a `FileProvider` URI.
- Supports **PNG** and **JPEG** export from the share dialog.

## Video recording

- Uses **MediaProjection** + **MediaRecorder** to record the screen to MP4 (~5 s with auto-rotate).
- Runs a **foreground service** (`MediaProjectionForegroundService`, type `mediaProjection`) while recording; Android 13+ may prompt for notification permission.
- Saves into `cacheDir/shared_videos/`.
- After recording finishes, the app shares the resulting MP4 via `FileProvider`.

## Notes

- MediaProjection requires a system permission dialog. The app suppresses the “return to login” redirect after this dialog.

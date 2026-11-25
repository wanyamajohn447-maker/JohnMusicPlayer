# Testing

## Automated
**Unit tests** (`./gradlew testDebugUnitTest`) — JVM, no device needed:
`FormatTest`, `LrcParserTest`, `M3uTest`, `LibraryAnalyzerTest`, `EqualizerCurveTest`.

**Instrumented tests** (`./gradlew connectedDebugAndroidTest`) — need a device/emulator:
`PlaylistDaoTest` (Room: playlists, reorder, cascade, favourites, history), `AboutScreenTest` (Compose UI: required branding).

Not yet written (gaps): scanner tests against a populated MediaStore, MediaSession/background-playback instrumented tests, permission-flow UI tests, ViewModel tests with fakes.

## Manual checklist (must be done on real hardware before release)
- [ ] Permission flows: grant, deny, "open settings", revoke while app is running (library + videos)
- [ ] Scan a large library (5,000+ songs): scrolling smooth, no ANR
- [ ] Play MP3, AAC/M4A, FLAC, WAV, OGG, OPUS; confirm which formats fail on the device
- [ ] Gapless transitions between two tracks of an album
- [ ] Lock the screen: playback continues, notification + lock-screen controls work
- [ ] Switch apps / swipe the app away while playing: music continues; stops when paused + swiped
- [ ] Incoming call / other app's audio: pauses and resumes correctly
- [ ] Unplug wired headphones: playback pauses. Bluetooth connect/disconnect, headset buttons
- [ ] Kill the process, reopen: queue/position restored (paused)
- [ ] Equalizer: sliders audibly change sound; presets; bass boost; virtualizer; unsupported-effect messaging
- [ ] Sleep timer: minutes (with fade), end of song, end of playlist, cancel
- [ ] Lyrics: manual .lrc load, lyrics-folder auto match, synced highlight, tap-to-seek
- [ ] Visualizer: each style, permission denial path, battery impact over 10 min
- [ ] Video: MP4/MKV/WebM, subtitles (SRT/VTT, embedded), audio tracks, speed, aspect ratio, rotation, brightness/volume gestures, resume, PiP
- [ ] Playlists: create/rename/delete/reorder, export + import M3U, missing-file handling
- [ ] File browser with an SD card folder
- [ ] Test on Android 7, 10, 13, 14/15; small phone and tablet; large font size / TalkBack
- [ ] Battery and memory profiling (Android Studio Profiler)

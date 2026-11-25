# Status & known limitations (be honest, per Rule 13/14)

**Nothing here has been compiled or run.** The code was written without an Android SDK or network access. Expect a round of compile fixes on first sync. Library versions in `libs.versions.toml` must be verified.

## Implemented in code (unverified)
Home dashboard (customizable sections) · Music library (songs/albums/artists/genres/folders/favourites/recent/added/most played, search, sort, list/grid, multi-select, share, rescan) · Now Playing (artwork, seek, shuffle/repeat, queue editing, favourite, lyrics, output route, speed) · Background playback via Media3 `MediaSessionService` (notification/lock-screen/Bluetooth/headset controls, audio focus, becoming-noisy, error skip, queue restore) · Playlists (Room, reorder, M3U import/export, share, missing-file handling) · Equalizer (real `android.media.audiofx` Equalizer/BassBoost/Virtualizer, 10 presets, custom presets, persistence, graceful "unsupported") · Visualizer (6 styles, real audio data) · Local lyrics (manual file, lyrics folder, LRC sync) · Video player (Media3 `PlayerView`, subtitles, audio/subtitle track selection, speed, aspect ratio, rotation, brightness/volume gestures, resume, PiP) · File browser (SAF, SD cards) · Sleep timer (minutes with fade, end of song/playlist) · Library insights (stats, duplicates, missing media) · Settings/personalization · splash, About, privacy.

## Deviations from the master prompt
- **Crossfade is not implemented** (ExoPlayer has no built-in crossfade). Gapless playback is provided by ExoPlayer for supported formats.
- **No Coil.** Artwork uses a small in-memory-cached loader (`core/ArtworkLoader.kt`) over MediaStore album-art URIs; some Android 10+ devices may not serve art through that URI, in which case a placeholder icon shows. Embedded-art extraction is a follow-up.
- **No Song/Album/Artist/UserPreference Room entities.** MediaStore stays the source of truth (avoids stale copies); playlists/favourites/history store MediaStore ids. Preferences use DataStore.
- **Embedded lyrics tags are not read**; lyrics come from loaded files or a chosen lyrics folder.
- **WMA / decoder extensions are not added** (licensing/dependency decision pending). MKV/H.265/VP9 depend on device decoders.
- **Visualizer needs RECORD_AUDIO** (an Android requirement); it may not work on some devices/outputs.
- Playlist reorder uses up/down buttons, not drag-and-drop. Queue likewise.
- "Player appearance" customization is limited to accent colour and circular artwork.
- Gradle wrapper not included; generate in Android Studio.
- Original app icon is a vector adaptive icon (no legacy PNG for API < 26 launchers; add via Image Asset Studio).

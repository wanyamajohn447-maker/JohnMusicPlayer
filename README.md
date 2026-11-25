# JOHN MUSIC PLAYER
**Your Music. Your Control.** — A Wanyama John Technologies product (developer: Wanyama John).

An offline-first, privacy-first Android music and video player. Kotlin, Jetpack Compose (Material 3), Media3 ExoPlayer + MediaSessionService, Room, DataStore, Hilt, Clean Architecture + MVVM. No internet permission, no accounts, no ads, no analytics.

> Status: all 13 phases are **written**, but the project has **not yet been compiled or run on a device**.
> See `docs/STATUS.md` for the honest feature/verification matrix and known limitations.

## Open and run
1. Android Studio (current stable) → *Open* the `JohnMusicPlayer` folder.
2. `gradle/libs.versions.toml` versions were chosen from memory: check them against current stable releases and fix any sync errors.
3. If Android Studio asks, create the Gradle wrapper (*File → Sync*, or run `gradle wrapper`).
4. Run on a real device (Android 7.0+/API 24+). Emulators have limited audio-effect and Bluetooth support.

## Structure (`app/src/main/java/com/wanyamajohn/musicplayer`)
| Package | Role |
|---|---|
| `ui/*` | Compose screens + ViewModels (Home, Library, Now Playing, Playlists, Equalizer, Visualizer, Video, Folders, Settings) |
| `domain/*` | Models and repository interfaces |
| `data/*` | MediaStore scanners, Room database, DataStore preferences, repositories |
| `playback/*` | `PlaybackService` (MediaSessionService), `PlayerConnection`, audio effects, visualizer, sleep timer, output monitor |
| `core/*` | LRC parser, M3U, library analyzer, artwork loader, utilities |
| `di/*` | Hilt modules |

Docs: `docs/PRIVACY_POLICY.md`, `docs/TESTING.md`, `docs/RELEASE.md`, `docs/STATUS.md`.

Not affiliated with VLC or VideoLAN. All branding assets are original.

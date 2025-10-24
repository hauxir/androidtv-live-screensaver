# Claude Code Context

## Project Overview

Android TV Live Screensaver - A screensaver app that plays live video streams on Android TV devices. Supports YouTube live streams and direct HLS streams.

**Important Context**: This project was "vibe-coded" - the original author had no prior Kotlin experience. Code style may reflect this.

## Tech Stack

- **Language**: Kotlin
- **Min SDK**: Android 5.0 (API 21)
- **Target SDK**: API 35
- **Build System**: Gradle with Kotlin DSL
- **Video Player**: Media3 ExoPlayer
- **Stream Extraction**: NewPipe Extractor (for YouTube)
- **HTTP Client**: OkHttp
- **Coroutines**: kotlinx-coroutines-android
- **UI Framework**: AndroidX Leanback (TV-optimized)

## Key Components

### LiveScreensaverService.kt
Main screensaver service extending `DreamService`. Key responsibilities:
- Video playback using ExoPlayer (`app/src/main/java/com/livescreensaver/tv/LiveScreensaverService.kt`)
- Stream extraction via inner `StreamExtractor` class
- Retry logic with exponential backoff (max 3 retries)
- Stall detection and recovery (10s timeout)
- URL caching (5-minute default expiration)
- StrictMode enabled for debug builds

**Important classes**:
- `LiveScreensaverService`: Main service (lines 35-311)
- `StreamExtractor`: YouTube URL extraction and caching (lines 314-497)
- `DownloaderImpl`: OkHttp-based downloader for NewPipe (lines 501-552)

### SettingsActivity.kt
Settings UI using Leanback preferences:
- Single preference for stream URL configuration
- Multi-line text input for URLs
- TV-optimized interface

## Architecture Patterns

### Stream Extraction Flow
1. Check if URL needs extraction (YouTube vs direct HLS)
2. Try cached URL first (if not force refresh)
3. Use file-based locking to prevent concurrent extractions
4. Extract via NewPipe, prefer HLS, fallback to best video stream
5. Cache extracted URL with expiration detection

### Error Handling
- Exponential backoff for retries: 1s, 2s, 4s
- Cache clearing on failures
- Stall detection via playback state monitoring
- Lock file timeout (15s) to prevent deadlocks

### Thread Safety
- NewPipe initialization is thread-safe via synchronized block and volatile flag
- Coroutines used for async operations (ServiceScope with SupervisorJob)
- Handler for main thread operations

## Build & Development

### Commands
```bash
make build          # Build release APK
make clean          # Clean build directory
./gradlew build     # Standard Gradle build
```

### Key Build Configuration
- Java 17 toolchain
- Core library desugaring enabled (for NIO on older Android)
- ProGuard/R8 disabled (minifyEnabled = false)
- BuildConfig generation enabled (for StrictMode checks)

## Important Notes

### YouTube Limitations
- **Only live streams are supported** - regular videos will not work
- URL extraction happens via NewPipe Extractor
- URLs expire and are re-extracted with caching

### StrictMode
- Enabled only for debug builds
- Helps catch threading issues and network operations on main thread
- See lines 70-85 in LiveScreensaverService.kt

### Caching Strategy
- Cached URLs stored in app cache directory
- File-based locking prevents concurrent extractions
- Expiration time extracted from YouTube URLs when available
- Fallback to 5-minute cache duration

## Testing Notes

- Tested on a single Android TV device (author's note)
- No automated tests in the codebase
- Use `run-tv-emulator.sh` for emulator testing (untracked file)

## Common Issues

1. **Black screen**: Stream loading delay or invalid URL
2. **YouTube not working**: Verify it's a live stream, not a regular video
3. **Playback failures**: Check logs for extraction errors or network issues
4. **Stalls**: Auto-recovery triggers after 10s stall

## File Structure
```
app/src/main/
  java/com/livescreensaver/tv/
    LiveScreensaverService.kt  # Main service + extraction logic
    SettingsActivity.kt        # Settings UI
  res/
    xml/dream.xml              # Screensaver configuration
    values/strings.xml         # App strings
```

## When Making Changes

- Consider the TV-specific UI constraints (Leanback design)
- Test on actual hardware if possible (emulator may behave differently)
- Be mindful of thread safety in coroutines and handler usage
- YouTube extraction is fragile - changes to NewPipe may break functionality
- Remember cache invalidation when changing extraction logic

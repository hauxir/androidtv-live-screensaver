# Android TV Live Screensaver

An Android TV screensaver that plays live video streams. Supports YouTube videos and direct HLS streams.

## Why?
   
Turn any live stream into your screensaver. Some examples:

### [Namib Desert Wildlife](https://www.youtube.com/watch?v=ydYDqZQpim8)
<img width="640" height="360" alt="Image" src="https://github.com/user-attachments/assets/19b39408-8d67-4699-87c9-bb218198190d" />

### [Times Square](https://www.youtube.com/watch?v=rnXIjl_Rzy4)
<img width="640" height="360" alt="Image" src="https://github.com/user-attachments/assets/5db52a77-24a2-4bd1-9698-d3f2258b4890" />

### [The News](https://www.youtube.com/watch?v=iipR5yUp36o)

<img width="640" height="360" alt="Image" src="https://github.com/user-attachments/assets/1d528a72-3d1b-4151-8e9c-347cdfe8d94c" />

## Requirements

- Android TV device running Android 5.0 (API 21) or higher
- Internet connection

**Disclaimer**: This project was entirely vibe-coded. I've never written Kotlin before in my life.

**Note**: This was tested exclusively on a single Android TV device. Your mileage may vary on other devices.

## Installation

### Build and Install

Build and install:
```bash
make build
adb install app/build/outputs/apk/release/app-release.apk
```

Or step by step:
```bash
make build  # Build the APK
```

Other commands:
```bash
make clean  # Remove build directory
```

## Usage

1. Open the **Live Screensaver** app on your Android TV
2. Enter a video URL:
   - YouTube: `https://www.youtube.com/watch?v=VIDEO_ID` **(live streams only)**
   - HLS stream: `https://example.com/stream.m3u8`
3. Go to **Settings** → **Display & Sound** → **Screen saver**
4. Select **Live Screensaver**

**Note**: Only live YouTube videos are supported. Regular (non-live) YouTube videos will not work.

## Troubleshooting

**YouTube videos don't play**:
- Make sure you're using a **live** YouTube stream - regular videos are not supported
- Check that the stream is publicly accessible

**Black screen**: Wait a few seconds for loading, or try a different URL

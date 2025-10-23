# Android TV Live Screensaver

An Android TV screensaver that plays live video streams. Supports YouTube videos and direct HLS streams.

> **Also available:** [macOS Live Screensaver](https://github.com/hauxir/macos-live-screensaver)

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

Download the latest APK from the [releases page](https://github.com/hauxir/androidtv-live-screensaver/releases) and install it on your Android TV device.

### Building from Source (Optional)

If you want to build the APK yourself:
```bash
make build  # Build the APK
adb install app/build/outputs/apk/release/app-release.apk
```

Other commands:
```bash
make clean  # Remove build directory
```

## Usage

1. Install the APK on your Android TV device (see Installation section above)
2. Go to **Settings** → **Device Preferences** → **Screen saver** → **Screen saver** → **Live Screensaver** → **Stream URL**
3. Enter a video URL:
   - YouTube: `https://www.youtube.com/watch?v=VIDEO_ID` **(live streams only)**
   - HLS stream: `https://example.com/stream.m3u8`

**Note**: Only live YouTube videos are supported. Regular (non-live) YouTube videos will not work.

## Troubleshooting

**YouTube videos don't play**:
- Make sure you're using a **live** YouTube stream - regular videos are not supported
- Check that the stream is publicly accessible

**Black screen**: Wait a few seconds for loading, or try a different URL

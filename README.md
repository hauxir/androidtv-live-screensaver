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

### Google TV: app doesn't appear as a screensaver option

On devices running **Google TV** (as opposed to the older "Android TV" UI) — e.g. Chromecast with Google TV, and newer Sony Bravia sets — **Settings → Device Preferences → Screen saver** only lists a curated set of screensavers (Google's own + whatever the OEM has pre-approved). Sideloaded apps that implement a `DreamService`, including this one, generally don't show up there, even though the app and the underlying Android daydream mechanism both work correctly.

The workaround is to select and enable the screensaver directly via `adb`, bypassing the picker UI entirely:

```bash
adb shell settings put secure screensaver_enabled 1
adb shell settings put secure screensaver_components com.livescreensaver.tv/.LiveScreensaverService
adb shell settings put secure screensaver_activate_on_sleep 1
```

Verify it took:
```bash
adb shell settings get secure screensaver_components
# should print: com.livescreensaver.tv/.LiveScreensaverService
```

To control how long the TV must be idle before the screensaver activates (this is a system-wide setting, independent of the app):
```bash
adb shell settings put system screen_off_timeout 60000   # 1 minute, in ms
```

**Note**: Since these are set outside the Settings UI, a factory reset or major system update may silently revert them (Sony's Settings app has no notion that a sideloaded screensaver is "selected"). If the screensaver stops appearing after such an event, just re-run the commands above.

#### Enabling ADB on the TV

If you don't already have `adb` access to the TV:

1. Enable Developer Options: **Settings → About → (click the build number ~7 times)**.
2. In Developer Options, enable **USB debugging**.
3. Try **Network debugging** too — on some Google TV builds this toggle is flaky and does nothing visible even after a reboot.

If network debugging won't cooperate, most Android TV/Google TV devices only support **USB debugging in device mode** through a specific port (often labeled "Service" on the back, distinct from any USB-A ports meant for flash drives/keyboards, which are host-only and won't work here). If your set doesn't have a compatible port, the reliable fallback is:

1. Connect the TV to a computer via USB (a working device-mode port), and accept the debugging authorization prompt that appears on the TV.
2. From that computer, run `adb devices` to confirm it's detected, then:
   ```bash
   adb tcpip 5555
   ```
3. Disconnect the USB cable and connect over the network instead, from any machine on the same LAN:
   ```bash
   adb connect <tv-ip>:5555
   ```

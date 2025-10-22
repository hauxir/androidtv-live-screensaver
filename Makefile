.PHONY: build clean install uninstall

build:
	./gradlew assembleRelease

clean:
	./gradlew clean

install: build
	adb install -r app/build/outputs/apk/release/app-release.apk

uninstall:
	adb uninstall com.livescreensaver.tv

.PHONY: build clean install uninstall

build:
	./gradlew assembleDebug

clean:
	./gradlew clean

install: build
	adb install -r app/build/outputs/apk/debug/app-debug.apk

uninstall:
	adb uninstall com.livescreensaver.tv

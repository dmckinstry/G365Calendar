.PHONY: build build-android test test-android test-integration test-integration-android dev dev-android clean clean-android

ANDROID_DIR ?= .
GRADLE ?= ./gradlew

build: build-android

build-android:
	cd $(ANDROID_DIR) && $(GRADLE) assembleDebug

clean: clean-android

clean-android:
	cd $(ANDROID_DIR) && $(GRADLE) clean

test: test-android

test-android:
	cd $(ANDROID_DIR) && $(GRADLE) test

test-integration: test-integration-android

test-integration-android:
	cd $(ANDROID_DIR) && $(GRADLE) integrationTest

dev: dev-android

dev-android: build
	@if command -v adb >/dev/null 2>&1 && adb devices | awk 'NR > 1 && $$2 == "device" { found=1 } END { exit found ? 0 : 1 }'; then \
		cd $(ANDROID_DIR) && $(GRADLE) installDebug; \
	else \
		echo "No Android device/emulator detected; attempting to start Pixel_8_API_36 emulator."; \
		if command -v emulator >/dev/null 2>&1; then \
			emulator -avd Pixel_8_API_36 & \
			for i in $$(seq 1 30); do \
				if adb devices | awk 'NR > 1 && $$2 == "device" { found=1 } END { exit found ? 0 : 1 }'; then \
					break; \
				fi; \
				sleep 2; \
			done; \
		fi; \
		cd $(ANDROID_DIR) && $(GRADLE) installDebug; \
	fi

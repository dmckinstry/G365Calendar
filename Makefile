.PHONY: build build-android build-garmin test test-android test-garmin dev dev-android dev-garmin clean clean-android clean-garmin clean-garming

ANDROID_DIR ?= android
GARMIN_DIR ?= garmin

build-android:
	$(MAKE) -C $(ANDROID_DIR) build

build-garmin:
	$(MAKE) -C $(GARMIN_DIR) build

clean-android:
	$(MAKE) -C $(ANDROID_DIR) clean

clean-garmin:
	$(MAKE) -C $(GARMIN_DIR) clean

clean-garming: clean-garmin

clean: clean-android clean-garmin

build: build-android build-garmin

test-android:
	$(MAKE) -C $(ANDROID_DIR) test

test-garmin:
	$(MAKE) -C $(GARMIN_DIR) test

test: test-android test-garmin

dev-android:
	$(MAKE) -C $(ANDROID_DIR) dev

dev-garmin:
	$(MAKE) -C $(GARMIN_DIR) dev

dev: dev-android dev-garmin

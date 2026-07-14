.PHONY: build build-garmin test test-garmin dev dev-garmin

GARMIN_DIR ?= .
GARMIN_DEVICE ?= venu3
GARMIN_DEV_KEY ?= ~/Desktop/developer_key

build: build-garmin

build-garmin:
	monkeyc -f $(GARMIN_DIR)/monkey.jungle -d $(GARMIN_DEVICE) -o $(GARMIN_DIR)/bin/G365Calendar.prg -y $(GARMIN_DEV_KEY)

test: test-garmin

test-garmin:
	monkeyc -f $(GARMIN_DIR)/monkey.jungle -d $(GARMIN_DEVICE) -t -o $(GARMIN_DIR)/bin/G365Calendar-test.prg -y $(GARMIN_DEV_KEY)
	connectiq && sleep 1 && monkeydo $(GARMIN_DIR)/bin/G365Calendar-test.prg $(GARMIN_DEVICE) -t

dev: dev-garmin

dev-garmin: build-garmin
	connectiq && sleep 1 && monkeydo $(GARMIN_DIR)/bin/G365Calendar.prg $(GARMIN_DEVICE)

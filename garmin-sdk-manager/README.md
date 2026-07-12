# Source

All work in this folder is based on [the work](https://github.com/jonesdavidj/garmin-sdk-mgr) from [David J. Jones](https://github.com/jonesdavidj).  No changes have been made to his scripts although I'm not necessarily using directly as intended.

*Original README follows*

---

# Connect IQ SDK Sidecar (Linux)

This repo sets up a headless Garmin Connect IQ SDK Manager environment in a Docker container, designed for Linux and CI/CD systems like Portainer.

It allows you to:

* Install the Connect IQ SDK Manager CLI
* Download the Garmin SDK (e.g. 8.1.1)
* Fetch official `.json` device definitions (e.g. `forerunner965.json`)

## 🧱 Project Structure

```
.
├── Dockerfile              # Dockerfile to build the container
├── sdk-init.sh             # Interactive SDK setup script
├── install-cli.sh          # CLI installer for Garmin SDK Manager CLI
├── docker-compose.yml      # Stack config for Portainer or local use
├── .env                    # (Not committed) Garmin login credentials
└── garmin-sdk/             # SDK + devices stored here (mounted volume)
```

## 🔐 Environment Variables

Create a `.env` file (add to `.gitignore`) in the root:

```env
GARMIN_USERNAME=your.email@example.com
GARMIN_PASSWORD=yourGarminPassword
```

> **Do not commit this file.** It's used by `sdk-init.sh`.

## 🚀 Getting Started (Portainer or Docker CLI)

1. **Clone this repo**

2. **Create your `.env` file** with your Garmin credentials

3. **Deploy the stack** in Portainer or run manually:

```bash
docker-compose up -d --build
```

4. **Open a terminal into the container** (via Portainer or CLI):

```bash
docker-compose exec sdkmgr bash
```

5. **Install the SDK Manager CLI:**

```bash
./usr/local/bin/install-cli.sh
```

6. **Run the setup script (SDK + devices):**

```bash
./usr/local/bin/sdk-init.sh
```

7. **Your downloaded SDK will be available at:**

```
./garmin-sdk/connectiq-sdk-<version>/
```

You can mount this into other containers that compile and build your Garmin watch faces.

## 🛡 Notes

* The Garmin license must be accepted manually during the `sdk-init.sh` step.
* This repo does **not** contain the SDK or `.json` device files — it downloads them officially.
* Scripts are made executable during build for direct usage inside the container.

## 🧼 Cleanup

To rebuild or reset:

```bash
rm -rf garmin-sdk/
docker-compose down --volumes
```

---

Built with ❤️ to make Garmin CIQ development easier in Linux environments.

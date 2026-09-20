# Release Guide

Foodario uses **SemVer** (`MAJOR.MINOR.PATCH`) with annotated git tags `vX.Y.Z`. Each release is built and signed by a GitHub Actions workflow (`.github/workflows/release.yml`) and published as a GitHub Release with the signed APK attached. The version comes from the tag, so no version-bump commit is required (and `main` stays protected).

---

## Table of contents

1. [Signing setup](#signing-setup)
2. [Releasing from GitHub](#releasing-from-github)
3. [Local release build](#local-release-build)
4. [Installing on an emulator](#installing-on-an-emulator)
5. [Troubleshooting](#troubleshooting)

---

## Signing setup

Release APKs are signed with a personal keystore, never committed to the repo.

### Local builds

1. Generate a release keystore at the repo root. Replace the placeholders with your own secure values:

   ```bash
   keytool -genkeypair \
     -v \
     -keystore foodario-release.jks \
     -alias foodario \
     -keyalg RSA \
     -keysize 2048 \
     -validity 10000 \
     -storepass <STORE_PASSWORD> \
     -keypass <KEY_PASSWORD> \
     -dname "CN=Foodario, OU=Development, O=Foodario, L=Local, ST=Local, C=US"
   ```

2. Create `keystore.properties` at the repo root (git-ignored by default):

   ```properties
   storeFile=foodario-release.jks
   storePassword=<STORE_PASSWORD>
   keyAlias=foodario
   keyPassword=<KEY_PASSWORD>
   ```

   `storeFile` is resolved relative to the repo root, so place the keystore at the repo root and use the path above.

### CI builds

The following GitHub Actions secrets must be set on the repository:

- `KEYSTORE_BASE64` — encode your keystore with:

  ```bash
  base64 < foodario-release.jks | pbcopy
  ```

  (or `base64 foodario-release.jks` and copy the output).

- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

> **Important:** never commit the `.jks` file, `keystore.properties`, or any password to the repository. Both `*.jks` and `keystore.properties` are already ignored in `.gitignore`.

---

## Releasing from GitHub

Both paths produce the same result: a tagged GitHub Release with the signed APK.

### A. Push a tag

```shell
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0
```

The workflow builds, signs and publishes the APK to the release `v1.0.0`.

### B. Create a release from the web UI

Go to GitHub → *Releases* → *Draft a new release*, set the tag to `v1.0.0`, and publish. The workflow builds, signs and attaches the APK to that release.

---

## Local release build

Build the signed release APK locally (requires `keystore.properties`, see [Signing setup](#signing-setup)):

```shell
./gradlew :androidApp:assembleRelease
```

The signed APK is produced at:

```
androidApp/build/outputs/apk/release/androidApp-release.apk
```

### Verify the APK signature

Modern APKs use APK Signature Scheme v2, so use `apksigner` instead of `jarsigner`:

```bash
$ANDROID_HOME/build-tools/<version>/apksigner verify --verbose androidApp/build/outputs/apk/release/androidApp-release.apk
```

Expected output includes:

```
Verifies
Verified using v2 scheme (APK Signature Scheme v2): true
Number of signers: 1
```

If you do not have a keystore configured, Gradle will still build the APK but it will be unsigned (`androidApp-release-unsigned.apk`) and cannot be installed on a device or emulator.

---

## Installing on an emulator

### 1. Start an emulator

List your available AVDs and launch one:

```shell
$ANDROID_HOME/emulator/emulator -list-avds
$ANDROID_HOME/emulator/emulator -avd <AVD_NAME> &
```

Or start it from Android Studio's Device Manager.

### 2. Verify the emulator is running

```shell
$ANDROID_HOME/platform-tools/adb devices
```

You should see something like:

```
List of devices attached
emulator-5554   device
```

### 3. Install the APK

```shell
$ANDROID_HOME/platform-tools/adb install androidApp/build/outputs/apk/release/androidApp-release.apk
```

### 4. Launch the app

```shell
$ANDROID_HOME/platform-tools/adb shell monkey -p com.foodario -c android.intent.category.LAUNCHER 1
```

### Installing on your device

Download the APK from the release page and install it, or use ADB:

```shell
$ANDROID_HOME/platform-tools/adb install androidApp/build/outputs/apk/release/androidApp-release.apk
```

---

## Troubleshooting

### `INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package com.foodario signatures do not match`

A previous build is signed with a different key (for example, the debug keystore). Uninstall it first and retry:

```shell
$ANDROID_HOME/platform-tools/adb uninstall com.foodario
$ANDROID_HOME/platform-tools/adb install androidApp/build/outputs/apk/release/androidApp-release.apk
```

### Debug vs release signing keys

Debug (`assembleDebug`) and release builds use different signing keys. Alternating between them on the same emulator/device requires `adb uninstall com.foodario` before installing the other flavor.

### Keystore not found during build

Make sure:

1. `foodario-release.jks` is at the repo root.
2. `keystore.properties` exists at the repo root with the correct values.
3. `storeFile=foodario-release.jks` matches the actual filename.

### Git wants to track the keystore

If `git status` shows `foodario-release.jks` or `keystore.properties` as untracked, do not add them. They should already be ignored by `.gitignore`:

```gitignore
*.jks
*.keystore
keystore.properties
```

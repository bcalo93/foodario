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

The release workflow (`.github/workflows/release.yml`) reads four **repository secrets**. These are not committed to the repo; they are configured in the GitHub web interface and injected into the workflow at runtime.

#### Required secrets

| Secret | Description | Example value |
|--------|-------------|---------------|
| `KEYSTORE_BASE64` | Base64-encoded content of your `foodario-release.jks` file. | `UEsDBBQACAAI...` (long string) |
| `KEYSTORE_PASSWORD` | The password used to open the keystore (`-storepass`). | `<STORE_PASSWORD>` |
| `KEY_ALIAS` | The alias you chose when generating the key (`-alias`). | `foodario` |
| `KEY_PASSWORD` | The password for the key entry (`-keypass`). | `<KEY_PASSWORD>` |

#### How to generate `KEYSTORE_BASE64`

From the repo root, run:

```bash
base64 < foodario-release.jks | pbcopy
```

> **Note for macOS:** use `base64 < foodario-release.jks` instead of `base64 foodario-release.jks`. The BSD version of `base64` on macOS does not always accept the file as a positional argument.

If you do not have `pbcopy`, run:

```bash
base64 < foodario-release.jks
```

and copy the entire output.

#### How to add the secrets in GitHub

1. Open the repository on GitHub.
2. Go to **Settings** → **Secrets and variables** → **Actions**.
3. Click **New repository secret**.
4. Add each secret one by one:
   - Name: `KEYSTORE_BASE64` → Value: the base64 string from the previous step.
   - Name: `KEYSTORE_PASSWORD` → Value: your keystore password.
   - Name: `KEY_ALIAS` → Value: your key alias.
   - Name: `KEY_PASSWORD` → Value: your key password.
5. Make sure the names match exactly; the workflow references them as `secrets.KEYSTORE_BASE64`, `secrets.KEYSTORE_PASSWORD`, `secrets.KEY_ALIAS`, and `secrets.KEY_PASSWORD`.

#### How to add the secrets with GitHub CLI

You can also use the [`gh`](https://cli.github.com/) command-line tool. Make sure you are authenticated (`gh auth status`) and inside the repository directory.

##### Generate the base64 keystore file

```bash
base64 < foodario-release.jks > foodario-release.jks.b64
```

##### Set the secrets

```bash
gh secret set KEYSTORE_BASE64 --body-file foodario-release.jks.b64
gh secret set KEYSTORE_PASSWORD --body "<STORE_PASSWORD>"
gh secret set KEY_ALIAS --body "foodario"
gh secret set KEY_PASSWORD --body "<KEY_PASSWORD>"
```

> **Tip:** to avoid leaving passwords in your shell history, omit `--body` and `gh` will prompt you interactively:
>
> ```bash
> gh secret set KEYSTORE_PASSWORD
> gh secret set KEY_PASSWORD
> ```

If you prefer not to create a temporary file for the base64 value, you can pipe it directly:

```bash
base64 < foodario-release.jks | gh secret set KEYSTORE_BASE64
```

##### Verify the secrets

```bash
gh secret list
```

You should see `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` in the list.

##### Clean up the temporary file

The `.b64` file is only needed while setting the secret. Delete it afterwards:

```bash
rm foodario-release.jks.b64
```

> **Important:** never commit the `.jks` file, `keystore.properties`, `.b64` temporary files, or any password to the repository. The keystore-related files are already ignored in `.gitignore`.

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

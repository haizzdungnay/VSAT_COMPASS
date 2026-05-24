# Android Release Runbook

## Scope

- APK release track is **independent** from the backend runtime release track.
- Backend tags use `v0.10.x`.
- Android APK tags use `android/vX.Y.Z`.
- First APK target is `android/v0.1.0`.
- APK-1 only prepares release config; APK-2 performs release build verification.

---

## Prerequisites

- JDK 17+
- Android SDK 36
- Gradle wrapper from repo (`./gradlew` or `gradlew.bat`)
- Release keystore stored **outside** the repository

---

## One-Time Keystore Generation

Run once to generate the release keystore. Store it **outside** the repo.

```
keytool -genkey -v -keystore vsat-release.keystore -alias vsat -keyalg RSA -keysize 2048 -validity 10000
```

> **WARNING:**
> - Store the keystore file outside the repository directory.
> - Do **not** commit the keystore to version control.
> - Back up the keystore securely (e.g., password manager, encrypted storage).
> - **Losing the keystore** means future APK updates signed with the same key are not possible.
>   If the keystore is lost, a new key must be generated and all users must reinstall the APK.

---

## keystore.properties Setup

1. Copy `keystore.properties.example` to `keystore.properties` at the repo root:

   ```
   cp keystore.properties.example keystore.properties
   ```

2. Fill in the values:

   ```properties
   storeFile=<path-to-keystore-outside-repo>
   storePassword=<your-keystore-password>
   keyAlias=vsat
   keyPassword=<your-key-password>
   ```

3. `keystore.properties` is listed in `.gitignore` and will **not** be committed.

---

## Build Command

From the repository root:

```bash
cd <repo-root>
./gradlew assembleRelease
```

**Windows alternative:**

```bat
gradlew.bat assembleRelease
```

If `keystore.properties` is missing, the release build type will not have a signing config wired.
Gradle sync and debug builds will still work without `keystore.properties`.

---

## Output APK

```
app/build/outputs/apk/release/app-release.apk
```

---

## Versioning Convention

| Field | Convention |
|-------|------------|
| `versionCode` | Increments by 1 for each Android release |
| `versionName` | Follows semver without prefix, e.g. `0.1.0` |
| Git tag | `android/vX.Y.Z`, e.g. `android/v0.1.0` |
| Backend tags | `v0.10.x` — remain separate from Android tags |

Current values: `versionCode=1`, `versionName="0.1.0"`.

---

## Sideload Distribution

Transfer the APK to a device and install:

**Via ADB:**

```
adb install -r app/build/outputs/apk/release/app-release.apk
```

**Manual transfer:**

1. Transfer the APK to the device (USB, cloud storage, etc.).
2. On the device, enable **Install from unknown sources** in Settings.
3. Open the APK file to install.

---

## Future Play Store Path

Google Play Console internal testing track setup is **deferred** until the app reaches a
feature-complete stage. This is not required for the first sideload APK distribution.

See the project roadmap in `task.md` for the APK Release Track status.

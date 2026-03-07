# Publishing Releases & Packages — NextGen Media Player

Step-by-step guide for publishing GitHub Releases (with APK) and Packages.

---

## Prerequisites

1. **GitHub CLI** installed:
   ```powershell
   winget install GitHub.cli
   ```

2. **Authenticate** with GitHub:
   ```powershell
   gh auth login
   ```
   - Select **GitHub.com** → **HTTPS** → **Login with a web browser**
   - Follow the browser prompt to complete authentication

3. **Verify** authentication:
   ```powershell
   gh auth status
   ```

---

## Part 1: Creating a GitHub Release (with APK)

### Step 1 — Build the APK

```powershell
cd C:\Games\MediaPlayer

# Debug APK (no signing required)
.\gradlew.bat assembleDebug

# Release APK (unsigned — fine for GitHub distribution)
.\gradlew.bat assembleRelease
```

**APK locations:**
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release-unsigned.apk`

### Step 2 — Commit & Push Your Code

```powershell
git add -A
git commit -m "v1.3.0: Feature description"
git push origin main
```

### Step 3 — Create the Release

**Option A: GitHub CLI (recommended)**

```powershell
gh release create v1.3.0 `
  "app\build\outputs\apk\release\app-release-unsigned.apk#NextGen-Player-v1.3.0.apk" `
  --repo praveen93h/NetGen_Player `
  --title "v1.3.0" `
  --notes "Release notes here"
```

**Breaking down the command:**
| Part | Meaning |
|------|---------|
| `v1.3.0` | Git tag name (created automatically) |
| `"path\to\file.apk#Display Name.apk"` | Attach file with custom display name |
| `--repo owner/repo` | Target repository |
| `--title "v1.3.0"` | Release title |
| `--notes "..."` | Release description |

**Option B: GitHub Website**

1. Go to https://github.com/praveen93h/NetGen_Player
2. Click **Releases** → **Create a new release**
3. Click **Choose a tag** → type `v1.3.0` → **Create new tag**
4. Set **Release title**: `v1.3.0`
5. Write release notes in the description box
6. Drag & drop the APK file into the **Attach binaries** area
7. Click **Publish release**

### Step 4 — Verify

```powershell
# List all releases
gh release list --repo praveen93h/NetGen_Player

# View a specific release
gh release view v1.3.0 --repo praveen93h/NetGen_Player
```

### Useful Release Commands

```powershell
# Delete a release
gh release delete v1.3.0 --repo praveen93h/NetGen_Player --yes

# Upload additional files to an existing release
gh release upload v1.3.0 "path\to\file.apk" --repo praveen93h/NetGen_Player

# Create release with auto-generated notes from commits
gh release create v1.3.0 --generate-notes --repo praveen93h/NetGen_Player

# Mark as pre-release
gh release create v1.3.0-beta --prerelease --repo praveen93h/NetGen_Player

# Mark as draft (not visible to public until published)
gh release create v1.3.0 --draft --repo praveen93h/NetGen_Player
```

---

## Part 2: Signing the APK (Optional but Recommended)

Unsigned APKs work for GitHub distribution but show warnings on install. To sign:

### Step 1 — Generate a Keystore

```powershell
keytool -genkey -v -keystore nextgen-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias nextgen
```

You'll be prompted for a password and identity info. **Save the keystore and passwords securely.**

### Step 2 — Configure Signing in Gradle

Add to `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../nextgen-release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = "nextgen"
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### Step 3 — Build Signed APK

```powershell
$env:KEYSTORE_PASSWORD = "your_password"
$env:KEY_PASSWORD = "your_key_password"
.\gradlew.bat assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk` (signed)

> **Important:** Never commit your keystore (`.jks`) or passwords to git. The `.gitignore` already excludes `*.jks` and `*.keystore`.

---

## Part 3: GitHub Packages (Maven/Gradle)

GitHub Packages lets you publish your Android library modules as Maven artifacts. This is useful if you want to share `core-player` or `feature-subtitle` as reusable libraries.

### Step 1 — Create a Personal Access Token

1. Go to https://github.com/settings/tokens
2. Click **Generate new token (classic)**
3. Select scopes: `write:packages`, `read:packages`
4. Copy the token

### Step 2 — Configure Gradle for Publishing

Add to the module's `build.gradle.kts` (e.g., `core-player/build.gradle.kts`):

```kotlin
plugins {
    id("maven-publish")
}

afterEvaluate {
    publishing {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/praveen93h/NetGen_Player")
                credentials {
                    username = System.getenv("GITHUB_USER") ?: ""
                    password = System.getenv("GITHUB_TOKEN") ?: ""
                }
            }
        }
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.nextgen.player"
                artifactId = "core-player"
                version = "1.3.0"
            }
        }
    }
}
```

### Step 3 — Publish

```powershell
$env:GITHUB_USER = "praveen93h"
$env:GITHUB_TOKEN = "ghp_your_token_here"
.\gradlew.bat :core-player:publish
```

### Step 4 — Verify

Go to https://github.com/praveen93h/NetGen_Player → **Packages** tab

> **Note:** GitHub Packages is mainly for library distribution. For an app like NextGen Player, **Releases with APK attachments** is the standard approach.

---

## Quick Reference

| Task | Command |
|------|---------|
| Build debug APK | `.\gradlew.bat assembleDebug` |
| Build release APK | `.\gradlew.bat assembleRelease` |
| Create release | `gh release create v1.3.0 "file.apk" --title "v1.3.0" --notes "..."` |
| List releases | `gh release list` |
| Delete release | `gh release delete v1.3.0 --yes` |
| Upload file to release | `gh release upload v1.3.0 "file.apk"` |
| Publish package | `.\gradlew.bat :module:publish` |
| Check auth | `gh auth status` |

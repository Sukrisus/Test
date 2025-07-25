# GitHub Workflows for Android Build

This repository contains two GitHub workflows for automated Android APK builds:

## 1. Android CI (`android-build.yml`)

**Triggers:**
- Push to `main` or `master` branch
- Pull requests to `main` or `master` branch
- Manual workflow dispatch

**Features:**
- Builds debug APK automatically
- Uploads APK as build artifact
- Creates automatic releases for main branch pushes
- Caches Gradle dependencies for faster builds

## 2. Android Release (`android-release.yml`)

**Triggers:**
- Push of tags matching `v*` pattern (e.g., v1.0, v2.1.3)

**Features:**
- Builds both debug and release APKs
- Creates GitHub releases with APKs attached
- Generates release notes automatically

## Setup Requirements

The workflows are configured to work automatically with GitHub Actions. No additional setup is required as they:

1. Set up JDK 21 automatically
2. Install Android SDK components
3. Accept Android SDK licenses
4. Cache Gradle dependencies
5. Build the APKs

## Usage

### For continuous integration:
- Simply push code to main/master branch or create pull requests
- The CI workflow will automatically build and test your app

### For releases:
1. Create and push a version tag:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
2. The release workflow will automatically create a GitHub release with APKs

## Build Artifacts

- **Debug APK**: Available as workflow artifact and in releases
- **Release APK**: Available in tagged releases (unsigned)

## Android SDK Components

The workflows install the following components:
- Android SDK Platform 35
- Build Tools 35.0.0
- Platform Tools
- NDK 27.1.12297006

These match the versions specified in your `app/build.gradle` file.
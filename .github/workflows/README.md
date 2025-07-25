# GitHub Workflows

This directory contains GitHub Actions workflows for automated building and testing of the MBLoader Android app.

## Workflows

### 1. Build Android APK (`build-android.yml`)
- **Triggers**: Push to main/master/develop branches, pull requests, manual dispatch
- **Purpose**: Builds debug APK on every push and PR
- **Outputs**: 
  - APK artifact uploaded to the workflow run
  - APK attached to releases if triggered by a tag

### 2. Build Release APKs (`release.yml`)
- **Triggers**: Release creation, version tags (v*.*.*), manual dispatch
- **Purpose**: Builds both debug and release APKs for releases
- **Outputs**:
  - Debug and release APK artifacts
  - APKs attached to GitHub releases
  - Automatically generated release notes

### 3. PR Build Check (`pr-check.yml`)
- **Triggers**: Pull requests to main/master/develop branches
- **Purpose**: Quick build verification and lint checks for PRs
- **Outputs**:
  - Build status check on PRs
  - Lint reports (if any issues found)

## How to Use

### For Regular Development
- Simply push to main/master/develop branches
- The build workflow will automatically run and create APK artifacts
- Download artifacts from the Actions tab

### For Releases
1. Create a new release on GitHub, or
2. Push a version tag (e.g., `v1.2.0`)
3. The release workflow will build both debug and release APKs
4. APKs will be automatically attached to the release

### For Pull Requests
- Open a PR against main/master/develop
- The PR check workflow will run automatically
- Verify the build passes before merging

## Requirements
- Android SDK 34 and 35
- Build Tools 34.0.0 and 35.0.0
- NDK 27.1.12297006
- Java 17

All requirements are automatically installed by the workflows.

## APK Outputs
- **app-debug.apk**: Debug build with debugging symbols and logging
- **app-release.apk**: Release build (unsigned) - ready for distribution

## Notes
- The workflows use Gradle caching to speed up subsequent builds
- All Android SDK components are automatically installed
- Release APKs are unsigned - you may want to add signing for production releases
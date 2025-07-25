# GitHub Actions Workflows

This document describes the automated workflows set up for the Origin Client project.

## 🔄 Available Workflows

### 1. CI Workflow (`ci.yml`)
**Triggers:** Push to main/master/develop, PRs, Weekly schedule (Sundays 2 AM UTC), Manual dispatch

**What it does:**
- Runs on multiple Android API levels (26, 35) for compatibility testing
- Executes unit tests and lint checks
- Builds debug APK
- Uploads test reports and lint results as artifacts
- Caches Gradle dependencies for faster builds

**Duration:** ~5-10 minutes

### 2. Android Build Workflow (`android-build.yml`)
**Triggers:** Push to main/master, PRs to main/master

**What it does:**
- Builds debug APK on every push/PR
- Uploads APK as downloadable artifact
- Sets up Android SDK and NDK automatically
- Copies APK to workspace root for easy access

**Duration:** ~3-7 minutes

### 3. Release Workflow (`release.yml`)
**Triggers:** Git tags matching `v*` pattern (e.g., v1.0.0, v2.1.3)

**What it does:**
- Builds both debug and release APKs
- Creates a GitHub release with APK files attached
- Auto-generates release notes with build information
- Tags the release with version information

**Duration:** ~5-10 minutes

## 🚀 How to Use

### Creating a Release
1. Ensure your code is ready for release
2. Create and push a version tag:
   ```bash
   git tag v1.2.0
   git push origin v1.2.0
   ```
3. The release workflow will automatically:
   - Build the APKs
   - Create a GitHub release
   - Attach APK files to the release

### Manual Workflow Trigger
1. Go to the "Actions" tab in your GitHub repository
2. Select the workflow you want to run
3. Click "Run workflow" button
4. Choose the branch and click "Run workflow"

### Downloading Artifacts
1. Go to the "Actions" tab
2. Click on a completed workflow run
3. Scroll down to "Artifacts" section
4. Download the APK files

## 🔧 Workflow Configuration

### Environment Requirements
- **Runner:** Ubuntu Latest
- **Java:** OpenJDK 17 (Temurin distribution)
- **Android SDK:** Automatically installed
- **NDK:** Version 27.1.12297006

### Caching Strategy
- Gradle wrapper and dependencies are cached
- Cache key based on Gradle files for efficient invalidation
- Reduces build time by ~50-70%

### Security
- Uses official GitHub actions from trusted sources
- No secrets required for basic building
- Release workflow uses `GITHUB_TOKEN` for creating releases

## 📊 Build Matrix

The CI workflow uses a build matrix to test compatibility:
- **API Level 26:** Minimum supported Android version
- **API Level 35:** Target Android version

## 🐛 Troubleshooting

### Common Issues

1. **NDK Download Failure**
   - The workflow automatically accepts SDK licenses
   - NDK version is pinned to ensure compatibility

2. **Build Timeout**
   - Workflows have default GitHub Actions timeouts
   - Gradle daemon and caching help speed up builds

3. **Artifact Upload Failure**
   - Check if APK was generated in expected path
   - Verify file size isn't exceeding GitHub limits

### Workflow Status
- ✅ Green: All checks passed
- ❌ Red: Build or tests failed
- 🟡 Yellow: Workflow in progress

## 📈 Performance Tips

1. **Use Gradle Caching:** Already configured in workflows
2. **Parallel Builds:** Gradle runs tasks in parallel where possible
3. **Incremental Builds:** Only changed modules are rebuilt
4. **NDK Caching:** NDK installation is cached between runs

## 🔄 Workflow Updates

To modify workflows:
1. Edit the YAML files in `.github/workflows/`
2. Test changes in a feature branch first
3. Monitor workflow runs after merging changes

For more information, see the [GitHub Actions documentation](https://docs.github.com/en/actions).
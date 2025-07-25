#!/bin/bash

# Origin Client Build Script
# This script automates the build process for the Origin Client

set -e  # Exit on any error

echo "🏗️  Origin Client Build Script"
echo "================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if gradlew exists and make it executable
if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    print_status "Made gradlew executable"
else
    print_error "gradlew not found! Make sure you're in the project root directory."
    exit 1
fi

# Check for ANDROID_HOME
if [ -z "$ANDROID_HOME" ]; then
    print_warning "ANDROID_HOME not set. This might cause build issues."
fi

# Clean build
print_status "Cleaning previous build..."
./gradlew clean

# Run tests
print_status "Running tests..."
if ./gradlew test; then
    print_success "Tests passed!"
else
    print_warning "Some tests failed, but continuing with build..."
fi

# Run lint checks
print_status "Running lint checks..."
if ./gradlew lint; then
    print_success "Lint checks passed!"
else
    print_warning "Lint found issues, but continuing with build..."
fi

# Build debug APK
print_status "Building debug APK..."
./gradlew assembleDebug

# Check if debug APK was created
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    print_success "Debug APK built successfully!"
    
    # Copy to workspace root for easy access
    cp app/build/outputs/apk/debug/app-debug.apk ./app-debug.apk
    print_status "Copied APK to workspace root: ./app-debug.apk"
    
    # Get APK size
    APK_SIZE=$(du -h ./app-debug.apk | cut -f1)
    print_success "APK size: $APK_SIZE"
else
    print_error "Debug APK build failed!"
    exit 1
fi

# Ask if user wants to build release APK
echo ""
read -p "Build release APK as well? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    print_status "Building release APK..."
    ./gradlew assembleRelease
    
    if [ -f "app/build/outputs/apk/release/app-release-unsigned.apk" ]; then
        cp app/build/outputs/apk/release/app-release-unsigned.apk ./app-release.apk
        print_success "Release APK built successfully: ./app-release.apk"
        
        RELEASE_SIZE=$(du -h ./app-release.apk | cut -f1)
        print_success "Release APK size: $RELEASE_SIZE"
    else
        print_error "Release APK build failed!"
    fi
fi

echo ""
print_success "Build process completed!"
echo ""
print_status "Generated files:"
if [ -f "./app-debug.apk" ]; then
    echo "  📱 app-debug.apk"
fi
if [ -f "./app-release.apk" ]; then
    echo "  📱 app-release.apk"
fi

echo ""
print_status "To install on your Android device:"
print_status "  adb install app-debug.apk"
echo ""
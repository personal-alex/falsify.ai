#!/bin/bash

# Production build script for Crawler Manager
# Builds both frontend and backend for production deployment

set -e

echo "🏗️  Building Crawler Manager for production..."

# Clean previous builds
echo "🧹 Cleaning previous builds..."
mvn clean

# Build the application (includes frontend build)
echo "📦 Building application..."
mvn package -DskipTests

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "✅ Production build completed successfully!"
    echo "📦 JAR file: target/quarkus-app/"
    echo "🌐 Frontend assets: src/main/resources/META-INF/resources/"
    echo ""
    echo "To run the production build:"
    echo "java -jar target/quarkus-app/quarkus-run.jar"
else
    echo "❌ Build failed!"
    exit 1
fi
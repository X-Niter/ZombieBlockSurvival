#!/bin/bash

# This script extracts the SQLite native libraries from the SQLite JAR
# and places them in the src/main/resources/native directory

# Constants
SQLITE_VERSION="3.43.0.0"
PROJECT_DIR="."
MAVEN_REPO="${HOME}/.m2/repository"
SQLITE_JAR="${MAVEN_REPO}/org/xerial/sqlite-jdbc/${SQLITE_VERSION}/sqlite-jdbc-${SQLITE_VERSION}.jar"
TARGET_DIR="${PROJECT_DIR}/src/main/resources/native"

# Check if SQLite JAR exists
if [ ! -f "${SQLITE_JAR}" ]; then
  echo "SQLite JAR not found at: ${SQLITE_JAR}"
  echo "Running Maven command to download it..."
  mvn dependency:get -Dartifact=org.xerial:sqlite-jdbc:${SQLITE_VERSION}
  
  if [ ! -f "${SQLITE_JAR}" ]; then
    echo "Failed to download SQLite JAR. Exiting."
    exit 1
  fi
fi

# Make sure the target directory exists
mkdir -p "${TARGET_DIR}"

# Extract native libraries from the SQLite JAR
echo "Extracting native libraries from ${SQLITE_JAR} to ${TARGET_DIR}..."

# Windows libraries
jar xf "${SQLITE_JAR}" org/sqlite/native/Windows/x86/sqlite-jdbc.dll
jar xf "${SQLITE_JAR}" org/sqlite/native/Windows/x86_64/sqlite-jdbc.dll

if [ -f "org/sqlite/native/Windows/x86/sqlite-jdbc.dll" ]; then
  mv "org/sqlite/native/Windows/x86/sqlite-jdbc.dll" "${TARGET_DIR}/sqlite-native-win-x86.dll"
  echo "Extracted Windows x86 library"
fi

if [ -f "org/sqlite/native/Windows/x86_64/sqlite-jdbc.dll" ]; then
  mv "org/sqlite/native/Windows/x86_64/sqlite-jdbc.dll" "${TARGET_DIR}/sqlite-native-win-x64.dll"
  echo "Extracted Windows x64 library"
fi

# macOS libraries
jar xf "${SQLITE_JAR}" org/sqlite/native/Mac/x86_64/libsqlitejdbc.jnilib

if [ -f "org/sqlite/native/Mac/x86_64/libsqlitejdbc.jnilib" ]; then
  mv "org/sqlite/native/Mac/x86_64/libsqlitejdbc.jnilib" "${TARGET_DIR}/libsqlite-native-mac.dylib"
  echo "Extracted macOS library"
fi

# Linux libraries
jar xf "${SQLITE_JAR}" org/sqlite/native/Linux/x86/libsqlitejdbc.so
jar xf "${SQLITE_JAR}" org/sqlite/native/Linux/x86_64/libsqlitejdbc.so
jar xf "${SQLITE_JAR}" org/sqlite/native/Linux/arm/libsqlitejdbc.so
jar xf "${SQLITE_JAR}" org/sqlite/native/Linux/aarch64/libsqlitejdbc.so

if [ -f "org/sqlite/native/Linux/x86/libsqlitejdbc.so" ]; then
  mv "org/sqlite/native/Linux/x86/libsqlitejdbc.so" "${TARGET_DIR}/libsqlite-native-linux-x86.so"
  echo "Extracted Linux x86 library"
fi

if [ -f "org/sqlite/native/Linux/x86_64/libsqlitejdbc.so" ]; then
  mv "org/sqlite/native/Linux/x86_64/libsqlitejdbc.so" "${TARGET_DIR}/libsqlite-native-linux-x64.so"
  echo "Extracted Linux x64 library"
fi

if [ -f "org/sqlite/native/Linux/arm/libsqlitejdbc.so" ]; then
  mv "org/sqlite/native/Linux/arm/libsqlitejdbc.so" "${TARGET_DIR}/libsqlite-native-linux-arm.so"
  echo "Extracted Linux ARM library"
fi

if [ -f "org/sqlite/native/Linux/aarch64/libsqlitejdbc.so" ]; then
  mv "org/sqlite/native/Linux/aarch64/libsqlitejdbc.so" "${TARGET_DIR}/libsqlite-native-linux-aarch64.so"
  echo "Extracted Linux ARM64 library"
fi

# Clean up temporary directories
rm -rf org

echo "Done extracting native libraries!"
echo "The libraries are now in: ${TARGET_DIR}"
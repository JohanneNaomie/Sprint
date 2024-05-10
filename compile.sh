#!/bin/bash

# Set variables for directories
SRC_DIR="src"
LIB_DIR="lib"
OUT_DIR="out"
OUT_JAR="FrontServlet.jar"

# Compile Java classes
echo "Compiling Java classes..."
mkdir -p "$OUT_DIR"
find "$SRC_DIR" -name "*.java" > sources.txt
javac -cp "$LIB_DIR/*" -d "$OUT_DIR" @sources.txt

# Check if compilation was successful
if [ $? -eq 0 ]; then
    echo "Compilation successful."

    # Create JAR file
    echo "Creating JAR file..."
    jar cf "$OUT_JAR" -C "$OUT_DIR" .

    echo "JAR file created: $OUT_JAR"
else
    echo "Compilation failed. Please check errors."
fi

# Clean up
rm sources.txt

# Suppression du répertoire temporaire out
rm -rf "$OUT_DIR"
echo "Répertoire out supprimé."

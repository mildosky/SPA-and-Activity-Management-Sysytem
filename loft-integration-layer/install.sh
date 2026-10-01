#!/bin/bash

echo "=========================================="
echo "  LOFT INTEGRATION LAYER - INSTALLER"
echo "=========================================="
echo ""

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed. Please install Java 17+ first."
    exit 1
fi

echo "Java version:"
java -version

echo ""
echo "Checking Maven..."
if ! command -v mvn &> /dev/null; then
    echo "Maven not found. Using Maven wrapper..."
    MVN_CMD="./mvnw"
else
    MVN_CMD="mvn"
fi

echo ""
echo "Building Loft Integration Layer..."
$MVN_CMD clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "Build failed. Please check the errors above."
    exit 1
fi

echo ""
echo "Build successful! Starting Setup Wizard..."
echo ""

# Run the application (the CommandLineRunner will trigger the wizard)
java -jar target/loft-integration-layer-0.1.0-SNAPSHOT.jar

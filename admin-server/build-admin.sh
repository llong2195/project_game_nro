#!/bin/bash

# Build script cho Standalone Admin Server

echo "=== Building Standalone Admin Server ==="

# Tạo thư mục build
mkdir -p build/classes
mkdir -p build/lib

# Download Gson dependency nếu chưa có
if [ ! -f "build/lib/gson.jar" ]; then
    echo "Downloading Gson library..."
    curl -L -o build/lib/gson.jar "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar"
fi

# Compile Java source
echo "Compiling Java source..."
javac -cp "build/lib/*" -d build/classes StandaloneAdminServer.java

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful!"
    
    # Tạo JAR file
    echo "Creating JAR file..."
    cd build/classes
    jar -cf ../standalone-admin-server.jar admin/
    cd ../..
    
    echo "✅ Build completed!"
    echo ""
    echo "Admin API will be available at: http://localhost:9090/admin"
    echo ""
    
    read -p "Do you want to start the Admin Server now? (Y/N): " choice
    if [[ $choice =~ ^[Yy]$ ]]; then
        echo ""
        echo "Starting Admin Server..."
        echo ""
        java -cp "build/lib/*:build/standalone-admin-server.jar" admin.server.StandaloneAdminServer
    else
        echo ""
        echo "You can run the server later with: ./build-admin.sh"
    fi
    
else
    echo "❌ Compilation failed!"
    exit 1
fi

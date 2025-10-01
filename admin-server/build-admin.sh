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
    
    # Tạo run script
    cat > run-admin-server.sh << 'EOF'
#!/bin/bash
echo "Starting Standalone Admin Server..."
java -cp "build/lib/*:build/standalone-admin-server.jar" admin.server.StandaloneAdminServer
EOF
    chmod +x run-admin-server.sh
    
    # Tạo Windows batch file
    cat > run-admin-server.bat << 'EOF'
@echo off
echo Starting Standalone Admin Server...
java -cp "build/lib/*;build/standalone-admin-server.jar" admin.server.StandaloneAdminServer
pause
EOF
    
    echo "✅ Build completed!"
    echo ""
    echo "To run the admin server:"
    echo "  Linux/Mac: ./run-admin-server.sh"
    echo "  Windows:   run-admin-server.bat"
    echo ""
    echo "Admin API will be available at: http://localhost:8080/admin"
    
else
    echo "❌ Compilation failed!"
    exit 1
fi

#!/bin/bash
echo "Starting Standalone Admin Server..."
java -cp "build/lib/*:build/standalone-admin-server.jar" admin.server.StandaloneAdminServer

#!/bin/bash

# Set variables for directories
# WEB_DIR="C:/Program Files/Apache Software Foundation/Tomcat 10.1/webapps/Sprint0Web" # Update this with your actual path
WEB_DIR="C:\xampp\tomcat\webapps\Sprint0Web"
OUT_JAR="FrontServlet.jar" # Update this with the name of your generated JAR file

# Copy JAR file to lib directory
echo "Copying JAR file to WEB-INF/lib directory..."
cp "$OUT_JAR" "$WEB_DIR/WEB-INF/lib/"

# Check if copy was successful
if [ $? -eq 0 ]; then
    echo "JAR file copied successfully to WEB-INF/lib directory."
else
    echo "Failed to copy JAR file. Please check errors."
fi

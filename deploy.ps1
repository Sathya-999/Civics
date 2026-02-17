# Civic Connect Automation Script
# This script ensures a clean build and deployment to Tomcat to prevent stale file issues.

$PROJECT_ROOT = "g:\JDS\civics"
$TOMCAT_WEBAPP = "G:\apache-tomcat-9.0.115-windows-x64\apache-tomcat-9.0.115\webapps\civics"
$TOMCAT_BIN = "G:\apache-tomcat-9.0.115-windows-x64\apache-tomcat-9.0.115\bin"
$JAVA_HOME = "C:\Program Files\Java\jdk-25"

Write-Host "--- Starting Clean Deployment ---" -ForegroundColor Cyan

# 1. Stop Tomcat
Write-Host "Stopping Tomcat..."
$env:JAVA_HOME = $JAVA_HOME
cd $TOMCAT_BIN
try {
    .\shutdown.bat 2>$null
} catch {}
Start-Sleep -Seconds 2

# 2. Clean Target classes in WebContent (The source of past issues)
Write-Host "Cleaning WebContent classes..."
if (Test-Path "$TOMCAT_WEBAPP\WEB-INF\classes") {
    Remove-Item -Path "$TOMCAT_WEBAPP\WEB-INF\classes\*" -Recurse -Force -ErrorAction SilentlyContinue
}

# 3. Copy WebContent to Tomcat (Static files)
Write-Host "Updating static files..."
# Delete folders first to ensure clean copy
Remove-Item -Path "$TOMCAT_WEBAPP\css" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "$TOMCAT_WEBAPP\js" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "$TOMCAT_WEBAPP\pages" -Recurse -Force -ErrorAction SilentlyContinue

Copy-Item -Path "$PROJECT_ROOT\WebContent\css" -Destination "$TOMCAT_WEBAPP\" -Recurse -Force
Copy-Item -Path "$PROJECT_ROOT\WebContent\js" -Destination "$TOMCAT_WEBAPP\" -Recurse -Force
Copy-Item -Path "$PROJECT_ROOT\WebContent\pages" -Destination "$TOMCAT_WEBAPP\" -Recurse -Force
Copy-Item -Path "$PROJECT_ROOT\WebContent\index.jsp" -Destination "$TOMCAT_WEBAPP\" -Force
Copy-Item -Path "$PROJECT_ROOT\WebContent\error.html" -Destination "$TOMCAT_WEBAPP\" -Force
Copy-Item -Path "$PROJECT_ROOT\WebContent\google-signin.html" -Destination "$TOMCAT_WEBAPP\" -Force
Copy-Item -Path "$PROJECT_ROOT\WebContent\WEB-INF\web.xml" -Destination "$TOMCAT_WEBAPP\WEB-INF\" -Force

# Ensure uploads directory exists
if (!(Test-Path "$TOMCAT_WEBAPP\uploads")) {
    New-Item -ItemType Directory -Path "$TOMCAT_WEBAPP\uploads" -Force
}

# 3b. Copy lib JARs (javax.mail, mysql-connector, etc.)
Write-Host "Updating WEB-INF/lib..."
if (!(Test-Path "$TOMCAT_WEBAPP\WEB-INF\lib")) {
    New-Item -ItemType Directory -Path "$TOMCAT_WEBAPP\WEB-INF\lib" -Force
}
Copy-Item -Path "$PROJECT_ROOT\WebContent\WEB-INF\lib\*.jar" -Destination "$TOMCAT_WEBAPP\WEB-INF\lib\" -Force

# 4. Copy Fresh Classes from build/classes
Write-Host "Deploying fresh Java classes..."
if (!(Test-Path "$TOMCAT_WEBAPP\WEB-INF\classes")) {
    New-Item -ItemType Directory -Path "$TOMCAT_WEBAPP\WEB-INF\classes"
}
Copy-Item -Path "$PROJECT_ROOT\build\classes\*" -Destination "$TOMCAT_WEBAPP\WEB-INF\classes\" -Recurse -Force

# 5. Start Tomcat
Write-Host "Starting Tomcat..."
.\startup.bat

Write-Host "--- Deployment Complete! ---" -ForegroundColor Green
Write-Host "Access the app at: http://localhost:9550/civics/" -ForegroundColor Yellow
cd $PROJECT_ROOT

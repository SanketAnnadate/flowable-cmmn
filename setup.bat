@echo off
echo Setting up Document Review Workflow System...

REM Create uploads directory
if not exist "uploads" mkdir uploads
echo Created uploads directory

REM Check if Maven is available
mvn --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Maven is not installed or not in PATH
    echo Please install Maven and add it to your PATH
    pause
    exit /b 1
)

echo Maven is available

REM Check if Java is available
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 17+ and add it to your PATH
    pause
    exit /b 1
)

echo Java is available

echo.
echo Setup complete! You can now run the application with:
echo mvn spring-boot:run
echo.
echo Then open http://localhost:8080 in your browser
pause
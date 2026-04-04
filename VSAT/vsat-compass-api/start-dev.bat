@echo off
echo ========================================
echo  VSAT Compass API - Dev Startup Script
echo ========================================
echo.

REM ===== Auto-detect JAVA_HOME =====
if "%JAVA_HOME%"=="" (
    echo [0/3] JAVA_HOME not set. Auto-detecting...
    if exist "C:\Program Files\Android\Android Studio\jbr" (
        set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
        echo Using Android Studio JBR: %JAVA_HOME%
    ) else if exist "C:\Program Files\Java\jdk-17" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-17"
        echo Using JDK 17: %JAVA_HOME%
    ) else (
        echo ERROR: Cannot find Java! Install JDK 17+ or set JAVA_HOME.
        pause
        exit /b 1
    )
) else (
    echo [0/3] JAVA_HOME=%JAVA_HOME%
)

REM Navigate to backend project directory
cd /d "%~dp0"
if not exist "gradlew.bat" (
    REM Try going up from VSAT/ subfolder
    cd /d "%~dp0..\.."
    if not exist "gradlew.bat" (
        echo ERROR: gradlew.bat not found!
        pause
        exit /b 1
    )
)

REM ===== Step 1: Firewall Rule =====
echo [1/3] Checking Windows Firewall rule for port 8080...
netsh advfirewall firewall show rule name="VSAT-Backend-8080" >nul 2>&1
if %errorlevel% neq 0 (
    echo Adding firewall rule (requires Admin)...
    netsh advfirewall firewall add rule name="VSAT-Backend-8080" dir=in action=allow protocol=TCP localport=8080
    if %errorlevel% equ 0 (
        echo Firewall rule added successfully!
    ) else (
        echo WARNING: Could not add firewall rule. Run this script as Administrator!
    )
) else (
    echo Firewall rule already exists. OK.
)

REM ===== Step 2: Load .env file =====
echo.
echo [2/3] Loading environment variables from .env...
if exist ".env" (
    for /f "usebackq tokens=1,* delims==" %%a in (".env") do (
        REM Skip comments and empty lines
        echo %%a | findstr /r "^#" >nul 2>&1
        if errorlevel 1 (
            if not "%%a"=="" if not "%%b"=="" (
                set "%%a=%%b"
            )
        )
    )
    echo .env loaded successfully!
) else (
    echo WARNING: .env file not found!
    echo Creating .env from .env.example...
    if exist ".env.example" (
        copy ".env.example" ".env" >nul
        echo Please edit .env with your Neon credentials, then run this script again.
        pause
        exit /b 1
    ) else (
        echo ERROR: No .env or .env.example found!
        pause
        exit /b 1
    )
)

REM ===== Step 3: Verify and Start =====
echo.
echo [3/3] Starting Spring Boot backend...
echo.
echo    Database: Neon PostgreSQL (ap-southeast-1)
echo    Port:     8080
echo    Context:  /api/v1
echo    Swagger:  http://localhost:8080/api/v1/swagger-ui.html
echo    Android:  http://10.0.2.2:8080/api/v1/
echo.
echo    Test accounts:
echo      student@vsat.com / Student@123  (STUDENT)
echo      collab@vsat.com  / Admin@123    (COLLABORATOR)
echo      content@vsat.com / Admin@123    (CONTENT_ADMIN)
echo      admin@vsat.com   / Admin@123    (SUPER_ADMIN)
echo.
echo ========================================

call gradlew.bat bootRun --args="--spring.profiles.active=dev"

if %errorlevel% neq 0 (
    echo.
    echo ERROR: Backend failed to start!
    echo Check JAVA_HOME is set and Java 17+ is installed.
    pause
)

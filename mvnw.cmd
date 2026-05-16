@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.3.2
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_ARG0_NAME__%"=="" (SET "__MVNW_ARG0_NAME__=%~nx0")
@SET __MVNW_CMD__=
@SET __MVNW_ERROR__=
@ECHO OFF

IF "%MAVEN_BATCH_ECHO%"=="on" ECHO ON

IF "%HOME%"=="" (SET "HOME=%HOMEDRIVE%%HOMEPATH%")

@setlocal
SET ERROR_CODE=0

@REM ==== FIND JAVA_HOME ====
IF NOT "%JAVA_HOME%"=="" GOTO OkJHome
FOR %%i IN (java.exe) DO SET "JAVA_EXE=%%~$PATH:i"
IF NOT "%JAVA_EXE%"=="" (
  FOR %%i IN ("%JAVA_EXE%") DO SET "JAVA_HOME=%%~dpi.."
  GOTO OkJHome
)
ECHO Error: JAVA_HOME not found in your environment. >&2
ECHO Please set the JAVA_HOME variable. >&2
SET ERROR_CODE=1
GOTO error
:OkJHome
SET "JAVA_EXE=%JAVA_HOME%\bin\java.exe"

@REM ==== FIND MAVEN WRAPPER JAR ====
SET "MAVEN_PROJECTBASEDIR=%~dp0"
IF NOT "%MAVEN_BASEDIR%"=="" SET "MAVEN_PROJECTBASEDIR=%MAVEN_BASEDIR%"
SET "MVNW_REPOURL=https://repo.maven.apache.org/maven2"

SET "wrapperJarPath=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar"
IF EXIST "%wrapperJarPath%" GOTO runMavenWithJarWrapper

@REM Download wrapper jar using PowerShell
SET "MVNW_JAR_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
ECHO Downloading maven-wrapper.jar from %MVNW_JAR_URL% ...
"%JAVA_EXE%" -cp "" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %* 2>NUL
IF %ERRORLEVEL% NEQ 0 (
  powershell -Command "& { Invoke-WebRequest -Uri '%MVNW_JAR_URL%' -OutFile '%wrapperJarPath%' }"
)

:runMavenWithJarWrapper
@REM ==== FIND/DOWNLOAD MAVEN DISTRIBUTION ====
FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties") DO (
  IF "%%A"=="distributionUrl" SET "distributionUrl=%%B"
)

SET "distributionUrlName=%distributionUrl:*apache-maven-=%"
SET "distributionVersion=%distributionUrlName:-bin.zip=%"

SET "MAVEN_USER_HOME=%USERPROFILE%\.m2"
SET "MAVEN_HOME=%MAVEN_USER_HOME%\wrapper\dists\apache-maven-%distributionVersion%"

IF EXIST "%MAVEN_HOME%\bin\mvn.cmd" GOTO launchMaven

ECHO Downloading Maven %distributionVersion% ...
powershell -Command "& { $zipFile = '%MAVEN_USER_HOME%\wrapper\dists\apache-maven-%distributionVersion%-bin.zip'; $destDir = '%MAVEN_USER_HOME%\wrapper\dists'; New-Item -ItemType Directory -Force -Path $destDir | Out-Null; Invoke-WebRequest -Uri '%distributionUrl%' -OutFile $zipFile; Expand-Archive -Path $zipFile -DestinationPath $destDir -Force; Remove-Item $zipFile }"

:launchMaven
SET "MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd"
IF NOT EXIST "%MAVEN_CMD%" (
  ECHO Error: Maven distribution could not be found at %MAVEN_HOME% >&2
  SET ERROR_CODE=1
  GOTO error
)

SET MAVEN_OPTS=%MAVEN_OPTS%
"%MAVEN_CMD%" %* && GOTO end

:error
SET ERROR_CODE=1

:end
@endlocal & SET ERROR_CODE=%ERROR_CODE%
IF NOT "%MAVEN_BATCH_PAUSE%"=="" PAUSE
EXIT /B %ERROR_CODE%

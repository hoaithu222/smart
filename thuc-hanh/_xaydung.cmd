@echo off
rem Bien dich XayDung.java roi chay voi tham so duoc chuyen vao.
rem File .cmd viet khong dau co y (cmd doc sai chu Viet sau chcp 65001).
setlocal
chcp 65001 >nul
cd /d "%~dp0"
rem Tim javac: PATH, roi JAVA_HOME, roi JDK di kem JavaCardKit (may co JCIDE).
rem Khong dung khoi ngoac vi PATH co the chua dau ngoac.
where javac >nul 2>nul
if errorlevel 1 if exist "%JAVA_HOME%\bin\javac.exe" set "PATH=%JAVA_HOME%\bin;%PATH%"
where javac >nul 2>nul
if errorlevel 1 if exist "C:\JavaCardKit\JDK\bin\javac.exe" set "PATH=C:\JavaCardKit\JDK\bin;%PATH%"
where javac >nul 2>nul
if errorlevel 1 (
  echo Khong thay javac. Can JDK 7 tro len ^(khuyen JDK 8^): them vao PATH hoac dat JAVA_HOME.
  exit /b 1
)
if not exist build\xaydung mkdir build\xaydung
javac -encoding UTF-8 -d build\xaydung cong-cu\XayDung.java cong-cu\src\thuchanh\CauHinh.java cong-cu\src\thuchanh\Hex.java
if errorlevel 1 exit /b 1
java -Dfile.encoding=UTF-8 -cp build\xaydung XayDung %*

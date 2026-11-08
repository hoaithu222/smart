@echo off
rem ================================================================
rem Tải 4 thư viện vào host\lib (chạy 1 lần, cần Internet; Windows 10+ có sẵn curl):
rem   flatlaf      - giao diện đẹp cho Java Swing (chạy trên Java 8)
rem   mssql-jdbc   - driver SQL Server, bản .jre8 cho JDK 8 / NetBeans 8.2
rem   sqlite-jdbc  - CSDL chạy thử không cần SQL Server + bộ test
rem   jcardsim     - thẻ giả lập trong phần mềm (card.mode=jcardsim) + bộ test
rem ================================================================
chcp 65001 >nul
cd /d "%~dp0"
if not exist lib mkdir lib
set M=https://repo1.maven.org/maven2
call :get flatlaf-3.5.4.jar               %M%/com/formdev/flatlaf/3.5.4/flatlaf-3.5.4.jar
call :get mssql-jdbc-12.8.1.jre8.jar       %M%/com/microsoft/sqlserver/mssql-jdbc/12.8.1.jre8/mssql-jdbc-12.8.1.jre8.jar
call :get sqlite-jdbc-3.41.2.2.jar         %M%/org/xerial/sqlite-jdbc/3.41.2.2/sqlite-jdbc-3.41.2.2.jar
call :get jcardsim-2.2.2.jar               %M%/com/licel/jcardsim/2.2.2/jcardsim-2.2.2.jar
echo.
dir /b lib
exit /b 0

:get
if exist lib\%1 (
  echo Da co  lib\%1
) else (
  echo Dang tai %1 ...
  curl -fsSL -o lib\%1 %2 || echo   LOI: khong tai duoc %1
)
exit /b 0

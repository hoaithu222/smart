@echo off
rem ================================================================
rem Chay lai 49 kich ban SimTest nhung CSDL la SQL SERVER THAT (khong phai SQLite).
rem - Doc host\app.local.properties (co mat khau sa, khong len git); khong co thi host\app.properties.
rem - Dung database RIENG the_sinh_vien_test: tu tao, chay xong tu xoa. KHONG dung toi the_sinh_vien.
rem - SimTest tu choi chay neu ten database khong ket thuc bang _test.
rem Can: SQL Server dang chay, JDK 8+ trong PATH, da co host\lib (tai-thu-vien.cmd).
rem File nay CO Y viet khong dau (cmd sau chcp 65001 doc sai dong co chu Viet).
rem ================================================================
chcp 65001 >nul
cd /d "%~dp0"
if not exist host\lib\mssql-jdbc-12.8.1.jre8.jar (
  echo Chua co thu vien - chay host\tai-thu-vien.cmd truoc
  exit /b 1
)
set SRC=host\app.properties
if exist host\app.local.properties set SRC=host\app.local.properties
set TMPCFG=%TEMP%\sinhvien-test-sqlserver.properties
powershell -NoProfile -Command "(Get-Content -Raw -Encoding UTF8 '%SRC%') -replace '(?i)databaseName=[A-Za-z0-9_]+','databaseName=the_sinh_vien_test' | Set-Content -Encoding UTF8 '%TMPCFG%'" || exit /b 1
set CP=host\lib\*
if exist build rmdir /s /q build
mkdir build
javac -encoding UTF-8 -cp "%CP%" -d build applet\src\sinhvien\StudentCard.java host\src\sinhvien\host\*.java host\src\sinhvien\ui\*.java test\src\SimTest.java || exit /b 1
xcopy /s /q /y host\src\sinhvien\host\db build\sinhvien\host\db\ >nul
echo ===== SimTest tren SQL Server that (database the_sinh_vien_test) =====
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dtest.config="%TMPCFG%" -cp "build;host\src;%CP%" SimTest
set RC=%ERRORLEVEL%
del "%TMPCFG%" >nul 2>&1
exit /b %RC%

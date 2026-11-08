@echo off
rem ================================================================
rem Biên dịch + chạy phần mềm Thẻ sinh viên không cần mở NetBeans.
rem Cần: JDK 8 trở lên trong PATH, đã chạy tai-thu-vien.cmd.
rem Cấu hình: app.properties cùng thư mục (card.mode, db.url ...).
rem ================================================================
chcp 65001 >nul
cd /d "%~dp0"
if not exist lib\flatlaf-3.5.4.jar (
  echo Chua co thu vien - chay tai-thu-vien.cmd truoc
  exit /b 1
)
if exist build rmdir /s /q build
mkdir build
rem Applet được biên dịch kèm để chạy được chế độ card.mode=jcardsim (dùng chung StudentCard.java với JCIDE)
javac -encoding UTF-8 -cp "lib\*" -d build ..\applet\src\sinhvien\StudentCard.java src\sinhvien\host\*.java src\sinhvien\ui\*.java || exit /b 1
xcopy /s /q /y src\sinhvien\host\db build\sinhvien\host\db\ >nul
java -Dfile.encoding=UTF-8 -cp "build;lib\*" sinhvien.ui.App app.properties

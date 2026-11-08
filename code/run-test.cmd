@echo off
rem ================================================================
rem Chạy 2 bộ kiểm thử (không cần JCIDE, không cần SQL Server):
rem   1. SimTest  - 49 kịch bản applet + thư viện máy tính + CSDL SQLite
rem   2. UiShots  - chạy đủ luồng qua các màn hình (thẻ giả lập + SQLite) và chụp 14 ảnh
rem                 vào test\anh-giao-dien\ (dùng cho báo cáo)
rem Cần: JDK 8+ trong PATH, đã chạy host\tai-thu-vien.cmd.
rem ================================================================
chcp 65001 >nul
cd /d "%~dp0"
set CP=host\lib\*
if not exist host\lib\jcardsim-2.2.2.jar (
  echo Chua co thu vien - chay host\tai-thu-vien.cmd truoc
  exit /b 1
)
if exist build rmdir /s /q build
mkdir build
javac -encoding UTF-8 -cp "%CP%" -d build applet\src\sinhvien\StudentCard.java host\src\sinhvien\host\*.java host\src\sinhvien\ui\*.java test\src\SimTest.java test\src\sinhvien\ui\UiShots.java || exit /b 1
echo ===== 1. SimTest =====
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "build;host\src;%CP%" SimTest || exit /b 1
echo.
echo ===== 2. UiShots =====
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "build;host\src;%CP%" sinhvien.ui.UiShots test\anh-giao-dien

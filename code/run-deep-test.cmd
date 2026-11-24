@echo off
rem ================================================================
rem Deep test: DeepTest.java (167 checks + fuzz 4400 APDU + doi chieu mat ma doc lap).
rem Khong can JCIDE, khong can SQL Server (dung SQLite trong bo nho).
rem Can: JDK 8+ trong PATH, da chay host\tai-thu-vien.cmd.
rem Ma thoat 0 = khong co kiem tra SAI. "PHAT HIEN" la diem yeu da chung minh, khong lam test that bai.
rem File nay viet khong dau co y (cmd doc sai chu Viet sau chcp 65001).
rem ================================================================
chcp 65001 >nul
cd /d "%~dp0"
if not exist host\lib\jcardsim-2.2.2.jar (
  echo Chua co thu vien - chay host\tai-thu-vien.cmd truoc
  exit /b 1
)
if exist build rmdir /s /q build
mkdir build
javac -encoding UTF-8 -cp "host\lib\*" -d build applet\src\sinhvien\StudentCard.java host\src\sinhvien\host\*.java test\src\DeepTest.java || exit /b 1
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "build;host\src;host\lib\*" DeepTest

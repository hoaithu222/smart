@echo off
rem ================================================================
rem Bien dich + chay phan mem The sinh vien khong can mo NetBeans.
rem Can: JDK 8 tro len trong PATH, da chay tai-thu-vien.cmd.
rem Cau hinh: app.local.properties neu co (file rieng cua may, co mat khau sa,
rem khong len git), khong thi app.properties.
rem Luu y: file nay CO Y viet khong dau - cmd doc sai dong co chu tieng Viet sau chcp 65001.
rem ================================================================
chcp 65001 >nul
cd /d "%~dp0"
if not exist lib\flatlaf-3.5.4.jar (
  echo Chua co thu vien - chay tai-thu-vien.cmd truoc
  exit /b 1
)
if exist build rmdir /s /q build
mkdir build
rem Applet duoc bien dich kem de chay duoc che do card.mode=jcardsim (dung chung StudentCard.java voi JCIDE)
javac -encoding UTF-8 -cp "lib\*" -d build ..\applet\src\sinhvien\StudentCard.java src\sinhvien\host\*.java src\sinhvien\ui\*.java || exit /b 1
xcopy /s /q /y src\sinhvien\host\db build\sinhvien\host\db\ >nul
set CFG=app.properties
if exist app.local.properties set CFG=app.local.properties
java -Dfile.encoding=UTF-8 -cp "build;lib\*" sinhvien.ui.App %CFG%

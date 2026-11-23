@echo off
rem ================================================================
rem Build applet StudentCard bang bo chuyen doi Java Card 2.2.2 (giong buoc Build cua JCIDE),
rem roi chay bo kiem tra bytecode Off-card Verifier cua Oracle tren file CAP.
rem DA CHAY THAT 23/11/2026: converter "conversion completed with 0 errors and 0 warnings",
rem verifier "Verification completed with 0 warnings and 0 errors".
rem Can: JDK 8 (javac -source 1.5 chi co o JDK 8) + bo Java Card 2.2.2 (JCDK: lib\api.jar, converter.jar, api_export_files).
rem Mac dinh dung dung duong dan cua JCIDE: C:\JavaCardKit\JDK va C:\JavaCardKit\JCDK
rem (lien ket thu muc toi JDK 8 va toi C:\Tools\oracle_javacard_sdks\jc222_kit).
rem Doi duong dan bang bien moi truong: JAVA8_HOME, JC222_HOME
rem File nay CO Y viet khong dau (cmd doc sai dong co chu tieng Viet sau chcp 65001).
rem ================================================================
setlocal
cd /d "%~dp0"
if "%JAVA8_HOME%"=="" set JAVA8_HOME=C:\JavaCardKit\JDK
if "%JC222_HOME%"=="" set JC222_HOME=C:\JavaCardKit\JCDK
if not exist "%JAVA8_HOME%\bin\javac.exe" (
  echo Khong thay JDK 8 tai "%JAVA8_HOME%" - dat JAVA8_HOME
  exit /b 1
)
if not exist "%JC222_HOME%\lib\converter.jar" (
  echo Khong thay Java Card 2.2.2 SDK tai "%JC222_HOME%" - dat JC222_HOME
  exit /b 1
)
if exist build rmdir /s /q build
mkdir build\classes
mkdir build\cap
"%JAVA8_HOME%\bin\javac.exe" -g -source 1.5 -target 1.5 -encoding UTF-8 -cp "%JC222_HOME%\lib\api.jar" -d build\classes src\sinhvien\StudentCard.java || exit /b 1
"%JAVA8_HOME%\bin\java.exe" -cp "%JC222_HOME%\lib\converter.jar;%JC222_HOME%\lib\offcardverifier.jar;%JC222_HOME%\lib\api.jar" com.sun.javacard.converter.Converter -classdir build\classes -exportpath "%JC222_HOME%\api_export_files" -d build\cap -out CAP EXP JCA -applet 0x4B:0x4D:0x41:0x53:0x56:0x01:0x01 sinhvien.StudentCard sinhvien 0x4B:0x4D:0x41:0x53:0x56:0x01 1.0 || exit /b 1
set EXP=%JC222_HOME%\api_export_files
"%JAVA8_HOME%\bin\java.exe" -cp "%JC222_HOME%\lib\offcardverifier.jar;%JC222_HOME%\lib\converter.jar;%JC222_HOME%\lib\api.jar" com.sun.javacard.offcardverifier.Verifier -nobanner "%EXP%\java\lang\javacard\lang.exp" "%EXP%\javacard\framework\javacard\framework.exp" "%EXP%\javacard\security\javacard\security.exp" "%EXP%\javacardx\crypto\javacard\crypto.exp" build\cap\sinhvien\javacard\sinhvien.cap || exit /b 1
echo Xong. File CAP trong build\cap\sinhvien\javacard\sinhvien.cap

@echo off
rem Shell APDU (giong Debug Shell cua JCIDE).
rem   chay.cmd                          mo shell, the trang
rem   chay.cmd bai2_Object.Applet2      cai, chon applet roi mo shell
rem   chay.cmd kich-ban\th3.apdu        chay mot kich ban co kiem ket qua
rem   chay.cmd --pcsc=JAVACOS           gui toi the ao JCIDE qua PC/SC
call "%~dp0_xaydung.cmd" chay %*

@echo off

call .\setenv.bat

set CLASSPATH=%JC22_HOME%\lib\apduio.jar
set CLASSPATH=%CLASSPATH%;%OCF_HOME%\lib\base-core.jar
set CLASSPATH=%CLASSPATH%;%OCF_HOME%\lib\base-opt.jar
set CLASSPATH=%CLASSPATH%;%MISC%\%PCSC_WRAPPER%\lib\%PCSC_WRAPPER%.jar
set CLASSPATH=%CLASSPATH%;%MISC%\%APDUIO_TERM%\lib\%APDUIO_TERM%.jar
set CLASSPATH=%CLASSPATH%;%MISC%\bcprov-jdk15on-150.jar

%SERVER_JAVA_HOME%\bin\java.exe --enable-preview -Djava.library.path=. -classpath %OUT%\;%CLASSPATH% %PROJECT%.%PKGSERVER%.%SERVER%

if errorlevel 1 goto error

pause
goto end

:error
echo **************
echo    ERROR !
echo **************
pause
goto end

:end
cls
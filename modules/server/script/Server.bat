SET JAVA_HOME=D:\DreamSky\Java8\jdk1.8.0_101
SET Classpath=%JAVA_HOME%\lib\tools.jar;%JAVA_HOME%\lib\dt.jar;
SET Path=%JAVA_HOME%\bin;

@echo off
setlocal enabledelayedexpansion

set Tag=DevOps-Server-System-JpomServerApplication
set MainClass=org.springframework.boot.loader.JarLauncher
set basePath=%~dp0
set Lib=%basePath%lib\
@REM �����޸�----------------------------------��
set LogName=server.log
@REM �����������Զ��޸Ĵ�����
set RUNJAR=
@REM �����޸�----------------------------------��
@REM �Ƿ�������̨��־�ļ�����
set LogBack=true
set JVM=-server
set ARGS= --devops.applicationTag=%Tag%  --spring.profiles.active=pro --devops.log=%basePath%log --server.port=2122

@REM ��ȡjar
call:listDir

if "%1"=="" (
    color 0a
    TITLE �Զ�����άƽ̨BAT����̨
    echo. ***** �Զ�����άƽ̨BAT����̨ *****
    ::*************************************************************************************************************
    echo.
        echo.  [1] ���� start
        echo.  [2] �ر� stop
        echo.  [3] �鿴����״̬ status
        echo.  [4] ���� restart
        echo.  [5] ���� use
        echo.  [0] �� �� 0
    echo.
    @REM ����
    echo.������ѡ������:
    set /p ID=
    IF "!ID!"=="1" call:start
    IF "!ID!"=="2" call:stop
    IF "!ID!"=="3" call:status
    IF "!ID!"=="4" call:restart
    IF "!ID!"=="5" call:use
    IF "!ID!"=="0" EXIT
)else (
     if "%1"=="restart" (
        call:restart
     )else (
        call:use
     )
)
if "%2" NEQ "upgrade" (
    PAUSE
)else (
 @REM ����ֱ�ӽ���
)
EXIT 0

@REM ����
:start
    if "%JAVA_HOME%"=="" (
        echo �����á�JAVA_HOME����������
        PAUSE
        EXIT 2
    )

	echo ������.....�رմ��ڲ�Ӱ������
	javaw %JVM% -Djava.class.path="%JAVA_HOME%/lib/tools.jar;%RUNJAR%" -Dapplication=%Tag% -Dbasedir=%basePath%  %MainClass% %ARGS% >> %basePath%%LogName%
	timeout 3
goto:eof


@REM ��ȡjar
:listDir
	if "%RUNJAR%"=="" (
		for /f "delims=" %%I in ('dir /B %Lib%') do (
			if exist %Lib%%%I if not exist %Lib%%%I\nul (
			    if "%%~xI" ==".jar" (
                    if "%RUNJAR%"=="" (
				        set RUNJAR=%Lib%%%I
                    )
                )
			)
		)
	)else (
		set RUNJAR=%Lib%%RUNJAR%
	)
	echo ���У�%RUNJAR%
goto:eof

@REM �ر�
:stop
	java -Djava.class.path="%JAVA_HOME%/lib/tools.jar;%RUNJAR%" %MainClass% %ARGS% --event=stop
goto:eof

@REM �鿴����״̬
:status
	java -Djava.class.path="%JAVA_HOME%/lib/tools.jar;%RUNJAR%" %MainClass% %ARGS% --event=status
goto:eof

@REM ����
:restart
	echo ֹͣ��....
	call:stop
	timeout 3
	echo ������....
	call:start
goto:eof

@REM ��ʾ�÷�
:use
	echo please use (start��stop��restart��status)
goto:eof



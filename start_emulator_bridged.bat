@echo off
REM Android模拟器桥接模式启动脚本
REM 使用方法：修改下面的AVD_NAME为你的模拟器名称，然后运行此脚本

REM 设置Android SDK路径（如果已设置ANDROID_HOME环境变量，则使用它）
if "%ANDROID_HOME%"=="" (
    REM 默认路径，请根据你的实际安装路径修改
    set ANDROID_SDK_ROOT=C:\Users\%USERNAME%\AppData\Local\Android\Sdk
) else (
    set ANDROID_SDK_ROOT=%ANDROID_HOME%
)

REM 设置模拟器可执行文件路径
set EMULATOR_PATH=%ANDROID_SDK_ROOT%\emulator\emulator.exe

REM 设置AVD名称（请修改为你的实际AVD名称）
set AVD_NAME=你的AVD名称

REM 检查模拟器是否已安装
if not exist "%EMULATOR_PATH%" (
    echo 错误：找不到模拟器，请检查ANDROID_SDK_ROOT路径是否正确
    echo 当前路径: %EMULATOR_PATH%
    pause
    exit /b 1
)

REM 列出可用的AVD
echo 正在查找可用的AVD...
"%EMULATOR_PATH%" -list-avds
echo.

REM 启动模拟器（桥接模式）
echo 正在以桥接模式启动模拟器: %AVD_NAME%
echo.
echo 桥接模式说明：
echo - 模拟器将直接连接到主机的网络接口
echo - 模拟器将获得与主机同一网段的独立IP地址
echo - 需要管理员权限才能使用桥接模式
echo.

REM 使用-netdelay none -netspeed full 和 -netdev user 参数
REM 对于桥接模式，需要使用 -netdev tap 参数（需要管理员权限）
REM 或者使用 -netdev user,id=net0 -device virtio-net-pci,netdev=net0

"%EMULATOR_PATH%" -avd %AVD_NAME% -netdelay none -netspeed full

pause

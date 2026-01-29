# 手动配置AVD桥接模式指南

如果Android Studio中没有"Show Advanced Settings"选项，可以通过手动编辑配置文件来设置桥接模式。

## 方法一：使用自动配置脚本（推荐）

运行配置脚本：
```powershell
.\configure_avd_bridged.ps1
```

脚本会自动：
1. 列出所有可用的AVD
2. 让你选择要配置的AVD
3. 列出可用的网络适配器
4. 修改AVD配置文件
5. 创建备份文件

## 方法二：手动编辑配置文件

### 步骤1：找到AVD配置文件位置

AVD配置文件通常位于：
- Windows: `C:\Users\<用户名>\.android\avd\<AVD名称>.avd\config.ini`
- 或者: `%ANDROID_AVD_HOME%\<AVD名称>.avd\config.ini`

### 步骤2：备份配置文件

在编辑前，先备份配置文件：
```powershell
Copy-Item "C:\Users\<用户名>\.android\avd\Television_1080p.avd\config.ini" "C:\Users\<用户名>\.android\avd\Television_1080p.avd\config.ini.backup"
```

### 步骤3：编辑config.ini文件

用文本编辑器打开 `config.ini` 文件，添加或修改以下行：

```ini
# 网络模式：bridged（桥接模式）
hw.network = bridged

# 网络适配器（可选，如果不设置会使用默认适配器）
# 查看可用适配器：在PowerShell中运行 Get-NetAdapter
net.if = <你的网络适配器名称>

# 网络速度
net.speed = full

# 网络延迟
net.delay = none
```

### 步骤4：查找网络适配器名称（可选）

如果需要指定网络适配器，在PowerShell中运行：
```powershell
Get-NetAdapter | Where-Object { $_.Status -eq "Up" } | Select-Object Name, InterfaceDescription
```

然后使用适配器的 `Name` 值。

### 步骤5：保存并重启模拟器

保存 `config.ini` 文件后，重启模拟器使配置生效。

## 方法三：通过命令行参数启动（临时）

如果不想修改配置文件，可以在启动模拟器时使用命令行参数：

```powershell
emulator -avd Television_1080p -netdelay none -netspeed full
```

注意：这种方式每次启动都需要添加参数，且不能完全实现桥接模式（需要配置文件支持）。

## 验证配置

启动模拟器后，可以通过以下方式验证：

1. **查看IP地址**：
   - 在模拟器中：设置 > 关于手机 > 状态信息 > IP地址
   - 或使用命令：`adb shell ip addr show`

2. **测试网络连接**：
   - 在模拟器中打开浏览器，访问局域网内的其他设备
   - 或者从主机ping模拟器的IP地址

## 常见问题

### Q: 配置文件在哪里？
A: 通常在 `%USERPROFILE%\.android\avd\<AVD名称>.avd\config.ini`

### Q: 如何查看我的AVD名称？
A: 运行命令：`emulator -list-avds`

### Q: 桥接模式不工作？
A: 检查：
- 配置文件是否正确保存
- 模拟器是否已重启
- 是否有管理员权限
- 网络适配器是否可用

### Q: 如何恢复默认设置？
A: 删除或注释掉添加的网络配置行，或者从备份文件恢复：
```powershell
Copy-Item "config.ini.backup" "config.ini" -Force
```

## 配置文件示例

完整的 `config.ini` 文件示例（仅显示网络相关部分）：

```ini
# ... 其他配置 ...

# 网络配置
hw.network = bridged
net.if = Wi-Fi
net.speed = full
net.delay = none

# ... 其他配置 ...
```

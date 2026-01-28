# Android模拟器桥接模式配置指南

## 方法一：通过Android Studio GUI配置（推荐）

### 步骤：

1. **打开Android Studio**
   - 启动Android Studio

2. **打开AVD Manager**
   - 点击 `Tools` > `Device Manager`（或 `AVD Manager`）
   - 或者点击工具栏上的设备管理器图标

3. **编辑AVD配置**
   - 找到你要配置的模拟器
   - 点击右侧的编辑图标（铅笔图标）或下拉菜单选择 `Edit`

4. **配置网络模式**
   - 在配置界面中，点击 `Show Advanced Settings`（显示高级设置）
   - 向下滚动找到 `Network`（网络）部分
   - 在 `Network` 下拉菜单中选择 `Bridged`（桥接模式）
   - 在 `Bridged Adapter` 下拉菜单中选择你的网络适配器（通常是你的WiFi或以太网适配器）

5. **保存设置**
   - 点击 `Finish` 保存配置
   - 重新启动模拟器使设置生效

## 方法二：通过命令行配置

### Windows PowerShell脚本

1. **修改脚本中的AVD名称**
   - 打开 `start_emulator_bridged.ps1`
   - 将 `$AVD_NAME = "你的AVD名称"` 改为你的实际AVD名称

2. **运行脚本**
   ```powershell
   .\start_emulator_bridged.ps1
   ```

### Windows批处理脚本

1. **修改脚本中的AVD名称**
   - 打开 `start_emulator_bridged.bat`
   - 将 `set AVD_NAME=你的AVD名称` 改为你的实际AVD名称

2. **运行脚本**
   ```cmd
   start_emulator_bridged.bat
   ```

## 方法三：直接使用命令行启动

### 查找AVD名称
```bash
emulator -list-avds
```

### 启动桥接模式模拟器
```bash
emulator -avd <AVD名称> -netdelay none -netspeed full
```

注意：命令行方式可能需要在AVD配置文件中手动设置网络模式。

## 桥接模式的优势

1. **独立IP地址**：模拟器获得与主机同一网段的独立IP地址
2. **直接网络访问**：可以直接访问局域网内的其他设备
3. **真实网络环境**：更接近真实设备的网络环境
4. **服务器测试**：可以方便地测试局域网内的HTTP服务器（如你的应用中的端口8099）

## 注意事项

1. **管理员权限**：桥接模式可能需要管理员权限
2. **防火墙设置**：确保防火墙允许模拟器访问网络
3. **网络适配器**：选择正确的网络适配器（WiFi或以太网）
4. **IP地址**：桥接模式下，模拟器的IP地址会显示在设置 > 关于手机 > 状态信息中

## 验证桥接模式是否生效

1. 启动模拟器后，打开设置
2. 进入 `设置` > `关于手机` > `状态信息`
3. 查看IP地址，应该与主机在同一网段
4. 或者在模拟器中运行：
   ```bash
   adb shell ifconfig
   ```
   查看网络接口配置

## 常见问题

### Q: 桥接模式选项不可用？
A: 确保：
- 以管理员权限运行Android Studio
- 网络适配器支持桥接模式
- 安装了最新的模拟器工具

### Q: 模拟器无法连接到网络？
A: 检查：
- 防火墙设置
- 网络适配器选择是否正确
- 主机网络连接是否正常

### Q: 如何查看模拟器的IP地址？
A: 在模拟器中：
- 设置 > 关于手机 > 状态信息 > IP地址
- 或使用命令：`adb shell ip addr show`

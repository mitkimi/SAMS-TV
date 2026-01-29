# Android TV M3U播放器应用

[暂时未完成]

这是一个基于Android TV的M3U播放列表播放器应用，支持遥控器操作和频道切换。

## 功能特性

### 1. 开机画面 (Splash Screen)
- 显示3秒品牌Logo
- 加载动画效果
- 后台初始化M3U频道数据
- 自动进入主界面

### 2. M3U播放列表功能
#### 列表加载
- 支持远程M3U URL加载
- 支持本地M3U文件加载
- 自动解析M3U格式：
```
#EXTM3U
#EXTINF:-1 tvg-id="频道ID" tvg-name="频道名称" tvg-logo="LogoURL" group-title="分组",频道名称
http://stream.url/直播流.m3u8
```

#### 频道播放
- 全屏播放当前频道
- 显示频道信息(名称、Logo)
- 播放控制(暂停、继续)
- 码率切换支持

#### 频道切换方式
##### 方式一：上下键切换
- 上键：上一个频道
- 下键：下一个频道
- 实时预览（可选）

##### 方式二：频道列表弹出
- OK键呼出频道列表
- 列表显示频道Logo和名称
- 支持分组显示
- 遥控器方向键选择
- Enter键确认切换

## 遥控器操作

| 按键 | 功能 |
|------|------|
| 上键 | 上一个频道 |
| 下键 | 下一个频道 |
| OK键/确认键 | 呼出频道列表 |
| 返回键 | 退出频道列表/返回 |
| 菜单键 | 显示设置选项 |

## 技术实现

### 核心组件
1. **SplashActivity** - 开机画面和初始化
2. **MainActivity** - 主播放界面
3. **M3UParser** - M3U文件解析器
4. **PlayerController** - ExoPlayer封装
5. **ChannelListDialogFragment** - 频道列表弹窗
6. **RemoteKeyHandler** - 遥控器输入处理

### 依赖库
- **ExoPlayer** - 视频播放器
- **Glide** - 图片加载
- **AndroidX Leanback** - TV UI组件
- **Kotlin Coroutines** - 异步处理

## 配置说明

### 修改M3U URL
在 `SplashActivity.kt` 中修改 `SAMPLE_M3U_URL` 常量：
```kotlin
private const val SAMPLE_M3U_URL = "您的M3U播放列表URL"
```

### 添加更多频道
修改 `getSampleChannels()` 方法中的示例频道数据。

## 性能优化

- 列表滚动流畅(60fps)
- 频道切换时间 < 2秒
- 内存使用优化
- 网络请求缓存
- 图片懒加载

## 测试要求

- 遥控器操作测试
- 不同M3U格式兼容性测试
- 网络异常处理测试
- 内存泄漏测试
- 播放稳定性测试

## 注意事项

- 遵循Android TV设计规范
- 适配不同分辨率电视
- 支持暗色/亮色主题
- 无障碍功能考虑

## 可选功能（TODO）

- 收藏频道功能
- 观看历史记录
- 家长控制
- 多清晰度切换
- EPG电子节目指南集成

## 构建和运行

1. 确保已安装JDK并配置JAVA_HOME
2. 安装Android SDK和必要组件
3. 使用Android Studio打开项目
4. 配置M3U播放列表URL
5. 构建并运行到Android TV设备或模拟器

## 许可证

MIT License
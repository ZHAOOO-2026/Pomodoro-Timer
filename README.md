# 番茄自习室

以专注计时和线上自习室为核心的多端产品原型。仓库包含原生 Android 初版、可交互 Web 原型、产品设计方案与功能说明。

## Android

Kotlin + Jetpack Compose + Material 3。主界面包含首页、番茄钟、自习室、计划、统计；打卡与个人主页由首页/顶部入口进入。Android Studio 打开 `android/` 目录后运行 `app`。

当前 Android 是本地交互原型：计时和计划尚未接数据库；登录、自习室实时协作和跨设备同步尚未接后端。主题色、背景和明暗模式保存在本机。

## Web 原型

打开仓库根目录的 `index.html`，查看响应式前端原型。在线房间与云端同步状态目前为演示数据。

## 产品资料

- [产品设计方案](产品设计方案.md)：信息架构、用户流程、内容文案、视觉规范、动效、隐私和 MVP 验收。
- [功能说明](功能说明.md)：平台规划、数据模型、API 草案、技术架构和分期。

## Android 环境

- Android Studio
- JDK 17 字节码目标
- Android SDK 35
- 最低 Android 8.0（API 26）

项目使用 Gradle Wrapper。中文路径检查已按 Android Gradle 插件提示关闭；Gradle 代理参数位于 `android/gradle.properties`，默认指向本机 Clash `127.0.0.1:7897`。如果没有使用该代理，请删除 `systemProp.*proxy*` 行或改成自己的代理地址。

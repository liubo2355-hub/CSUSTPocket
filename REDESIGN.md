# 掌上长理

“掌上长理”基于 CreaMakers 的校园产品体系重构：Android 客户端沿用 `CSUSTPocket` 的业务能力，通过其既有依赖接入 `CSUSTDataGet` 获取教务数据，并继续兼容 `changli-planet-backend` 提供的用户、工具与 Web 服务。

## 新的信息架构

- 顶部门户：悬浮分段导航提供概览、我的、教务系统、网络课程中心入口。
- 概览：双列卡片展示课表、成绩、电量、作业、考试与公告。
- 左侧目录：按教务、网课、校园工具和其他查询分组，可直接进入现有功能。
- 我的：采用账号管理、设置、帮助与支持三组列表式卡片。
- 已取消情报站：底部入口、导航目标、字符串资源和相关 Activity 注册均已移除。

## 视觉规范

- 主色：`#1697D5`；深色品牌色：`#173D78`；辅助薄荷色：`#74D7CA`。
- 页面背景：`#F5F5FA`；卡片：白色；正文：`#14213D`。
- 统一圆角、轻投影、小间距双列卡片和顶部悬浮导航。
- Material 3 全局色板、字阶、圆角和深色模式均已统一。

## 构建

```powershell
$env:ANDROID_HOME='C:\Users\Ivan\AppData\Local\Android\Sdk'
.\gradlew.bat :app:assembleDebug
```

生成文件位于 `app/build/outputs/apk/debug/CSUSTPocket_debug_v2.0.15.apk`。

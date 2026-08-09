# PearlWind_chargeAnalysisDeBug

**Release label:** `MC 1.21.5 - Fabric Loader 0.19.3 - v1.0.0`  
**Author:** RideAnt

PearlWind_chargeAnalysisDeBug is a singleplayer-only testing companion for PearlWind_chargeAnalysis. It provides controlled P1/P2 camera locking, deterministic projectile-spread testing, and optional diagnostic logging for repeatable trajectory experiments.

这是 PearlWind_chargeAnalysis 的单人调试附属模组，用于建立可重复的珍珠与风弹轨迹测试环境。它不是主模组的必需组件，也不是多人战斗工具。

## 功能

- 将玩家视角锁定到主模组提供的有效 P1/P2 解析目标。
- 按 `` ` / ~ `` 在有效珍珠目标之间切换。
- 可选移除原版投射物随机散布，便于重复实验。
- 可选写入诊断日志。
- 所有 Debug 功能默认关闭。

## 主模组关系

必须同时安装 `PearlWind_chargeAnalysis`。

附属模组不会锁定主模组的具体迭代版本，元数据使用通配依赖；两者仍需处于兼容的游戏与 Fabric 环境。主模组可以单独运行，附属模组不能脱离主模组运行。

## 必需依赖

- Fabric API
- Cloth Config API

## 可选依赖

- Mod Menu（用于打开图形设置页）

## 安装

1. 安装 PearlWind_chargeAnalysis 主模组。
2. 将本附属 JAR 和上述依赖放入同一个 `mods` 文件夹。
3. 安装 Mod Menu 时，可通过其页面打开 `PearlWind_chargeAnalysisDeBug` 设置。
4. 只启用当前实验需要的功能。

普通游戏只安装不带 `sources` 的 JAR。`sources.jar` 只用于阅读和调试源码。

## 自动视角锁定

官方版本只允许在单人集成服务器中使用自动视角锁定。启用后：

1. 投掷末影珍珠并手持风弹。
2. 按 `` ` / ~ `` 锁定 P1、P2 或关闭锁定。
3. 当主模组没有有效解析结果时，附属不会改变玩家视角。

该功能只修改本地玩家视角，不会自动投掷物品。

## 随机偏移测试

“移除投射物随机偏移”会将原版 `ProjectileEntity#setVelocity(..., uncertainty)` 使用的随机散布参数改为零。

- 仅在单人世界的逻辑服务端生效。
- 不会修改远程多人服务器或独立服务器上的真实投射物行为。
- 会影响所有经过该原版发射逻辑的投射物，不只限于珍珠和风弹。
- 不改变投掷速度、速度继承、阻力、重力、碰撞或爆炸规则。

## 配置迁移与冲突

首次运行且新配置不存在时，会尝试读取旧 `projectileaccuracytest.properties` 和主模组旧版 Debug 设置作为迁移初值。旧配置不会被删除。

不要与旧 `PearlWind_ProjectileAccuracyTest` / `projectileaccuracytest` 同时安装。两个项目修改相同的随机散布逻辑，并已在元数据中声明互斥。

## 使用边界

- 所有功能默认关闭。
- 官方版本的自动视角锁定与随机偏移测试均限定在单人环境。
- 不提供多人服务器上的自动瞄准或投射物修改。
- 不自动投掷物品，不强制碰撞，不触发珍珠传送，也不改变风弹爆炸。

## 构建

Windows：

```bat
.\gradlew.bat build
```

Linux/macOS：

```bash
./gradlew build
```

生成文件位于：

```text
build/libs/PearlWind_chargeAnalysisDeBug-1.0.0.jar
build/libs/PearlWind_chargeAnalysisDeBug-1.0.0-sources.jar
```

## License

MIT License. See `LICENSE` for details.

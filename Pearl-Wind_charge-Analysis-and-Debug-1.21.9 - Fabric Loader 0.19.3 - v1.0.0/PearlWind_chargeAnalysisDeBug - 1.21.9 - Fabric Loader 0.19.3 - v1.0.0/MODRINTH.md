# Modrinth publication text

## Version

- Version name: `MC 1.21.9 - Fabric Loader 0.19.3 - v1.0.0`
- Version number: `1.0.0`
- Release channel: `Beta`

## Summary

```text
Singleplayer-only debug companion for PearlWind_chargeAnalysis, with P1/P2 camera locking, deterministic projectile-spread testing, and optional diagnostic logs.
```

## Description

```markdown
# PearlWind_chargeAnalysisDeBug

PearlWind_chargeAnalysisDeBug is a singleplayer-only testing companion for PearlWind_chargeAnalysis. It provides controlled camera locking, deterministic projectile-spread testing, and diagnostic logging for repeatable ender-pearl and wind-charge experiments.

## Features

- Locks the camera to a valid P1 or P2 trajectory target.
- Cycles between available pearl targets with the <kbd>`</kbd> / <kbd>~</kbd> key.
- Optionally removes vanilla projectile random spread for repeatable tests.
- Provides optional diagnostic logging.
- Keeps all Debug features disabled by default.
- Uses trajectory results calculated by the main mod.

## Why use it?

Random projectile spread can make repeated trajectory experiments difficult to compare. This companion provides a controlled singleplayer environment for reproducing tests, checking calculated angles, and diagnosing differences between predicted and observed projectile movement.

## Singleplayer restriction

The official build only enables automatic camera locking in an integrated singleplayer world.

Projectile-spread removal only affects the logical server inside a singleplayer session. It does not change projectile behavior on remote multiplayer or dedicated servers.

The mod does not automatically throw items, change projectile speed, remove gravity, alter drag, force collisions, trigger pearl teleportation, or modify wind-charge explosions.

## Required main mod

PearlWind_chargeAnalysis must be installed. The dependency does not pin a specific iteration of the main mod, although both projects must use a compatible game and Fabric environment.

## Supporting dependencies

- Fabric API
- Cloth Config API
- Mod Menu

## Usage

1. Install PearlWind_chargeAnalysis and this Debug companion.
2. Open the Debug companion settings through Mod Menu.
3. Enable only the testing features you need.
4. Throw an ender pearl and hold a wind charge.
5. Press <kbd>`</kbd> / <kbd>~</kbd> to lock onto or cycle between valid P1/P2 targets.
6. Disable the feature when the test is complete.

## Compatibility notice

Do not install this companion together with the old PearlWind_ProjectileAccuracyTest mod. Both projects modify the same projectile-spread behavior and are declared incompatible.

This project is intended for singleplayer testing and development diagnostics. It is not a multiplayer combat tool.

---

# 中文说明

PearlWind_chargeAnalysisDeBug 是 PearlWind_chargeAnalysis 的单人调试附属模组，用于建立可重复的末影珍珠与风弹轨迹测试环境。

## 功能

- 将玩家视角锁定到有效的 P1/P2 解析目标。
- 按 <kbd>`</kbd> / <kbd>~</kbd> 在有效珍珠目标之间切换。
- 可选移除原版投射物随机散布。
- 可选写入诊断日志。
- 所有 Debug 功能默认关闭。

## 单人限制

官方版本只允许在单人集成服务器中使用自动视角锁定。随机散布移除也只作用于单人世界的逻辑服务端，不会修改远程多人服务器或独立服务器上的真实投射物行为。

本附属不会自动投掷物品，也不会修改投射物速度、重力、阻力、碰撞、珍珠传送或风弹爆炸规则。

## 安装关系

必须同时安装 PearlWind_chargeAnalysis。附属不限制主模组的具体迭代版本，但两者必须处于兼容的游戏与 Fabric 环境。

不要与旧版 PearlWind_ProjectileAccuracyTest 同时安装。
```

## Version changelog

```markdown
Initial public test build.

- Added singleplayer-only P1/P2 camera locking.
- Added deterministic projectile-spread testing.
- Added optional diagnostic logging.
- Kept every Debug feature disabled by default.
- Required PearlWind_chargeAnalysis without pinning a specific main-mod iteration.
- Declared incompatibility with the old PearlWind_ProjectileAccuracyTest mod.
```

## Gallery titles

- `Debug configuration screen`
- `P1/P2 camera lock test`
- `Deterministic projectile-spread test`

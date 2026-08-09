# PearlWind_chargeAnalysis

**Release label:** `MC 1.21.10 - Fabric Loader 0.19.3 - v1.0.0`  
**Author:** RideAnt

PearlWind_chargeAnalysis is a client-side Fabric HUD mod that analyzes the three-dimensional trajectories of ender pearls and wind charges. It tracks up to two active pearls, calculates recommended wind-charge angles, and presents the result through a compact HUD and configurable visual guides.

这是一个用于分析末影珍珠与风弹三维轨迹的客户端 HUD 模组，适合轨迹实验、移动技巧练习和技术测试。

## 主要功能

- 按出现顺序追踪最先生成的两颗飞行珍珠，并标记为 P1、P2。
- 显示珍珠投掷角、飞行时间、推荐风弹仰角及当前角度差。
- 支持定点解析与动点解析。
- 支持独立角轴与真实透视投影两种辅助线模式。
- P1/P2 辅助目标接近时合并显示，避免相互覆盖。
- 支持推荐仰角范围、最大检索范围、时序补偿和路径碰撞验证设置。
- 提供简体中文和英文界面文本。

## 必需依赖

- Fabric API
- Cloth Config API

## 可选依赖

- Mod Menu（用于打开图形设置页）

## 安装

1. 将主模组 JAR 和必需依赖放入同一个 `mods` 文件夹。
2. 启动游戏并进入世界。
3. 安装 Mod Menu 时，可通过其页面打开设置。

普通游戏只安装不带 `sources` 的 JAR。`sources.jar` 用于阅读和调试源码，不应放入游戏的 `mods` 文件夹。

## 基本操作

1. 投掷末影珍珠。
2. 在珍珠仍处于飞行状态时手持风弹。
3. 根据 HUD 查看 P1/P2 推荐角度和当前角度差。
4. 按 `O` 切换定点解析与动点解析。
5. 在 Mod Menu 中调整辅助线、角度范围、时序补偿和路径验证。

## HUD 说明

定点解析：

```text
Wn beta: xx.xx deg
Pn beta: xx.xx deg
delta beta: xx.xx deg  delta gamma: xx.xx deg
```

动点解析：

```text
Wn beta: xx.xx deg  gamma: xx.xx deg
Pn beta: xx.xx deg  gamma: xx.xx deg
delta beta: xx.xx deg  delta gamma: xx.xx deg
```

- `Wn`：玩家当前风弹瞄准角，编号与当前目标珍珠一致。
- `Pn`：第 n 颗珍珠对应的推荐风弹角。
- `beta`：仰角。
- `gamma`：Minecraft 世界水平偏航角。
- `delta beta`、`delta gamma`：当前角度与推荐角度的差值。
- `READY`：两个角度差均进入就绪阈值。

## 解析模式

### 定点解析

用于玩家仍接近珍珠投掷点的情况。玩家距离投掷点超过 `0.8 block` 后，不再把该珍珠视为有效定点目标。

### 动点解析

从玩家当前头部参考点求解，并考虑玩家当前运动。地面状态继承水平速度，空中状态同时考虑垂直速度。

玩家头部参考点定义为当前碰撞箱水平中心，以及碰撞箱顶部向下 `0.25 block` 的位置。

## 辅助线

- **独立角轴**：竖线只随偏航角左右移动，横线只随仰角上下移动。
- **真实透视投影**：将完整三维目标方向投影到画面，两个角度可能产生透视耦合。

辅助线最多处理 P1/P2。两个目标相距不超过 5 像素时会合并显示为 `P1/P2`。

## 设置边界

“推荐仰角范围”和“最大检索范围”是不同设置：

- 推荐仰角范围决定一个数学解是否允许作为最终推荐结果，合法边界为 `-90 deg` 到 `90 deg`。
- 最大检索范围决定 HUD 和辅助线允许显示多大的当前角度差。

当推荐仰角超出允许范围时，手持风弹只显示珍珠相关信息，不显示推荐瞄准结果。

## 轨迹与碰撞限制

- 模组始终采用最早数学相遇解，不会在最早解失效后搜索更晚解。
- 珍珠或风弹预测路径经过流体时，结果会被判定为无法解析。
- 路径碰撞验证默认关闭；开启后，预计撞击方块的路径不会作为有效推荐结果。
- 计算结果表示预测的几何轨迹，不代表原版一定生成投射物碰撞事件。
- 主模组不会自动转动视角、投掷物品、修改投射物速度、触发传送或改变爆炸规则。

部分多人服务器可能禁止轨迹或瞄准信息辅助。请在多人环境使用前确认服务器规则。

## Debug 附属

自动视角锁定、随机偏移测试和调试日志由独立项目 `PearlWind_chargeAnalysisDeBug` 提供，不包含在主模组中。普通使用不需要安装 Debug 附属。

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
build/libs/PearlWind_chargeAnalysis-1.0.0.jar
build/libs/PearlWind_chargeAnalysis-1.0.0-sources.jar
```

## License

MIT License. See `LICENSE` for details.

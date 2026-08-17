# Vanish++

<div align="center">

**现代 Paper/Folia 服务器管理员隐身插件的绝对标准。**

[**English**](README.md) | [**中文**](README_zh-CN.md)

[![Modrinth](https://img.shields.io/modrinth/v/kbKpK1bc?label=Modrinth&logo=modrinth)](https://modrinth.com/plugin/vanish++)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/kbKpK1bc?logo=modrinth)](https://modrinth.com/plugin/vanish++)
[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://adoptium.net/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.6--26.2-brightgreen)](https://www.minecraft.net/)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

</div>

> ### ⚠️ 分支状态 — 请先阅读
>
> 本仓库是 Vanish++ 的**多语言分支**，目前**仅维护 `vanishpp-paper` 模块**：
>
> - `vanishpp-velocity` 模块与根目录的 `src/` 目录保持原样保留，本分支**不维护、不修改**它们。
> - 所有改动——多语言支持（`languages/messages_*.yml`）、GUI 修复、静默箱子修复等——都限定在 `vanishpp-paper` 模块内。
> - **仅在 Purpur 1.21.11 build 2558 上完成测试。** 其他 Paper 系版本应当可用，但**未经本分支验证**。

---

Vanish++ 让其他隐身插件显得过时。它专为现代 **Paper** 服务器打造，通过数据包拦截、原生物理操控和深度 API 钩子，让你做到**数学上无法被察觉**——同时**优先支持 Folia**，也兼容 Purpur、Spigot 和 Bukkit。

开箱即用，无需任何配置。

---

## 功能特性

<details>
<summary><b>真正的不存在感（物理引擎）</b></summary>
<br>

大多数插件只是视觉上隐藏你。Vanish++ 将你从物理层面移除。

- **无敌：** 不受伤害、免疫所有药水效果、不会燃烧。
- **智能怪物 AI：** 怪物完全无视隐身玩家。在怪物锁定攻击路径之前，`EntityTargetEvent` 就已取消目标锁定。已锁定你的怪物会立即强制解除锁定。
- **弹射物穿透：** 箭、三叉戟、雪球通过原生 Paper 事件物理穿透你的身体——无法击中隐身玩家。
- **零碰撞：** 你推不动玩家、怪物或船，它们也推不动你。
- **无物理触发：** 踩过海龟蛋、作物、压力板、绊线、幽匿感测体，不触发任何振动。
- **阻止袭击：** 观看村庄时不会触发不祥之兆袭击。

</details>

<details>
<summary><b>深层协议隐身（数据包级）</b></summary>
<br>

直接挂钩服务器协议，从客户端抹除你的存在。*（需要 ProtocolLib）*

- **彻底的 Tab 补全清除：** 在聊天、原版命令或插件命令中补全你的名字，不会返回任何结果。
- **服务器列表隐藏：** 玩家数量经过数学调整。若只有你在线，服务器显示 0/20。
- **管理员幽灵视图：** 有权限的管理员在 Tab 列表中看到隐身玩家为灰色斜体的旁观者。
- **管理员发光指示：** 隐身玩家以发光轮廓渲染，仅管理员可见——在数据包层面注入。
- **Dynmap 与 EssentialsX 钩子：** 自动将你从网页地图和 `/who`、`/list`、`/online` 中隐藏。

</details>

<details>
<summary><b>沉浸感与兼容性</b></summary>
<br>

- **原生语言假消息：** 假的"玩家离开了游戏"消息使用服务器原生翻译数据包——在任何语言下都与真实断线无法区分。
- **原生 TAB 插件支持：** 直接挂钩 TAB（NEZNAMY 出品），自动显示你的隐身前缀。
- **旧插件兼容：** 设置标准 Bukkit Metadata（`vanished`），CMI、TAB 和自定义脚本自动尊重你的隐身状态。
- **静默箱子：** 静默打开容器——无动画、无声音、完整物品交互。关闭时同步变更。
- **DiscordSRV 集成：** 抑制 Discord 上的加入、退出、进度和死亡播报。假消息遵循 DiscordSRV 完整的嵌入、颜色、头像和 webhook 配置。
- **Simple Voice Chat 集成：** 隐身时自动在语音聊天中隔离你。
- **智能物品拾取：** 通过 `/vanishpickup` 切换物品拾取。

</details>

<details>
<summary><b>精细控制与安全</b></summary>
<br>

- **旁观快速切换：** 隐身时双击 Shift 立即进入旁观模式。需要 `vanishpp.spectator`。
- **隐身计分板（`/vscoreboard`）：** 完全可配置的侧边栏计分板，显示世界、TPS、玩家数、实时坐标、方向、生物群系、延迟、生命值、饥饿值、护甲、时间、日期等。`%time%`/`%date%` 遵循 `scoreboards.yml` 中的 `timezone`（IANA ID 或 `"default"` 使用服务器时间）和 `timezone-offset-hours`。坐标通过 ProtocolLib 数据包监听在移动时刷新。支持所有内置占位符以及完整的 PlaceholderAPI。
- **实时配置编辑器（`/vconfig`）：** 在游戏内直接编辑 `config.yml` 中的任何设置。
- **个人规则系统（`/vrules`）：** 针对每个玩家的开关，控制方块破坏、实体交互、聊天确认、物品拾取、怪物目标锁定等。
- **异步数据持久化：** 所有数据异步保存。状态跨重启保留。
- **数据库连接监控：** 数据库连接失败时在游戏内通知管理员。
- **原生 Velocity 代理插件：** 配套的 `vanishpp-velocity` 插件提供所有 Paper 服务器与 Velocity 之间的专用实时消息通道。隐身状态、配置更改和 `/vanishreload` 即时全网传播。定时规则过期通知会投递到玩家当前所在的服务器——无需重新连接。服务器启动时自动检测代理，未发现则回退到独立模式。参见[代理集成指南](PROXY_INTEGRATION_GUIDE.md)。

</details>

---

## 命令

| 命令 | 别名 | 说明 | 权限 |
| :--- | :--- | :--- | :--- |
| `/vhelp [command]` | `/vanishhelp` | 交互式帮助菜单与指南 | *（无）* |
| `/vanish [player]` | `/v`、`/sv` | 切换隐身状态 | `vanishpp.vanish` |
| `/vrules [player] <rule> [val]` | `/vanishrules` | 配置个人规则 | `vanishpp.rules` |
| `/vconfig <key> [val]` | `/vanishconfig` | 实时编辑配置 | `vanishpp.config` |
| `/vperms` | — | 无需权限插件管理权限 | `vanishpp.manageperms` |
| `/vlist` | `/vanishlist` | 隐身玩家交互式列表 | `vanishpp.list` |
| `/vignore [player]` | `/vanishignore` | 切换启动警告 | `vanishpp.ignorewarning` |
| `/vchat confirm` | `/vanishchat` | 确认聊天消息（若安全功能开启） | `vanishpp.chat` |
| `/vreload` | `/vanishreload` | 重载配置并重新同步所有隐身效果 | `vanishpp.reload` |
| `/vscoreboard` | — | 切换隐身侧边栏计分板 | `vanishpp.scoreboard` |
| `/vspec <player\|stop>` | — | 快速旁观某玩家。`/vspec stop` 返回。 | `vanishpp.spec` |
| `/vfollow <player\|stop>` | — | 锁定镜头静默跟随玩家。 | `vanishpp.follow` |
| `/vhistory [player]` | — | 查看隐身/取消隐身审计日志。 | `vanishpp.history` |
| `/vautovanish [player]` | — | 切换玩家加入时自动隐身。 | `vanishpp.autovanish` |
| `/vstats [player]` | — | 查看隐身时长统计。 | `vanishpp.stats` |
| `/vadmin` | — | 游戏内隐身概览仪表盘 GUI。 | `vanishpp.admin` |
| `/vwand` | — | 给予隐身魔杖（烈焰棒切换物品）。 | `vanishpp.wand` |
| `/vzone <create\|delete\|list\|reload>` | — | 管理禁止隐身区域。 | `vanishpp.zone` |
| `/vincognito [player] [fakename]` | — | 启用/禁用假名模式。 | `vanishpp.incognito` |

---

## PlaceholderAPI

| 占位符 | 示例 | 说明 |
| :--- | :--- | :--- |
| `%vanishpp_is_vanished%` | `是` / `否` | 当前状态文本 |
| `%vanishpp_is_vanished_bool%` | `true` / `false` | 布尔状态 |
| `%vanishpp_vanished_count%` | `3` | 在线隐身玩家数 |
| `%vanishpp_visible_online%` | `15` | 总数减去隐身（假数量） |
| `%vanishpp_prefix%` | `[已隐身]` | 已配置前缀（可见时为空） |
| `%vanishpp_pickup%` | `已启用` | 当前物品拾取状态 |
| `%vanishpp_vanished_list%` | `Notch, Herobrine` | 在线隐身玩家名列表 |
| `%vanishpp_visible_player_list%` | `Steve, Alex` | 在线非隐身（可见）玩家名列表 |

---

## 个人规则（`/vrules`）

| 规则 | 默认 | 说明 |
| :--- | :--- | :--- |
| `can_break_blocks` | `false` | 隐身时允许破坏方块 |
| `can_place_blocks` | `false` | 隐身时允许放置方块 |
| `can_interact` | `true` | 允许交互（箱子、按钮） |
| `can_hit_entities` | `false` | 允许攻击玩家/怪物 |
| `can_pickup_items` | `false` | 允许拾取物品 |
| `can_drop_items` | `false` | 允许从物品栏丢弃物品 |
| `can_chat` | `false` | 需要确认才能说话 |
| `can_trigger_physical` | `false` | 压力板、作物等 |
| `can_throw` | `false` | 投掷物品、射弓 |
| `mob_targeting` | `false` | 怪物无视你 |
| `spectator_gamemode` | `true` | 双击 Shift 进入旁观 |
| `show_notifications` | `true` | 行动被阻止的警告 |

---

## 要求与兼容性

**要求：**
- Java 21
- Paper 1.20.6+（或兼容的分支）
- ProtocolLib 5.3.0+ *（强烈推荐——隐身特性必需）*

**支持的平台：**

| 平台 | 状态 | 说明 |
| :--- | :--- | :--- |
| **Paper** | 推荐 | 完整功能支持 |
| **Purpur** | 支持 | Paper 分支，完全兼容 |
| **Folia** | 支持 | 多区域调度桥接，自动检测 |
| **Spigot** | 兼容 | 无 Paper API 时物理/弹射物特性降级 |
| **Bukkit** | 兼容 | 与 Spigot 相同的限制 |

**支持的版本：** Minecraft 1.20.6 — 26.2

> **⚠️ 本分支的测试平台：** **Purpur 1.21.11 build 2558** — `vanishpp-paper` 模块仅在此确切的服务端构建上编译和测试。与其他 Paper 系版本的兼容性预期可用，但未经本分支验证。

**可选集成：** TAB（NEZNAMY）、PlaceholderAPI、Dynmap、EssentialsX、DiscordSRV、Simple Voice Chat

**存储选项：** YAML（默认）、MySQL 5.7+、PostgreSQL 12+、Redis（跨服同步）

---

## 安装

1. 从 [Modrinth](https://modrinth.com/plugin/vanish++) 下载最新版本
2. 将 `vanishpp-x.x.x.jar` 放入服务器的 `plugins/` 文件夹
3. 启动或重启服务器
4. *（可选）* 安装 ProtocolLib 以获得完整隐身特性支持

无需配置即可开始。在游戏内运行 `/vhelp` 探索。

---

## 从源码构建

```bash
# 克隆仓库
git clone https://github.com/TheCommandCraft/Vanishpp.git
cd Vanishpp

# 构建（需要 Java 21 和 Maven）
mvn clean package -DskipTests

# 输出 JAR
ls target/vanishpp-*.jar
```

---

## 贡献

欢迎提交 Pull Request。对于重大变更，请先开启 issue 讨论你想修改的内容。

提交 PR 前，请确保完整测试套件通过：

```bash
mvn clean verify
```

---

## 许可

本项目以 [GNU General Public License v3.0](LICENSE) 许可发布。

作为 Spigot/Bukkit 插件，GPL v3 是遵守 Bukkit API 许可所必需的。

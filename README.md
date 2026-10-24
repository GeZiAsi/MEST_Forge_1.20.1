<div align="center">

# ME 拼接终端 · MEST

**可拖拽、可拼接的 AE2 无线终端 —— Forge 1.20.1 移植版，适配 GTL 整合包**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-62B47A?style=flat-square)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.1%2B-DF8E2B?style=flat-square)](https://files.minecraftforge.net/)
[![AE2](https://img.shields.io/badge/AE2-15.x-6E5BA8?style=flat-square)](https://github.com/AppliedEnergistics/Applied-Energistics-2)
[![Release](https://img.shields.io/github/v/release/GeZiAsi/MEST_Forge_1.20.1?style=flat-square&label=%E4%B8%8B%E8%BD%BD)](https://github.com/GeZiAsi/MEST_Forge_1.20.1/releases)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](#许可证)

[下载](https://github.com/GeZiAsi/MEST_Forge_1.20.1/releases) ·
[问题反馈](https://github.com/GeZiAsi/MEST_Forge_1.20.1/issues) ·
[原版仓库](https://github.com/lhy512103/MEST) ·
[English](#english)

</div>

| | |
| --- | --- |
| **原作者** | [lhy512103](https://github.com/lhy512103) — 原版仓库（NeoForge 1.21.1）：<https://github.com/lhy512103/MEST> |
| **移植作者** | [GeZiAsi](https://github.com/GeZiAsi)（格兹亚西） — 移植版仓库：<https://github.com/GeZiAsi/MEST_Forge_1.20.1> |
| **游戏版本** | Minecraft 1.20.1 · Forge 47.1 及以上 |
| **辅助开发** | 代码由 Claude（Anthropic）辅助编写 |
| **许可证** | MIT，保留原作者版权声明（全文见[文末](#许可证)） |

ME 拼接终端把存储、合成、样板和工具放进同一个界面：ME 列表、合成台、样板编码、样板管理、垃圾桶、工具包……
每个模块都是一个面板，可以随意移动、缩放，像拼积木一样拼成你想要的工作台。

## 目录

- [安装](#安装)
- [获取终端](#获取终端)
- [模块](#模块)
- [布局](#布局)
- [扩展快捷栏与工具包](#扩展快捷栏与工具包)
- [面板快捷键](#面板快捷键)
- [JEI 联动](#jei-联动)
- [GTL 兼容](#gtl-兼容)
- [配置文件](#配置文件)
- [与原版的差异](#与原版的差异)
- [为其他模组添加模块](#为其他模组添加模块)
- [构建](#构建)
- [更新日志](#更新日志)
- [致谢](#致谢)
- [许可证](#许可证)

## 安装

| 组件 | 版本 | 是否必需 |
| --- | --- | :---: |
| Minecraft / Forge | 1.20.1 / 47.1+ | 必需 |
| Applied Energistics 2 | 15.x（按 GTL 使用的 15.4.10 开发） | 必需 |
| AE2WTLib | 15.x | 必需 |
| JEI | 15.19 – 15.49 | 可选 |
| ExtendedAE | 1.20-1.4.x | 可选 |
| gtlcore（GTL） | 1.2.2+；精确数量显示需要 1.2.3+ | 可选 |

1. 从 [Releases](https://github.com/GeZiAsi/MEST_Forge_1.20.1/releases) 下载 jar。
2. 放进整合包的 `mods` 目录。
3. 替换旧版本时，先删掉旧的 `mesplicedterminal-*.jar`，两个同时存在会报模组重复。

## 获取终端

- **锻造台升级**：工程处理器（模板）+ AE2WTLib 无线通用终端 + 合成单元 → ME 拼接终端，原终端的绑定、能量和升级卡都会保留。
- **终端合并**：和其他 AE2WTLib 终端一样，可以在合成台里与无线通用终端合并。
- 它就是一个普通的 AE2WTLib 终端：在无线访问点绑定，放在背包或 Curios 槽里，用快捷键打开（按键设置里的"打开 ME 拼接终端"）。
- 升级卡栏的格数跟随 AE2WTLib 报告的容量，拿不到时默认 3 格。

## 模块

| 模块 | 作用 |
| --- | --- |
| ME 列表 | AE2 物品列表：排序、显示模式、视图元件、仅可合成、合成置顶、中键自动合成、手持容器灌装/倒出、Shift+滚轮单个存取 |
| 合成 / 合成终端 | 3×3 合成台，可分别清空到网络或背包；合成终端额外有盔甲槽和人物预览 |
| 样板编码 | 合成、处理、锻造、切石四种模式，替代、处理数量编辑、输出轮换与倍率缩放 |
| 样板管理 | 按 ExtendedAE 方式分组的样板供应器，可直接交换槽位，并远程打开供应器旁的机器 |
| 样板缓存 / 垃圾桶 / 背包 | 额外的样板存放区、垃圾桶和玩家背包 |
| 工具包 | 只放不可堆叠物品，支持记忆槽，驱动扩展快捷栏 |
| 网络工具包 | 终端自带网络工具的升级卡槽 |
| 无线设置 | 选取方块、补货、磁铁、工具包相关开关 |
| 面板快捷键 | 用组合键开关模块、切换布局预设 |

## 布局

- **拼接**：每个窗口是一棵分割树。把面板拖到另一个面板的边缘即可拼接；窗口里有富余空间时，拖入的模块会直接填进空位。
- **调整与拆分**：拖动分隔线调整比例；按住 Alt 拖动某一栏的标题可把它拆出来；按住 Ctrl 拖动只移动当前抓住的拼接窗口。
- **锚定与悬浮**：锚定窗口跟随终端并挂载工具栏，标题栏右上角的悬浮按钮可让任意拼接组合整体悬浮；悬浮窗口还可以钉在最上层。始终至少保留一个可见的锚定窗口。
- **组合模块**：在布局编辑器里，悬浮窗口标题栏的按钮可把它设为组合模块 1、2 或 3，之后可在更多设置栏和面板快捷键里整体显示或隐藏。
- **布局编辑器**：全屏编辑，带模块面板、属性面板、三个可命名预设、撤销、重置、导入导出（分享文件夹 / zip / 剪贴板）与备份。
- **小屏幕**：屏幕放不下时，面板不会被压到最小尺寸以下而互相重叠；这时可以把 ME 列表调矮，或把部分模块拆成悬浮窗口。

## 扩展快捷栏与工具包

- 打开工具包快捷栏后，原版快捷栏两侧各多一条 9 格栏，用滚轮或专用按键切过去，原版快捷栏本身不动。
- 工具包属于玩家而不是终端：死亡不掉落，终端丢了东西也还在，只是要带着终端才能使用。
- 记忆槽会把捡起的同类工具自动放回原位。

## 面板快捷键

给任意模块、"切换布局预设"以及预设 1–3 绑定组合键（Ctrl / Shift / Alt + 按键）：左键点按钮再按组合键即可绑定，右键清除。
关闭"仅终端"后，在世界中按下也会先打开终端再执行。

## JEI 联动

打开拼接终端后查看任意 JEI 配方，每个配方右侧会多出两个按钮：

| 按钮 | 作用 |
| --- | --- |
| 工作台图标 | 在终端的合成网格里合成：从 ME 网络取料放进合成网格；按住 **Ctrl** 会自动下单合成缺少的材料。缺料时按钮会变色提示。 |
| 样板图标 | 把这个配方写进样板编码区（合成、锻造、切石、处理配方都支持）。 |

- 点击后自动回到终端。工作台按钮只出现在合成配方上。
- JEI 自带的"+"按钮仍然可用，目标是你最后点过的合成模块或编码模块。
- 无线设置里的"JEI/EMI 显示拉取物品按钮"可以关闭这两个按钮。
- 兼容 JEI 15.19 – 15.49。

## GTL 兼容

安装 gtlcore 时自动生效，没有 gtlcore 时完全不影响：

- **精确数量**：ME 列表显示 gtlcore 超出 long 上限的精确数量（需要 gtlcore 1.2.3+）。
- **通用电路优先**：编码时优先选择 GTL 的通用电路。
- **虚拟材料**：按住 Ctrl 转移 GT 配方时，机器不消耗的输入（电路、模具、透镜等）会编码为虚拟材料。

另外，与 Inventory Tweaks 一起使用时，会自动移除它加在终端上的整理按钮。

## 配置文件

| 文件 | 内容 |
| --- | --- |
| `config/mesplicedterminal/layout.json`、`presets.json` | 布局与预设（客户端）。原子写入，旧格式自动迁移，损坏的文件会被隔离而不是覆盖 |
| `config/mesplicedterminal/hotkeys.json` | 面板快捷键（客户端） |
| `serverconfig/mesplicedterminal-server.toml` | 样板缓存、垃圾桶、工具包、网络工具包的槽位数（按存档，同步给客户端）；放一份到 `defaultconfigs/` 可作为新存档默认值 |

## 与原版的差异

**新增**
- JEI 配方旁的"在终端合成 / 写入样板编码"按钮。
- GTL 兼容：精确数量、通用电路优先、虚拟材料。

**调整**
- 垃圾桶可以拼接进窗口；拖入的模块优先填进窗口里的空位。
- 屏幕放不下时不再压扁面板；置顶的网络工具面板不再挡住拼接位置；缩放手柄移到外置导轨的外角。

**未移植**（1.20.1 / GTL 中不存在或不适用）
- EMI、Some Useless Things、Lightning、导入导出、ClientSort 兼容。
- ExtendedAE Plus 的上传目标选择只保留了占位。

## 为其他模组添加模块

<details>
<summary>展开开发者说明</summary>

MEST 在 `com.lhy.mest.api` 提供两个 mod 总线事件。模块 ID 必须是小写，建议带命名空间（`yourmod:panel`），不能与内置模块重名，也不能以 `action:` 开头。

1. **菜单槽位（可选，两端都要）**：`RegisterMestModuleSlotsEvent`，在通用初始化阶段触发。每次打开终端菜单时，服务端和客户端各调用一次提供者，两边必须按相同顺序添加相同的槽位。附加模块的槽位排在所有内置槽位之后，附加模块之间按 ID 排序，所以两端槽位编号总是一致。槽位里的内容需要自己保存。
2. **面板（仅客户端）**：`RegisterMestModulesEvent`，在客户端初始化阶段触发。工厂返回一个 `ModulePanel`，其 `id()` 必须与注册的 ID 相同；返回 `null` 表示不添加。`context.slots()` 返回第 1 步添加的槽位。重写 `defaults()`、`icon()` / `renderIcon()` 可以控制模块第一次出现时的状态和按钮图标。

```java
// 通用（两端）
modBus.addListener((RegisterMestModuleSlotsEvent event) ->
        event.register("yourmod:spares", context -> {
            InternalInventory inv = SpareInventory.of(context.terminal());
            for (int i = 0; i < inv.size(); i++) {
                context.addSlot(new AppEngSlot(inv, i));
            }
        }));

// 仅客户端
modBus.addListener((RegisterMestModulesEvent event) ->
        event.register("yourmod:spares", context -> new SparesPanel(context.slots())));
```

面板在 `layoutSlots()` 里用 `placeSlot` 摆放槽位，不可见时用 `hideSlot` 隐藏。AE2 的 Shift 快速移动会把附加模块的槽位当作终端侧槽位；如果不希望这些槽位接收 Shift 点击，请在 `mayPlace` 里拒绝。附加模组被移除或新增时布局照常可用：不存在的模块会被丢弃，新模块会分配一个默认窗口。

</details>

## 构建

需要 **JDK 17**（项目路径含中文时，Gradle 本身也必须用 JDK 17 运行）。

```powershell
.\gradlew.bat build   # 产物在 build/libs/
.\gradlew.bat test    # 单元测试
```

<details>
<summary>自动化开发测试</summary>

`runClientDevtest` 会启动一个开发客户端：自动建测试世界、搭建 ME 网络、打开终端，逐项检查编码、JEI 转移、面板拼接等功能，并把截图保存到 `run/devtest/screenshots/`，日志里用 `MEST-DEVTEST` 过滤可以看到每项的 PASS / FAIL。

| 参数 | 作用 |
| --- | --- |
| `-PmestGtlMods=<GTL 实例的 mods 目录>` | 把本机 GTL 的依赖链加进开发环境 |
| `-PmestDevtestJei=<版本>` | 切换运行时的 JEI 版本，例如 `15.20.0.106` |
| `-PmestDevtestLayout=<目录>` | 从某份 `layout.json` / `presets.json` 的副本开始测试 |
| `-PmestDevtestWidth/Height/GuiScale` | 窗口尺寸与 GUI 缩放 |

</details>

## 更新日志

### v0.0.3.2-forge1.20.1

- 修复：左键点库存为 0 的可合成流体 / 气体，不会弹出下单数量界面。
- 修复：Shift + 左键点流体，装好的桶留在鼠标上而不是放进背包；流体点击改回与 AE2 原版终端一致。
- 修复：装了 Inventory Tweaks 时，在 ME 列表上按中键（默认整理键）被它拦下，无法中键下单合成。

### v0.0.3.1-forge1.20.1

- 修复：正在合成的物品在 ME 列表置顶行显示为黑块。
- 修复：鼠标上拿着的物品被上层浮动面板挡住，只露出数量。
- 修复：JEI 物品提示框被终端面板遮挡。
- 修复：布局编辑器内容微调时，变暗区域重叠处颜色加深。
- 修复：从 FTB 任务等界面查 JEI 配方后，点"在终端合成 / 写入样板编码"回到了任务界面而不是终端。
- 新增：左侧设置条可拖动成为独立窗口，拖回终端左边缘或双击底部抓手即吸附回去。
- 调整：jar 内附带 README（含 MIT 许可证全文）。

### v0.0.3.0-forge1.20.1 · 移植 by GeZiAsi

- 移植：NeoForge 1.21.1 → Forge 1.20.1（AE2 15、AE2WTLib 15、JEI 15.19–15.49），目标整合包 GTL。
- 新增：JEI 配方旁的"在终端合成"和"写入样板编码"按钮。
- 新增：GTL 兼容——gtlcore 精确数量显示、通用电路优先、Ctrl 转移时不消耗的输入编码为虚拟材料。
- 调整：垃圾桶可拼接；拼接时优先填入窗口内的富余空间；窗口不再把面板压到最小尺寸以下。
- 修复：滚动条条纹贴图、视图元件列错位、底部面板压到边框、置顶网络工具面板挡住拼接、缩放手柄被外置导轨遮挡、Inventory Tweaks 整理按钮乱飘。

<details>
<summary>原版更新记录（NeoForge 1.21.1，作者 lhy512103）</summary>

#### v0.0.3.0

1. 新增：模块 API。其他模组可以通过 `RegisterMestModulesEvent`（客户端面板）和 `RegisterMestModuleSlotsEvent`（菜单槽位，两端按相同顺序构建）为终端添加自己的模块。
2. 调整：内置模块也改为走同一套注册；默认是否显示、浮动、钉住、显示终端按钮以及图标都由面板自己声明。
3. 调整：悬浮与钉住改为窗口属性，任意面板拼接后都能整体悬浮；按住 Alt 拖动标题栏可从锚定窗口中拆出单个面板。
4. 新增：组合模块，可在更多设置栏和面板快捷键里整体显示或隐藏一个悬浮窗口。
5. 调整：按住 Ctrl 拖动时只移动当前抓住的拼接窗口。
6. 修复：组合模块按钮仅在窗口拼接后显示；点击拼接面板不再散开，且每窗只显示一个钉住按钮。

#### v0.0.2.8

1. 新增：样板编码面板可直接编码无用之物的万象样板。
2. 新增：无用之物加入编译与运行时依赖。
3. 修复：万象样板编码改为走 MEST 自己的编码动作。

#### v0.0.2.7

1. 修复：按键设置里打开终端的快捷键显示为中文名称。
2. 新增：面板快捷键模块，可开关各终端面板、轮换或直接切换布局预设。
3. 修复：在上层窗口上滚动时，下方面板不再跟着滚动。
4. 修复：拼接终端的名称不再显示为 item.minecraft.air。

#### v0.0.2.6

1. 新增：开发运行时加入 Some Useless Things 与 Sophisticated Backpacks。
2. 修复：将 Data Energistics 升到 3.2.2，避免 Useless Mod 合金炉配方预热崩溃，并补上所需的 LDLib2。
3. 修复：将 Sophisticated Storage 升到 1.5.91，对齐 Sophisticated Core 1.5.1。
4. 修复：玩家构造未完成时不再覆盖主手，避免进档空指针（服务端与客户端）。
5. 修复：扩展栏右键装备不再客户端预测，避免装备被复制。
6. 修复：样板编码槽改用 AE2 原版可合成查询，"+" 角标不再被方块材料挡住。
7. 修复：开发客户端改用 JEI 19.51 并暂时去掉 AE2 Utility，避免进档即崩。
8. 修复：没有手持原版网络工具时，内存卡会从终端自带网络工具面板的升级槽取放升级卡。
9. 修复：合成置顶按原版在关界面后回收，上限随 ME 列表列数变化。
10. 新增：非处理样板编码后自动上传到 ECO 样板库和闪电科技合成装配，插入前去重（走 PatternContainer 分组）。
11. 修复：服务端没装 ExtendedAE Plus 时，请求供应器列表不再崩服。
12. 修复：从网络取物放入工具包或垃圾桶改为扣能量，并按实际取出数量放入。
13. 修复：远程打开供应器旁的机器时遵守出生点保护和领地模组，不再触发拉杆、门和按钮。
14. 修复：网络工具的升级槽改为跟随工具本身；终端界面与扩展快捷栏共用同一份工具包库存，不再刷物品。
15. 调整：工具包、记忆槽和扩展栏设置改为跟随玩家，死亡不掉落；旧终端上的数据自动合并到玩家身上。
16. 修复：样板供应器列表在重建快照时溢出后不再永久为空；撤回上次上传不会再从别的供应器取错样板。
17. 修复：槽位数配置改为服务端配置并同步给客户端，专用服上菜单槽位不再错位。
18. 修复：layout.json 能正常读取时以它为准；取消布局编辑时一并撤掉编辑器里的撤销记录。
19. 修复：被上层窗口挡住的面板不能再被隔着拖动滚动条；网络工具面板的旧版位置迁移不再带走其他模块。
20. 调整：移除拉伸拼接和紧凑拼接，拼接窗口统一使用外壳拼接。
21. 修复：保存退出布局编辑器等菜单同步不再清空扩展快捷栏里的物品图标。

</details>

## 致谢

- [lhy512103](https://github.com/lhy512103) —— ME 拼接终端原作者。
- [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2)、[AE2WTLib](https://github.com/Mari023/AE2WirelessTerminalLibrary)（终端与图标，MIT，by mari023）、[ExtendedAE](https://github.com/GlodBlock/ExtendedAE)、[JEI](https://github.com/mezz/JustEnoughItems)。
- GTL（GregTech Leisure）整合包与 gtlcore。

## 许可证

本项目以 MIT 许可证发布。修改或再分发时请保留以下全部内容。

```text
MIT License

Copyright (c) 2026 lhy512103
Copyright (c) 2026 GeZiAsi (格兹亚西, Forge 1.20.1 port)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## English

**ME Spliced Terminal (MEST)** is a wireless terminal for Applied Energistics 2 and AE2WTLib that puts
storage, crafting, pattern work and your tools into one screen whose modules you can move, resize and
splice together. This is the **Forge 1.20.1 port** for the **GTL (GregTech Leisure)** modpack.

- **Original author:** [lhy512103](https://github.com/lhy512103) — original NeoForge 1.21.1 repository: <https://github.com/lhy512103/MEST>
- **Port:** [GeZiAsi](https://github.com/GeZiAsi) (格兹亚西) — <https://github.com/GeZiAsi/MEST_Forge_1.20.1>
- **Development:** code written with assistance from Claude (Anthropic).
- **Requires:** Minecraft 1.20.1, Forge 47.1+, AE2 15.x, AE2WTLib 15.x. Optional: JEI 15.19–15.49, ExtendedAE, gtlcore.
- **New in the port:** "craft in terminal" (Ctrl requests missing ingredients) and "encode pattern" buttons beside every JEI recipe; GTL support (gtlcore precise amounts, universal circuit preference, Ctrl virtual ingredients); a spliceable trash; layout fixes for small screens.
- **Not ported:** EMI, Some Useless Things, Lightning, import/export and ClientSort integrations.
- **Build:** JDK 17, `gradlew build`.
- **License:** MIT — see [the full text above](#许可证).

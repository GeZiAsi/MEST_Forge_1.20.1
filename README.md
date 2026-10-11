# ME Spliced Terminal — Forge 1.20.1 移植版

[中文](#forge-1201-移植版说明) | [English](#forge-1201-port)

> 本分支是 **ME 拼接终端（MEST）** 的 **Forge 1.20.1** 移植版，面向 **GTL（GregTech Leisure）** 整合包。
>
> - 原作者：**lhy512103** — 原版仓库（NeoForge 1.21.1）：<https://github.com/lhy512103/MEST>
> - 移植作者：**格兹亚西**（移植版源码仓库尚未发布）
> - 许可证：MIT（保留原作者版权声明，见 [LICENSE](LICENSE)）

## Forge 1.20.1 移植版说明

### 运行环境

| 组件 | 版本 |
| --- | --- |
| Minecraft / Forge | 1.20.1 / 47.1 及以上 |
| Applied Energistics 2 | 15.x（按 GTL 使用的 15.4.10 开发） |
| AE2WTLib | 15.x（15.3.3） |
| JEI（可选） | 15.19 – 15.49 |
| ExtendedAE（可选） | 1.20-1.4.x |
| GTL / gtlcore（可选） | 1.2.2 及以上；精确数量显示需要 1.2.3 及以上 |

把 `mesplicedterminal-*-forge1.20.1.jar` 放进整合包的 `mods` 目录即可。

### 与原版的差异

- **新增：JEI 配方按钮。** 打开拼接终端后查看 JEI 配方，每个配方右侧多两个按钮：工作台图标在终端合成网格里合成（按住 Ctrl 自动下单缺少的材料），样板图标写入样板编码。兼容 JEI 15.19–15.49。
- **新增：GTL 兼容。** ME 列表显示 gtlcore 超出 long 上限的精确数量；编码时优先通用电路；Ctrl 转移 GT 配方时把不消耗的输入（电路、模具等）编码为虚拟材料。未安装 gtlcore 时这些都不生效。
- **调整：** 垃圾桶可以拼接进窗口；窗口空间有富余时，拖入的模块直接填进空位；屏幕放不下时面板不再被压到最小尺寸以下而互相重叠；置顶的网络工具面板不再挡住拼接位置。
- **未移植（1.20.1 / GTL 中不存在或不适用）：** EMI、Some Useless Things、Lightning、导入导出总线面板、ClientSort 兼容；ExtendedAE Plus 的上传目标选择只保留了占位。
- 与 Inventory Tweaks 一起使用时，会自动移除它加在终端上的整理按钮。

### 构建

需要 JDK 17（项目路径含中文时，Gradle 也必须用 JDK 17 运行）。

```powershell
.\gradlew.bat build
```

产物在 `build/libs/`。`runClientDevtest` 会启动一个自动化开发客户端：建测试世界、打开终端、逐项检查并截图到 `run/devtest/screenshots/`；
`-PmestGtlMods=<GTL 实例的 mods 目录>` 会把本机 GTL 的依赖链加进开发环境，`-PmestDevtestJei=<版本>` 可切换 JEI 版本。

## Forge 1.20.1 port

This branch ports **ME Spliced Terminal (MEST)** to **Forge 1.20.1** for the **GTL (GregTech Leisure)** modpack.

- Original author: **lhy512103** — original NeoForge 1.21.1 repository: <https://github.com/lhy512103/MEST>
- Port: **格兹亚西** (the port's own repository is not published yet)
- License: MIT, original copyright kept (see [LICENSE](LICENSE)).

Requires Forge 47.1+, AE2 15.x and AE2WTLib 15.x; JEI 15.19–15.49, ExtendedAE and gtlcore are optional.
Additions over the original: craft-in-terminal and encode-pattern buttons beside JEI recipes, GTL
support (gtlcore precise amounts, universal circuit preference, Ctrl virtual ingredients), a
spliceable trash, and layout fixes for small screens. EMI, Some Useless Things, Lightning,
import/export and ClientSort integrations are not ported. Build with JDK 17: `gradlew build`.

---

> 以下是原版（NeoForge 1.21.1）的说明，大部分功能在本移植版中相同；涉及 EMI、NeoForge 等内容以上文的差异说明为准。
>
> The rest of this file is the original NeoForge 1.21.1 documentation; see the port notes above for differences.

[English](#english) | [中文](#中文)

## English

ME Spliced Terminal (MEST) is a wireless terminal for Applied Energistics 2 and AE2WTLib on
NeoForge 1.21.1. It puts storage, crafting, pattern work and your tools into one screen whose
modules you can move, resize and splice together however you like.

### Getting the terminal

- Craft it, or upgrade an AE2WTLib Wireless Universal Terminal with a smithing transform. The
  upgrade keeps the base terminal's link, energy and upgrade cards.
- It is a normal AE2WTLib terminal: link it at a wireless access point, carry it in the inventory
  or a Curios slot, and open it with its hotkey ("Open ME Spliced Terminal" in Controls).
- The upgrade column follows the capacity AE2WTLib reports and falls back to three slots.

### Modules

| Module | What it does |
| --- | --- |
| ME list | AE2's item repository: sorting, view modes, view cells, craftable-only, pinned crafts, pick-item autocrafting, held-container fill/empty and shift-wheel single-item moves |
| Crafting / Crafting terminal | The 3x3 grid, with separate clear-to-network and clear-to-inventory actions; the crafting terminal adds armor slots and the player preview |
| Pattern encoding | Crafting, processing, smithing and stonecutting modes, substitutions, processing amount editor, output cycling and scaling |
| Pattern access | Pattern providers grouped like ExtendedAE, with direct slot exchange and remote opening of the machine next to a provider |
| Pattern cache, trash, inventory | Extra pattern storage, a trash bin and your own inventory |
| Toolkit | Unstackable tools with remembered slots; drives the extended hotbar |
| Network toolkit | Upgrade cards for the terminal's built-in network tool |
| Wireless settings | Pick block, restock, magnet and toolkit options |
| Panel hotkeys | Key combos that toggle modules or switch layout presets |
| Provider picker | ExtendedAE Plus upload target selection (only with that mod) |

### Layout

- Every window is a tree of splits. Drag a panel onto another panel's edge to splice them, drag a
  divider to resize the sections, and drag a section's title out to detach it again.
- Spliced windows are drawn as one outer shell with thin section rules, so no seam can appear.
- Floating is a window property. Anchored windows move with the terminal and carry the toolbar;
  the float button in an anchored window's top-right title bar lets any spliced group float on its
  own. Floating windows can be pinned above the rest. Alt+drag a section title to take it out of an
  anchored window, Ctrl+drag to move only the spliced window you grab. At least one anchored window
  always stays visible.
- Combined modules: in the layout editor, the title-bar button of a floating window makes it
  combined module 1, 2 or 3 (the trash cannot join). The terminal's more-settings bar and the panel
  hotkeys then show or hide that whole window like a single module.
- The full-screen layout editor adds a module palette, a property inspector, three named presets,
  undo, reset, share-folder/zip/clipboard import and export, and a backup.
- The layout is a client-side preference in `config/mesplicedterminal/` (`layout.json`,
  `presets.json`). Files are written atomically, older formats are migrated on load and a corrupt
  file is quarantined instead of overwritten.

### Extended hotbar and toolkit

- With the toolkit bar on, two 9-cell bars sit beside the vanilla hotbar. Scroll or use the bar
  keys to move onto them; the vanilla hotbar itself never moves.
- The toolkit belongs to the player, not the terminal: it survives death and losing the terminal.
  A carried terminal is only needed to use it. Toolkit data from older terminals is merged into
  the player automatically.
- Remembered slots pull matching tools back in when you pick them up.

### Panel hotkeys

Bind a combo (Ctrl / Shift / Alt + key) to any module, to "cycle layout preset" or to presets 1-3.
Left-click a key button and press the combo, right-click to clear. With "Terminal only" off, the
combo also works in the world by opening the terminal first. Bindings are stored in
`config/mesplicedterminal/hotkeys.json`.

### Server configuration

Slot counts (pattern cache, trash, toolkit, network toolkit) are a server config, synced to
clients so both sides build the same menu. It lives per world in
`serverconfig/mesplicedterminal-server.toml`; put a copy in `defaultconfigs/` to preset new worlds.

### JEI and EMI

Recipe transfer targets the last focused crafting or encoding module. Crafting recipes use AE2's
crafting transfer when crafting is focused; with the encoder focused, crafting, smithing,
stonecutting and processing recipes fill the matching encoding mode.

### Adding modules from another mod

MEST exposes two mod-bus events in `com.lhy.mest.api`. Module ids must be lowercase, should be
namespaced (`yourmod:panel`), may not reuse a built-in id and may not start with `action:`.

1. **Menu slots (optional, both sides)** - `RegisterMestModuleSlotsEvent`, fired during common
   setup. The provider runs once per terminal menu on the server and on the client and must add
   the same slots in the same order. Add-on slots come after all built-in slots, and add-ons are
   ordered by id, so indices always match. Store the slot contents yourself, for example in a data
   component on `context.terminal()` or a player attachment.
2. **Panel (client)** - `RegisterMestModulesEvent`, fired during client setup. The factory returns
   a `ModulePanel` whose `id()` equals the registered id, or `null` to skip. `context.slots()`
   returns the slots from step 1. Override `defaults()`, `icon()` / `renderIcon()` to control how
   the module first appears and how its button looks.

```java
// Common (both sides)
modBus.addListener((RegisterMestModuleSlotsEvent event) ->
        event.register("yourmod:spares", context -> {
            InternalInventory inv = SpareInventory.of(context.terminal());
            for (int i = 0; i < inv.size(); i++) {
                context.addSlot(new AppEngSlot(inv, i));
            }
        }));

// Client only
modBus.addListener((RegisterMestModulesEvent event) ->
        event.register("yourmod:spares", context -> new SparesPanel(context.slots())));
```

A panel places its slots in `layoutSlots()` with `placeSlot`, and hides them with `hideSlot` while
it is not visible. AE2 quick-move treats add-on slots as terminal-side slots, so reject items in
`mayPlace` if the slots should not receive shift-clicks. The layout keeps working when an add-on
is removed or added: unknown modules are dropped and new ones get a default window.

### Building and verification

```powershell
.\gradlew.bat build
.\gradlew.bat test
```

`runClient` starts a development client with JEI; `runClientEmi` starts the same client with EMI.
`-PmestRunJei=false` and `-PmestRunEmi=true` change the viewer set, and
`checkViewerLaunchClasspaths` checks the declared combinations without starting the game.
Automated tests cover the layout model and codec, storage, packet codecs, provider state, toolkit
and hotkey rules; they do not replace in-game checks, see `docs/compatibility-verification.md`.

---

## 中文

ME 拼接终端（MEST）是运行在 NeoForge 1.21.1 上的 AE2 / AE2WTLib 无线终端。它把存储、合成、样板和
工具都放进同一个界面，各个模块可以随意移动、缩放和拼接。

### 获取终端

- 直接合成；或者在锻造台上把 AE2WTLib 的无线通用终端升级成拼接终端，原终端的绑定、能量和升级卡都会保留。
- 它就是一个普通的 AE2WTLib 终端：在无线访问点绑定，放在背包或 Curios 槽里，用快捷键打开（按键设置里的
  “打开 ME 拼接终端”）。
- 升级卡栏的格数跟随 AE2WTLib 报告的容量，拿不到时默认 3 格。

### 模块

| 模块 | 作用 |
| --- | --- |
| ME 列表 | AE2 原版物品列表：排序、显示模式、视图元件、仅可合成、合成置顶、中键自动合成、手持容器灌装/倒出、Shift+滚轮单个存取 |
| 合成 / 合成终端 | 3×3 合成台，可分别清空到网络或背包；合成终端额外有盔甲槽和人物预览 |
| 样板编码 | 合成、处理、锻造、切石四种模式，替代、处理数量编辑、输出轮换与倍率缩放 |
| 样板管理 | 按 ExtendedAE 方式分组的样板供应器，可直接交换槽位，并远程打开供应器旁的机器 |
| 样板缓存、垃圾桶、背包 | 额外的样板存放区、垃圾桶和玩家背包 |
| 工具包 | 只放不可堆叠物品，支持记忆槽，驱动扩展快捷栏 |
| 网络工具包 | 终端自带网络工具的升级卡槽 |
| 无线设置 | 选取方块、补货、磁铁、工具包相关开关 |
| 面板快捷键 | 用组合键开关模块、切换布局预设 |
| 供应器选择 | ExtendedAE Plus 上传目标选择（仅在装了该模组时出现） |

### 布局

- 每个窗口是一棵分割树。把面板拖到另一个面板的边缘即可拼接，拖动分隔线调整比例，把某一栏的标题拖出来即可拆分。
- 拼接窗口统一画成一个外壳，内部只有细分隔线，不会出现接缝。
- 悬浮是窗口属性。锚定窗口跟随终端移动并挂载工具栏；锚定窗口右上角标题栏的悬浮按钮可以让任意拼接组合整体悬浮，悬浮窗口还可以
  钉在最上层。按住 Alt 拖动某一栏的标题可将其从锚定窗口中拆出，按住 Ctrl 拖动只移动当前抓住的拼接窗口；始终至少保留一个可见的锚定窗口。
- 组合模块：在布局编辑器中，悬浮窗口标题栏的按钮可把它设为组合模块 1、2 或 3（垃圾桶不能参与）。之后在终端的更多设置栏和面板快捷键里，
  就能像单个模块一样整体显示或隐藏这个窗口。
- 全屏布局编辑器提供模块面板、属性面板、三个可命名预设、撤销、重置、分享文件夹 / zip / 剪贴板的导入导出，以及备份。
- 布局是客户端偏好，保存在 `config/mesplicedterminal/`（`layout.json`、`presets.json`）。写盘是原子替换；旧格式在
  读取时自动迁移；文件损坏时会被隔离，而不是被覆盖。

### 扩展快捷栏与工具包

- 打开工具包快捷栏后，原版快捷栏两侧各多一条 9 格栏，用滚轮或专用按键切过去；原版快捷栏本身不会移动。
- 工具包属于玩家而不是终端：死亡不掉落，终端丢了东西也还在，只是需要带着终端才能使用。旧终端上的工具包数据会自动合并到玩家身上。
- 记忆槽会把捡起的同类工具自动放回原位。

### 面板快捷键

可以给任意模块、“切换布局预设”以及预设 1–3 绑定组合键（Ctrl / Shift / Alt + 按键）。左键点按钮再按下组合键即可绑定，
右键清除。关闭“仅终端”后，在世界中按下也会先打开终端再执行。绑定保存在 `config/mesplicedterminal/hotkeys.json`。

### 服务端配置

样板缓存、垃圾桶、工具包、网络工具包的槽位数属于服务端配置，会同步给客户端，保证两端菜单一致。配置按存档保存在
`serverconfig/mesplicedterminal-server.toml`；放一份到 `defaultconfigs/` 可作为新存档的默认值。

### JEI 与 EMI

配方转移的目标是最后聚焦的合成或编码模块。聚焦合成时走 AE2 原版的合成转移；聚焦编码器时，合成、锻造、切石和处理配方
会填入对应的编码模式。

### 其他模组添加模块

MEST 在 `com.lhy.mest.api` 提供两个 mod 总线事件。模块 ID 必须是小写，建议带命名空间（`yourmod:panel`），不能与内置
模块重名，也不能以 `action:` 开头。

1. **菜单槽位（可选，两端都要）**：`RegisterMestModuleSlotsEvent`，在通用初始化阶段触发。每次打开终端菜单时，服务端和
   客户端各调用一次提供者，两边必须按相同顺序添加相同的槽位。附加模块的槽位排在所有内置槽位之后，附加模块之间按 ID
   排序，所以两端槽位编号总是一致。槽位里的内容需要自己保存，例如存在 `context.terminal()` 的数据组件或玩家附加数据上。
2. **面板（仅客户端）**：`RegisterMestModulesEvent`，在客户端初始化阶段触发。工厂返回一个 `ModulePanel`，其 `id()`
   必须与注册的 ID 相同；返回 `null` 表示不添加。`context.slots()` 返回第 1 步添加的槽位。重写 `defaults()`、
   `icon()` / `renderIcon()` 可以控制模块第一次出现时的状态和按钮图标。

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

面板在 `layoutSlots()` 里用 `placeSlot` 摆放槽位，不可见时用 `hideSlot` 隐藏。AE2 的 Shift 快速移动会把附加模块的
槽位当作终端侧槽位；如果不希望这些槽位接收 Shift 点击，请在 `mayPlace` 里拒绝。附加模组被移除或新增时布局照常可用：
不存在的模块会被丢弃，新模块会分配一个默认窗口。

### 构建与验证

```powershell
.\gradlew.bat build
.\gradlew.bat test
```

`runClient` 启动带 JEI 的开发客户端，`runClientEmi` 启动带 EMI 的同一客户端。`-PmestRunJei=false`、
`-PmestRunEmi=true` 可切换配方查看器组合，`checkViewerLaunchClasspaths` 不启动游戏即可检查各组合的声明。
自动化测试覆盖布局模型与编解码、存储、数据包编解码、供应器同步状态、工具包与快捷键规则，但不能代替游戏内验证，
详见 `docs/compatibility-verification.md`。

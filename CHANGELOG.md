# Changelog

## v0.0.3.0-forge1.20.1（移植：格兹亚西 / port by 格兹亚西）

### 中文

1. 移植：NeoForge 1.21.1 → Forge 1.20.1（AE2 15、AE2WTLib 15、JEI 15.19–15.49），目标整合包 GTL。
2. 新增：JEI 配方旁的"在终端合成"和"写入样板编码"按钮。
3. 新增：GTL 兼容——gtlcore 精确数量显示、通用电路优先、Ctrl 转移时不消耗输入编码为虚拟材料。
4. 调整：垃圾桶可拼接；拼接时优先填入窗口内的富余空间；窗口不再把面板压到最小尺寸以下。
5. 修复：滚动条条纹贴图、视图元件列错位、底部面板压到边框、置顶网络工具面板挡住拼接、缩放手柄被外置导轨遮挡、Inventory Tweaks 整理按钮乱飘。
6. 未移植：EMI、Some Useless Things、Lightning、导入导出、ClientSort 兼容。

### English

1. Ported from NeoForge 1.21.1 to Forge 1.20.1 (AE2 15, AE2WTLib 15, JEI 15.19–15.49) for the GTL modpack.
2. Added: "craft in terminal" and "encode pattern" buttons beside JEI recipes.
3. Added: GTL support — gtlcore precise amounts, universal circuit preference, Ctrl virtual ingredients.
4. Changed: the trash can be spliced; drops fill spare room inside a window; windows never squeeze panels below their minimum.
5. Fixed: striped scrollbar texture, misaligned view-cell column, bottom panel overlapping the frame, the pinned network-tool panel blocking splices, the resize grip hidden under outside rails, stray Inventory Tweaks sort buttons.
6. Not ported: EMI, Some Useless Things, Lightning, import/export and ClientSort integrations.

## v0.0.3.0

### English

1. Added: Module API. Other mods can add their own terminal modules through `RegisterMestModulesEvent` (client panel) and `RegisterMestModuleSlotsEvent` (menu slots, built identically on both sides), see the README.
2. Changed: Built-in modules use the same registration path; each panel now declares its own default visibility, floating and pinned state, terminal button and icon instead of MEST checking hard-coded module ids.
3. Changed: Floating and pinned are now properties of a window instead of its modules, so any combination of spliced panels can float together. Anchored windows get a float button in their top-right title bar, Alt+drag takes a section out of an anchored window, and older layouts keep their current look.
4. Added: Combined modules. In the layout editor a floating window's title-bar button turns it into combined module 1, 2 or 3 (the trash cannot join one); the terminal's more-settings bar and the panel hotkeys can then show or hide the whole window at once, using the layout preset 1-3 icons.
5. Changed: Ctrl+drag now moves only the spliced window you grab, in the terminal and in the editor, instead of every window at once.
6. Fixed: Combined-module controls now appear only for joined windows; joined panels stay together on click, require Alt+drag to detach, and share one pin and matching button backgrounds.

### 中文

1. 新增：模块 API。其他模组可以通过 `RegisterMestModulesEvent`（客户端面板）和 `RegisterMestModuleSlotsEvent`（菜单槽位，两端按相同顺序构建）为终端添加自己的模块，用法见 README。
2. 调整：内置模块也改为走同一套注册；默认是否显示、浮动、钉住、显示终端按钮以及图标都由面板自己声明，不再按写死的模块 ID 判断。
3. 调整：悬浮与钉住改为窗口属性，不再按模块决定，任意面板拼接后都能整体悬浮。锚定窗口右上角标题栏有悬浮按钮，按住 Alt 拖动标题栏可从锚定窗口中拆出单个面板；旧布局加载后外观保持不变。
4. 新增：组合模块。在布局编辑器中，悬浮窗口标题栏的按钮可把它设为组合模块 1、2 或 3（垃圾桶不能参与）；之后在终端的更多设置栏和面板快捷键里即可整体显示或隐藏该窗口，图标沿用布局预设 1–3。
5. 调整：按住 Ctrl 拖动时只移动当前抓住的拼接窗口（终端与编辑器中都一样），不再移动全部窗口。
6. 修复：组合模块按钮仅在窗口拼接后显示；点击拼接面板不再散开，须按住 Alt 拖动才能拆分，且每窗只显示一个钉住按钮并统一按钮底图。

## v0.0.2.8

### English

1. Added: Direct encoding of Some Useless Things Omniversal patterns from the pattern panel.
2. Added: Some Useless Things file 8962878 as a compile-time and runtime dependency.
3. Fixed: Omniversal encoding now uses MEST's own encoding action instead of AE2's vanilla-menu-only transfer path.

### 中文

1. 新增：样板编码面板可直接编码无用之物的万象样板。
2. 新增：无用之物文件 8962878 加入编译与运行时依赖。
3. 修复：万象样板编码改为走 MEST 自己的编码动作，不再依赖只支持原版终端的转移路径。

## v0.0.2.7

### English

1. Fixed: The open-terminal hotkey in the controls screen now shows its name instead of the raw translation key.
2. Added: Panel Hotkeys module. Bind a key combo (Ctrl / Shift / Alt + key) to open or close each terminal panel, cycle layout presets or switch straight to preset 1, 2 or 3; bindings that are not terminal-only also work from the world by opening the terminal first.
3. Fixed: Scrolling over a raised window no longer also scrolls the pattern access, pattern cache or provider picker panels beneath it.
4. Fixed: The spliced terminal no longer shows as item.minecraft.air in the back button of sub-screens such as crafting amount, or anywhere AE2WTLib uses the terminal's name.

### 中文

1. 修复：按键设置里打开终端的快捷键显示为中文名称，不再显示原始键名。
2. 新增：面板快捷键模块。可绑定组合键（Ctrl / Shift / Alt + 按键）来打开或关闭各个终端面板、轮换布局预设或直接切换到预设 1、2、3；未勾选“仅终端”的快捷键在世界中按下会先打开终端再执行。
3. 修复：在上层窗口上滚动时，下方的样板管理、样板缓存和供应器选择面板不再跟着滚动。
4. 修复：拼接终端的名称不再显示为 item.minecraft.air（例如合成数量界面的返回按钮，以及 AE2WTLib 用到终端名称的地方）。

## v0.0.2.6

### English

1. Added: Some Useless Things and Sophisticated Backpacks as development runtime dependencies.
2. Fixed: Bumped Data Energistics to 3.2.2 so Useless Mod no longer crashes while prewarming alloy-furnace recipes.
3. Added: LDLib2 as a runtime dependency required by Data Energistics 3.2.2.
4. Fixed: Bumped Sophisticated Storage to 1.5.91 so it matches Sophisticated Core 1.5.1.
5. Fixed: Skip toolkit hand override while ServerPlayer is still constructing, so Useless Mod can no longer NPE join with invalid player data.
6. Fixed: Same join-time spectator NPE on the client LocalPlayer, which crashed Camera.setup after login.
7. Fixed: Extra-bar right-click equip is applied only on the server, so swapping diamond leggings onto Advanced AE quantum leggings no longer leaves quantum pieces in both slots.
8. Fixed: Pattern-encoding slots now use AE2's craftable query and raise the "+" overlay in z, so 3D block ingredients no longer hide the marker.
9. Fixed: Dev client now uses JEI 19.51 and skips AE2 Utility, so ExtendedAE Plus and AE2 Utility JEI mixins no longer crash on enter-world.
10. Fixed: Memory cards now pull and return upgrade cards from the spliced terminal's built-in network toolkit when no vanilla network tool is carried.
11. Fixed: Crafting pins now prune on close like AE2, the ME list scrollbar reserves the pin row, and the pin cap follows the visible column count.
12. Added: Non-processing encode auto-upload now targets ECO pattern storage and Lightning Tech crafting assemblers, with duplicate detection before insert.
13. Fixed: ECO auto-upload now uses PatternContainer group routing instead of compile-time neoecoae APIs.
14. Fixed: Requesting the provider list no longer crashes servers without ExtendedAE Plus.
15. Fixed: Moving items from the network into the toolkit or trash now uses AE power and only hands out what was actually extracted.
16. Fixed: Remotely opening machines next to pattern providers now respects spawn protection and claim mods, and no longer toggles levers, doors or buttons.
17. Fixed: Network tool upgrade slots now follow the tool stack itself, so cards can no longer be duplicated after moving the tool away.
18. Fixed: The terminal screen and the extra hotbar now share one toolkit inventory, so two diverging copies can no longer duplicate items.
19. Changed: The toolkit, its remembered slots and the extra-bar settings now belong to the player and are kept on death; toolkit data stored on existing terminals is merged into the player automatically.
20. Fixed: The pattern provider list no longer stays empty forever after overflowing during a snapshot rebuild.
21. Fixed: Returning the last upload now locates the provider itself and checks the pattern, so a snapshot rebuild can no longer make it take a pattern from another provider.
22. Fixed: Slot-count settings are now a server config synced to clients, so menus no longer misalign on dedicated servers. Note: values now live per world in `serverconfig/mesplicedterminal-server.toml` and old `config/` values are not carried over; put a copy in `defaultconfigs/` to preset new worlds.
23. Fixed: A readable layout.json now wins over the active preset slot instead of being replaced by an older copy.
24. Fixed: Cancelling the layout editor also drops the undo steps made inside it, so undo in the terminal cannot bring back discarded edits.
25. Fixed: Scrollbars of panels covered by a higher window can no longer be dragged through it.
26. Fixed: The legacy network tool panel placement migration only moves a standalone panel window, no longer dragging spliced modules along.
27. Changed: Removed the stretch and compact splice modes; spliced windows now always use the outer-shell style. Layouts saved in the old modes are resized to fit their content once on load.
28. Fixed: Items in the extra hotbar no longer disappear after leaving the layout editor or any other menu sync while the toolkit panel is closed.

### 中文

1. 新增：开发运行时加入 Some Useless Things 与 Sophisticated Backpacks。
2. 修复：将 Data Energistics 升到 3.2.2，避免 Useless Mod 合金炉配方预热崩溃。
3. 新增：补上 Data Energistics 3.2.2 所需的 LDLib2 运行时依赖。
4. 修复：将 Sophisticated Storage 升到 1.5.91，对齐 Sophisticated Core 1.5.1 的 API。
5. 修复：ServerPlayer 构造未完成时不再覆盖主手，避免 Useless Mod 进档空指针并提示无效的玩家数据。
6. 修复：客户端 LocalPlayer 构造时同样的旁观检查空指针，进档后 Camera.setup 崩溃。
7. 修复：扩展栏右键装备不再客户端预测，避免钻石护腿换上量子胫甲后手上和身上都变成量子胫甲。
8. 修复：样板编码槽改用 AE2 原版可合成查询，并把 “+” 抬到物品模型之上，避免方块类材料挡住角标。
9. 修复：开发客户端改用 JEI 19.51 并暂时去掉 AE2 Utility，避免 Plus 与 Utility 的 JEI mixin 进档即崩。
10. 修复：没有手持原版网络工具时，内存卡会从终端自带网络工具面板的升级槽取放升级卡。
11. 修复：合成置顶按原版在关界面后回收，滚动为置顶行预留一页，上限随 ME 列表当前列数变化。
12. 新增：非处理样板编码后自动上传到 ECO 样板库和闪电科技合成装配，插入前做去重。
13. 修复：ECO 自动上传改为走 PatternContainer 分组，不再编译依赖 neoecoae API。
14. 修复：服务端没装 ExtendedAE Plus 时，请求供应器列表不再崩服。
15. 修复：从网络取物放入工具包或垃圾桶改为扣能量，并按实际取出数量放入，不再多给。
16. 修复：远程打开供应器旁的机器时遵守出生点保护和领地模组，不再触发拉杆、门和按钮。
17. 修复：工具包里网络工具的升级槽改为跟随工具本身，移走工具后不能再取出升级卡刷物品。
18. 修复：终端界面与扩展快捷栏共用同一份工具包库存，两份副本不再分叉刷物品。
19. 调整：工具包、记忆槽和扩展栏设置改为跟随玩家，死亡不掉落；已有终端上的工具包数据会自动合并到玩家身上。
20. 修复：样板供应器列表在重建快照时溢出后不再永久停在空列表。
21. 修复：撤回上次上传改为按供应器本身定位并核对样板，快照重建后不会再从别的供应器取错样板。
22. 修复：槽位数配置改为服务端配置并同步给客户端，专用服上两端的菜单槽位不再错位。注意：配置改为按存档保存在 `serverconfig/mesplicedterminal-server.toml`，原 `config/` 下的旧值不会自动迁移；放一份到 `defaultconfigs/` 可作为新存档的默认值。
23. 修复：layout.json 能正常读取时以它为准，不再被较旧的当前预设覆盖。
24. 修复：取消布局编辑时一并撤掉编辑器里产生的撤销记录，回到终端后撤销不会恢复已放弃的编辑。
25. 修复：被上层窗口挡住的面板不能再被隔着拖动滚动条。
26. 修复：网络工具面板的旧版位置迁移只移动单独成窗的面板，不再带着拼接在一起的其他模块一起平移。
27. 调整：移除拉伸拼接和紧凑拼接两种模式，拼接窗口统一使用外壳拼接；旧模式保存的布局会在加载时按内容自动调整一次窗口大小。
28. 修复：工具包面板关闭时，保存退出布局编辑器等菜单同步不再清空扩展快捷栏里的物品图标。

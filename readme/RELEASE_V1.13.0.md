# KeiOS v1.13.0 Release Notes

<!-- markdownlint-disable MD013 -->

## 中文

KeiOS v1.13.0 是 v1.12.0 之后的展示层、导航与交互性能更新。93 个提交将 Bottom Sheet、Alert、Action Sheet、Toast、Dropdown 和操作菜单迁移到统一的窗口内 Liquid Glass 展示层，同时完善主页面切换、BA 与 OS 路由、特权通道一致性、卡片堆叠和高刷新率体验。

### Liquid Glass 展示层

- Bottom Sheet、Alert、Action Sheet、Toast、Dropdown 和操作菜单统一在应用窗口内呈现，可以直接采样当前页面的实时背景，并保持正确的深浅色材质。
- 弹出层共享同一套遮罩、返回、外部点击、拖动关闭与进退场驱动；多层弹出时，上层会自然压暗下层，关闭过程保持连续。
- 编辑内容后关闭 Sheet 会使用 Action Sheet 呈现“放弃更改 / 继续编辑”，危险操作和取消操作拥有稳定的排列与间距。
- 顶部操作栏重构为分组工具栏，每组只绘制一个玻璃容器；点击、轻微手指位移、拖动形变与 Popup 锚点在可横向切页的页面上保持稳定。
- 浮动工具栏、顶部标题卡和底栏会优先接管自身手势，减少按下按钮时被页面滑动或列表滚动取消的情况。
- 菜单、卡片和弹出层的阴影按实际表面裁切，修复圆角周围的方形暗角、滚动裁切产生的方形阴影及淡出时阴影收缩。

### 卡片堆叠与 BA

- 活动日历与卡池页面恢复真实 Liquid Glass 材质；卡片进入堆叠区后通过缩放、抬升、渐进虚化和轻度压暗表达深度。
- 后退卡片的标题、图像和布局保持可辨识，前景玻璃边缘保持清晰，长内容卡片不会在堆叠时突然消失或变成空白表面。
- 学生图鉴在导航发出后并行准备缓存快照，温缓存可以在首屏组合前到达；冷缓存继续走原有加载路径，点击到转场的路径保持轻量。
- 图鉴图片预取范围限制在当前活动视口附近，减少离屏解码和快速滚动时的额外工作。
- BA 活动日历与卡池从独立 Activity 迁移到统一 Miuix Nav 路由，获得一致的页面转场、边缘返回、返回栈、主题与动态背景生命周期。

### 主导航与页面操作

- OS Shell Runner 接入主路由栈，返回操作会回到原页面，并共享 MainActivity 已建立的特权 Shell 生命周期。
- 主页面滑动保留松手动量；跨越多个标签时通过一个中间页完成视觉移动，减少远距离切换同时绘制多页的压力。
- 页面运动与分区切换会申请设备可用的峰值刷新率；投票范围跟随实际运动，静止后及时释放。
- GitHub 的新增追踪、OS 的新增卡片和 MCP 的服务切换移入各自概览卡片，浮动 Dock 保留更高频且更适合单手操作的动作。
- 下拉刷新触发距离提升到约 128dp，普通的顶部下滑更容易回弹，减少一次误触带来的网络刷新。

### 特权模式与 GitHub

- 未明确保存特权选项时，默认状态统一为“关闭”；新安装、未知值和损坏值都会安全落到关闭状态。升级后从未主动选择过模式的用户需要重新选择 Shizuku 或 Root。
- Home 状态 Pill、设置页应用列表诊断、GitHub 本机应用发现和 OS Shell 会统一反映当前选择的特权后端。
- GitHub 应用选择器可通过当前 Shizuku 或 Root 通道发现更完整的本机应用；列表布局更紧凑，滚动进入视口的应用图标会按需加载。
- APK 信息 Sheet 新增资产 SHA-256，包体校验结果可以直接在详情中查看。
- GitHub 概览移除低价值的条目自定义 Sheet，保留稳定版、预发布版和失败项的核心统计与动作。
- MCP 在 Root 模式首次执行网络命令时会主动探测命令能力，减少初次使用时的错误状态判断。

### 性能、帧率与启动

- Home 概览卡与全局状态 Pill 采用批量玻璃绘制，多个相邻组件共享材质处理，减少重复的模糊与高光节点。
- 不可见页面、被覆盖路由、闲置 Sheet 和编辑面板会暂停背景或 Backdrop 生产；页面切换期间只保留当前动画实际需要的采样链路。
- 动态背景继续跟随显示器 VSYNC 相位，并将静止页面的重绘上限设为 60fps。物理 120Hz 设备的 39 秒静止测试从 4658 帧降至 2758 帧，减少约 42% 的空闲绘制，同时保持背景运动速度。
- 主 Pager 保留手势释放动量并在运动期间申请高刷新率；后台与浮层的投票范围收窄，避免长期占用整块屏幕的高帧率预算。
- Baseline Profile 覆盖主页面、BA 日历与卡池、GitHub Actions 历史、Settings、About、WebDAV、MCP Skill、OS Shell、展示层菜单和卡片堆叠等 11 条旅程。
- Release 路径移除 benchmark 专用的 jank callback 开销，并清理无视觉作用的 GraphicsLayer 节点。

### 安装信息

- 包名：`os.kei`
- ABI：`arm64-v8a`
- Android：Android 15+（`minSdk 35`）
- Target SDK：Android 17 / API 37
- APK：`KeiOS_1.13.0.apk`
- 校验文件：`KeiOS_1.13.0.apk.sha256`

### 升级建议

建议所有 v1.12.x 用户升级到 v1.13.0。经常切换主页面、使用浮动操作、Sheet / Dialog / 菜单、BA 活动与图鉴、Root / Shizuku，或使用高刷新率设备的用户会获得最明显的改善。升级后请在设置页确认特权模式；未明确保存过模式的安装会采用“关闭”。

## English

KeiOS v1.13.0 is a presentation, navigation, and interaction-performance update after v1.12.0. Across 93 commits, bottom sheets, alerts, action sheets, toasts, dropdowns, and action menus move to one in-window Liquid Glass presentation layer, accompanied by smoother main-page switching, routed BA and OS destinations, consistent privilege handling, more legible card piles, and better high-refresh-rate behavior.

### Liquid Glass Presentation

- Bottom sheets, alerts, action sheets, toasts, dropdowns, and action menus now present inside the app window, directly sample the live page, and use theme-correct light and dark materials.
- Presentations share scrim, back, outside-tap, drag-dismiss, and transition behavior. Stacked presentations dim the layer below and close through one continuous motion path.
- Closing a sheet with edited content now opens an action sheet for Discard Changes or Keep Editing, with stable destructive and cancel placement.
- The top action bar is now a grouped toolbar with one glass container per group. Taps, small finger movement, expressive drag deformation, and popup anchors remain stable over horizontally pageable content.
- Floating toolbars, title cards, and bottom bars claim their own gestures so page swipes and list scrolling are less likely to cancel an intended action.
- Menu, card, and presentation shadows follow the actual surface, fixing square corner patches, scroll-clipped rectangular shadows, and shrinking shadow spread during fades.

### Card Piles And BA

- Calendar and Pool pages regain real Liquid Glass. Cards entering the edge stack now use scale, lift, progressive content blur, and a restrained dim layer to communicate depth.
- Receding cards keep identifiable titles, images, and layout while the front glass edge remains crisp; long content cards no longer disappear or turn into empty plates in the pile.
- Student Guide prepares its cached snapshot in parallel after navigation begins. A warm snapshot can arrive before first composition, while a cold cache keeps the established loading path and the click-to-transition path stays lightweight.
- Guide image prefetch is bounded around the active viewport, reducing off-screen decoding and extra work during fast scrolling.
- BA Calendar and Pools move from standalone activities into Miuix Nav routes, gaining shared transitions, edge back, back-stack behavior, theming, and dynamic-background lifecycle.

### Main Navigation And Page Actions

- OS Shell Runner joins the main route stack, returns to its source page, and shares the privileged Shell lifecycle already owned by MainActivity.
- Main-page swipes preserve release momentum. Long tab jumps render through one visual intermediate page, reducing the number of pages drawn during the move.
- Page motion and tabbed-section switches request the display's available peak refresh rate, with votes scoped to actual movement and released after settling.
- Add Tracking on GitHub, Add Card on OS, and the MCP server toggle move into their overview cards, leaving floating docks focused on frequent one-handed actions.
- Pull-to-refresh now arms at roughly 128dp of finger travel, allowing ordinary top-of-list downward flicks to rebound without starting a network refresh.

### Privilege Mode And GitHub

- An unset privilege preference now consistently resolves to Off. New installs, unknown values, and corrupt values use the safe Off state. Existing installations that never explicitly selected a mode need to choose Shizuku or Root again after upgrading.
- The Home status pill, Settings app-list diagnostics, GitHub local-app discovery, and OS Shell consistently report and use the selected privilege backend.
- GitHub's app picker discovers local apps through the selected Shizuku or Root path, uses a denser layout, and loads icons when rows enter the viewport.
- APK details add the asset SHA-256 so the verified package digest is visible in the information sheet.
- GitHub Overview removes the low-value entry-customization sheet while retaining the core stable, prerelease, and failed-item signals and actions.
- MCP probes Root networking commands on first use, improving initial capability detection.

### Performance, Frame Rate, And Startup

- Home overview cards and shared status pills batch their glass rendering so adjacent components reuse material work and avoid repeated blur/highlight nodes.
- Hidden pages, covered routes, dormant sheets, and idle edit panels pause background or Backdrop production, leaving only the sampling chain required by the active transition.
- The dynamic background remains phase-aligned to display VSYNC while idle invalidation is capped at 60fps. On a physical 120Hz device, a 39-second idle run dropped from 4,658 to 2,758 frames, about 42% fewer idle draws, while preserving animation speed.
- The main pager keeps release momentum and requests high refresh rate during motion. Background and overlay votes are narrowed so they do not hold the whole display at a high rate after interaction ends.
- The Baseline Profile now covers 11 journeys across main pages, BA Calendar and Pools, GitHub Actions History, Settings, About, WebDAV, MCP Skill, OS Shell, presentation menus, and card-pile interactions.
- Release builds drop benchmark-only jank callback overhead and remove GraphicsLayer nodes with no visual effect.

### Package

- Package name: `os.kei`
- ABI: `arm64-v8a`
- Android: Android 15+ (`minSdk 35`)
- Target SDK: Android 17 / API 37
- APK: `KeiOS_1.13.0.apk`
- Checksum file: `KeiOS_1.13.0.apk.sha256`

### Upgrade Advice

Every v1.12.x user should upgrade to v1.13.0. The largest improvements apply to frequent main-page switching, floating actions, sheets, dialogs, menus, BA Calendar and Student Guide, Root / Shizuku workflows, and high-refresh-rate devices. Check privilege mode in Settings after upgrading; installations without an explicitly saved selection will use Off.

# FamilyGuardian（「我在」）代码复核与完善建议

- 复核对象：`wycdpg/FamilyGuardian-public`，提交 `6b593a3`（v1.15 / versionCode 16）
- 复核日期：2026-11-14
- 复核方式：静态阅读全部源码（3 个 Java 文件共约 3470 行、3 个 HTML 共约 1000 行、Gradle/Manifest 配置）。**未编译、未真机运行**，未改动任何代码。
- 标注约定：**[已确认]** = 代码中直接可见；**[待验证]** = 依赖机型/系统版本，建议真机确认。

---

## 一、总体评价

这是一个定位清晰、场景有温度的项目：老人佩戴手环自动采集（心率/血氧/血压/HRV/血糖/压力/体温/睡眠），子女端通过家庭码远程查看，并带有摔倒三闸门判定、分级告警、短信/电话通知。作者对手环 SDK 的行为（超时、不支持、失败回退）有大量实战经验沉淀，注释里能看到不少"踩坑修复"。

但目前整体处于「个人 Demo/内测」阶段，距离「可靠的安全类产品」还有明显差距。最需要关注的是三类问题：

1. **安全与隐私**：云存储密钥必须内置在 APK 中、家庭码即唯一鉴权、健康数据与家人手机号被上传、WebView 注入面较大。
2. **告警可靠性**：家人端只在 App 位于前台时才轮询，后台基本收不到告警；老人端离线无感知；告警可能反复发短信。对"救命类"功能这是最核心的功能缺口。
3. **工程结构**：一个 2757 行的 `VpCollectorService` 承担了十几种职责，状态用 40 多个布尔量手工维护，没有测试，存在若干生命周期泄漏。

优先级说明：**P0** = 上线/推广前必须解决；**P1** = 近期应完成；**P2** = 持续改进。

---

## 二、问题总览（按优先级）

| # | 优先级 | 类别 | 问题 |
|---|---|---|---|
| 1 | P0 | 安全 | 云存储 AK/SK 内置于客户端，反编译即可读写全部家庭数据 |
| 2 | P0 | 安全 | 家庭码是唯一"鉴权"，且示例值可猜（`wozai-1011`） |
| 3 | P0 | 功能 | 家人端仅前台轮询，后台/锁屏收不到告警 |
| 4 | P0 | 功能 | 老人端掉线/没电/服务被杀时，家人端无感知（"离线"只看 JSON 里的字段，不看时间戳） |
| 5 | P0 | 隐私 | 默认开启上传 CSV 与 `alarm.log`，其中含家人手机号；Logcat 打印健康数据与手机号 |
| 6 | P0 | 安全 | Java→JS 用字符串拼接注入云端 JSON；`innerHTML` 无转义；`addJavascriptInterface` 暴露面大 |
| 7 | P0 | 可靠性 | 手机重启后采集服务不会自启（无开机广播接收器） |
| 8 | P0 | 可靠性 | 手环按名称含 `H600` 即连接，可能连到邻居的手环 |
| 9 | P1 | 告警逻辑 | 持续异常时每约 10 分钟循环发短信，无升级/静默/确认机制 |
| 10 | P1 | 数据质量 | CSV/上传使用"上一次的值"补位，旧数据被当成新数据；无逐项时间戳 |
| 11 | P1 | 生命周期 | Service 销毁时未取消 Handler 循环与闹钟，多个接收器未注销；重启后循环叠加 |
| 12 | P1 | 权限 | 蓝牙/定位运行时权限未主动申请；`saveEcg` 在 Android 10+ 大概率写入失败 |
| 13 | P1 | 架构 | `VpCollectorService` 上帝类；状态机靠布尔量；广播携带 60+ 个 extra |
| 14 | P1 | 性能 | 每 30 秒全量读取并解析 CSV 两次；CSV/日志无限增长；每次请求新建 TOS 客户端 |
| 15 | P1 | 构建 | targetSdk 29、Debug 包分发、无 R8、无签名配置、未使用的依赖（已排查源码未引用） |
| 16 | P2 | 规范 | 无测试、无 CI、乱码注释、硬编码中文文案、无 LICENSE（且仓库含第三方闭源 SDK） |
| 17 | P2 | 合规 | 非医疗级测量值（血糖/血脂/尿酸/抑郁风险等）缺少产品层面的免责与分级展示 |

---

## 三、安全与隐私

### 3.1 [P0][已确认] 云存储 AK/SK 内置于客户端

`CloudSync.java:22-24` 的 `BUCKET/AK/SK` 为 `static final String`，编译后进入 APK。README 的使用方式也是"自己填 AK/SK 再编译"。
这意味着**任何拿到 APK 的人**（包括蒲公英链接的所有下载者）都能提取出密钥，从而：读取/改写/删除桶内**所有家庭**的健康数据；改写任意家庭的 `config.json`（远程修改告警阈值，把阈值调到永不触发）；用你的账号额度写入大量垃圾数据产生费用。

> 说明：公开仓库已脱敏是对的，但"脱敏"解决不了"客户端必须持有长期密钥"这个架构问题。

**建议**
- 引入一个极薄的后端（云函数/API 网关均可）：客户端只拿**短期 STS 临时凭证**或**预签名 URL**，且策略限定到 `家庭ID/` 前缀的读写。
- 过渡方案（仍不安全，仅降低风险）：使用权限最小化的子账号密钥（仅限该桶、仅 Get/Put、禁止 List/Delete），并配置桶的用量告警；不要在 Debug 包里放主账号密钥。
- 任何一次密钥曾写入过 Git 历史的，需要立即轮换（本仓库只有 1 个提交且已脱敏，但作者本地私有仓库历史需要自查）。

### 3.2 [P0][已确认] 家庭码即鉴权，且可猜

`MainActivity.java:70` 只要求长度 ≥ 3；提示示例为 `wozai-1011`。家人端 `addCode` 也没有格式校验。配合 3.1，任何人遍历 `wozai-0000 ~ wozai-9999` 就能读取别人的健康数据。即使修复 3.1，3 位字符的家庭码也远不够。

**建议**
- 家庭码改为服务端生成的高熵随机串（≥ 128 bit，或 8 位以上随机字母数字 + 校验位），老人端生成后以二维码/一次性配对码分享给家人。
- 配对关系放后端：老人端为"所有者"，家人端为"只读成员"，**阈值修改**需要"管理员"权限；支持踢出成员/更换家庭码。
- 为 `config.json` 增加签名或服务端校验，避免被篡改后静默降低告警灵敏度。

### 3.3 [P0][已确认] 健康数据与家人手机号被上传、日志泄露

- `CloudSync.logUploadOn()` **默认 true**（`CloudSync.java:164`）；每小时把完整 `vp_data.csv`（含历史）和 `alarm.log` 上传到 `家庭码/logs/`。
- `alarm.log` 中写入了家人手机号：`"SMS SEND begin p1=" + familyPhone1`（`VpCollectorService.java:1462` 等），因此**手机号也被上传到云端**。
- 所有 `Log.w(...)`（共 212 处，不是 debug 级别）在 Logcat 中输出整行健康数据（`"csv: " + sb`）、手机号、阈值；Release 包也不会被剥离。
- 健康数据属于《个人信息保护法》中的**敏感个人信息**，需要单独同意；默认开启 + 老人端设置里才能关，不符合"默认最小化、明示同意"。

**建议**
- 上传开关默认**关闭**，首次使用时用大字、通俗语言明确征得老人（而不是子女）的同意，并记录同意时间。
- 日志中手机号脱敏（如 `138****5678`）；`Log.w` 统一换成自带 `Logger` 封装，Release 下仅保留必要 `Log.e`，并配合 R8 `-assumenosideeffects`。
- 云端日志文件设置生命周期（如保留 30 天），并只上传"当日增量"，而不是整份历史。
- 增加"一键清除云端数据 / 注销家庭"功能（合规要求中的删除权）。
- Manifest 加 `android:allowBackup="false"`（或配置 `dataExtractionRules`），避免 `adb backup`/云备份带走 SharedPreferences 里的手机号和健康缓存。

### 3.4 [P0][已确认] WebView 注入面

1. **Java→JS 拼接字符串**：`MainActivity.java:150,171` 把云端下载的 `json` 直接拼进 `onCloudUpdate('code', <json>)`；`code` 被单引号包裹但不转义。若桶内数据被篡改（见 3.1），JSON 中可含 `U+2028/2029` 或闭合表达式，造成脚本执行。
2. `jsStr()`（`MainActivity.java:303`）只转义 `\` 和 `"`，**没有处理换行、`</script>`、U+2028/2029**；`jsToast` 只是删除单引号，遇到反斜杠、换行会导致 JS 语法错误甚至注入。
3. `family.html` / `app.html` 大量使用 `innerHTML` 拼接视图，且全文无任何转义函数（`escape/esc` 命中 0 次）。家人昵称、告警 `msg`、云端字段都会进入 DOM。
4. `addJavascriptInterface(new Bridge(), "AndroidBridge")` 暴露了 30 多个方法，包括 `setFamily`（改紧急联系人）、`saveEcg`（写文件）、`saveThresholds`（写云端）、`sendWeChat`、`resetRole`。一旦页面被注入，等同于任意调用这些能力。
5. 未显式禁用 `setAllowFileAccess`；`debug.html` 也被打进正式包。

**建议**
- 所有 Java→JS 数据统一用 `JSONObject.quote()` 或把 payload `Base64`/`JSON.stringify` 后经 `evaluateJavascript("window.__push(" + JSONObject.quote(payload) + ")")`，页面里再 `JSON.parse`。
- 前端所有动态文本用 `textContent` 或统一 `esc()`；引入严格 CSP（`<meta http-equiv="Content-Security-Policy" ...>`）。
- 使用 `WebViewAssetLoader`（`https://appassets.androidplatform.net/`）取代 `file:///android_asset/`，同时 `setAllowFileAccess(false)`、`setAllowContentAccess(false)`。
- Bridge 方法按角色拆分（老人端/家人端各自只暴露自己需要的），敏感方法（改手机号、改阈值）加二次确认或 Java 侧校验调用来源。
- 删除 `debug.html`，或放入 `src/debug/assets`。

### 3.5 [P1][已确认] Manifest 与权限细节

- 缺 `android:allowBackup="false"`、`android:networkSecurityConfig`（目前 targetSdk 29 默认允许明文流量）。
- `BLUETOOTH_ADVERTISE`、`ACCESS_COARSE_LOCATION` 与功能不匹配（App 只扫描/连接，不广播）；`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`、`SEND_SMS`、`CALL_PHONE` 属于应用商店高敏权限（仅自分发可用，上架会被拒）。
- 前台服务未声明 `android:foregroundServiceType="connectedDevice"`（以及 Android 14 的 `FOREGROUND_SERVICE_CONNECTED_DEVICE` 权限）；`POST_NOTIFICATIONS`、`SCHEDULE_EXACT_ALARM/USE_EXACT_ALARM` 也未声明——**当前因 targetSdk 29 不会报错，但一旦提升 targetSdk 立刻失效**，需要提前规划。
- `tools:replace="android:label"` 没有对应冲突来源，疑似多余。
- 资源只有 `mipmap-xxxhdpi/ic_launcher.png`，没有 `strings.xml`、自适应图标、主题资源。

---

## 四、功能与可靠性

### 4.1 [P0][已确认] 家人端后台收不到告警

`MainActivity.onPause()`（`MainActivity.java:237-243`）会把 `familyPoll` 移除；轮询只在 `onResume()`/`setupUi()` 里启动。也就是说**子女手机息屏、切到别的 App、或 App 被系统回收后，不会再拉取云端数据，`maybeNotify` 的系统通知也就不会触发**。老人摔倒后云端虽有 `fall_timeout` 告警，家人要等到主动打开 App 才能看到，只有短信/电话这条链路（依赖老人手机）在起作用。

**建议**（按推荐顺序）
1. 接入推送：国内可用各厂商推送通道（华为/小米/OPPO/vivo）或聚合推送（个推/极光等），由后端在收到告警时主动推送；海外可用 FCM。
2. 如暂时没有后端：用 WorkManager 周期任务（最短 15 分钟）+ 高优先级通知兜底，同时在 UI 明确提示"后台延迟可能较长，请以短信/电话为准"。
3. 告警通知需要 `PendingIntent` 点击跳转、`setCategory(CATEGORY_ALARM)`、声音/震动、独立渠道；目前通知无点击行为、图标用系统自带 `ic_dialog_alert`、且固定 id `2001`（后到的告警会覆盖前一条）。
4. `pollCloud(0)` 在 `setupUi` 与 `onResume` 都会调用，启动时重复拉取一次；`pollCloud` 对每个家庭码各开一个 `new Thread`，建议改为线程池/单次批量。

### 4.2 [P0][已确认] 老人端失联无感知

`family.html:95-98` 的"手环离线"只读 JSON 里的 `connected` 字段，**没有用 `ts` 判断数据是否过期**。老人手机关机/没电/断网/服务被杀后，云端的最后一份 JSON 里 `connected` 仍可能是 1，家人看到的是"手环已连接 + 旧数值"，这对安全类产品是误导性的。

**建议**
- 家人端用 `now - ts` 展示"X 分钟前更新"，超过阈值（如 15 分钟）显示"数据已过期/老人手机可能离线"，超过 30~60 分钟升级为告警。
- 上传快照中为每项指标带 `measuredAt`（见 4.5）。
- 老人端增加低电量/断网/权限被撤销的主动上报。

### 4.3 [P0][已确认] 开机不自启、被杀不自愈

Manifest 中没有 `BOOT_COMPLETED` 接收器。手机重启（老人手机没电再开机很常见）后，采集服务不会恢复，要等老人主动点开 App。`START_STICKY` 在国产 ROM 上不可靠，用户划掉最近任务也可能被杀。

**建议**
- 增加 `RECEIVE_BOOT_COMPLETED` + 开机/应用升级（`MY_PACKAGE_REPLACED`）接收器拉起服务。
- 引导页按机型（华为/小米/OPPO/vivo）指引"自启动/后台运行/电池无限制"设置。
- 服务内记录心跳，家人端发现长时间无心跳时提示老人端检查（对应 4.2）。

### 4.4 [P0][已确认] 手环匹配过宽

`VpCollectorService.java:639`：`dMac.equalsIgnoreCase(TARGET_MAC) || dName.toUpperCase().contains("H600")`。公开版 `TARGET_MAC` 是占位符，因此实际**只按名称包含 H600 连接**，在养老院、小区、医院等场所有可能连到别人的手环，拿到他人的健康数据并上传到自己的家庭码下。

**建议**
- 首次配对：扫描列表让用户选择（或扫手环背面二维码），连接成功后把 MAC 存入 SharedPreferences，之后只连这一个 MAC。
- 提供"更换手环/解绑"入口，并支持多型号（目前只认 H600）。

### 4.5 [P1][已确认] 数据质量：旧值冒充新值

`finishRound()` 把 `lastHr/lastSpo2/lastBpH/lastSugar/lastStress/lastTemp…` 原样写入每一行 CSV 和 `seq`，哪怕这一轮该指标根本没有测到（血压/血糖/体温有 30 分钟节拍；心率因运动判定被保留 `lastHr`；佩戴异常时沿用旧值）。后果：

- CSV 中同一个血压/血糖值会重复出现很多行，7 天均值（`computeHistJson`）按行数而不是按"实际测量次数"加权，统计失真。
- 云端 `latest.json` 只带一个上传时间 `ts`，没有各项指标自己的测量时间，家人看到的"当前血压"可能是 30 分钟甚至更久以前的值。
- 摔倒后手环脱落、佩戴不良等情况下，旧心率会让家人误以为"一切正常"。

**建议**
- 每项指标独立记录 `value + measuredAt`，写 CSV 时只在"本轮确有新测量值"时写入，否则留空；UI 展示"测量于 HH:mm"。
- 佩戴状态（`STATE_HEART_WEAR_ERROR`、`UNPASS_WEAR`）单独作为字段上报，家人端据此显示"未佩戴"。

其他同类问题：
- `finishHrv()` 在成功路径内调用 `storeHrv()`，后者又**追加一行 CSV** 并调用 `broadcastAll()`，一轮里会出现两行；且该行只有 11 列（缺体温列），与 12 列表头不一致；日志文案"(from manual test)"与实际用途不符。
- `loadSeqFromCsv` 里"修复表头"字符串是 12 列，日志却写 "11 cols"，说明历史上改过列数；旧版本数据迁移靠字符串拼接修补，建议改用显式 schema 版本。
- `computeHistJson` 在循环内 `Double.parseDouble` 无单行容错，任何一行脏数据会使整段历史返回 `[]`。

### 4.6 [P1][已确认] 告警逻辑

1. **反复发短信**：`maybeAlertHr/Spo2/Sugar` 的第 2 阶段触发后把 stage 置 0，下一次仍异常又回到第 1 阶段（手环提醒）、再下一次才第 2 阶段（短信），在 5 分钟采样周期下，持续异常会**约每 10 分钟发一次短信 + 云端告警**，无升级策略、无静默期、无"已确认/已处理"。建议：增加冷却时间（如同类告警 1 小时内只发一次）、升级链（手环提醒 → 老人确认 → 家人 A → 家人 B → 电话）、家人端"已知晓"回执。
2. **"连续"的定义偏弱**：第 2 次越界只要求与第 1 次相隔 ≥ 30 秒，而实际采样间隔 ≥ 3~5 分钟；第 1 次若是单点噪声（HR 只取"第一个有效值"，`startHeart` 一旦 `hrDone` 就停止测量），整条链路缺乏平滑。建议用滑动窗口/中位数，并结合运动状态（`accMotionG`）与佩戴状态抑制误报。
3. **阈值不分人群**：HR 上限 140/下限 45、SpO2 90、血糖 3.9 等是全局默认，老人个体差异大（β 受体阻滞剂、房颤、COPD 基线血氧偏低等）。建议首次设置时按年龄/基础病给出推荐值，并允许夜间/日间不同阈值。
4. **`sendBandMsg` 以 "10086" 作为发送方**（`VpCollectorService.java:2342`）向手环推"短信"，可能被老人当成运营商短信忽略或误解。建议使用中性标识并配合振动。
5. **阈值设置入口分裂**：老人端本地设置、家人端云端设置、手环固件心率报警（`setupHeartWarning`）三处，家人端调整要最长 3 分钟生效，且 `pullCloudConfig` 以"云端覆盖本地"为准，没有版本号/来源/冲突处理。建议配置带 `version` 与 `updatedBy`，并回写"已生效"确认给家人端。

### 4.7 [P1][已确认 / 部分待验证] 摔倒检测

优点：三闸门（撞击 → 静止 → 姿态翻转）+ 30 秒确认 + 2 分钟冷却，比单一阈值可靠得多，日志也完整。需要关注：

1. **采样率**：代码以"约 1 个样本/秒"为假设（`FALL_STILL_N=3` 注释"约 3 秒"、UI 转发限频 1 秒）。如果手环 GSensor 通过 BLE 推送的频率也在 1Hz 左右，则几十到几百毫秒的撞击峰值**很可能直接漏采**。[待验证] 建议用真实跌倒/ADL（日常活动）数据评估灵敏度与误报率，并优先确认手环是否有固件级跌倒检测可用。
2. **基线校准**：`accBaselineMg` 取前 8 个样本平均，假设佩戴者此时静止；断连重连、服务重启后不会重新校准，运动中校准会让 `g` 值整体偏移。建议重连后重置并在静止窗口内校准。
3. **除零保护**：`ux = x / mg` 当 `mg == 0` 时得到 NaN，会让后续姿态比较恒为假。
4. **确认界面可见性**：确认弹层是 WebView 内的 JS 元素。老人没开着 App 或手机息屏时，`fallPrompt` 广播没有对应的声音/震动/全屏通知，只靠手环短信式提醒。建议使用高优先级通知 + 全屏意图（Full-screen intent）+ 手机铃声/TTS 语音"您是否摔倒了？"，并支持手环按键确认（若硬件支持）。
5. **倒计时靠 `ui.postDelayed`**：30 秒窗口内进程被杀，则既不会发短信也不会通知家人。建议用前台服务内的持久化状态（写 SharedPreferences）并在重建时恢复未完成的告警。
6. **后台拨号**：`sendFallCall` 最后回退到 `startActivity(ACTION_CALL)`（`VpCollectorService.java:1572-1575`），在 Android 10+ 后台启动 Activity 受限，大概率静默失败；`TelecomManager.placeCall` 路径更可靠，但仍要求 `CALL_PHONE` 已授予。
7. 手机号校验仅为 `length() >= 11`，没有区号/格式校验，也没有拨号失败后的备选（比如拨第二联系人）。

### 4.8 [P1][已确认] 权限申请不完整

`MainActivity.java:98` 只请求了 `SEND_SMS` 与 `CALL_PHONE`，**没有请求定位/蓝牙运行时权限**；`ensureReady()` 只在缺少定位权限时 toast "缺少定位权限，无法扫描蓝牙"（`VpCollectorService.java:623-627`），不会拉起授权。结果：首次安装在 Android 6–11 上很可能扫描不到手环，需要用户自己去设置里授权。Android 12+ 的 `BLUETOOTH_SCAN/CONNECT` 在当前 targetSdk 下的行为需要真机验证 [待验证]。另外，权限请求结果（`onRequestPermissionsResult`）无处理，用户拒绝后没有任何引导；电池优化白名单弹窗在每次 `setupUi()` 都会尝试弹出，被拒绝后会反复打扰。

**建议**：做一个首次使用引导页，依次说明并申请：蓝牙/定位、通知、短信、电话、电池优化、自启动；拒绝时展示"缺什么 → 影响什么 → 去设置"，并在首页持续显示"权限缺失"的醒目状态。

### 4.9 [P1][已确认] 其他功能缺陷

- **`saveEcg`**（`MainActivity.java:368-385`）直接写 `getExternalStoragePublicDirectory(Pictures)`，Manifest 无 `WRITE_EXTERNAL_STORAGE`，也未设置 `requestLegacyExternalStorage`；在 Android 10+ 上**大概率保存失败**。应改用 `MediaStore`（无需权限）。
- **身份切换残留**：`resetRole()` 只清空 role 与 code，**没有停止前台采集服务**，也不清理本机缓存的家人列表和 `latest_*`、`config_*`。老人端改选家人端后，服务继续上传/发短信。
- **`app.html` 内存在第二套身份概念**（`ROLE`、`chooseRole('child')`、`fg_role`），与 Java 层 `CloudSync.role()` 重复，可能出现两层状态不一致。
- **家人端按名称称呼**仅是 `name` 字段，无头像/关系标签；多家庭码时没有按"未读告警"排序。
- `onBackPressed()` 已废弃，使用 `OnBackPressedDispatcher`。
- Activity 没有处理屏幕旋转/配置变更（WebView 会被重建，页面状态丢失），建议锁定竖屏或声明 `configChanges`；`web` 在 `onDestroy` 未销毁，存在泄漏。

---

## 五、架构

### 5.1 [P1][已确认] `VpCollectorService` 上帝类（2757 行）

一个类同时承担：BLE 扫描/连接/重连、采集调度（AlarmManager）、手环命令状态机、告警规则、跌倒算法、短信/电话、CSV 读写与聚合、睡眠修正算法、云同步调度、阈值管理、微体检/ECG/血液成分等一次性检测、向 UI 广播。可读性、可测性、可维护性都很差，改一处风险很大。

**建议拆分（示意）**

```
service/
  CollectorService          # 仅做前台服务壳 + 生命周期 + 调度入口
ble/
  BandConnection            # 扫描/连接/重连/状态回调（对外暴露连接状态 Flow/LiveData）
  BandCommands              # 对 Veepoo SDK 的薄封装（统一超时、统一回调转协程/Future）
collect/
  RoundScheduler            # 5/3 分钟节拍、Doze/闹钟
  RoundRunner               # 一轮采集：用"步骤列表 + 超时"替代布尔量
alert/
  AlertEngine               # 阈值 + 滑动窗口 + 冷却 + 升级策略（纯 Java，可单测）
  FallDetector              # 三闸门算法（纯 Java，可用采集数据回放测试）
  Notifier                  # 短信/电话/手环提醒/云端告警
data/
  HealthRepository          # Room/SQLite，替代 CSV + SharedPreferences JSON
  SleepAnalyzer             # 睡眠选择/修正/分期（纯函数）
cloud/
  CloudClient               # 抽象接口：Upload/Download/Config，TOS 实现可替换
```

### 5.2 [P1][已确认] 手工布尔状态机

采集链路 `readBattery → readStep → startHeart → startSpo2 → maybeBp → maybeHrv → maybeStress → maybeSugar → maybeTemperature → maybeSleep → finishRound` 用大量字段（`batteryDone/stepDone/hrRequested/hrDone/spo2Requested/…`，约 40 个）和**每步各自的 `postDelayed` 超时**拼接。常见隐患：

- 各 `xxxRequested` 标志在 `startFullRound/startLightRound/startBoostRound` 中重置得不一致（例如 `stressRequested/sugarRequested/tempRequested` 没有被重置，断连后靠 90/90/30 秒超时才自愈）。
- 超时 Runnable 不会取消；上一轮遗留的定时器可能在下一轮的错误阶段触发回调。
- 某一步 SDK 回调永不返回时，没有"整轮看门狗"，`flowStarted` 会一直为 true 直到断连事件重置，期间所有 tick 被跳过。
- 一轮最坏耗时：心率 90s + 血氧 180s + 血压 75s + HRV 50s + 压力 90s + 血糖 90s + 体温 30s + 睡眠 30s ≈ 10 分钟，超过 5 分钟节拍，实际周期会被拉长。

**建议**：抽象 `Step` 接口（`start()/onResult()/timeout()/cancel()`），由 `RoundRunner` 顺序执行并统一管理超时与取消；增加整轮 watchdog（如 12 分钟强制收尾）；考虑用协程/`CompletableFuture` 改写回调嵌套。

### 5.3 [P1][已确认] Java↔WebView 通信

Service → Activity 用 `LocalBroadcastManager`（已废弃）携带 60 多种 extra，Activity 再逐项 `evaluateJavascript("setXxx(...)")`；JS → Java 是 30 多个 `@JavascriptInterface`。每新增一个指标要同时改 Service、`rx` 的 if 链、HTML 里的 `setXxx`。

**建议**
- 定义统一的**状态快照对象**（`HealthState`），Service 持有，UI 订阅（`StateFlow/LiveData`），WebView 只收一份 JSON：`window.__render(state)`。
- 命令方向用单一入口 `AndroidBridge.invoke(name, jsonArgs)` + Java 侧白名单分发，便于加校验与日志。
- 中长期建议把老人端界面（字体大、操作少）改成原生 UI（Compose/XML）；家人端复杂图表可以保留 WebView 或改为 MPAndroidChart/Compose Charts。WebView 的"老人界面"在老旧低端机上性能与无障碍（TalkBack、字体缩放）都不理想。

### 5.4 [P1][已确认] 前端工程

- `app.html` 约 126 KB 单文件，JS/CSS/HTML 内联，`family.html` 另有一套类似逻辑（视图、图表、阈值表单重复）。
- 数据持久化混用 `localStorage`（用药提醒、目标步数、ECG 历史、手机号、告警开关、阈值）与 Java 侧 SharedPreferences，同一份数据两处存（如手机号既在 `fg_em1/fg_em2`，又在 `fg_family`，阈值既在 `fg_thresh`(localStorage) 又在 `fg_thresh`(SharedPreferences)），清除 WebView 缓存或重装就会不一致。
- **用药提醒（`medTick`）运行在 WebView 的 JS 定时器里**，App 退到后台/息屏后 WebView 会被暂停，提醒可能不触发；这类提醒应该用 `AlarmManager` + 通知实现。
- 建议引入轻量构建（Vite + 模块拆分 + 单元测试），或至少把 JS/CSS 拆成独立文件、`data → view` 分层。

### 5.5 [P1][已确认] 数据层

- 用 CSV 做时序存储，聚合每次全量读取（见 6.1）；用 `SharedPreferences` 存 JSON 数组（告警、微体检报告）；用 `localStorage` 存 ECG 波形（可能超过 5MB 限额而静默失败）。
- 建议统一迁移到 **Room（SQLite）**：表 `measurement(type, value, measuredAt, source)`、`alert`、`report`，查询、聚合、保留策略、导出 CSV 都更简单；顺便解决 4.5 的"旧值冒充新值"。

---

## 六、性能与资源

### 6.1 [P1][已确认] CSV 全量读写

- `broadcastAll()` 每 30 秒（连接时，`recsTick`）以及每轮结束、每次 `storeHrv` 时调用，其中 `computeHistJson()` 和 `computeRecsJson()` **各自完整读取并 `split("\n")` 整个 CSV**。CSV 永不轮转，每天约 288 行，一年约 10 万行、数 MB，持续 GC 抖动与耗电。
- 小时级上传把整份 CSV `readAllBytes` 进内存再上传，文件名只按"日期"区分，实际内容却是全部历史，带宽与存储逐日放大。
- `dbg()` 写 `debug.log`、`writeAlarmLog()` 写 `alarm.log`：无大小限制、无轮转。`FileWriter` 等未使用 try-with-resources，异常时可能泄漏句柄。

**建议**：改用 Room；只在数据变化时推送增量；日志按天/按大小轮转；上传只传当日增量并压缩；`FileWriter/InputStream` 一律 try-with-resources。

### 6.2 [P1][已确认] 云同步

- `CloudSync` 里每个方法都 `new TOSV2ClientBuilder().build(...)`，用完 `close()`。底层是重量级 HTTP 客户端，应当复用单例；`upload/download/saveConfig/downloadConfig/uploadBytes` 五段近乎相同的代码应抽成两个私有方法。
- `InputStream` 在异常路径上没有关闭（`in.close()` 不在 `finally` 内）。
- 每 3 分钟固定上传全量快照，与数据是否变化无关；网络错误无退避重试，也不区分无网/鉴权失败。
- 老人端同时读取 `config.json` + 写 `latest.json`，没有使用 `ETag/If-None-Match`。
- 没有对象版本化、没有大小限制、没有写失败后的本地待传队列。

### 6.3 [P1][已确认] 线程与并发

- `connected/flowStarted/last*/thresholds` 等字段会被：主线程 Handler、BLE SDK 回调线程、`new Thread` 的上传/配置拉取线程同时读写，**均非 `volatile`/未同步**。例如 `uploadSnapshot` 的后台线程读取 `lastHr` 等；`pullCloudConfig` 的后台线程直接修改阈值与 stage。建议统一回到同一个串行 Executor/`Looper`，或在边界处传不可变快照。
- 大量 `new Thread(...)`：建议用统一的线程池。
- `accGWindow`（`ArrayList<Double>` + `remove(0)`）、`ecgWaveBuf`（`ArrayList<Integer>`，上限 60000 且 `remove(0)` 是 O(n)）在高频回调中使用，建议换 `ArrayDeque` 或环形缓冲。
- BLE 回调里直接做 `Log.w` + 字符串拼接 + 文件写入，建议把磁盘 IO 移出回调线程。

### 6.4 [P1][已确认] 生命周期泄漏

`VpCollectorService.onDestroy()`（`VpCollectorService.java:2742-2755`）：

- 只注销了 `rx/rx2/rx3/rx4/btRx`，**未注销 `rxFamily/rxThresh/rxFall/rx5/rxAcc` 和 `smsResultRx`**（后者是系统级 `registerReceiver`，会产生 "Leaked IntentReceiver" 并持有 Service）。
- **没有 `ui.removeCallbacksAndMessages(null)`**：`scheduleCloudUpload`（每 3 分钟递归 `postDelayed(this)`）、`recsTick`（每 30 秒）以及各类超时 Runnable 在 Service 销毁后仍会继续运行；而 `onCreate` 每次又会再注册一份新的循环 → **进程内如果 Service 被销毁重建（`START_STICKY`、系统回收等），云上传与广播循环会叠加成多份**，同一份数据被重复上传。
- 没有取消 `AlarmManager` 里的 `ACTION_TICK/ACTION_RETRY`。
- `wakeLock` 在 `setReferenceCounted(false)` 下每次 `acquire(15min)`，如果一轮失败且从未走到 `finishRound`，只能靠 15 分钟超时释放。

### 6.5 [P2][已确认] 电量

- 持续 BLE 连接 + 每 5 分钟（醒来后 3 分钟）主动触发整套测量，手环与手机都耗电；在睡眠时段与白天采用同一密度，没有"静止/睡眠时降频"策略。
- `setExactAndAllowWhileIdle` 在 Doze 下系统会限制同一应用约每 9 分钟一次，实际节拍并不精确，文档与注释应据此修正期望。
- 提升 targetSdk 后需要 `SCHEDULE_EXACT_ALARM`（Android 12+）并引导用户授权。

---

## 七、构建、依赖与发布

| 项 | 现状 | 建议 |
|---|---|---|
| targetSdk | 29（compileSdk 34） | 逐步升到 34/35，并按 3.5/4.8 先补齐权限、前台服务类型、精确闹钟、通知权限；Android 14+ 对 targetSdk 过低的 App 有安装/运行限制 |
| 发布包 | 仅有 `debug` buildType，README 明确使用 Debug 包分发 | 增加 `release` 签名配置（密钥库不入库，放 CI 密钥）、开启 R8 混淆与资源压缩，并为 SDK 与 JS 接口添加 keep 规则（`@JavascriptInterface` 方法不可被混淆） |
| 依赖管理 | `fileTree(include: ['*.jar','*.aar'])` 全量引入 `libs/` | 逐个声明依赖，便于审计和升级 |
| 未使用依赖 | 源码仅引用 `com.veepoo.*`、`com.inuker.*`、`com.volcengine.tos`；`JL_Watch`、`jl_bt_ota`、`jl_rcsp`、`BmpConvert`、`abpartool`、`libdfu`、`libfastdfu`、`mcumgr-*`、`nordic scanner`、`gson-2.2.4`（2012 年版本）等在项目源码中均未直接引用 | [待验证] 其中部分可能是 Veepoo SDK 的传递依赖，请逐个移除并编译/真机验证后裁剪，可显著缩小 APK、降低攻击面 |
| 原生库 | `jniLibs` 含 arm64/armeabi-v7a/x86/x86_64 | 通过 `ndk.abiFilters` 只保留需要的 ABI；若升级 targetSdk 35，需确认 `.so` 满足 16KB 页大小要求 |
| 仓库源 | 阿里云镜像 + Gradle 腾讯镜像 | 国内开发方便，但要加入官方源作为兜底，并给 wrapper 增加 `distributionSha256Sum` |
| Gradle/AGP | AGP 8.1.1 + Gradle 8.12，旧式 `compileSdkVersion 34` 写法 | 升级到当前稳定 AGP，改用 `compileSdk`/`minSdk` DSL；引入 version catalog（`libs.versions.toml`） |
| 依赖库版本 | `localbroadcastmanager`（已停止维护）、`mcumgr 2.7.4`、`ve-tos-java-sdk 2.9.4` | 替换 LocalBroadcastManager；评估 TOS SDK 在 Android 上的体积（Java SDK 较重，必要时改用 REST + OkHttp + 预签名 URL） |
| Wrapper 属性文件 | 首行含 BOM 字符 | 统一为 UTF-8 无 BOM |

---

## 八、代码规范与可维护性

1. **乱码注释**：`VpCollectorService.java` 中第 1263、2199、2202 行的注释为 GBK/UTF-8 混编后的乱码；`.editorconfig` + 统一 UTF-8，并在 CI 里检测。
2. **风格不一**：同一文件中混用匿名内部类与 lambda、`android.content.BroadcastReceiver` 全限定名与导入名、制表/缩进错位（如 `private boolean accRunning=false;` 缩进异常、`accMotionG = ...;try {` 同一行多语句、`}    // 一次性检查…` 等），建议引入 Spotless/google-java-format 或 Android Studio 默认格式化 + CI 校验。
3. **命名**：`rx/rx2/rx3/rx4/rx5/rxAcc` 无语义、`hrDone/hrvDone` 等；常量散落（`MIN3/MIN5/MIN10/MIN30/MIN60`、各种 90000/180000 魔数）；建议集中为 `Config`/`Thresholds` 常量并写明单位。
4. **注释与代码脱节**：如 `isNeedCurve=false` 注释与实际传 `true` 不符（`VpCollectorService.java:2644-2645`），`maybeEnableAutoHrv` 前缺换行，`storeHrv` 日志文案与用途不符。
5. **文案硬编码**：全部中文字符串写在 Java/HTML 里，未使用 `strings.xml`；部分地方用了 `已保...` 转义，风格不统一。建议至少 Java 侧提取到资源文件，便于未来多语言/大字版本。
6. **日志规范**：212 处 `Log.w`，几乎全部是 warn 级；应分级（d/i/w/e），统一 TAG，敏感信息脱敏（见 3.3）。
7. **异常处理**：大量 `catch (Exception ignore) { }`，吞掉异常无记录；建议至少记录，并在关键链路（上传、短信、拨号）上报失败状态到 UI/云端。
8. **没有任何自动化测试**：建议优先给以下**纯逻辑**补单元测试——`FallDetector`（用真实/合成加速度序列回放）、`AlertEngine`、`calcHrvRmssd`、睡眠选择与修正（`pickSleep/correctSleepEnd`）、CSV 解析与聚合、阈值 clamp。这些逻辑拆出 Service 后几乎零成本即可测试。
9. **缺少文档**：README 没有架构图、权限清单、数据字段说明、告警策略说明、隐私说明与手环型号兼容表。
10. **许可证与合规**：仓库无 `LICENSE`；`app/libs` 中含 Veepoo、Jieli 等第三方闭源 SDK 二进制，公开再分发通常需要厂商许可，建议核对授权或改为"由使用者自行下载放入 libs"。
11. **版本信息**：版本只在 `app/build.gradle` 手工维护，建议由 CI 生成并写入 About 页，便于定位问题。

---

## 九、产品与合规

1. **医疗声明**：血糖（无创估算）、血液成分（尿酸/总胆固醇/甘油三酯/HDL/LDL）、抑郁风险、皮电、ECG 的"疾病风险提示"等，来自消费级手环，**不是医疗器械级结果**。App 把它们与"告警/短信通知家人"放在同一体验里，需要：显著的非诊断声明、值域/可信度标识、对"疾病风险"类文案的审慎措辞，避免老人或家人据此做医疗决策；若面向公众发布，需评估医疗器械/互联网诊疗相关监管要求。
2. **同意与告知**：老人是数据主体，需要在老人端以大字清晰告知"会采集什么、上传到哪里、谁能看、如何关闭/删除"；子女代装时也应有老人知情同意记录。
3. **误报与告警疲劳**：参见 4.6；建议在家人端提供"告警历史 + 已处理标记 + 静音时段"，在老人端提供"我没事"的一键回复。
4. **无障碍与适老化**：老人端需要 TalkBack 可用、字体大小跟随系统、足够的触控区域与对比度，WebView 页面中很多 `onclick` 的 `div` 缺少语义（role/label）。
5. **紧急情况兜底**：SOS 页面（`startSos`）建议在 Java 侧完成"拨号 + 短信 + 云端告警"完整闭环并提示失败，不依赖 WebView 前台状态。
6. **多手环/多老人**：当前一台老人手机只能对应一个家庭码、一个手环型号；若要扩展需要在数据模型里引入 `deviceId/elderId`。

---

## 十、建议的落地路线图

**第 1 阶段（约 1~2 周，止血）——先解决 P0**
1. 撤掉客户端内置的主密钥：改为最小权限子账号 + 用量告警；同步启动后端/预签名方案的设计。
2. 家庭码改为高熵随机串；`addCode` 增加格式校验。
3. 上传日志默认关闭、手机号脱敏、日志分级；Manifest 设置 `allowBackup=false`。
4. Java→JS 统一用安全序列化；前端动态文本统一转义；删除 `debug.html`。
5. 家人端：展示数据更新时间与过期提示；加开机自启接收器；手环绑定到具体 MAC。
6. 补全 `onDestroy` 的接收器注销与 `removeCallbacksAndMessages`、取消闹钟。

**第 2 阶段（约 2~4 周）——告警可靠性与数据质量**
1. 家人端后台推送（厂商通道/聚合推送）+ 带点击跳转的告警通知；老人端心跳上报。
2. 告警引擎：滑动窗口、冷却、升级链、家人端"已知晓"回执；跌倒确认改为全屏通知 + 声音。
3. 指标独立时间戳，CSV/上传仅写入真实新测量值；佩戴状态上报。
4. 引导页：集中申请蓝牙/定位/通知/短信/电话/电池优化，并持续显示权限缺失。

**第 3 阶段（约 1~2 个月）——架构重构**
1. 拆分 `VpCollectorService`（见 5.1），采集步骤改为 `Step` 抽象 + 统一超时 + 整轮 watchdog。
2. 数据层迁移 Room；云层抽象 `CloudClient`；统一 `HealthState` 通信模型替代 60+ 广播 extra。
3. 为 `FallDetector/AlertEngine/Sleep/Hrv/CSV` 补单元测试并接入 CI（lint + 单测 + assemble）。
4. 构建升级：targetSdk 提升、R8、release 签名、依赖裁剪、ABI 过滤。

**第 4 阶段（持续）**
- 老人端原生化与适老化；家人端图表优化；多手环/多老人模型；隐私与合规文档；跌倒算法用真实数据持续标定。

---

## 十一、亮点（建议保留）

- 对 SDK 行为的容错意识强：几乎每一步都有超时兜底、`try/catch`，蓝牙关闭、扫描启动失败、HRV 不支持等边界都有处理；
- 摔倒检测采用多闸门 + 人工确认 + 冷却，思路正确，日志（`alarm.log`）对后续调参很有价值；
- 阈值有 `clamp` 范围保护，远程配置也做了范围校验；
- 采集节拍用 `AlarmManager.setExactAndAllowWhileIdle` + `WakeLock` 应对息屏/Doze，并有三轴数据静默看门狗；
- 多家庭码、远程阈值、睡眠修正等细节体现了对真实使用场景的打磨；
- 公开版已主动移除真实密钥/MAC/备份文件，并在 README 中明确提示。

---

## 附：关键位置索引

| 主题 | 位置 |
|---|---|
| 内置 AK/SK | `app/src/main/java/com/family/guardian/CloudSync.java:22-24` |
| 日志上传默认开启 | `CloudSync.java:164` |
| 手机号写入 alarm.log | `VpCollectorService.java:1462-1472, 1550` |
| 手环仅按名称匹配 | `VpCollectorService.java:639` |
| JS 注入点 | `MainActivity.java:150, 171, 303-321` |
| 家人端仅前台轮询 | `MainActivity.java:219-248` |
| 离线判断未用时间戳 | `app/src/main/assets/family.html:95-98` |
| 权限请求不完整 | `MainActivity.java:96-99`、`VpCollectorService.java:620-629` |
| 摔倒判定 | `VpCollectorService.java:1275-1426` |
| 告警阶段逻辑 | `VpCollectorService.java:937-950, 2223-2269` |
| 采集状态机 | `VpCollectorService.java:789-1240` |
| onDestroy 泄漏 | `VpCollectorService.java:2742-2755` |
| CSV 全量读取 | `VpCollectorService.java:2044-2145` |
| `saveEcg` 公共目录写入 | `MainActivity.java:368-385` |
| `resetRole` 未停服务 | `MainActivity.java:485-489` |

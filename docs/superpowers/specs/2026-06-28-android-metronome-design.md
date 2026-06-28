# Android 节拍器移植 — 设计文档

- **日期**: 2026-06-28
- **作者**: Brainstorming 会话产出
- **状态**: 待用户审阅
- **参考实现**: `D:\Github\Metronome` (Windows 桌面端，Python)

---

## 1. 目标与背景

将 Windows 版「超慢跑节拍器」移植到 Android 平台，**功能完全对齐**，并补足 Android 跑步场景必须的锁屏控件与后台稳定运行能力。

**移植基线（来自 Windows 版 README & 源码）**：
- 超慢跑 120/150/180 BPM 步频实时切换
- 30/45/自由/自定义 4 种计时模式
- 双音色木鱼交替（重音左脚 / 轻音右脚）
- C 大三和弦结束提示音
- 运动记录 + 月历视图 + ISO 周聚合 + 连续天数统计
- < 3 分钟运动不计入
- Fluent 暗黑风格 UI

**Android 端新增能力**：
- Foreground Service 后台稳定运行（锁屏 / 切后台继续发声）
- MediaSession 锁屏 / 通知 / 蓝牙耳机控件
- **横屏适配策略**：手机强制竖屏（与 Windows 版 480x680 一致），平板支持横屏（左右双栏：左 = 主控件，右 = 日历缩略）

---

## 2. 决策摘要

| 维度 | 选择 | 理由 |
|------|------|------|
| 技术栈 | Kotlin + Jetpack Compose + Oboe + Room + Coroutines | 原生体验最佳；Oboe 提供 sample-accurate 音频回调（对标 Windows 版 sounddevice） |
| minSdk / targetSdk | 26 / 35 | 覆盖 ~95% 设备；Foreground Service 类型受限少；Oboe 全功能可用 |
| 架构 | Service 总指挥（Approach A） | Foreground Service 拥有音频引擎、计时器、MediaSession；ViewModel 仅做状态镜像 |
| 本地化 | 仅中文 | 与 Windows 版一致 |
| Wear OS | 不做 | YAGNI，开发量翻倍且用户群体小 |
| 测试策略 | TDD 强制（红绿重构） | 核心引擎 ≥ 90% 覆盖率；UI ≥ 60% |

---

## 3. 架构总览

```
┌─────────────────────────────────────────────────────────────┐
│  UI 层 (Compose)                                            │
│  ├─ HomeScreen      (主屏: 时间 / 模式 / BPM / 控制)        │
│  ├─ CalendarScreen  (月历: 运动记录可视化)                 │
│  └─ WeekStatsCard   (周聚合统计)                            │
└─────────────────────┬───────────────────────────────────────┘
                      │ collectAsState (StateFlow)
┌─────────────────────▼───────────────────────────────────────┐
│  ViewModel 层 (状态镜像，每个屏幕一个)                      │
│  ├─ HomeViewModel       — 主屏状态镜像 + 用户意图转发        │
│  └─ CalendarViewModel   — 日历屏聚合状态镜像                 │
└─────────────────────┬───────────────────────────────────────┘
                      │ bind + Intent (binder RPC)
┌─────────────────────▼───────────────────────────────────────┐
│  Service 层 (MetronomeService, Foreground Service)          │
│  ├─ MetronomeController (状态机: stopped/running/paused)    │
│  ├─ AudioEngine         (Oboe PCM 回调, sample-accurate)   │
│  ├─ TimerEngine         (高精度差分计时协程)                │
│  ├─ MediaSessionManager (锁屏/通知/蓝牙控件)                │
│  └─ SessionRecorder     (运动会话数据持久化)                │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│  Data 层                                                    │
│  ├─ Room DB (exercise_sessions 单表)                        │
│  └─ Repository (ExerciseRepository)                        │
└─────────────────────────────────────────────────────────────┘
```

**Service 是唯一权威状态源**。所有状态变化由 Service 内部 StateFlow 推送，ViewModel 通过 `LocalBinder` 接收并镜像到 Compose。

---

## 4. 状态机

### 4.1 运行状态（与 Windows 版严格对齐）

```
                  ┌──────────────────────────────┐
                  ▼                              │
   ┌────────┐ start  ┌─────────┐  pause  ┌─────────┐
   │stopped │───────▶│ running │────────▶│ paused │
   └────────┘        └─────────┘◀────────└─────────┘
        ▲                 │       start
        │ stop            │ 自然倒计时结束 (timer → 0)
        └─────────────────┘
```

- **3 个状态**: `STOPPED` / `RUNNING` / `PAUSED`
- **状态迁移合法性**：
  - `stopped → running` (start)
  - `running → paused` (pause)
  - `paused → running` (start 恢复)
  - `running → stopped` (stop 手动) 或 (自然倒计时结束)
  - `paused → stopped` (stop 手动)
- **非法迁移**: 运行中调用 `setMode` 被拒绝（与 Windows 版一致）

### 4.2 计时模式

- `30min` — 固定 30 分钟倒计时
- `45min` — 固定 45 分钟倒计时
- `free` — 无倒计时，统计 elapsed
- `custom` — 5~120 分钟（步长 5 分钟，默认 20 分钟）

### 4.3 BPM 设置

- 范围 60~240（与 Windows 版一致）
- 三档预设：120 / 150 / 180
- 自定义范围 120~200（步长 1 BPM）
- **运行时可热切换**，仅更新 `beat_interval_samples`，**不重合成**波形

---

## 5. 核心数据结构

### 5.1 MetronomeState（Service 内 StateFlow<MetronomeState>）

```kotlin
data class MetronomeState(
    val runState: RunState,            // STOPPED / RUNNING / PAUSED
    val mode: TimerMode,               // 30min / 45min / free / custom
    val customMinutes: Int,            // 1-120, 步长 5
    val bpm: Int,                      // 60-240
    val volume: Float,                 // 0.0-1.0
    val timeLeftSec: Int,              // 倒计时剩余（free 模式 = 0）
    val timeElapsedSec: Int,           // 已跑时长
    val totalDurationSec: Int,         // 本次目标时长
    val beatVisualTrigger: BeatType    // HEAVY / LIGHT / NONE（LED 脉动）
)
```

### 5.2 Room Schema

```sql
CREATE TABLE exercise_sessions (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    start_time_ms   INTEGER NOT NULL,
    end_time_ms     INTEGER NOT NULL,
    duration_sec    INTEGER NOT NULL,   -- 冗余存储，加速聚合
    mode            TEXT NOT NULL,      -- "30min" | "45min" | "custom" | "free"
    completed       INTEGER NOT NULL    -- 0=手动停止, 1=自然倒计时结束
);

CREATE INDEX idx_start_time ON exercise_sessions(start_time_ms);
```

**过滤规则**: `duration_sec >= 180`（< 3 分钟不计入统计，与 Windows 版一致）

### 5.3 DAO 关键查询

- `observeAllValidSessions()`: Flow 订阅所有有效会话
- `aggregateMonth(fromMs, toMs)`: 按日 GROUP BY，返回 `(day, total_sec, cnt)`
- `aggregateWeek(fromMs, toMs)`: 按 ISO 周 GROUP BY
- `recentActiveDays(limit)`: 用于连续天数计算

**连续天数算法**（与 Windows 版 `get_streak` 一致）：
1. 取今天日期；今天无运动则从昨天起算
2. 从该日往前逐日检查 `total_seconds > 0`
3. 遇到无运动日停止计数

---

## 6. 关键时序

### 6.1 BPM 实时切换
```
用户点击 BPM 预设按钮
       │
       ▼
ViewModel.setBpm(150) ──▶ Service.MetronomeController.setBpm(150)
       │                            │
       │                            ├─▶ AudioEngine.setBpm(150)
       │                            │       └─ 更新 beat_interval_samples
       │                            │       （不重合成波形）
       │                            │
       │              StateFlow<MetronomeState> 推送新 bpm
       ◀────────────────────────────┘
```

### 6.2 倒计时归零
```
TimerEngine 协程（每 100ms tick）
       │
       ▼ (timeLeft ≤ 0)
MetronomeController
       ├─ runState = STOPPED
       ├─ AudioEngine.stop()           # 立即停节拍木鱼
       ├─ AudioEngine.playEndChime()   # 播放 C 大三和弦琶音
       └─ SessionRecorder.save(completed=true)  # 若 elapsed ≥ 180s
```

### 6.3 LED 脉动同步（与 Windows 版完全对齐）
```
Oboe 音频回调线程 (audio callback)
       │ 设置 beatVisualTrigger = HEAVY/LIGHT
       ▼
TimerEngine 协程 (100ms tick)
       │ collect beatVisualTrigger
       ├─ 立即推送 StateFlow
       ├─ Compose recomposition 启动 LED 动画
       └─ 90ms 后 LED 淡出
       │ 设置 beatVisualTrigger = NONE
```

### 6.4 锁屏控件
```
锁屏 / 通知 / 蓝牙耳机按钮
       │
       ▼
MediaSession Callback (MetronomeService 内)
       │
       ├─ play → MetronomeController.start()
       ├─ pause → MetronomeController.pause()
       └─ stop → MetronomeController.stop() + SessionRecorder.save()
       │
       ▼
更新 MediaSession 元数据 (BPM / 剩余时间 / 状态)
       │
       ▼
系统锁屏 UI 实时刷新
```

---

## 7. 模块文件清单

| 路径 | 职责 |
|------|------|
| `app/src/main/java/com/tangpenghui/metronome/audio/AudioEngine.kt` | Oboe PCM 回调，BPM 解耦的波形预合成 |
| `app/src/main/java/com/tangpenghui/metronome/service/MetronomeService.kt` | Foreground Service，整合 4 个引擎 |
| `app/src/main/java/com/tangpenghui/metronome/service/MetronomeController.kt` | 状态机 + 模式 |
| `app/src/main/java/com/tangpenghui/metronome/service/TimerEngine.kt` | Coroutine + `System.nanoTime()` 差分计时 |
| `app/src/main/java/com/tangpenghui/metronome/service/MediaSessionManager.kt` | MediaSession + 通知 + 锁屏控件 |
| `app/src/main/java/com/tangpenghui/metronome/service/SessionRecorder.kt` | 运动会话保存（< 3 分钟过滤） |
| `app/src/main/java/com/tangpenghui/metronome/data/MetronomeDatabase.kt` | Room 数据库 |
| `app/src/main/java/com/tangpenghui/metronome/data/SessionDao.kt` | DAO 查询 |
| `app/src/main/java/com/tangpenghui/metronome/data/ExerciseRepository.kt` | 仓储模式封装 |
| `app/src/main/java/com/tangpenghui/metronome/data/ExerciseSession.kt` | Room Entity |
| `app/src/main/java/com/tangpenghui/metronome/data/DayAggregate.kt` | 聚合数据类 |
| `app/src/main/java/com/tangpenghui/metronome/data/WeekAggregate.kt` | 聚合数据类 |
| `app/src/main/java/com/tangpenghui/metronome/ui/home/HomeScreen.kt` | 主屏 UI |
| `app/src/main/java/com/tangpenghui/metronome/ui/home/HomeViewModel.kt` | 主屏 ViewModel |
| `app/src/main/java/com/tangpenghui/metronome/ui/calendar/CalendarScreen.kt` | 日历屏 UI |
| `app/src/main/java/com/tangpenghui/metronome/ui/calendar/CalendarViewModel.kt` | 日历屏 ViewModel |
| `app/src/main/java/com/tangpenghui/metronome/ui/theme/Theme.kt` | Material 3 主题（静态色板） |
| `app/src/main/java/com/tangpenghui/metronome/ui/theme/Color.kt` | 配色：极客绿 #10B981、活力蓝 #3B82F6 |
| `app/src/main/java/com/tangpenghui/metronome/MetronomeApp.kt` | Application 类，启动 Service |
| `app/src/main/AndroidManifest.xml` | 声明 Service + 权限 |
| `app/src/test/java/...` | JVM 单元测试 |
| `app/src/androidTest/java/...` | UI / Service 集成测试 |

---

## 8. 错误处理

| 失败场景 | 处理 |
|---------|------|
| Oboe 初始化失败（设备不支持 AAudio） | 降级到 `AudioTrack` MODE_STREAM，仍保证可播放 |
| AudioTrack 也不可用 | 弹出对话框"无法在当前设备播放音频"，禁用开始按钮 |
| 数据库写入失败 | 静默重试 3 次后放弃，日志上报，不阻塞核心功能 |
| Service 被系统杀死 | `START_STICKY` 自动重建，从 `onStartCommand` 恢复状态 |
| 通知权限被拒（Android 13+） | 检测到 `POST_NOTIFICATIONS` 拒绝时，提示用户开启 |
| Foreground Service 类型合规失败（targetSdk 34+） | Service 声明 `foregroundServiceType="mediaPlayback"` |

---

## 9. 测试策略（TDD 强制）

按 `test-driven-development` 技能执行：**先红后绿**。

| 层 | 测试框架 | 关键场景 |
|------|---------|---------|
| **AudioEngine** | JUnit5 | BPM 切换产生正确采样间隔；音量 clamp 到 [0,1]；波形长度固定 |
| **TimerEngine** | JUnit5 + 虚拟时钟 | 100ms tick 推进 elapsed；free 模式不减 timeLeft；倒计时归零触发回调 |
| **MetronomeController** | JUnit5 + Mockito | 状态机迁移合法性；运行中 setMode 被拒；自动保存会话（completed=true） |
| **SessionDao** | Robolectric | 插入 100 条记录聚合正确；连续天数边界（今天/昨天/跨月）；空库返回空 |
| **ExerciseRepository** | JUnit5 | < 3 分钟过滤；按月/周查询 |
| **MetronomeViewModel** | Turbine + Mock Service | Intent 转发正确；StateFlow 推送与重组幂等 |
| **Compose UI** | Compose Test | BPM 切换按钮响应；倒计时归零显示 00:00；日历屏月聚合渲染 |
| **Service 集成** | AndroidTest | 启动 10 分钟后真触发 endChime；START_STICKY 重启后状态正确 |

**覆盖率目标**: 核心引擎 (Audio/Timer/Controller) ≥ 90%；UI ≥ 60%

---

## 10. 性能基准

| 指标 | Windows 版 | Android 目标 |
|------|-----------|-------------|
| 1 小时运行漂移 | 0 (声卡晶振) | ≤ 10ms (Oboe AAudio 优先级线程) |
| BPM 切换响应延迟 | < 50ms | < 50ms（无波形重合成） |
| LED 脉动延迟 | < 90ms | < 90ms |
| CPU 占用 (空闲) | < 1% | < 3% (含 Service / Wakelock) |
| 内存占用 | ~80MB | ≤ 100MB (含 Oboe native lib) |

---

## 11. 范围边界（YAGNI）

### V1 必须有 ✅
- 节拍引擎 + 双音色木鱼（C 大三和弦结束音）
- 4 种计时模式 + BPM 热切换（含自定义）
- 运动记录 + 月历视图 + ISO 周聚合 + 连续天数
- MediaSession 锁屏 / 通知 / 蓝牙控件
- 横屏适配（手机锁竖屏 / 平板支持横屏双栏）
- Foreground Service 后台稳定运行
- < 3 分钟过滤
- 单元测试 + 集成测试

### V1 不做 ❌
- GPS 距离 / 配速追踪
- 心率联动
- 多节奏型（2/4、3/4、6/8）
- Tap Tempo
- 闹钟 / 睡眠定时
- 社交分享
- Wear OS 伴侣
- 与 Windows 版数据同步
- 应用内购买 / 广告

---

## 12. 项目位置

待用户确认。Windows 版位于 `D:\Github\Metronome`，建议 Android 版路径：
- `D:\Github\MetronomeAndroid` 或 `D:\Github\Metronome-Android`

---

## 13. 待办

- [ ] 用户审阅本文档
- [ ] 确认项目根目录位置
- [ ] 调用 `writing-plans` 技能创建实施计划

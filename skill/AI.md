# 实体 AI 系统详解

> 依据 `neoforge-21.1.233-merged.jar` 内反编译源码逐行核对。
> 结论先行：**Goal 隔 tick 决策 -> Navigation 出路径 -> MoveControl 出参数 -> travel 才真正位移**。

---

## 0. 一句话总览

一个 Mob 的服务端 tick：

```
sensing.tick()
targetSelector.tick() / goalSelector.tick()      // 隔 tick
navigation.tick()                                // 每 tick
moveControl.tick() -> lookControl.tick() -> jumpControl.tick()   // 每 tick
travel(xxa, yya, zza)                            // 每 tick，真正改坐标
```

四层职责：

| 层 | 类 | 干什么 |
|---|---|---|
| 决策 | `Goal` / `GoalSelector` | 决定"做什么" |
| 战略 | `PathNavigation` | 算 A* 路径，逐节点给 MoveControl 设目标 |
| 战术 | `MoveControl` / `LookControl` / `JumpControl` | 把目标翻译成 `yRot`、`speed`、`zza`、`jumping` |
| 执行 | `LivingEntity.travel` | 读参数，真正 `move()` 改坐标 |

---

## 1. 核心指导：三大移动机制怎么选？

这是自定义 AI 最容易踩坑的地方。**它们底层最终都是通过 `MoveControl` 修改 `zza`（前后）和 `xxa`（左右）并配合 `yRot`（身体朝向）来产生位移的，但控制方式完全不同，绝不能混用。**

| 场景 | 用什么 | 底层行为与特征 | 适用情况 |
|---|---|---|---|
| **远距离、复杂地形** | `PathNavigation.moveTo(...)` | 算 A* 路径。**底层是在替你把路径节点传给 `MoveControl`。** 在它运行期间（`!isDone()`），**绝对不要手写代码去调 `MoveControl`**，否则两者会打架（每 tick 互相覆盖目标）。 | 巡逻、逃跑、寻找远处的目标、需要绕开方块和跨越障碍。 |
| **短距离、定向直驱** | `MoveControl.setWantedPosition(...)` | 直驱。它会计算到目标的向量，并**改变身体和头部的朝向**（`mob.setYRot`）向前走。 | 冲刺、冲向目标、短距离逼近。 |
| **战斗走位（侧移/后撤）** | `MoveControl.strafe(front, right)` | 以**自身当前朝向为基准**前后左右平移。**完全不影响身体和头部朝向**。 | 保持距离（风筝）、绕圈走位、一边后退一边看着玩家射击。 |

### ⚠️ 核心避坑：直驱 Goal 必须覆写 `requiresUpdateEveryTick`
`GoalSelector` 是隔 tick 检查的。如果你用 `setWantedPosition` 或 `strafe` 做直驱移动，你的 Goal **必须**覆写：
```java
@Override
public boolean requiresUpdateEveryTick() {
    return true; 
}
```
**原因**：`MoveControl.tick()` 每次执行完 `MOVE_TO` 或 `STRAFE` 后，会立即把状态重置为 `WAIT`。在下一个 tick，如果状态是 `WAIT`，它会执行 `mob.setZza(0)` 把速度清零。如果你不每 tick 发送移动指令，实体会走一下、停一下，平均速度腰斩。

---

## 2. 一 tick 的真实调用链

```java
LivingEntity.tick() {
    ...
    this.aiStep();                    // AI 都在这里跑
    ...
    this.tickHeadTurn(f1, f2);        // 身体/头部朝向协调
}

LivingEntity.aiStep() {
    ...
    if (this.isEffectiveAi())         // 服务端才跑
        this.serverAiStep();          // Mob 里是 final
    ...
    this.travel(vec31);               // == 真正位移 ==
}

// Mob.serverAiStep（final，不能被覆写）
Mob.serverAiStep() {
    int i = this.tickCount + this.getId();
    if (i % 2 != 0 && this.tickCount > 1) {
        this.targetSelector.tickRunningGoals(false);  // 只跑 requiresUpdateEveryTick 的
        this.goalSelector.tickRunningGoals(false);
    } else {
        this.targetSelector.tick();                   // 完整：canUse/canContinueToUse
        this.goalSelector.tick();
    }

    this.navigation.tick();          // == 每 tick ==
    this.customServerAiStep();       // 给子类留的钩子
    this.moveControl.tick();         // == 每 tick ==
    this.lookControl.tick();
    this.jumpControl.tick();
}
```

要点：

- **`serverAiStep` 是 `final`**。自定义逻辑写 `customServerAiStep()`。
- **普通 Goal 的 `tick()` 最多每 2 tick 一次**。需要每 tick 驱动的必须覆写 `requiresUpdateEveryTick`。
- **导航与控制器每 tick 都跑**，不受奇偶影响。
- `travel` 只在服务端执行普通 Mob 的权威位移。**客户端靠插值包，所以客户端要读取的状态（如动画标志）必须通过 `SynchedEntityData` 同步。**

---

## 3. GoalSelector 与 TargetSelector

**两者都是 `GoalSelector`**。`targetSelector` 只是专门放 `Flag.TARGET` 的 Goal 而已。

### 3.1 四种 Flag 与互斥

```java
public enum Flag { MOVE, LOOK, JUMP, TARGET; }
```
- Goal 通过 `setFlags(...)` 声明占用。
- **不同 Flag 互不冲突**（MOVE 和 LOOK 可同时跑），**同一 Flag 只能被一个 Goal 占用**。
- `TargetGoal` 统一占 `TARGET`。`MeleeAttackGoal` 占 `MOVE + LOOK`。

### 3.2 控制 Flag 的临时禁用
`Mob.tick()` 每 5 tick 会调 `updateControlFlags()`。如果实体被其他 Mob 骑乘，或坐在船上，`MOVE` / `JUMP` / `LOOK` Flag 会被禁用，对应的 Goal 会立刻 `stop()`。**"我的 Goal 没跑" 先查这里。**

### 3.3 目标选择 (TargetGoal)
- `TargetGoal` 默认不是每 tick 扫全图，而是有 `randomInterval`（通常 5 tick）降频。
- 判定标准：`FOLLOW_RANGE`（跟随距离）、视线（`hasLineOfSight`）、队伍判定。
- **先手反制**：若想让怪主动攻击把它设为目标的人，可用 `getEntitiesOfClass(Mob.class, bb, e -> e.getTarget() == this)` 扫出威胁，丢进高优先级的 TARGET Goal。

---

## 4. 控制器 Control 详解

### 4.1 MoveControl — 战术移动
- `setWantedPosition(x, y, z, speed)`：定向移动。会计算角度并让实体身体转向目标。如果目标高度差大于 `maxUpStep`，会自动跳跃。
- `strafe(forward, right)`：侧向平移。基于实体现有朝向，只改 `xxa` 和 `zza`，**不会让实体转头**。
- **注意**：`Mob.setSpeed` 会顺手 `setZza(speed)`（1.21 特性）。MOVE_TO 分支不需要再写 `setZza(1.0)`。

### 4.2 JumpControl — 跳跃
- `jump()` 只是打个标记。真正的起跳在 `aiStep` 里。必须在当 tick 的控制器阶段调用。

### 4.3 LookControl 与 BodyRotationControl — 视线与身体
- `LookControl` 只动头（`yHeadRot` 和 `xRot`），不动身体（`yBodyRot`）。
- `BodyRotationControl` 负责身体朝向与头部的协调（滞后跟随）。

---

## 5. PathNavigation 详解

**定位：仅用于路途较远，且路径复杂的情况。** 它的特征并非故意走走停停，而是为了实现绕障，必须将 A* 路径拆分成节点。

- `createPath(...)` 是**同步 A***。如果路径极长或复杂，会在当 tick 卡顿。
- 每 tick 它会把下一个节点的坐标通过 `MoveControl.setWantedPosition` 喂给控制器。
- 当实体到达节点判定距离（`maxDistanceToWaypoint`）后，推进到下一个节点。
- **卡住检测**：每 100 tick 检查一次位移，若位移不足则判定卡住并 `stop()`。
- **重算节流**：目标移动后，路径重算有 20 tick 的节流限制。

**死锁警告**：在 `PathNavigation` 运行时（`!isDone()`），你绝对不能在 Goal 里手写 `MoveControl.setWantedPosition`。导航会在每个 tick 覆盖你的指令。

---

## 6. 移动速度：参数含义、乘数关系与参考值

### 6.1 一句话

`navigation.moveTo(target, x)` 和 `moveControl.setWantedPosition(..., x)` 里的 **x 是乘数，不是速度值，也不会覆盖属性**。
真正进入 `LivingEntity.getSpeed()` 的是：

```
getSpeed() = MOVEMENT_SPEED属性 × x
```

源码调用链：

```java
PathNavigation.moveTo(Entity, double speed)      { this.speedModifier = speed; }
// 每 tick：
this.mob.getMoveControl().setWantedPosition(x, y, z, this.speedModifier);
MoveControl.tick()  // MOVE_TO 分支
    this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
Mob.setSpeed(float s) { super.setSpeed(s); this.setZza(s); }        // ← 关键：同时写 zza
LivingEntity.travel(Vec3)                                          // 才真正位移
    this.moveRelative(this.getFrictionInfluencedSpeed(f), travelVector);
```

### 6.2 为什么速度是“平方关系”（最容易踩的坑）

`Mob.setSpeed(s)` 把 `s` **同时**写进 `speed` 和 `zza`；`travel` 里 `moveRelative(amount = speed, relative = (xxa, yya, zza))`，
`getInputVector` 先按向量长度归一化（长度 > 1 才归一），再乘 `amount`。

所以当 `s ≤ 1` 时，每 tick 的速度增量 ∝ `s²`，稳态速度也 ∝ `s²`：

```
地面、默认摩擦 0.6：  格/秒 ≈ 44 × s²          (s = 属性 × 导航系数，s ≤ 1)
                    长度 > 1 后归一化，变成 44 × s（线性）
```

**玩家不适用这个公式**：玩家的 `zza` 来自键盘输入（±1），所以速度 ∝ `属性 × 1`，`0.1 → 4.32 格/秒`（步行）。
这也是为什么“同样 0.1，玩家比怪快得多”。

### 6.3 换算表（地面、默认摩擦，导航系数 1.0）

| 属性值 s | 约格/秒 | 参照 |
|---|---|---|
| `0.23` | 2.3 | 原版僵尸 |
| `0.25` | 2.8 | 骷髅 / 苦力怕 / 铁傀儡 / 女巫 |
| `0.28` | 3.4 | 比僵尸快 50% |
| `0.30` | 4.0 | 蜘蛛 / 末影人，≈ 玩家步行（4.32） |
| `0.35` | 5.4 | 卫道士 / 掠夺者，≈ 玩家冲刺（5.6） |
| `0.40` | 7.0 | **比冲刺玩家还快** |
| `0.50` | 11.0 | 明显失真 |
| `≥1.0` | 线性，1.0 → 44 | 基本等于瞬移 |

想让怪“快 20%”，系数乘 `1.1` 就够了（`1.1² = 1.21`）；乘 1.5 实际快了 2.25 倍——**这是绝大多数“我的怪为什么飞起来”的根因**。

### 6.4 导航系数/突袭系数怎么取

| x | 效果 |
|---|---|
| `1.0` | 正常，只用属性值 |
| `1.1 ~ 1.2` | 略快，适合追击 |
| `1.3 ~ 1.4` | 明显加速，**推荐上限** |
| `1.5 ~ 2.0` | 已经有“漂移”感，慎用 |
| `≥3` | 速度变成 9 倍以上，基本瞬移，不要用 |

### 6.5 原版参考值

| 生物 | `MOVEMENT_SPEED` | 说明 |
|---|---|---|
| 玩家 | `0.1` | 但不走平方公式，实际 4.32 格/秒 |
| 僵尸 | `0.23` | 2.3 格/秒 |
| 骷髅 / 苦力怕 / 铁傀儡 / 女巫 | `0.25` | |
| 蜘蛛 / 末影人 / 狼 / 狐狸 / 劫掠兽 / 监守者 | `0.3` | |
| 卫道士 / 掠夺者 / 猪灵 | `0.35` | 追玩家用 |
| 村民 | `0.5` | 是属性高，但导航系数只有 0.5 |
| 骆驼 `0.09`、悦灵/嗅探兽 `0.1` | | 慢速 |
| 鱼/青蛙/美西螈 `1.0`、海豚 `1.2` | | **水中**才用这个值，和陆地不可比 |

### 6.6 `AttributeSupplier.Builder.add` 是覆盖基础值

```java
Zombie.createAttributes()               // 已有 MOVEMENT_SPEED=0.23
        .add(Attributes.MOVEMENT_SPEED, 0.4F)   // 覆盖成 0.4，不是 0.23+0.4
```

- `.add(attr, v)` = 设**基础值**，覆盖父类给的值。
- 想“在原值上加”必须用 `AttributeModifier`，不能靠 `.add`。

### 6.7 `AttributeModifier` 三种运算（1.21 名字）

| Operation | 公式 | 用途 |
|---|---|---|
| `ADD_VALUE` | `base + x` | 加固定值（如最大生命 -1） |
| `ADD_MULTIPLIED_BASE` | `base + base × x` | **百分比加成**（+10% 写 0.1） |
| `ADD_MULTIPLIED_TOTAL` | `(base + ΣADD_VALUE) × (1 + Σx)` | 最终乘算 |

旧版名称：`ADDITION` / `MULTIPLY_BASE` / `MULTIPLY_TOTAL`。

### 6.8 其他属性参考范围

| 属性 | 原版范围 | 备注 |
|---|---|---|
| `MAX_HEALTH` | 普通怪 10~30；铁傀儡/劫掠兽 100；监守者 500 | 基础 20 |
| `ATTACK_DAMAGE` | 普通怪 2~5；铁傀儡 15；劫掠兽 12；监守者 30 | 空手玩家 1 |
| `ARMOR` | 0~2（僵尸自带 2）；钻石套 20 | 每点减 4% 伤害 |
| `ARMOR_TOUGHNESS` | 0~20 | 只影响高伤害衰减 |
| `KNOCKBACK_RESISTANCE` | 0~1 | 1 = 完全免疫（铁傀儡、监守者） |
| `ATTACK_KNOCKBACK` | 0~1.5 | 劫掠兽 / 监守者 1.5 |
| `FOLLOW_RANGE` | 默认 16；僵尸 35；掠夺者 32；末影人 64 | 目标搜索半径 |
| `STEP_HEIGHT` | 默认 0.6；1.0 = 能直接上 1 格 | 劫掠兽 / 铁傀儡 / 末影人 |

---

## 7. 攻击 doHurtTarget

- **必须用 `Mob` 的 `doHurtTarget`**，统一攻击入口。
- **不检查距离**：调用前必须由 Goal 自己判定距离。
- **不控制冷却**：必须由 Goal 自己决定攻击时机。
- **不播动画**：必须自己调用动画函数，比如 `mob.swing(InteractionHand.MAIN_HAND)`。

---

## 8. 自定义 Goal 的姿势清单

1. **声明 Flag**：不占 MOVE 不动，不占 LOOK 不转头。
2. **覆写 requiresUpdateEveryTick**：直驱移动的 Goal 必须返回 `true`。
3. **做节流**：`canUse` 里别全图搜，加冷却；攻击前判距离和冷却。
4. **收尾**：`stop()` 里清理状态，`navigation.stop()`。

---

## 9. 常见坑速查

| 坑 | 症状 / 解释 |
|---|---|
| 覆写 `serverAiStep` | 它是 `final`，编译不过；写 `customServerAiStep` |
| 直驱 Goal 没开 `requiresUpdateEveryTick` | MoveControl 隔 tick 走 `WAIT` 分支把速度清零 -> 一卡一卡、平均速度减半 |
| 直驱和 A* 同时开 | `PathNavigation.tick()` 会覆盖 MoveControl 的 wanted；两者必须二选一 |
| Goal 没设置 Flag | 不占 MOVE 就不会移动；不占 LOOK 就不会转头 |
| MoveControl 直驱撞墙 | 它不绕障；卡住需用 `navigation.moveTo` 兜底一次 |
| 动态目标疯狂 `moveTo` | 每次同步跑 A*，代价高且导致走走停停 |
| 用 `doHurtTarget` 前没判距离/冷却 | 隔空打人、每 tick 狂打、无 swing |
| 跨端读 AI 状态 | `travel`/AI 只在服务端权威，客户端渲染必须读同步字段 |
| 把 `moveTo` 第二参数当速度值 | 它是**乘数**：`0.4` 配属性 `0.4` = 7 格/秒，比冲刺玩家还快 |
| 想快 20% 就乘 1.2 | 平方关系，实际快 44%；乘 1.1 才是快 21% |
| 用 `.add(MOVEMENT_SPEED, x)` 想“加成” | 它是覆盖基础值，父类的 0.23 会被丢掉 |
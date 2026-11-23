# 添加状态效果

## 一、注册

`register/EffectRegister.java`：

```java
public class EffectRegister {

    static final DeferredRegister<MobEffect> EFFECT =
            DeferredRegister.create(Registries.MOB_EFFECT, ThriveMod.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> ILLUMINANT =
            EFFECT.register("illuminant", Illuminant::new);
    public static final DeferredHolder<MobEffect, Appetite> APPETITE =
            EFFECT.register("appetite", Appetite::new);

    public static void register(IEventBus bus) {
        EFFECT.register(bus);
    }
}
```

直接静态赋值，不用 `static {}` 块。主类调 `EffectRegister.register(bus)`。

`DeferredHolder<MobEffect, Xxx>` 是注册表 holder：`get()` 拿到的是注册后的实例，`getId()`/`getKey()` 是注册名。判“实体是否带此效果”时，传的是 `DeferredHolder`（它实现了 `Holder<MobEffect>`），不是效果实例本身。

## 二、命名

不加 `Effect` 后缀，除非与其它类冲突。

| 效果 | 类名 |
|---|---|
| 光源 | `Illuminant` |
| 食欲 | `Appetite` |
| 炫目 | `Blinding` |
| 超级饥饿 | `SuperHunger` |

冲突时才加后缀，如 `LanternBerryJuiceEffect`。

## 三、效果类

`generic/effect/Xxx.java`：

```java
public class Xxx extends MobEffect {

    public Xxx() {
        super(MobEffectCategory.BENEFICIAL, 0xFFFFA500);
    }
}
```

### 3.1 构造器参数的含义

```java
protected MobEffect(MobEffectCategory category, int color)
protected MobEffect(MobEffectCategory category, int color, ParticleOptions particle)
```

| 参数 | 含义 | 作用 |
|---|---|---|
| `category` | `BENEFICIAL` / `HARMFUL` / `NEUTRAL` | 决定 `isBeneficial()` 返回值；影响 HUD 边框颜色、部分 AI/逻辑判断。**不是**决定图标颜色。 |
| `color` | `0xRRGGBB` | **粒子颜色**。默认构造器会用它生成 `ENTITY_EFFECT` 颜色粒子；环境效果（ambient）alpha 为 38，否则 255。**不影响** HUD 图标。 |
| `particle` | `ParticleOptions` | 可选的固定粒子。传入后不再根据 ambient/color 动态生成。 |

`MobEffectCategory` 只有三个值：`BENEFICIAL`、`HARMFUL`、`NEUTRAL`。`isBeneficial()` 仅在 `BENEFICIAL` 时为 true。

### 3.2 纯数据效果（无逻辑）

```java
public class Appetite extends MobEffect {
    public Appetite() {
        super(MobEffectCategory.BENEFICIAL, 0xFFFFA500);
    }
}
```

不重写任何回调 → `applyEffectTick` 永不执行，效果只作为“标记/属性修改载体/其他代码查询用”。适合“我要判断实体有没有这个效果，但效果本身不做任何事”的场景。

### 3.3 每 tick 逻辑：`shouldApplyEffectTickThisTick` 与 `applyEffectTick`

```java
@Override
public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
    return true;
}

@Override
public boolean applyEffectTick(LivingEntity entity, int amplifier) {
    // ...
    return true;
}
```

#### `shouldApplyEffectTickThisTick(int duration, int amplifier)` —— 调度门控

- **作用**：每 tick 被调用一次，决定本 tick 是否执行 `applyEffectTick`。默认 `return false`，不重写则 `applyEffectTick` **永不执行**。
- **`duration` 的含义**：**剩余时间**，每 tick 减 1。想“每 20 tick 触发”写 `duration % 20 == 0`。
- **`amplifier`**：效果倍率，`0` = I 级，`1` = II 级。可用于让高等级效果更频繁触发。

#### `applyEffectTick(LivingEntity entity, int amplifier)` —— 实际逻辑

- **作用**：仅当 `shouldApplyEffectTickThisTick` 返回 `true` 时，本 tick 被调用。默认 `return true`。
- **返回值是存活判定**：返回 `false` → **立即调用 `entity.removeEffect(effect)`**，

- 正常情况必须返回 `true`；只有确实想让效果提前结束时才返回 `false`。

#### 频率控制示例

| 需求 | `shouldApplyEffectTickThisTick` |
|---|---|
| 每 tick | `return true;` |
| 每 2 tick | `return duration % 2 == 0;` |
| 每 20 tick（约每秒） | `return duration % 20 == 0;` |
| 高等级更快 | `return duration % Math.max(1, 20 >> amplifier) == 0;` |
| 仅 II 级及以上生效 | `return amplifier >= 1 && duration % 20 == 0;` |
| 前 100 tick 不触发 | `return duration < 100 && duration % 20 == 0;` |

### 3.4 即时效果

瞬间治疗、瞬间伤害这类“只执行一次”的效果，应重写 `isInstantenous` 和`applyInstantenousEffect`：

```java
public class InstantHeal extends MobEffect {
    public InstantHeal() {
        super(MobEffectCategory.BENEFICIAL, 0xF82423);
    }

    @Override
    public boolean isInstantenous() {
        return true;  // 返回true，表明在使用效果时，不创建持续效果实例，直接调用一次applyInstantenousEffect
    }

    @Override
    public void applyInstantenousEffect(@Nullable Entity source, @Nullable Entity indirectSource,
                                        LivingEntity entity, int amplifier, double health) {
        // 只执行一次的逻辑
    }
}
```

- `MobEffect.applyInstantenousEffect` 默认会调用 `applyEffectTick`，所以重写`applyEffectTick`也行，但建议用applyInstantenousEffect。

## 四、效果生命周期回调（`MobEffect` 上可重写的方法）

这些是 `MobEffect` 提供、由 `MobEffectInstance` / `LivingEntity` 转发调用的回调。理解“何时触发、传什么参数”是写效果的关键。

### 4.1 `onEffectAdded(LivingEntity entity, int amplifier)`

- **触发时机**：效果**首次**被添加到实体时（实体原本没有同种效果）。
- **不触发**：实体已有同种效果，再次施加（即使等级更高、时长更长）。
- **amplifier**：本次添加实例的 amplifier。
- **典型用途**：初始化只属于这次效果周期的状态（自定义计时器、播放一次性音效）。
- **注意**：`MobEffectEvent.Added` 事件不等于它。

### 4.1.1 `withSoundOnAdded(SoundEvent sound)`
- **触发时机**：与onEffectAdded一致。
- **作用**：设置首次添加触发的音效。

### 4.2 `onEffectStarted(LivingEntity entity, int amplifier)`

- **触发时机**：**每次** `addEffect` 都调用，包括**首次添加**、刷新时长、提升等级、甚至新实例被 `update` 拒绝的情况。
- **amplifier**：**新传入实例**的 amplifier，不是当前生效实例的 amplifier。新实例被拒绝时，这里得到的也是新实例的 amplifier。
- **典型用途**：“每次施加效果时都要执行”的逻辑，如播放音效、提示。

### 4.3 `onEffectStarted` vs `onEffectAdded` 速查

| 场景 | `onEffectAdded` | `onEffectStarted` |
|---|---|---|
| 首次添加 | ✅ | ✅ |
| 同种效果刷新时长（update 返回 true） | ❌ | ✅ |
| 同种效果提升等级（update 返回 true） | ❌ | ✅ |
| 同种效果新实例被拒绝（update 返回 false） | ❌ | ✅ |
| `forceAddEffect` 首次 | ✅ | ❌（`forceAddEffect` 不调 `onEffectStarted`） |

### 4.4 `onMobRemoved(LivingEntity entity, int amplifier, Entity.RemovalReason reason)`

- **触发时机**：实体被移除（`KILLED` 或 `DISCARDED`）时，对每个生效效果调用一次。
- **不走** `MobEffectEvent.Remove`，直接清空 `activeEffects`。
- **典型用途**：清理效果添加时创建的外部状态。

### 4.5 `onMobHurt(LivingEntity entity, int amplifier, DamageSource source, float amount)`

- **触发时机**：实体受伤、伤害结算完成、血量已扣之后。
- **amount**：最终伤害值（0值可能是被盾牌格挡，而虚弱药水这样的伤害为0的情况，不会触发onMobHurt）。
- **典型用途**：反伤、受伤时触发效果、记录伤害。
- **注意**：这是**效果定义**上的回调；如果需要更早/更细粒度的伤害控制，用 `LivingIncomingDamageEvent` / `LivingDamageEvent.Pre/Post`。

### 4.6 `onEffectRemoved` 相关

`MobEffect` 本身没有 `onEffectRemoved` 回调；移除逻辑在 `LivingEntity.onEffectRemoved(instance)` 里，负责：

- `effectsDirty = true`
- 服务端：`effect.removeAttributeModifiers(attributes)` + `refreshDirtyAttributes()`
- 同步给乘客 `ClientboundRemoveMobEffectPacket`

若要在效果被移除时执行自定义逻辑，监听 `MobEffectEvent.Remove`（可取消）。

## 五、`MobEffectEvent` 事件一览

所有子类在 `NeoForge.EVENT_BUS` 上 post。

| 事件 | 触发时机 | 可取消 | 关键方法 |
|---|---|---|---|
| `MobEffectEvent.Applicable` | `addEffect` 开头，检查能否应用 | 否（用 `Result`） | `getEffectInstance()`、`getEffectSource()`、`getApplicationResult()`、`setResult(Result)` |
| `MobEffectEvent.Added` | **每次** `addEffect`，不论是否已有同种效果 | 否 | `getEffectInstance()`（新实例）、`getOldEffectInstance()`（可能 null）、`getEffectSource()` |
| `MobEffectEvent.Remove` | 显式 `removeEffect`、`applyEffectTick` 返回 false、cure 移除、`removeAllEffects` | 是 | `getEffect()`、`getCure()`（可能 null）、`getEffectInstance()`（可能 null） |
| `MobEffectEvent.Expired` | 持续时间降到 0 自然过期 | 是 | `getEffectInstance()` |

### 5.1 `Applicable` 的 `Result`

| `Result` | 含义 |
|---|---|
| `APPLY` | 强制应用，跳过 `canBeAffected` |
| `DEFAULT` | 用 `entity.canBeAffected(instance)` 的结果 |
| `DO_NOT_APPLY` | 强制不应用 |

`getApplicationResult()` 就是按上表算出的最终布尔值。

### 5.2 移除路径对照

| 移除原因 | 触发点 | 事件 | 可取消 |
|---|---|---|---|
| `applyEffectTick` 返回 `false` | `MobEffectInstance.tick()` 内 `entity.removeEffect` | `Remove` | 是 |
| 持续时间降到 0 | `LivingEntity.tickEffects()` 中 `!instance.tick(...)` | `Expired` | 是 |
| `removeEffect(...)` 显式调用 | `LivingEntity.removeEffect` | `Remove` | 是 |
| `removeAllEffects` / `removeEffectsCuredBy` | 对应方法 | `Remove`（cure 非 null） | 是 |
| 死亡/丢弃 | `triggerOnDeathMobEffects` | **不走事件**，直接 `activeEffects.clear()` | 否 |

## 六、同种效果的合并规则（`MobEffectInstance.update`）

同种 `MobEffect` 在实体身上永远只有一个“当前生效”的 `MobEffectInstance`。再次施加时，由 `update` 决定怎么合并：

- **新等级更高**：
  - 新时长更短 → 旧实例压入 `hiddenEffect` 链，当前替换为新等级/时长；高等级结束后旧实例恢复。
  - 新时长不更短 → 直接替换等级和时长，**旧实例直接丢弃**。
- **新等级相同**：新时长更长才刷新时长；不更长则不更新时长。
- **新等级更低**：新时长更长时放入 `hiddenEffect`（递归更新），当前等级不变。
- `ambient` / `visible` / `showIcon` 变化也可能让 `update` 返回 `true`，即使等级和时长都没变。

`isInfiniteDuration()` 为 true 时视为“无限长”，`isShorterDurationThan` 会正确处理。

## 七、属性修改器

### 7.0 为什么 `-1` 会自动变成「每级 -1」

这是最容易懵的一点：**你只写了一个数，等级缩放是框架替你做的**，不用自己写 `for` 循环、也不要在 `applyEffectTick` 里改血量。

完整链路（全部源码核对过）：

```java
// 1) 你写的这一行：只是"存起来"，没有任何效果
public MobEffect addAttributeModifier(Holder<Attribute> attribute, ResourceLocation id,
                                     double amount, AttributeModifier.Operation operation) {
    this.attributeModifiers.put(attribute, new AttributeTemplate(id, amount, operation, null));
    return this;
}

// 2) 真正决定"每级多少"的地方
public record AttributeTemplate(ResourceLocation id, double amount,
                                AttributeModifier.Operation operation,
                                @Nullable Int2DoubleFunction curve) {
    public AttributeModifier create(int level) {                 // level 就是 amplifier
        if (curve != null)                                       // NeoForge 重载：自定义曲线
            return new AttributeModifier(this.id, this.curve.apply(level), this.operation);
        // ↓ 乘 (amplifier + 1)，这就是"每级"的来源
        return new AttributeModifier(this.id, this.amount * (double)(level + 1), this.operation);
    }
}
```

```java
// 3) 谁来调 create()
LivingEntity.addEffect(instance)
├─ 首次添加 → onEffectAdded(instance)
│    └ effect.addAttributeModifiers(this.getAttributes(), instance.getAmplifier())
│         └ attributeInstance.addPermanentModifier(template.create(amplifier))
└─ 已有同种效果且 update() 返回 true → onEffectUpdated(instance, forced = true)
     ├ effect.removeAttributeModifiers(attributes)              // 先按 id 删掉旧的
     ├ effect.addAttributeModifiers(attributes, 新 amplifier)    // 再按新等级加
     └ refreshDirtyAttributes()
```

所以 `-1.0` 的实际值：

| `amount` | amplifier | 实际修改值 |
|---|---|---|
| `-1.0` | `0`（I 级） | `-1` |
| `-1.0` | `1`（II 级） | `-2` |
| `-1.0` | `4`（V 级） | `-5` |

**三条必须记住的推论**：

1. `amount` 的语义是「**每级**的值」，不是总量。
2. **不是累加**。换级时先 `removeModifier(id)` 再 `addPermanentModifier`，同一时刻只有「当前等级」那一个修改器生效。
3. `attributeModifiers` 的 **key 是属性**：一个效果对同一个属性只能挂一个修改器（后写覆盖先写）。跨效果靠 `id` 区分，**`id` 撞了会互相顶掉**。

### 7.1 `MobEffect.addAttributeModifier`

```java
addAttributeModifier(Holder<Attribute> attribute, ResourceLocation id,
                     double amount, AttributeModifier.Operation operation)
```

- 向 `attributeModifiers` Map 放入 `AttributeTemplate(id, amount, operation)`。
- **同一个 `Holder<Attribute>` 键会被覆盖**，所以一个效果对同一属性只能有一个修改器。
- `id` 必须**全局唯一**，用于去重/移除。建议格式 `thrive:effect.<效果名>.<属性>`。
- 返回 `this`，支持链式。

NeoForge 重载：

```java
addAttributeModifier(Holder<Attribute> attribute, ResourceLocation id,
                     AttributeModifier.Operation operation,
                     Int2DoubleFunction curve)
```

允许自定义“效果等级 → 修改值”的曲线。内部 `AttributeTemplate(id, curve.apply(0), operation, curve)`。

### 7.2 `AttributeTemplate.create(int level)`

- `curve != null`：`new AttributeModifier(id, curve.apply(level), operation)`，**不再乘 `(level + 1)`**。
- `curve == null`：`new AttributeModifier(id, amount * (level + 1), operation)`，默认线性。
- `level` 传入的是 amplifier，0 表示 I 级。

### 7.2.1 拿 `MAX_HEALTH` 做「减血上限」的三个坑

以 `Saponification`（`-1.0`，`ADD_VALUE`，作用于 `Attributes.MAX_HEALTH`）为例：

| 现象 | 原因 |
|---|---|
| 上限压不到 0，人不会因为这个死 | `MAX_HEALTH` 是 `RangedAttribute(20.0, 1.0, 1024.0)`，`sanitizeValue` 把下限钳在 **1** |
| 加上效果瞬间「掉血」 | 上限下降后 `refreshDirtyAttributes → onAttributeUpdated` 会 `setHealth(maxHealth)` 把当前血量钳到新上限 |
| 效果结束后上限回来了，血却没回来 | 移除只删修改器，**不会回血**；要回血得自己 `entity.heal(...)` 或等自然恢复 |

想要别的量级/形状：

| 目标 | 写法 |
|---|---|
| 每级 -10% 基础值 | `ADD_MULTIPLIED_BASE` + `-0.1`（`-0.1 × (amp+1)` 是基础值的百分比） |
| 非线性（如每级 -1、-3、-6…） | NeoForge 重载 `addAttributeModifier(attr, id, op, Int2DoubleFunction)`，`curve.apply(amplifier)` 直接给值 |
| **持续掉血** | 别用属性修改器，在 `applyEffectTick` 里 `entity.hurt(...)` |

### 7.3 `AttributeInstance` 的 permanent vs transient

`LivingEntity.onEffectAdded` / `onEffectUpdated(forced=true)` 调用 `MobEffect.addAttributeModifiers`，内部用 `AttributeInstance.addPermanentModifier`：

| 方法 | 进 `permanentModifiers` | 随实体存档 | 用途 |
|---|---|---|---|
| `addPermanentModifier` | ✅ | ✅ | 效果属性修改器（当前用法） |
| `addTransientModifier` | ❌ | ❌ | 临时 modifier，如冲刺、细雪减速 |

因此效果属性修改器会随实体 NBT 保存；重进世界后由 `readAdditionalSaveData` 重新加载。

`AttributeInstance.getValue()` 计算顺序：base → 加 `ADD_VALUE` → 加 `ADD_MULTIPLIED_BASE`（基于 base）→ 乘 `ADD_MULTIPLIED_TOTAL` → `sanitizeValue`。

### 7.4 `blendDurationTicks`

`MobEffect.setBlendDuration(int)` 设置混合持续时间，`MobEffectInstance.BlendState` 用它做 HUD 渐隐/渐显：

- `blendDuration == 0` → factor 直接 1。
- 否则每 tick 向目标（剩余时间 > blendDuration ? 1 : 0）逼近，步长 `1/blendDuration`。

**纯客户端 HUD 效果**，与效果逻辑、服务端无关。

## 八、治疗物（EffectCure）

### 8.1 它是什么

`EffectCure` **不是物品**，是一个「疗程标签」——就是一个字符串单例。原版只有三个：

| 常量 | 名字 | 谁触发 |
|---|---|---|
| `EffectCures.MILK` | `milk` | 喝牛奶 |
| `EffectCures.HONEY` | `honey` | 喝蜂蜜瓶（原版只对**中毒**生效） |
| `EffectCures.PROTECTED_BY_TOTEM` | `protected_by_totem` | 不死图腾生效时 |

它**不是注册表**：

```java
private static final Map<String, EffectCure> CURES = new ConcurrentHashMap<>();
public static EffectCure get(String name) {          // 懒创建 + 记忆
    return CURES.computeIfAbsent(name, EffectCure::new);
}
```

所以「同名 = 同一个 cure」，跨 mod 会撞车——自定义名带上命名空间，例如 `thrive:alkali`。

### 8.2 一个效果「能不能被治好」是怎么判定的

三条链，缺一不可：

**① 构造实例时把 cure 缓存进去**

```java
// MobEffectInstance 构造器最后一行
this.effect.value().fillEffectCures(this.cures, this);
```

`fillEffectCures` 是 NeoForge 打在 `IMobEffectExtension` 上的 default 方法：

```java
default void fillEffectCures(Set<EffectCure> cures, MobEffectInstance effectInstance) {
    cures.addAll(EffectCures.DEFAULT_CURES);                        // milk + protected_by_totem
    if (this.self() == MobEffects.POISON.value()) cures.add(EffectCures.HONEY);
}
```

> **这就是为什么你什么都没写，喝牛奶却能把效果解掉。**
> 任何效果默认都属于 `milk` 和 `protected_by_totem`。

**② 治疗物去调 `removeEffectsCuredBy`**

```java
// MilkBucketItem.finishUsingItem
if (!level.isClientSide) entityLiving.removeEffectsCuredBy(EffectCures.MILK);

// HoneyBottleItem.finishUsingItem
if (!level.isClientSide) entityLiving.removeEffectsCuredBy(EffectCures.HONEY);

// LivingEntity.checkTotemDeathProtection
this.removeEffectsCuredBy(EffectCures.PROTECTED_BY_TOTEM);
```

**③ 真正移除**

```java
public boolean removeEffectsCuredBy(EffectCure cure) {
    if (this.level().isClientSide) return false;
    for (MobEffectInstance effect : this.activeEffects.values())
        if (effect.getCures().contains(cure)
            && !EventHooks.onEffectRemoved(this, effect, cure)) {   // 走 MobEffectEvent.Remove
            this.onEffectRemoved(effect);
            itr.remove();
            ...
        }
}
```

移除走 `MobEffectEvent.Remove`，此时 `getCure()` 非空，**事件可取消**。

### 8.3 加一个自己的治疗物

**第一步：定义 cure**（放在效果类里最省事）

```java
public static final EffectCure ALKALI = EffectCure.get("thrive:alkali");
```

**第二步：改写效果的 cure 集合**

```java
@Override
public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance effectInstance) {
    cures.clear();              // 不想被牛奶/图腾解掉，就必须清掉默认值
    cures.add(ALKALI);
    // 想保留牛奶：别 clear，改成 super.fillEffectCures(cures, effectInstance);
}
```

**第三步：让物品去治**

```java
public class AlkaliNeutralizerItem extends Item {
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            entity.removeEffectsCuredBy(Saponification.ALKALI);
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
```

不是饮用物品也行——喷溅药水、图腾、方块交互、`LivingEntity#removeEffectsCuredBy` 直接调，都是同一个入口。

### 8.4 注意事项

| 事项 | 说明 |
|---|---|
| 缓存时机 | `cures` 在**实例构造时**填好；之后改代码 / 改注册都不影响已存在的实例，重进世界重建才生效 |
| 只对活体 | 是 `LivingEntity` 的方法；对 `MobEffectInstance` 直接 `removeEffect` 不经过 cure |
| 纯服务端 | 客户端调用直接 `return false`，不需要同步代码 |
| `clear()` 与 `super` | 覆盖 `fillEffectCures` 时**不调用 super 就没有默认的牛奶/图腾**，这是最容易忘的一步 |
| 一个效果多个 cure | 集合，随便加；`contains` 判断，顺序无关 |
| 单个实例定制 | `MobEffectInstance.Details` 里有 `neoforge:cures` 字段（可序列化），能给某一次施加的实例单独挂 cure，一般用不到 |
| 与 `Milk` 效果无关 | 项目里的 `generic/effect/Milk.java` 是个普通效果，和 `EffectCures.MILK` 不是一回事 |

## 九、材质

每个效果需要一张 **16×16 PNG**：

```
assets/thrive/textures/mob_effect/<注册名>.png
```

**必须 16×16**，与原版一致。效果图标渲染时是把整张 sprite 画进 **16×16** 的格子（`AbstractContainerScreen` 里的 `guiGraphics.blit(x, y, 0, 16, 16, sprite)`），18×18 会被缩到 16×16，反而发糊。

想要原版物品的图标，直接从 `minecraft_1.21.1_client.jar`（`neoformruntime/artifacts/` 下）提取 `assets/minecraft/textures/item/<物品>.png` 复制过来即可，本身就是 16×16。

| 效果 | 文件 |
|---|---|
| 光源 | `assets/thrive/textures/mob_effect/illuminant.png`（原版灯笼） |
| 食欲 | `assets/thrive/textures/mob_effect/appetite.png` |
| 牛奶 | `assets/thrive/textures/mob_effect/milk.png`（原版奶桶） |

**缺失症状**：紫黑格（missing texture），不崩但难看。

**图标颜色由 PNG 自己决定**，和构造器里的颜色值无关——那个值是粒子色。

## 十、语言文件

`assets/thrive/lang/{en_us,zh_cn}.json`：

```json
"effect.thrive.illuminant": "光源",
"effect.thrive.appetite": "食欲"
```

键格式：`effect.<命名空间>.<注册名>`。

## 十一、清单

| 文件 | 改动 |
|---|---|
| `register/EffectRegister.java` | `EFFECT.register("xxx", Xxx::new)` |
| `generic/effect/Xxx.java` | 继承 `MobEffect`，按需覆写 tick / 事件 / 属性修改器 |
| `generic/effect/Xxx.java` | 自定义治疗物时覆写 `fillEffectCures` |
| 治疗物品（可选） | `finishUsingItem` 里 `entity.removeEffectsCuredBy(cure)` |
| `assets/thrive/textures/mob_effect/xxx.png` | 16×16 图标 |
| `assets/thrive/lang/{en_us,zh_cn}.json` | `effect.thrive.xxx` |
| `ThriveMod.java` | `EffectRegister.register(bus)` |

## 十二、常见坑

| 坑 | 症状 |
|---|---|
| 以为 `addAttributeModifier` 的 `amount` 是总量 | 实际是**每级**值，框架乘 `(amplifier + 1)`；等级越高效果越强，以为是重复叠加 |
| 自己再在 `applyEffectTick` 里改血量来做「每级」 | 和属性修改器重复计算，数值翻倍 |
| 以为属性修改器能把人压死 | `MAX_HEALTH` 下限是 1，压不到 0 |
| 以为效果结束血量会回满 | 只恢复上限，不回当前血量 |
| 效果被牛奶莫名解掉 | `fillEffectCures` 默认加了 `milk` / `protected_by_totem`，要独占必须 `cures.clear()` |
| 覆写 `fillEffectCures` 没调 super | 默认的牛奶/图腾全部丢失（有时正是想要的，注意是主动选择） |
| `EffectCure.get("alkali")` 不带命名空间 | 和其它 mod 同名 cure 撞车，互相治疗 |
| 给已存在的实例改 cure | 不生效，cure 在实例构造时就缓存了 |
| 忘了 `shouldApplyEffectTickThisTick` | `applyEffectTick` 永不执行 |
| `applyEffectTick` 返回 `false` | 立即 `removeEffect`，走 `Remove` 事件（可取消）；不是等下一 tick |
| 以为 `applyEffectTick` 返回 false 走 `Expired` | 实际走 `Remove`；`Expired` 只用于持续时间耗尽 |
| 在 `onEffectStarted` 里假设 amplifier 是当前生效等级 | 新实例被拒绝时仍用新实例 amplifier 触发 |
| 混淆 `onEffectAdded` 和 `onEffectStarted` | 刷新效果时预期执行的回调没触发，或只应执行一次的逻辑被重复执行 |
| 以为 `MobEffectEvent.Added` 只在首次 post | 每次 `addEffect` 都 post，监听时需自己判 `getOldEffectInstance() == null` |
| 属性修改器 ID 不唯一 | 属性加成被其它效果/系统覆盖，表现异常 |
| 用 `addTransientModifier` 加效果属性修改器 | 不存档，重进世界后消失；应用 `MobEffect.addAttributeModifier`（走 `addPermanentModifier`） |
| 即时效果未继承 `InstantenousMobEffect` | 逻辑在特定路径下不执行，或执行时机不符合预期 |
| 死亡时在 `MobEffectEvent.Remove` 里做清理 | 死亡/丢弃不走该事件，清理逻辑不执行；应重写 `onMobRemoved` |
| `@EventBusSubscriber` 挂在非注册类 | 事件不触发，静默 |
| 事件里 `entity.getEffect(...)` 判空漏写 | NPE |
| 语言键缺命名空间 | 显示原始 key |
| 材质非 16×16 | 缩放发糊（18×18 也会被缩） |
| 材质没放 | 紫黑格 |
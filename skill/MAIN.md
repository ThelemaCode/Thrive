# Thrive 开发规范

> 全局约定。任务手册见文末索引。

---

# 项目编译指令
- ./gradlew build，在 D:\all\true\Thrive下执行

## 一、代码风格

- **最少属性原则**：任何属性，都不能由其它属性直接或组合得来，即功能是无法被其它属性替代的。
- **字段默认 public**：不为封装而封装。只有需要时才 getter/setter。
- **无冗余检查**：能由调用方保证的，不在被调方重复，调用方尽量不给被调用方注意点。
- **删掉死代码**：没被用到的，立刻删，不留"以后可能有用"。
- **封装要有价值**：易于理解 + 复用度高 + 封装的逻辑多。即：1：不能看不懂。2：不能只有1个地方用它。3：不要为1或2条语句封装。
- **有价值的封装**：1：对象的对外动作方法封装。2：能被多个地方复用的，复杂的逻辑。


---

## 二、双端分离与逻辑判断

Minecraft 的客户端与服务端运行同一套代码。为避免跨端崩溃和逻辑错乱，必须严格遵循以下规范（详见 `SERVER_CLIENT.md`）。

- **抛弃 `isClientSide`，拥抱 `instanceof ServerLevel`**：执行服务端逻辑时，不要用 `if (!level.isClientSide)` 然后手动强转。直接使用 `if (level instanceof ServerLevel serverLevel)`，利用 Java 模式匹配，既安全又省去强转代码。
- **服务端专属方法参数直接声明为 `ServerLevel`**：若方法只修改世界、生成实体或发粒子，参数类型直接写 `ServerLevel`。在编译期强制要求调用方提供正确端，内部无需判断 `isClientSide`。
- **`isClientSide` 的仅存场景**：纯客户端拦截（如渲染、粒子动画，无需服务端实例），或原版方法签名强制要求 `Level` 且无需进一步操作的卫语句。
- **保持手感**：Mixin 拦截原版方法时，即使只在 `instanceof ServerLevel` 分支里执行了服务端逻辑，也要在外部统一 `cir.setReturnValue(InteractionResult.SUCCESS)`，避免客户端预测失败导致手感发粘。

---

## 三、包结构

```
com.thelema.thrive
├── ThriveMod.java
├── generic
│   ├── block              方块
│   │   └── other          方块辅助（SurfaceItem 等组合能力）
│   ├── blockentity        方块实体
│   │   ├── biomeModifier      转换器实现
│   │   └── other          方块实体辅助
│   ├── item               物品
│   ├── entity             实体
│   │   └── goal           实体 AI
│   ├── effect             状态效果
│   ├── fluid              流体
│   ├── world
│   │   └── feature        地物
│   └── Ritual.java        仪式
├── register               所有注册
│   ├── BlockRegister / BlockEntityRegister / ItemRegister
│   ├── EntityRegister / EffectRegister / SoundRegister
│   ├── ParticleRegister / FluidRegister / FeatureRegister
│   └── RecipeRegister / TabRegister / ...
├── client
│   ├── render
│   │   ├── blockentity    方块实体渲染
│   │   │   └── other      渲染辅助（SurfaceRenderer 等工具类）
│   │   ├── entity / effect / item
│   ├── model/entity
│   ├── particle
│   └── register           BlockEntityRenderRegister / ...
├── mixin
├── config
└── pack
```

---

## 四、注册规范

所有注册器共用同一模板：

```java
public class XxxRegister {
    static final DeferredRegister<Xxx> XXX = DeferredRegister.create(..., ThriveMod.MOD_ID);

    public static final DeferredHolder<Xxx, XxxType> NAME = XXX.register("name", () -> ...);
    

    public static void register(IEventBus bus) {
        XXX.register(bus);
    }
}
```

`BlockRegister` 额外提供 `registerWithItem`：

```java
public static <B extends Block> DeferredHolder<Block, B> registerWithItem(String name, Supplier<B> supplier) {
    DeferredHolder<Block, B> block = BLOCK.register(name, supplier);
    ITEM.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    return block;
}
```

---

## 五、继承与组合

**强制基类只能继承**（`Block`、`BlockEntity`、`Item`……），继承位留给它。

**额外能力用组合**：一组相关的状态 + tick + NBT，打包成独立类，**持有**进宿主。宿主不需要 `is-a`，只需要 `has-a`。

**无状态的共享逻辑用静态工具类**。

判据：

| 东西 | 方案 |
|---|---|
| 强制基类（不继承用不了） | `extends` |
| 一组状态 + 行为 + NBT | 组合 |
| 无状态纯逻辑 | 静态工具类 |
| 仅枚举/布尔/方向等简单状态 | `BlockState` 属性或字段 |

---

## 六、任务手册

| 任务 | 手册 |
|---|---|
| 双端分离与逻辑判断 | `SERVER_CLIENT.md` |
| 添加实体 | `ENTITY.md` |
| 实体 AI（Goal / 控制器 / 导航 / 攻击） | `AI.md` |
| 添加物品 | `ITEM.md` |
| 添加方块（无 BE） | `BLOCK.md` |
| 添加方块 + 方块实体 | `BLOCK_ENTITY.md` |
| 添加流体 | `FLUID.md` |
| 添加状态效果 | `EFFECT.md` |
| 添加药水 | `POTION.md` |
| 添加粒子 | `PARTICLE.md` |
| 添加配方 | `RECIPE.md` |
| 添加结构 | `STRUECTURE.md` |
| Jade 集成 | `JADE.md` |

新增任务型文档在此登记。

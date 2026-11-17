
# 添加结构

以"平原树 `biome_core_tree_plains`"为例，从头到尾走一遍。你加别的结构时，把这个名字换掉就行。

原版 `jigsaw` 结构类型，纯 JSON 实现，不写 Java。

## 一、五类文件，一次性列清

结构 `biome_core_tree_plains` 需要：

```
data/thrive/structure/biome_core_tree_plains.nbt
data/thrive/worldgen/template_pool/biome_core_tree_plains/start_pool.json
data/thrive/worldgen/structure/biome_core_tree_plains.json
data/thrive/worldgen/structure_set/biome_core_tree_plains.json
data/thrive/tags/worldgen/biome/has_structure/biome_core_tree_plains.json
```

**第一处注意**：`tags` 那一层不能少——biome 标签比 biome 定义多一个 `tags` 目录。少写会把 tag 当 biome 解析，报 `No key spawn_costs in MapLike[...]`，看不出是人话。

**第二处注意**：NBT 路径是 `structure`（单数），不是 `structures`。

## 二、NBT 文件

在游戏里用结构方块或 `/place template` 造好结构后，NBT 会落在：

```
run/saves/<世界名>/generated/minecraft/structures/biome_core_tree_plains.nbt
```

把它**复制**到：

```
src/main/resources/data/thrive/structure/biome_core_tree_plains.nbt
```

游戏运行时读的是后者，前者只是编辑器缓存的产物。

## 三、template_pool

路径：`data/thrive/worldgen/template_pool/biome_core_tree_plains/start_pool.json`

```json
{
  "name": "thrive:biome_core_tree_plains/start_pool",
  "fallback": "minecraft:empty",
  "elements": [
    {
      "weight": 1,
      "element": {
        "element_type": "minecraft:single_pool_element",
        "location": "thrive:biome_core_tree_plains",
        "processors": "minecraft:empty",
        "projection": "rigid"
      }
    }
  ]
}
```

**注意 `location`**——这是 NBT 的 namespace:path，**不含扩展名**。写成 `thrive:biome_core_tree_plains.nbt` 会报找不到。

**注意 `start_pool` 是惯例名**——jigsaw 结构必须有一个"起点池"。名字随意，只要 structure 里引用的和这里一致。

**如果你有多个变体想随机出**（比如平原上生成三种不同外观的树），在 `elements` 里加多条，各给 `weight`：

```json
"elements": [
  { "weight": 3, "element": { ... "location": "thrive:biome_core_tree_plains_a" } },
  { "weight": 1, "element": { ... "location": "thrive:biome_core_tree_plains_b" } }
]
```

权重 3:1 = 前者概率是后者 3 倍。

## 四、structure

路径：`data/thrive/worldgen/structure/biome_core_tree_plains.json`

```json
{
  "type": "minecraft:jigsaw",
  "biomes": "#thrive:has_structure/biome_core_tree_plains",
  "step": "surface_structures",
  "spawn_overrides": {},
  "terrain_adaptation": "beard_thin",
  "start_pool": "thrive:biome_core_tree_plains/start_pool",
  "size": 1,
  "start_height": { "absolute": 0 },
  "project_start_to_heightmap": "WORLD_SURFACE_WG",
  "max_distance_from_center": 80,
  "use_expansion_hack": false
}
```

**地形适配靠 `terrain_adaptation`**：

| 值 | 效果 |
|---|---|
| `beard_thin` | **推荐**。结构下方自动填一层薄土，补平小坡 |
| `beard_thick` | 填更厚，适合底座大、地面明显不平的结构 |
| `none` | 不填土，贴合原地形，坡上会悬空 |

**`step` 决定生成阶段**——`surface_structures` 在地表，`underground_structures` 在地下。放地表用前者。

**`size` 是 jigsaw 拼图段数**，单个 NBT 写 1。多段拼接才需要往上加。

## 五、structure_set（稀有度）

路径：`data/thrive/worldgen/structure_set/biome_core_tree_plains.json`

```json
{
  "structures": [
    {
      "structure": "thrive:biome_core_tree_plains",
      "weight": 1
    }
  ],
  "placement": {
    "type": "minecraft:random_spread",
    "salt": 123456789,
    "spacing": 48,
    "separation": 24,
    "spread_type": "linear"
  }
}
```

**稀有度就看 `spacing`**：

| `spacing` | 密度 |
|---|---|
| 8 | 极密 |
| 32 | 常见 |
| **48** | **推荐：稀有** |
| 96 | 极稀有（林地府邸 80） |
| 160 | 传说级 |

**`separation` 保持 `spacing / 2`**。这是两个结构中心的最小距离，防止贴脸。

**`salt` 每个结构不一样**。两个结构都用 `123456789` 会撞位置。给你个项目内的规矩：第一个用 `123456789`，第二个 `123456790`，依次递增。

**`spread_type`**：`linear` 均匀分布，`triangular` 中心密边缘疏，`linear_triangular` 混合。**开阔地形用 `linear`。**

## 六、biome 标签

路径：`data/thrive/tags/worldgen/biome/has_structure/biome_core_tree_plains.json`

```json
{
  "replace": false,
  "values": [
    "minecraft:plains",
    "minecraft:sunflower_plains"
  ]
}
```

**`replace: false` 必须有**——这是追加到原版 `#minecraft:is_overworld` 等已有标签的语义，写 `true` 会覆盖。

**只列想让结构生成的群系**。想加自己的 moss 群系就加 `thrive:moss`。

## 七、验证

进世界：

```
/locate structure thrive:biome_core_tree_plains
```

能定位到坐标就成功了。

## 八、调试：手动精准放置

### 8.1 指令

```
/place template thrive:biome_core_tree_plains 100 64 200
```

`/place template` **直接放 NBT**，不走 jigsaw，不贴地形，坐标精确。调位置、试效果用这个。

对比：`/place structure thrive:biome_core_tree_plains 100 64 200` 会走结构系统，自动 `beard_thin` 贴地形。

### 8.2 代码

```java
public static void placeTemplate(ServerLevel level, BlockPos pos) {
    StructureTemplateManager manager = level.getStructureManager();
    ResourceLocation id = ResourceLocation.fromNamespaceAndPath("thrive", "biome_core_tree_plains");

    Optional<StructureTemplate> templateOpt = manager.get(id);
    if (templateOpt.isEmpty()) return;
    StructureTemplate template = templateOpt.get();

    StructurePlaceSettings settings = new StructurePlaceSettings()
            .setRotation(Rotation.NONE)
            .setMirror(Mirror.NONE)
            .setIgnoreEntities(false);

    template.placeInWorld(
            level,                    // 目标世界
            pos,                      // offset：真正决定落点的参数（局部原点变换后所在处）
            pos,                      // pos：只传给处理器当"结构位置"，不影响落点
            settings,                 // 放置参数
            level.getRandom(),        // 随机源
            Block.UPDATE_ALL          // 方块更新标志
    );
}
```

`placeInWorld` 返回 `boolean`，失败返回 false，生产代码里应检查。

## 九、加第二个结构

比如加"苔藓地风格的树"，步骤：

1. **NBT**：`data/thrive/structure/biome_core_tree_moss.nbt`
2. **template_pool**：`data/thrive/worldgen/template_pool/biome_core_tree_moss/start_pool.json`——把 JSON 里所有 `biome_core_tree_plains` 换成 `biome_core_tree_moss`
3. **structure**：`data/thrive/worldgen/structure/biome_core_tree_moss.json`——同上替换
4. **structure_set**：`data/thrive/worldgen/structure_set/biome_core_tree_moss.json`——**别忘了改 `salt`**
5. **biome tag**：`data/thrive/tags/worldgen/biome/has_structure/biome_core_tree_moss.json`——替换 `values` 为 `thrive:moss` 等

**要改的四处**：5 个文件名里的后缀、4 个 JSON 里的 id 字符串、`salt`、biome 列表。**其余原样。**

加非树的结构（遗迹、小屋）也一样——把 `biome_core_tree_plains` 全换成新名字，`salt` 换成没用过的数，biome 列表按目标地形填。

---

# 原理篇：`StructurePlaceSettings` 与 `placeInWorld`

> 下面每一条都是拿 `neoforge-21.1.233-sources.jar` 里的真源码核对过的：
> `StructureTemplate.java`（857 行）、`StructurePlaceSettings.java`（158 行）、`StructureProcessor.java`、
> `SinglePoolElement.java`、`PlaceCommand.java`、`Level.java`。

## 十、先给结论

> **`StructurePlaceSettings` 是个纯数据袋**：11 个字段 + 一串 setter，**自身没有任何放置逻辑**。
> **`StructureTemplate.placeInWorld` 是执行器**：它是隔着 settings 读参数的一条流水线——
> `选调色板 → 局部坐标变换成世界坐标 → 过处理器链改写 → 逐块 setBlock → 补液体 → 补形状 → 放实体`。

读源码时把这两个类当成"参数对象 + 执行器"，就不用背方法名了。`StructurePlaceSettings` 里没有 `place()` 之类的方法，一个都没有——这一点最容易让人以为漏看了什么。

## 十一、`StructurePlaceSettings` 的 11 个字段

| 字段 | 类型 | 默认值 | setter | 影响流水线的哪一步 |
|---|---|---|---|---|
| `mirror` | `Mirror` | `NONE` | `setMirror` | 坐标变换（X/Z 翻转）、方块状态朝向 |
| `rotation` | `Rotation` | `NONE` | `setRotation` | 坐标变换（绕 Y 轴）、方块状态朝向 |
| `rotationPivot` | `BlockPos` | `ZERO` | `setRotationPivot` | 镜像/旋转的**中心点** |
| `ignoreEntities` | `boolean` | `false` | `setIgnoreEntities` | 有 NBT 的实体要不要跟着放 |
| `boundingBox` | `BoundingBox?` | `null` | `setBoundingBox` | 裁剪：只放落在盒子里的方块/实体 |
| `liquidSettings` | `LiquidSettings` | `APPLY_WATERLOGGING` | `setLiquidSettings` | 放置后要不要"补回"水和含水方块 |
| `random` | `RandomSource?` | `null` | `setRandom` | 选调色板 + 处理器里的随机（**有状态**，见 11.1） |
| `palette` | `int` | `0` | **无 public setter** | **死字段，源码里没有任何读取点**（见 11.1） |
| `processors` | `List<StructureProcessor>` | 空表 | `addProcessor` / `popProcessor` / `clearProcessors` | 处理器链，逐块改写 |
| `knownShape` | `boolean` | `false` | `setKnownShape` | `true` = 跳过放置后的形状修补（快路径） |
| `finalizeEntities` | `boolean` | `false` | `setFinalizeEntities` | 实体若是 `Mob`，是否调 `finalizeSpawn` |
| — | — | — | `copy()` | 复制一份（见 12.10） |

### 11.1 两个"看了源码才知道"的字段

- **`palette` 是死字段。** 它只有声明和 `copy()` 里的赋值，全类没有任何地方读它。想控制用哪套调色板，源码走的是 `getRandomPalette(palettes, pos)`——按随机数挑，你设不了。要多套变体就多写几个 NBT，或者用 template_pool 的 `weight`。
- **`random` 是"有状态"的。** `getRandom(pos)` 的写法是：

```java
public RandomSource getRandom(@Nullable BlockPos seedPos) {
    if (this.random != null) return this.random;                       // 给了就直接用，全局共享一个流
    return seedPos == null ? RandomSource.create(Util.getMillis())     // 没给位置：用系统时间，不可复现
                           : RandomSource.create(Mth.getSeed(seedPos)); // 有位置：由坐标派生，可复现
}
```

所以：**不设 `random`**，同一个坐标放同一份模板，结果永远一样（每次都是新流）；**设了 `random`**，就变成"按调用顺序往下消耗"，同一个 settings 放两次结果会不同。做结构生成想稳定复现，就别设。

### 11.2 setter 都能链式调用

所有 setter 都是 `return this;`，所以可以一路点下去。但注意它们**改的是同一个对象**——想"复制一份再改"，用 `copy()`。

## 十二、`placeInWorld` 的完整流程

签名（`StructureTemplate` 第 226 行）：

```java
public boolean placeInWorld(
    ServerLevelAccessor level,   // 目标世界（WorldGenLevel 也满足）
    BlockPos offset,             // ① 决定落点
    BlockPos pos,                // ② 只喂给处理器，不参与定位
    StructurePlaceSettings settings,
    RandomSource random,         // ③ 注意：不在 settings 里，单独传
    int flags                    // ④ setBlock 的更新标志
);
```

三个"看起来重复"的参数，各管各的：

| 参数 | 到底被用在哪 |
|---|---|
| `offset` | 调色板随机种子、**每个方块的世界坐标**、实体的世界坐标 |
| `pos` | **只**作为参数转交给 `StructureProcessor.process(level, offset, pos, ...)`，不做任何计算 |
| `random` | **只**用来给带战利品的容器抽 `LootTableSeed`（`random.nextLong()`） |

也就是说：**`pos` 填错了不会影响方块落点**，只会影响那些"读 pos 做判断"的处理器。网上大量教程传 `pos, pos` 就是这个原因——能跑，但两个参数的语义完全不同。

### 12.1 伪代码（把 226–370 行压缩）

```
placeInWorld(level, offset, pos, settings, random, flags):
    if 模板没有调色板:                       return false
    blocks = 选中的调色板.blocks
    if blocks 为空 且 (忽略实体 或 没有实体):  return false
    if 模板 size 任一维 < 1:                 return false

    处理后的块 = processBlockInfos(level, offset, pos, settings, blocks, this)
                   ↑ 这里做坐标变换 + 过处理器链

    for 块 in 处理后的块:
        if boundingBox 存在 且 块不在盒子里:  continue
        记下原液体状态（若 apply_waterlogging）
        if 块有 NBT: 清掉旧方块实体 → 放 BARRIER 占位
        if level.setBlock(块.坐标, 块.状态.mirror(mirror).rotate(rotation), flags):
            记录到 list3（用于后续补形状 / 补 NBT / 统计 AABB）
            if 有 NBT: 取方块实体 → 回填 NBT（战利品表容器补 LootTableSeed）

    补液体（见 12.6）
    if 至少放成功一块 且 !knownShape:
        建 DiscreteVoxelShape → updateShapeAtEdge 修补贴空气的边界
        for 每块: updateFromNeighbourShapes → 变了就再 setBlock → blockUpdated
    if !ignoreEntities: addEntitiesToWorld(level, offset, settings)
    return true
```

### 12.2 返回 false 的三种情况

```java
if (this.palettes.isEmpty()) return false;                                  // 模板空/没 load
if (blocks.isEmpty() && (settings.isIgnoreEntities() || entities.isEmpty())
    || size 任一 < 1) return false;                                         // 没东西可放
```

**反过来 `true` 不代表每块都放成功了**：被 `boundingBox` 裁掉的、被处理器返回 `null` 丢掉的、`setBlock` 返回 `false`（状态没变化）的，都不算失败。所以别拿返回值判断"结构完整放置"。

### 12.3 选调色板：一次调用只选一次

```java
List<StructureBlockInfo> list = settings.getRandomPalette(this.palettes, offset).blocks();
```

一个 NBT 可以有多套 palette（`palettes` 数组，同一套坐标、不同方块）。**整次放置共用同一套**，不会出现"半棵树是橡木、半棵是云杉"。种子来自 `offset`，所以同一个 offset 反复放，选中的永远是同一套。

### 12.4 坐标变换 + 处理器链：`processBlockInfos`

这是全流程里唯一会"改坐标和改方块"的地方（第 414–443 行）：

```java
for (BlockInfo 原始 : blocks) {
    BlockPos world = calculateRelativePosition(settings, 原始.pos)   // 镜像→旋转→加 pivot
                        .offset(offset);                            // 再加放置原点
    BlockInfo 当前 = 新副本(world, 原始.state, 原始.nbt?.copy());
    for (processor : settings.getProcessors()) {
        当前 = processor.process(level, offset, pos, 原始, 当前, settings, template);
        if (当前 == null) break;        // 被丢弃 → 这块永远不出现
    }
    if (当前 != null) { processed.add(当前); original.add(原始); }
}
for (processor : settings.getProcessors())
    processed = processor.finalizeProcessing(level, offset, pos, original, processed, settings);
```

注意三件事：

1. **位置只算一次**，方块朝向（stairs 朝向、log 的 axis）**不在这里改**，是在 `setBlock` 那句 `state.mirror(mirror).rotate(rotation)` 里改的。
2. **任一处理器返回 `null` = 这块彻底消失**，后面的处理器也不会再看到它。
3. **`finalizeProcessing` 是唯一能"加方块"的钩子**——它拿到的是全量列表，可以返回一个更长的表。`process` 只能改/删。

### 12.5 逐块写入与 NBT

- 有 NBT 的块会先 `Clearable.tryClear(旧BE)`，再放一个 `BARRIER` 占位（flags `20` = 不同步客户端 | 不触发形状更新），**然后**才放真正的方块。这是为了防止旧方块实体和新 NBT 打架。
- 真正的 NBT 回填发生在 `setBlock` **成功之后**：`getBlockEntity` → `loadWithComponents(nbt, registryAccess())`；如果方块实体是 `RandomizableContainer`，先塞 `LootTableSeed = random.nextLong()`（这就是 `placeInWorld` 里那个 `random` 参数唯一的用途）。
- **`setBlock` 返回 `false` 的块会被跳过**。`Level.setBlock` 在"新旧状态相同"时返回 `false`（`LevelChunk.setBlockState` 返回 null 就直接 false）。后果很实际：把一个结构**原样叠在它自己上面**，第二次不会有 NBT 回填，也不会有形状修补。

### 12.6 补液体（`liquidSettings`）

默认 `APPLY_WATERLOGGING`。逐块放置时，先记下"这块原来的液体状态"，再放方块：

| 新方块的情况 | 处理 |
|---|---|
| 新状态自带**水源**（`getFluidState().isSource()`） | 记进 `list2`（"这格已经是水源头了"） |
| 方块是 `LiquidBlockContainer`（台阶、栅栏、活板门等） | 调 `placeLiquid(...)` 灌进去；不是水源则记进 `list1` 等后续扩散 |
| 都不是 | 不管 |

最后有个 `while` 循环，反复扫 `list1`：从 UP/NORTH/EAST/SOUTH/WEST 五个邻居里找水源，找到了就灌。所以**水下放结构，水会自动补满**。不想要这个行为（比如做"排水"效果），`setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING)`。

### 12.7 补形状（`knownShape == false` 时）

放完之后还有一轮"善后"，两步：

1. `updateShapeAtEdge`：用 `BitSetDiscreteVoxelShape` 把这次放的所有方块记成一体，只处理**暴露在空气中的边界面**——`updateShape` 双向调，栅栏、玻璃板、墙会自动连上。
2. 每块 `Block.updateFromNeighbourShapes(...)`，状态变了就再 `setBlock(flags & -2 | 16)`（去掉邻居更新、加上"已知形状"），最后 `blockUpdated` 通知邻居。

`setKnownShape(true)` 会把这两步**全部跳过**。世界生成（jigsaw）就靠这个省性能——`SinglePoolElement.place` 传的 flags 是 `18`（`16 | 2`）。

### 12.8 实体

`addEntitiesToWorld(level, offset, settings)` → `processEntityInfos`：

1. 位置用 `StructureTemplate.transform(Vec3, mirror, rotation, pivot)` 变换。**注意这是另一套公式**：镜像用的是 `1.0 - d`（以"块边界"为轴翻转），和方块位置用的 `-k`（以原点为轴）不是同一个数学约定。
2. NBT 里删掉 `UUID`（防止复制出重复实体），重写 `Pos`。
3. 朝向：`entity.rotate(rotation)` 再加镜像修正。
4. `shouldFinalizeEntities()` 为 true 且实体是 `Mob` → 调 `finalizeSpawn(level, 当前位置难度, MobSpawnType.STRUCTURE, null)`。**这就是 jigsaw 结构里的怪为什么能带正确的装备/属性**。
5. `addFreshEntityWithPassengers`（会带上乘客/拴绳的动物）。

### 12.9 那 `boundingBox` 到底裁什么

`settings.getBoundingBox()` 在两处被用来 `isInside`：

- `placeInWorld` 的逐块循环（方块）
- `addEntitiesToWorld`（实体）

另外 `filterBlocks(...)` 也用它。**用 `null` 就是全放**。jigsaw 会给每个 piece 传一个"本区块已生成范围"，避免结构跨区块写到还没生成的区块里——手动调用时你不用设，除非想只放一部分（做"从地基往上补齐"之类的效果）。

### 12.10 `copy()` 复制的是什么

新对象 + 字段逐个赋值 + `processors.addAll(...)`。所以：**列表是新的，里面的处理器实例是同一个**。给 jigsaw 里的每个 piece 发一份独立 settings 时用它，改一个不会影响另一个。

## 十三、坐标：千万别把 `offset` 当"最小角"

### 13.1 `transform` 的真公式

```java
public static BlockPos transform(BlockPos targetPos, Mirror mirror, Rotation rotation, BlockPos offset)
```

先把镜像作用在局部坐标上，再绕 `offset`（= `rotationPivot`）旋转（`l` = pivot.x，`i1` = pivot.z）：

| 变换 | 结果（x, y, z） |
|---|---|
| `NONE` | `(i, j, k)` |
| `CLOCKWISE_90` | `(l + i1 - k, j, i1 - l + i)` |
| `COUNTERCLOCKWISE_90` | `(l - i1 + k, j, l + i1 - i)` |
| `CLOCKWISE_180` | `(2l - i, j, 2i1 - k)` |
| `Mirror.LEFT_RIGHT` | 先 `k = -k` |
| `Mirror.FRONT_BACK` | 先 `i = -i` |

### 13.2 为什么旋转后会跑到负方向

`rotationPivot` 默认是 `(0,0,0)`。拿一个 5×5 的模板举例，`offset = pos`：

| rotation | 实际占位（相对 pos） |
|---|---|
| `NONE` | x: `[0, 4]`, z: `[0, 4]` |
| `CLOCKWISE_90` | x: `[-4, 0]`, z: `[0, 4]` ← 往 **-X** 长 |
| `COUNTERCLOCKWISE_90` | x: `[0, 4]`, z: `[-4, 0]` ← 往 **-Z** 长 |
| `CLOCKWISE_180` | x: `[-4, 0]`, z: `[-4, 0]` |

`offset` 是**局部原点**变换后的落点，不是包围盒最小角。想让它"从 pos 往 +X/+Z 长"，必须自己换算。

### 13.3 `getZeroPositionWithTransform`：官方给的换算器

```java
BlockPos offset = StructureTemplate.getZeroPositionWithTransform(minCorner, mirror, rotation, sizeX, sizeZ);
```

它做的事就是按上表反推：把"我想要的最小角"翻译成该传给 `placeInWorld` 的 `offset`。**手动放置 + 带旋转时，这个必须用**，否则结构会偏移一整个宽度。

### 13.4 想知道自己算对没：`getBoundingBox`

```java
BoundingBox box = template.getBoundingBox(settings, offset);  // 变换后真实占位
```

内部是 `transform((0,0,0))` 和 `transform((size-1))` 两个角、`fromCorners` 归一化、再 `move(startPos)`。拿它跟预期比一比，比在游戏里数方块快。

## 十四、`flags`：四个常用组合

`Block` 里的常量（已核对）：

| 值 | 常量 | 作用 |
|---|---|---|
| `1` | `UPDATE_NEIGHBORS` | `blockUpdated` → 通知邻居（红石、比较器），会连锁 |
| `2` | `UPDATE_CLIENTS` | 发包给客户端刷新渲染 |
| `4` | `UPDATE_INVISIBLE` | （客户端侧）不重发这块的渲染更新；服务端无影响 |
| `16` | `UPDATE_KNOWN_SHAPE` | 不递归触发 `updateNeighbourShapes`（跳过形状连锁） |
| `64` | `UPDATE_MOVE_BY_PISTON` | 标记为活塞推动，`setBlock` 内部据此走"移动"分支 |

组合：

| flags | 等于 | 谁在用 | 效果 |
|---|---|---|---|
| `3` | `UPDATE_ALL` | 日常手动放置 | 发客户端 + 通知邻居，最"正常"，也最慢 |
| `2` | `UPDATE_CLIENTS` | `/place template` | 只发客户端；靠末尾的"补形状"把楼梯栅栏连上 |
| `18` | `16 \| 2` | jigsaw 世界生成 | 发客户端 + 完全不碰邻居，最快；**楼梯/栅栏不会自动连**（靠 NBT 里存好的状态） |
| `20` | `16 \| 4` | 放 BARRIER 占位 | 不同步、不形状更新，纯服务端临时标记 |

**手动放结构用 `3`（`Block.UPDATE_ALL`）**：`2` 会让红石/漏斗之类的连接关系不更新。

## 十五、处理器链：真正用来"改造"的扩展点

### 15.1 接口（1.21.1 的签名）

```java
public abstract class StructureProcessor {
    protected abstract StructureProcessorType<?> getType();

    // 逐块改写；返回 null = 丢弃这块
    public StructureBlockInfo process(LevelReader level, BlockPos offset, BlockPos pos,
            StructureBlockInfo original, StructureBlockInfo current,
            StructurePlaceSettings settings, @Nullable StructureTemplate template) { ... }

    // 全部块处理完之后，整体再改一次；可以返回更长的列表（唯一能加方块的钩子）
    public List<StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level, BlockPos offset, BlockPos pos,
            List<StructureBlockInfo> original, List<StructureBlockInfo> processed, StructurePlaceSettings settings) { ... }

    // 实体版的 process，返回 null = 这个实体不放
    public StructureEntityInfo processEntity(LevelReader level, BlockPos seedPos,
            StructureEntityInfo raw, StructureEntityInfo current,
            StructurePlaceSettings settings, StructureTemplate template) { ... }
}
```

`StructureBlockInfo` 是 record：`pos()` / `state()` / `nbt()`。

### 15.2 内置处理器（都在 `templatesystem` 包）

| 类 | 注册名 | 干什么 |
|---|---|---|
| `BlockRotProcessor` | `block_rot` | 按 `integrity` 概率删方块（`random > integrity` 就 `return null`）。`/place template ... 0.5` 的"残缺"效果 |
| `BlockIgnoreProcessor` | `block_ignore` | 丢弃指定方块，预设 `STRUCTURE_BLOCK` / `AIR` / `STRUCTURE_AND_AIR` |
| `JigsawReplacementProcessor` | `jigsaw_replacement` | 把模板里的 jigsaw 方块换成拼出来的子结构 |
| `RuleProcessor` | `rule` | 规则表替换（`ProcessorRule` + `RuleTest`），做"随机替换成苔石"这类 |
| `GravityProcessor` | `gravity` | 按高度图把方块往下贴地形 |
| `ProtectedBlockProcessor` | `protected_blocks` | 目标位置是保护方块时**不覆盖**（保住地形/已有建筑） |
| `CappedProcessor` | `capped` | 给"带概率的规则"限总次数 |
| `BlockAgeProcessor` | `block_age` | 石砖/裂纹砖的"变旧"随机替换 |
| `NopProcessor` | `nop` | 什么都不做（占位） |

### 15.3 自己写一个（NeoForge 1.21.1）

```java
public class MossyProcessor extends StructureProcessor {
    // 1.21.1 的 codec() 是 MapCodec，不是 Codec
    public static final MapCodec<MossyProcessor> CODEC = MapCodec.unit(MossyProcessor::new);

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos,
            StructureTemplate.StructureBlockInfo original, StructureTemplate.StructureBlockInfo current,
            StructurePlaceSettings settings) {
        if (current.state().is(Blocks.COBBLESTONE)) {
            return new StructureTemplate.StructureBlockInfo(
                    current.pos(), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), current.nbt());
        }
        return current;      // 返回 null 可以整块删掉
    }

    @Override
    protected StructureProcessorType<?> getType() { return ModProcessors.MOSSY.get(); }
}
```

注册：

```java
public static final DeferredRegister<StructureProcessorType<?>> PROCESSOR_TYPES =
        DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, "thrive");

public static final Supplier<StructureProcessorType<MossyProcessor>> MOSSY =
        PROCESSOR_TYPES.register("mossy", () -> () -> MossyProcessor.CODEC);
```

在 Java 里直接挂：

```java
settings.addProcessor(new MossyProcessor());
```

或者走 JSON（template_pool 的 `"processors"` 填一个 `processor_list` 的 id）：

```json
// data/thrive/worldgen/processor_list/mossy.json
{ "processors": [ { "processor_type": "thrive:mossy" } ] }
```

## 十六、反直觉结论 & 踩坑清单

1. **决定落点是 `offset`，不是 `pos`。** `pos` 只喂给处理器。第 8.2 节的注释原本写成"pos 是放置起点（用于计算偏移）"，是错的，已订正。
2. **`rotation != NONE` 时结构会往负方向长。** 传 `pos` 当 `offset` 只会让你以为"旋转后位置变了"。用 `getZeroPositionWithTransform`。
3. **`settings.random` 一旦设了，就是有状态的。** 同一份 settings 连放两次，调色板和处理器随机都不一样。要可复现就别设。
4. **`palette` 字段没用。** 别在它身上花时间。
5. **返回值 `true` ≠ 全部成功。** 想确认，就比 `getBoundingBox(settings, offset)` 和实际。
6. **`setBlock` 因为状态没变化返回 `false`，会连带跳过 NBT 回填和形状修补。** 原地重复放置要留意。
7. **`knownShape=true` 会跳过"补形状"。** 手动放置别开，否则该连的栅栏不连。
8. **处理器返回 `null` 是"删块"，不是"跳过这个处理器"。** 想"不改"，`return current`。
9. **`processBlockInfos` 会先 `nbt.copy()`。** 你在处理器里改 `current.nbt()` 不会污染模板本体（模板是缓存复用的，这点很重要）。
10. **`placeInWorld` 不保证区块已加载。** 手动调用前自己确认（`level.isLoaded(pos)` 或 `level.getChunkSource()`），否则会触发同步加载，在世界生成期尤其危险。

## 十七、给本项目的一句话落地版

放一棵树/一个小结构，最稳的写法：

```java
public static boolean placeTemplate(ServerLevel level, ResourceLocation id, BlockPos minCorner, Rotation rot) {
    StructureTemplate template = level.getStructureManager().get(id).orElse(null);
    if (template == null) return false;

    StructurePlaceSettings settings = new StructurePlaceSettings()
            .setRotation(rot)
            .setMirror(Mirror.NONE)
            .setIgnoreEntities(false)
            .setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);   // 树上不想被灌水

    BlockPos offset = StructureTemplate.getZeroPositionWithTransform(
            minCorner, Mirror.NONE, rot, template.getSize().getX(), template.getSize().getZ());

    return template.placeInWorld(level, offset, minCorner, settings, level.getRandom(), Block.UPDATE_ALL);
}
```

四句话总结：**`offset` 定位、`settings` 定"怎么变"、`processors` 定"改成什么"、`flags` 定"改动怎么传播"。**

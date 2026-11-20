package com.thelema.thrive.register;

import com.thelema.thrive.ThriveMod;
import com.thelema.thrive.generic.block.*;
import com.thelema.thrive.generic.block.wood.DesertPoplarLeaves;
import com.thelema.thrive.generic.block.wood.WoodSignBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static com.thelema.thrive.register.ItemRegister.ITEM;

public class BlockRegister {

    public static final DeferredRegister<Block> BLOCK = DeferredRegister.createBlocks(ThriveMod.MOD_ID);

    public static void register(IEventBus bus) {
        BLOCK.register(bus);
    }

    public static <B extends Block> DeferredHolder<Block, B> registerWithItem(String name, Supplier<B> supplier) {
        DeferredHolder<Block, B> block = BLOCK.register(name, supplier);
        // 注册对应的方块物品
        ITEM.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }


    // ========== 群系转化相关 ==========

    // 群系核心
    public static final DeferredHolder<Block, Block> BIOME_CORE = registerWithItem("biome_core", BiomeCore::new);

    // 群系树苗
    public static final DeferredHolder<Block, Block> BIOME_SAPLING = registerWithItem("biome_sapling", BiomeSapling::new);

    // ========== 花哨的树 ==========
    public static final DeferredHolder<Block, Block> FANCY_ACACIA_SAPLING_1 = registerWithItem("fancy_acacia_sapling_1", () -> new FancySapling(ThriveMod.MOD_ID, "fancy_acacia_tree_1", 3, 0, 4));
    public static final DeferredHolder<Block, Block> FANCY_ACACIA_SAPLING_2 = registerWithItem("fancy_acacia_sapling_2", () -> new FancySapling(ThriveMod.MOD_ID, "fancy_acacia_tree_2", 4, 0, 5));
    public static final DeferredHolder<Block, Block> FANCY_BIRCH_SAPLING_1 = registerWithItem("fancy_birch_sapling_1", () -> new FancySapling(ThriveMod.MOD_ID, "fancy_birch_tree_1", 3, 0, 3));
    public static final DeferredHolder<Block, Block> FANCY_BERRY_SAPLING_1 = registerWithItem("fancy_cherry_sapling_1", () -> new FancySapling(ThriveMod.MOD_ID, "fancy_cherry_tree_1", 8, 0, 5));


    // ========== misc ==========
    public static final DeferredHolder<Block, Block> GRASS_BLOCK = registerWithItem("grass_block", () -> new GrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK)));

    // ========== moss ==========

    public static final DeferredHolder<Block, Block> MOSS_FRAME = registerWithItem("moss_frame", () -> new Block(Block.Properties.of().strength(4.0F)));            // 硬度 4（原木为 2）.sound(SoundType.WOOD)       // 木质音效.requiresCorrectToolForDrops() // 可选：需要合适工具));

    public static final DeferredHolder<Block, Block> MOSS_PLANTING_BLOCK = registerWithItem("moss_planting_block", MossPlantingBlock::new);

    public static final DeferredHolder<Block, Block> LANTERN_BERRY = registerWithItem("lantern_berry", LanternBerry::new);

    public static final DeferredHolder<Block, Block> LANTERN_BERRY_PEEL = registerWithItem("lantern_berry_peel", () -> new Block(Block.Properties.of().strength(1.0F).sound(SoundType.MUD)));

    public static final DeferredHolder<Block, Block> SUN_BERRY = registerWithItem("sun_berry", SunBerry::new);


    // ========== garden ==========
    public static final DeferredHolder<Block, Block> GARDEN_BEE_NEST = registerWithItem("garden_bee_nest", GardenBeeNest::new);

    // ========== cloud ==========
    public static final DeferredHolder<Block, Block> CLOUD = registerWithItem("cloud", Cloud::new);

    public static final DeferredHolder<Block, Block> WISPY_CLOUD = registerWithItem("wispy_cloud", Cloud::new);

    // ========== tugai ==========
    public static final DeferredHolder<Block, Block> SALT_BLOCK = registerWithItem("salt_block", () -> new Block(BlockBehaviour.Properties.of().strength(0.6F).sound(SoundType.CALCITE).mapColor(DyeColor.WHITE)));

    public static final DeferredHolder<Block, Block> ALKALI_BLOCK = registerWithItem("alkali_block", () -> new Block(BlockBehaviour.Properties.of().strength(0.6f).sound(SoundType.CALCITE).mapColor(DyeColor.WHITE)));

    public static final DeferredHolder<Block, Block> DUST_BLOCK = registerWithItem("dust_block", DustBlock::new);

    // 胡杨 desert_poplar
    // 去皮关系在 data/thrive/neoforge/data_maps/block/strippables.json 里，不用写 Java

    public static final DeferredHolder<Block, Block> STRIPPED_DESERT_POPLAR_LOG = registerWithItem("stripped_desert_poplar_log", () -> new RotatedPillarBlock(logProps(MapColor.WOOD, MapColor.WOOD)));

    public static final DeferredHolder<Block, Block> STRIPPED_DESERT_POPLAR_WOOD = registerWithItem("stripped_desert_poplar_wood", () -> new RotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()));

    public static final DeferredHolder<Block, Block> DESERT_POPLAR_LOG = registerWithItem("desert_poplar_log", () -> new RotatedPillarBlock(logProps(MapColor.WOOD, MapColor.COLOR_BROWN)));

    public static final DeferredHolder<Block, Block> DESERT_POPLAR_WOOD = registerWithItem("desert_poplar_wood", () -> new RotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()));

    public static final DeferredHolder<Block, Block> DESERT_POPLAR_PLANKS = registerWithItem("desert_poplar_planks", () -> new Block(planksProps()));

    public static final DeferredHolder<Block, Block> DESERT_POPLAR_STAIRS = registerWithItem("desert_poplar_stairs", () -> new StairBlock(DESERT_POPLAR_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(DESERT_POPLAR_PLANKS.get())));

    public static final DeferredHolder<Block, Block> DESERT_POPLAR_SLAB = registerWithItem("desert_poplar_slab", () -> new SlabBlock(planksProps()));

    public static final DeferredHolder<Block, Block> DESERT_POPLAR_FENCE = registerWithItem("desert_poplar_fence", () -> new FenceBlock(BlockBehaviour.Properties.of().mapColor(DESERT_POPLAR_PLANKS.get().defaultMapColor()).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).ignitedByLava().sound(SoundType.WOOD)));

    public static final DeferredHolder<Block, Block> DESERT_POPLAR_FENCE_GATE = registerWithItem("desert_poplar_fence_gate", () -> new FenceGateBlock(WoodTypeRegister.DESERT_POPLAR, BlockBehaviour.Properties.of().mapColor(DESERT_POPLAR_PLANKS.get().defaultMapColor()).forceSolidOn().instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).ignitedByLava()));

    public static final DeferredHolder<Block, Block> DESERT_POPLAR_DOOR = registerWithItem("desert_poplar_door", () -> new DoorBlock(WoodTypeRegister.DESERT_POPLAR_SET, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR).mapColor(DESERT_POPLAR_PLANKS.get().defaultMapColor())));

    public static final DeferredHolder<Block, Block> DESERT_POPLAR_TRAPDOOR = registerWithItem("desert_poplar_trapdoor", () -> new TrapDoorBlock(WoodTypeRegister.DESERT_POPLAR_SET, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR).mapColor(MapColor.WOOD)));

    public static final DeferredHolder<Block, Block> DESERT_POPLAR_PRESSURE_PLATE = registerWithItem("desert_poplar_pressure_plate", () -> new PressurePlateBlock(WoodTypeRegister.DESERT_POPLAR_SET, BlockBehaviour.Properties.of().mapColor(DESERT_POPLAR_PLANKS.get().defaultMapColor()).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollission().strength(0.5F).ignitedByLava().pushReaction(PushReaction.DESTROY)));

    public static final DeferredHolder<Block, Block> DESERT_POPLAR_BUTTON = registerWithItem("desert_poplar_button", () -> new ButtonBlock(WoodTypeRegister.DESERT_POPLAR_SET, 30, BlockBehaviour.Properties.of().noCollission().strength(0.5F).pushReaction(PushReaction.DESTROY)));

    public static final DeferredHolder<Block, Block> DESERT_POPLAR_SIGN = BLOCK.register("desert_poplar_sign", () -> new WoodSignBlocks.Standing(WoodTypeRegister.DESERT_POPLAR, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SIGN).mapColor(DESERT_POPLAR_PLANKS.get().defaultMapColor())));

    public static final DeferredHolder<Block, Block> DESERT_POPLAR_WALL_SIGN = BLOCK.register("desert_poplar_wall_sign", () -> new WoodSignBlocks.Wall(WoodTypeRegister.DESERT_POPLAR, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WALL_SIGN).mapColor(DESERT_POPLAR_PLANKS.get().defaultMapColor())));

    public static final DeferredHolder<Block, Block> DESERT_POPLAR_HANGING_SIGN = BLOCK.register("desert_poplar_hanging_sign", () -> new WoodSignBlocks.CeilingHanging(WoodTypeRegister.DESERT_POPLAR, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_HANGING_SIGN).mapColor(DESERT_POPLAR_PLANKS.get().defaultMapColor())));

    public static final DeferredHolder<Block, Block> DESERT_POPLAR_WALL_HANGING_SIGN = BLOCK.register("desert_poplar_wall_hanging_sign", () -> new WoodSignBlocks.WallHanging(WoodTypeRegister.DESERT_POPLAR, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WALL_HANGING_SIGN).mapColor(DESERT_POPLAR_PLANKS.get().defaultMapColor())));

    static {
        // 告示牌物品必须是 SignItem / HangingSignItem：
        // 普通 BlockItem 只会放「立式」那一个方块，贴墙放置时不会换成 wall 变体。
        // SignItem 内部按玩家朝向在 standing / wall 之间二选一（getPlacementState）。
        ITEM.register("desert_poplar_sign", () -> new SignItem(new Item.Properties().stacksTo(16),
                DESERT_POPLAR_SIGN.get(), DESERT_POPLAR_WALL_SIGN.get()));
        ITEM.register("desert_poplar_hanging_sign", () -> new HangingSignItem(
                DESERT_POPLAR_HANGING_SIGN.get(), DESERT_POPLAR_WALL_HANGING_SIGN.get(), new Item.Properties().stacksTo(16)));
    }

    public static final DeferredHolder<Block, Block> DESERT_POPLAR_LEAVES = registerWithItem("desert_poplar_leaves", () -> new DesertPoplarLeaves(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES).mapColor(MapColor.TERRACOTTA_PURPLE)));

    public static final DeferredHolder<Block, Block> DESERT_POPLAR_SAPLING = registerWithItem("desert_poplar_sapling", DesertPoplarSapling::new);

    public static final DeferredHolder<Block, Block> POTTED_DESERT_POPLAR_SAPLING = BLOCK.register("potted_desert_poplar_sapling", () -> new FlowerPotBlock(DESERT_POPLAR_SAPLING.get(), BlockBehaviour.Properties.of().instabreak().noOcclusion().pushReaction(PushReaction.DESTROY)));

    /**
     * 木制品的燃烧属性。
     *
     * <p>原版把燃烧写死在 {@code FireBlock} 的两张表里（{@code Bootstrap.bootStrap()} 时注册），
     * 模组方块不在表里 → 默认不燃。NeoForge 的三个默认实现
     * （{@code isFlammable} / {@code getFlammability} / {@code getFireSpreadSpeed}）
     * 就是去查这两张表，所以往表里塞等价于覆写那三个方法，但代码少得多。
     *
     * <p>参数顺序容易被名字骗：{@code setFlammable(block, encouragement, flammability)}
     * <ul>
     *   <li>{@code encouragement} → {@code igniteOdds} → 被 {@code getFireSpreadSpeed} 读，**蔓延速度**</li>
     *   <li>{@code flammability} → {@code burnOdds} → 被 {@code getFlammability} 读，**被烧掉的概率**</li>
     * </ul>
     *
     * <p>数值对齐原版：木板系 {@code (5, 20)}、原木系 {@code (5, 5)}、树叶 {@code (30, 60)}。
     * 注意原版**没有**给门/活板门/按钮/压力板/告示牌设燃烧，这里也不设。
     */
    public static void registerFlammables() {
        FireBlock fire = (FireBlock) Blocks.FIRE;

        fire.setFlammable(DESERT_POPLAR_LOG.get(), 5, 5);
        fire.setFlammable(DESERT_POPLAR_WOOD.get(), 5, 5);
        fire.setFlammable(STRIPPED_DESERT_POPLAR_LOG.get(), 5, 5);
        fire.setFlammable(STRIPPED_DESERT_POPLAR_WOOD.get(), 5, 5);

        fire.setFlammable(DESERT_POPLAR_PLANKS.get(), 5, 20);
        fire.setFlammable(DESERT_POPLAR_STAIRS.get(), 5, 20);
        fire.setFlammable(DESERT_POPLAR_SLAB.get(), 5, 20);
        fire.setFlammable(DESERT_POPLAR_FENCE.get(), 5, 20);
        fire.setFlammable(DESERT_POPLAR_FENCE_GATE.get(), 5, 20);

        fire.setFlammable(DESERT_POPLAR_LEAVES.get(), 30, 60);
    }

    /**
     * 原木/木的通用属性：Y 轴用顶面颜色，其余轴用侧面颜色。
     */
    private static BlockBehaviour.Properties logProps(MapColor top, MapColor side) {
        return BlockBehaviour.Properties.of()
                .mapColor(state -> state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y ? top : side)
                .instrument(NoteBlockInstrument.BASS)
                .strength(2.0F)
                .sound(SoundType.WOOD)
                .ignitedByLava();
    }

    /**
     * 木板/台阶通用的属性。
     */
    private static BlockBehaviour.Properties planksProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .instrument(NoteBlockInstrument.BASS)
                .strength(2.0F, 3.0F)
                .sound(SoundType.WOOD)
                .ignitedByLava();
    }


}
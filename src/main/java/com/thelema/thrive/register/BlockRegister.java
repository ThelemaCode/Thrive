package com.thelema.thrive.register;

import com.thelema.thrive.ThriveMod;
import com.thelema.thrive.generic.block.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
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

    public static final DeferredHolder<Block, Block> MOSS_FRAME = registerWithItem("moss_frame", () -> new Block(Block.Properties.of().strength(4.0F)));            // 硬度 4（原木为 2）.sound(SoundType.WOOD)       // 木质音效.requiresCorrectToolForDrops() // 可选：需要合适工具));

    public static final DeferredHolder<Block, Block> MOSS_PLANTING_BLOCK = registerWithItem("moss_planting_block", MossPlantingBlock::new);

    public static final DeferredHolder<Block, Block> LANTERN_BERRY = registerWithItem("lantern_berry", LanternBerry::new);

    public static final DeferredHolder<Block, Block> LANTERN_BERRY_PEEL = registerWithItem("lantern_berry_peel", () -> new Block(Block.Properties.of().strength(1.0F).sound(SoundType.MUD)));

    public static final DeferredHolder<Block, Block> SUN_BERRY = registerWithItem("sun_berry", SunBerry::new);

    public static final DeferredHolder<Block, Block> GRASS_BLOCK = registerWithItem("grass_block", () -> new GrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK)));

    public static final DeferredHolder<Block, Block> GARDEN_BEE_NEST = registerWithItem("garden_bee_nest", GardenBeeNest::new);

    public static final DeferredHolder<Block, Block> CLOUD = registerWithItem("cloud", Cloud::new);

    public static final DeferredHolder<Block, Block> WISPY_CLOUD = registerWithItem("wispy_cloud", Cloud::new);

    // ------ 群系转化相关 ------

    // 群系核心
    public static final DeferredHolder<Block, Block> BIOME_CORE = registerWithItem("biome_core", BiomeCore::new);

    // 群系树苗
    public static final DeferredHolder<Block, Block> BIOME_SAPLING = registerWithItem("biome_sapling", BiomeSapling::new);

    // ------ 花哨的树 ------
    public static final DeferredHolder<Block, Block> FANCY_ACACIA_SAPLING_1 = registerWithItem("fancy_acacia_sapling_1", ()->new FancySapling(ThriveMod.MOD_ID, "fancy_acacia_tree_1", 3, 0, 4));
    public static final DeferredHolder<Block, Block> FANCY_ACACIA_SAPLING_2 = registerWithItem("fancy_acacia_sapling_2", ()->new FancySapling(ThriveMod.MOD_ID, "fancy_acacia_tree_2", 4, 0, 5));
    public static final DeferredHolder<Block, Block> FANCY_BIRCH_SAPLING_1 = registerWithItem("fancy_birch_sapling_1", ()->new FancySapling(ThriveMod.MOD_ID, "fancy_birch_tree_1", 3, 0, 3));
    public static final DeferredHolder<Block, Block> FANCY_BERRY_SAPLING_1 = registerWithItem("fancy_cherry_sapling_1", ()->new FancySapling(ThriveMod.MOD_ID, "fancy_cherry_tree_1", 8, 0, 5));

    // ------ moss ------
    // ------ garden ------


}
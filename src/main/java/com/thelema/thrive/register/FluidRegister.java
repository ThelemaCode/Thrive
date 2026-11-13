package com.thelema.thrive.register;

import com.thelema.thrive.ThriveMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import javax.annotation.Nullable;

@EventBusSubscriber(modid = ThriveMod.MOD_ID)
public class FluidRegister {

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, ThriveMod.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, ThriveMod.MOD_ID);

    // ─── 流体：灯笼浆果汁 ───

    /**
     * 流体物理属性。
     * density：密度，>0 下沉、<0 上升（水=1000，岩浆=3000）
     * temperature：温度，影响音效与部分交互
     * canConvertToSource：能否无限再生（水=true，岩浆=false）
     * lightLevel：流体方块的光照等级 0~15，LiquidBlock.getLightEmission 会读它
     * viscosity 在 NeoForge 1.21 未接入 tickDelay，改流速靠覆写 getTickDelay
     */
    public static final DeferredHolder<FluidType, FluidType> LANTERN_BERRY_JUICE_TYPE =
            FLUID_TYPES.register("lantern_berry_juice_type", () -> new FluidType(
                    FluidType.Properties.create()
                            .density(2000)
                            .temperature(350)
                            .canConvertToSource(false)
                            .lightLevel(15)
                            .canSwim(true)
                            .supportsBoating(true)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)){

            });

    /**
     * 源头流体。
     * getTickDelay：每次扩散的间隔 tick 数。水=5，岩浆=30。
     * 这里取 20，介于两者之间。
     */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LANTERN_BERRY_JUICE_STILL =
            FLUIDS.register("lantern_berry_juice_still", () -> new BaseFlowingFluid.Source(lanternBerryJuiceProps()) {
                @Override
                public int getTickDelay(LevelReader level) {
                    return 20;
                }
            });

    /**
     * 流动流体。getTickDelay 必须与 Source 一致，否则源头慢、流动快。
     */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> LANTERN_BERRY_JUICE_FLOWING =
            FLUIDS.register("lantern_berry_juice_flowing", () -> new BaseFlowingFluid.Flowing(lanternBerryJuiceProps()) {
                @Override
                public int getTickDelay(LevelReader level) {
                    return 20;
                }
            });

    /**
     * 流体方块（世界里的实际形态）。
     * entityInside：实体浸入时加效果。entityInside 只在服务端执行，
     *               每 5 tick 续一次，避免每 tick 重复 addEffect 造成闪烁。
     * getLightEmission：从 FluidType 读光照，原版 LiquidBlock 默认返回 0。
     */
    public static final DeferredHolder<Block, LiquidBlock> LANTERN_BERRY_JUICE_BLOCK =
            BlockRegister.BLOCK.register("lantern_berry_juice_block", () -> new LiquidBlock(
                    LANTERN_BERRY_JUICE_STILL.get(),
                    Block.Properties.ofFullCopy(Blocks.WATER).noLootTable()
            ) {
                @Override
                public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
                    super.entityInside(state, level, pos, entity);
                    if (entity.tickCount % 5 != 0 || !(entity instanceof LivingEntity living)) return;
                    living.addEffect(new MobEffectInstance(EffectRegister.LANTERN_BERRY_JUICE, 2400));
                }

                @Override
                public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
                    return state.getFluidState().getFluidType().getLightLevel();
                }

            });

    public static final DeferredHolder<Item, BucketItem> LANTERN_BERRY_JUICE_BUCKET =
            ItemRegister.ITEM.register("lantern_berry_juice_bucket", () -> new BucketItem(
                    LANTERN_BERRY_JUICE_STILL.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    /**
     * 流体通用参数。
     * slopeFindDistance：向下寻找斜坡的最大水平距离（水=4，岩浆=2）
     * levelDecreasePerBlock：每水平扩散一格损失的液面高度（水=1，岩浆=2）
     * 两者都取岩浆值 → 铺得短、流得慢
     */
    private static BaseFlowingFluid.Properties lanternBerryJuiceProps() {
        return new BaseFlowingFluid.Properties(LANTERN_BERRY_JUICE_TYPE, LANTERN_BERRY_JUICE_STILL, LANTERN_BERRY_JUICE_FLOWING)
                .block(LANTERN_BERRY_JUICE_BLOCK)
                .bucket(LANTERN_BERRY_JUICE_BUCKET)
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2);
    }

    // ─── 流体：天空水 ───

    // 1. 流体类型
    public static final DeferredHolder<FluidType, FluidType> SKY_WATER_TYPE =
            FLUID_TYPES.register("sky_water_type", () -> new FluidType(
                    FluidType.Properties.create()
                            .canConvertToSource(false)
                            .canSwim(true)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)){
                @Override
                public boolean isVaporizedOnPlacement(Level level, BlockPos pos, FluidStack stack) {
                    return level.dimensionType().ultraWarm();
                }

                @Override
                public void onVaporize(@Nullable Player player, Level level, BlockPos pos, FluidStack stack) {
                    SoundEvent sound = SoundEvents.FIRE_EXTINGUISH;
                    level.playSound(player, pos, sound, SoundSource.BLOCKS, 0.5F, 2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F);
                    if (level instanceof ServerLevel serverLevel) {
                        for (int i = 0; i < 8; ++i) {
                            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                                    pos.getX() + Math.random(), pos.getY() + Math.random(), pos.getZ() + Math.random(),
                                    1, 0.0, 0.0, 0.0, 0.0);
                        }
                    }
                }
            });

    // 2. 源方块 (禁止自身向四周扩散)
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SKY_WATER_STILL =
            FLUIDS.register("sky_water_still", () -> new BaseFlowingFluid.Source(skyWaterProps()) {
                @Override
                protected boolean canSpreadTo(BlockGetter level, BlockPos fromPos, BlockState fromBlockState, Direction direction, BlockPos toPos, BlockState toBlockState, FluidState toFluidState, Fluid fluid) {
                    return false;
                }
            });

    // 3. 流动方块 (彻底阻断任何流动)
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> SKY_WATER_FLOWING =
            FLUIDS.register("sky_water_flowing", () -> new BaseFlowingFluid.Flowing(skyWaterProps()) {
                @Override
                protected boolean canSpreadTo(BlockGetter level, BlockPos fromPos, BlockState fromBlockState, Direction direction, BlockPos toPos, BlockState toBlockState, FluidState toFluidState, Fluid fluid) {
                    return false;
                }
            });

    // 4. 流体方块
    public static final DeferredHolder<Block, LiquidBlock> SKY_WATER_BLOCK =
            BlockRegister.BLOCK.register("sky_water_block", () -> new LiquidBlock(
                    SKY_WATER_STILL.get(),
                    Block.Properties.ofFullCopy(Blocks.WATER).noLootTable()
            ) {
                @Override
                public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
                    return 0; // 确保方块状态也不发光
                }
            });

    // 5. 占位水桶 (复用原版水桶的贴图，但倒出来的是 sky_water)
    public static final DeferredHolder<Item, BucketItem> SKY_WATER_BUCKET =
            ItemRegister.ITEM.register("sky_water_bucket", () -> new BucketItem(
                    SKY_WATER_STILL.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    private static BaseFlowingFluid.Properties skyWaterProps() {
        return new BaseFlowingFluid.Properties(SKY_WATER_TYPE, SKY_WATER_STILL, SKY_WATER_FLOWING)
                .block(SKY_WATER_BLOCK)
                .bucket(SKY_WATER_BUCKET);
    }

    public static void register(IEventBus bus) {
        FLUID_TYPES.register(bus);
        FLUIDS.register(bus);
    }

    /**
     * 流体交互注册
     */
    @SubscribeEvent
    public static void register(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 果汁与岩浆
            FluidInteractionRegistry.addInteraction(
                    NeoForgeMod.LAVA_TYPE.value(),
                    new FluidInteractionRegistry.InteractionInformation(
                            FluidRegister.LANTERN_BERRY_JUICE_TYPE.get(),
                            (fluidState) -> fluidState.isSource()
                                    ? Blocks.OBSIDIAN.defaultBlockState()
                                    : Blocks.COBBLESTONE.defaultBlockState()
                    )
            );

            // 天空水与岩浆（与原版水一致）
            FluidInteractionRegistry.addInteraction(
                    NeoForgeMod.LAVA_TYPE.value(),
                    new FluidInteractionRegistry.InteractionInformation(
                            FluidRegister.SKY_WATER_TYPE.get(),
                            (fluidState) -> fluidState.isSource()
                                    ? Blocks.OBSIDIAN.defaultBlockState()
                                    : Blocks.COBBLESTONE.defaultBlockState()
                    )
            );
        });
    }
}
package com.thelema.thrive;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.thelema.thrive.generic.entity.misc.Beam;
import com.thelema.thrive.register.EntityRegister;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.joml.Vector3f;

@EventBusSubscriber
public class ThriveCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(
                Commands.literal("thrive")
                        .requires(source -> source.hasPermission(2))
                        // /thrive beam <起点> <终点> <类型>
                        .then(Commands.literal("beam")
                                .then(Commands.argument("startPos", Vec3Argument.vec3())
                                        .then(Commands.argument("endPos", Vec3Argument.vec3())
                                                .then(Commands.argument("type", StringArgumentType.string())
                                                        .executes(ThriveCommand::execBeam)
                                                )
                                        )
                                )
                        ));
    }

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        ThriveCommand.register(event.getDispatcher(), event.getBuildContext());
    }

    private static int execBeam(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();

        // 解析坐标，支持 ~ 相对坐标
        Vec3 start = Vec3Argument.getCoordinates(ctx, "startPos").getPosition(source);
        Vec3 end = Vec3Argument.getCoordinates(ctx, "endPos").getPosition(source);
        String type = StringArgumentType.getString(ctx, "type");

        Beam beam = EntityRegister.BEAM.get().create(source.getLevel());
        if (beam == null) return 0;

        beam.setPos(start);
        beam.getEntityData().set(Beam.START, new Vector3f((float) start.x, (float) start.y, (float) start.z));
        beam.getEntityData().set(Beam.END, new Vector3f((float) end.x, (float) end.y, (float) end.z));
        beam.getEntityData().set(Beam.TYPE, type);

        source.getLevel().addFreshEntity(beam);

        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                String.format("生成Beam | start:(%.2f,%.2f,%.2f) end:(%.2f,%.2f,%.2f) type:%s",
                        start.x, start.y, start.z,
                        end.x, end.y, end.z,
                        type)
        ), true);
        return 1;
    }
}

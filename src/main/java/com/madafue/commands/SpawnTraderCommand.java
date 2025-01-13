package com.madafue.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnLocation;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.passive.TraderLlamaEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;


public class SpawnTraderCommand {

    private static final int MAX_TRADER_SPAWN_RADIUS = 48;

    private static int executeCommand(CommandContext<ServerCommandSource> context) {
//        context.getSource().sendFeedback(() -> Text.literal("Loading..."), false);
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();

        if (!world.getGameRules().getBoolean(GameRules.DO_TRADER_SPAWNING)) {
            source.sendError(Text.of("Trader spawning is disabled in this world!"));
            return 0;
        }

        BlockPos playerPos = Objects.requireNonNull(source.getPlayer()).getBlockPos();

        BlockPos spawnPos = getNearbySpawnPos(world, playerPos, MAX_TRADER_SPAWN_RADIUS);
        if (spawnPos == null || !doesNotSuffocateAt(world, spawnPos)) {
            source.sendError(Text.of("Failed to find a valid spawn location for the Wandering Trader."));
            return 0;
        }

        WanderingTraderEntity trader = EntityType.WANDERING_TRADER.spawn(world, spawnPos, SpawnReason.COMMAND);
        if (trader != null) {
            trader.setDespawnDelay(48000);
            trader.setWanderTarget(playerPos);
            trader.setPositionTarget(playerPos, 16);

            for (int i = 0; i < 2; i++) {
                spawnLlama(world, trader, 4);
            }

            source.sendFeedback(() -> Text.literal("A Wandering Trader has been spawned!"), true);
            return 1;
        } else {
            source.sendError(Text.of("Failed to spawn the Wandering Trader."));
            return 0;
        }

    }

    private static void spawnLlama(ServerWorld world, WanderingTraderEntity trader, int range) {
        BlockPos llamaSpawnPos = getNearbySpawnPos(world, trader.getBlockPos(), range);
        if (llamaSpawnPos != null) {
            TraderLlamaEntity llama = EntityType.TRADER_LLAMA.spawn(world, llamaSpawnPos, SpawnReason.COMMAND);
            if (llama != null) {
                llama.attachLeash(trader, true);
            }
        }
    }

    @Nullable
    private static BlockPos getNearbySpawnPos(ServerWorld world, BlockPos pos, int range) {
        for (int i = 0; i < 10; i++) {
            int x = pos.getX() + world.getRandom().nextInt(range * 2) - range;
            int z = pos.getZ() + world.getRandom().nextInt(range * 2) - range;
            int y = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, x, z);
            BlockPos potentialPos = new BlockPos(x, y, z);

            SpawnLocation spawnLocation = SpawnRestriction.getLocation(EntityType.WANDERING_TRADER);
            if (spawnLocation != null && spawnLocation.isSpawnPositionOk(world, pos, EntityType.WANDERING_TRADER)) {
                return potentialPos;
            }
        }
        return null;
    }

    private static boolean doesNotSuffocateAt(ServerWorld world, BlockPos pos) {
        for (BlockPos checkPos : BlockPos.iterate(pos, pos.add(1, 2, 1))) {
            if (!world.getBlockState(checkPos).getCollisionShape(world, checkPos).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(
                CommandManager.literal("spawntrader")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(SpawnTraderCommand::executeCommand)
        );

    }





}

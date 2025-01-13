package com.madafue.util;

import com.madafue.commands.SpawnTraderCommand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class ModRegistries {
    public static void registerModThingies(){
        registerCommands();
    }

    private static void registerCommands(){
        CommandRegistrationCallback.EVENT.register(SpawnTraderCommand::register);
    }
}

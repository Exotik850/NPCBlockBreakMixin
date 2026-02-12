package com.byt3.plugin;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class BreakBlockMixinPlugin extends JavaPlugin {
    public BreakBlockMixinPlugin(@NonNullDecl JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void start() {
        HytaleLogger.getLogger().atInfo().log("Hello from BreakBlockMixinPlugin!");
    }
}

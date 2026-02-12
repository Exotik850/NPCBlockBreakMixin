package com.byt3.mixins;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.plugin.early.ClassTransformer;
import com.hypixel.hytale.plugin.early.EarlyPluginLoader;
import com.hypixel.hytale.plugin.early.TransformingClassLoader;
import com.hypixel.hytale.server.core.HytaleServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;

@Mixin(HytaleServer.class)
public class LoadPluginsMixin {
    @Inject(method = "<init>", at = @At("HEAD"))
    private static void init(CallbackInfo info) {
        try {
            loadClasses();
        } catch (Exception e) {
            HytaleLogger.getLogger().atSevere().withCause(e).log("Failed to load mixin transformers, plugins may not work correctly.");
        }
    }

    private static void loadClasses() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Optional<ClassTransformer> possible = EarlyPluginLoader.getTransformers().stream().filter(
                (t) -> t.getClass().getCanonicalName().equalsIgnoreCase("com.build_9.hyxin.HyxinTransformer")).findAny();
        if (possible.isEmpty()) {
            throw new RuntimeException("failed to load mixins");
        }
        URL url = possible.get().getClass().getProtectionDomain().getCodeSource().getLocation();
        TransformingClassLoader loader = (TransformingClassLoader) Thread.currentThread().getContextClassLoader();
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        method.setAccessible(true);
        method.invoke(loader, url);
        HytaleLogger.getLogger().atInfo().log("Hyxin mixin transformer loaded successfully.");
    }
}
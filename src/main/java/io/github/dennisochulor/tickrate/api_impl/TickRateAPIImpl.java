package io.github.dennisochulor.tickrate.api_impl;

import io.github.dennisochulor.tickrate.api.TickRateAPI;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickManager;

public final class TickRateAPIImpl implements TickRateAPI {

    private static TickRateAPI INSTANCE;

    public static TickRateAPI getInstance() {
        if(INSTANCE == null) throw new IllegalStateException("The MinecraftServer must be fully initialised first before using the TickRateAPI!");
        return INSTANCE;
    }

    /** Only ever called ONCE by TickRate's ModInitializer */
    public static void init(MinecraftServer server) {
        if(INSTANCE != null) throw new IllegalStateException("Only one instance can be created!");
        INSTANCE = new TickRateAPIImpl(server);
    }


    private final MinecraftServer server;
    private final ServerTickManager tickManager;

    private TickRateAPIImpl(MinecraftServer server) {
        this.server = server;
        this.tickManager = this.server.getTickManager();
    }

}

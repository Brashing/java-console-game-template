package com.example.dungeon.core;

public final class WorldInfo {
    private static final StringBuilder log = new StringBuilder();

    static {
        log.append("[static init WorldInfo]\n");
        ClassLoader cl = WorldInfo.class.getClassLoader();
        log.append("ClassLoader: ").append(cl).append("\n");
        if (cl != null) log.append("Parent: ").append(cl.getParent()).append("\n");
    }

    public static void touch(String who) {
        log.append("touched by ").append(who).append("\n");
    }

    private WorldInfo() {
    }
}

package me.serega100.dice.nms;

import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class HeadCreator {
    private final Constructor<? extends Head> constructor;

    public HeadCreator(Server server) throws UnsupportedVersion {
        final String packageName = server.getClass().getPackage().getName();
        final String version = packageName.substring(packageName.lastIndexOf('.') + 1);
        if (version.equals("craftbukkit")) {
            // Before renamed NMS
            throw new UnsupportedVersion();
        }
        try {
            Class simpleClass = Class.forName(Head.class.getPackage().getName() + ".Head_" + version);
            Class<? extends Head> clazz = (Class<? extends Head>) simpleClass;
            constructor = clazz.getConstructor(String.class);
        } catch (final Exception e) {
            // No support for this version
            throw new UnsupportedVersion();
        }
    }
    
    public Head newHead(String baseCode) {
        try {
            return constructor.newInstance(baseCode);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static class UnsupportedVersion extends Exception {

    }
}

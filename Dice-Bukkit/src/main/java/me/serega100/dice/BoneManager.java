package me.serega100.dice;

import me.serega100.dice.nms.Head;
import me.serega100.dice.nms.HeadCreator;
import org.bukkit.configuration.file.FileConfiguration;

public class BoneManager {
    private final Head[] heads;

    public BoneManager(FileConfiguration config, HeadCreator headCreator) {
        System.out.println("BoneManager loading..."); // todo debug
        heads = new Head[6];
        for (int i = 0; i < 6; i++) {
            String baseCode = config.getString("bones.number" + (i+1));
            heads[i] = headCreator.newHead(baseCode);
        }
        System.out.println("BoneManager loaded."); // todo debug
    }

    public Head getBone(int number) {
        return heads[number-1];
    }
}

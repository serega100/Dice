package me.serega100.dice.nms;

import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public abstract class Head {
    protected String base64;
    protected ItemStack item;

    protected Head(String baseCode) {
        this.base64 = baseCode;
        this.item = createHeadAsItem();
    }

    public ItemStack getAsItem() {
        return item;
    }

    public abstract void setAsBlock(Block block);

    protected abstract ItemStack createHeadAsItem();
}

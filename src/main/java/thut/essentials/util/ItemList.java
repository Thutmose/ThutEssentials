package thut.essentials.util;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;

public class ItemList extends Items
{
    public static Map<ResourceLocation, Set<Item>> pendingTags = Maps.newHashMap();

    public static boolean is(final ResourceLocation tag, final Object toCheck)
    {
        if (toCheck instanceof Item)
        {
            final Item item = (Item) toCheck;
            boolean tagged = ItemTags.getAllTags().getTagOrEmpty(tag).contains(item);
            tagged = tagged || ItemList.pendingTags.getOrDefault(tag, Collections.emptySet()).contains(item);
            if (!tagged) return item.getRegistryName().equals(tag);
            return tagged;
        }
        else if (toCheck instanceof ItemStack) return ItemList.is(tag, ((ItemStack) toCheck).getItem());
        else if (toCheck instanceof Block)
        {

            final Block block = (Block) toCheck;
            final boolean tagged = BlockTags.getAllTags().getTagOrEmpty(tag).contains(block);
            if (!tagged) return block.getRegistryName().equals(tag);
            return tagged;
        }
        else if (toCheck instanceof BlockState) return ItemList.is(tag, ((BlockState) toCheck).getBlock());
        return false;
    }
}
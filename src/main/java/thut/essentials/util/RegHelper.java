package thut.essentials.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class RegHelper
{
    public static ResourceLocation getKey(EntityType<?> type)
    {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type);
    }

    public static ResourceLocation getKey(Block type)
    {
        return BuiltInRegistries.BLOCK.getKey(type);
    }

    public static ResourceLocation getKey(Item type)
    {
        return BuiltInRegistries.ITEM.getKey(type);
    }

    public static ResourceLocation getKey(ItemStack stack)
    {
        return getKey(stack.getItem());
    }

    public static ResourceLocation getKey(Entity mob)
    {
        return getKey(mob.getType());
    }

    public static ResourceLocation getKey(BlockState blockState)
    {
        return getKey(blockState.getBlock());
    }
}

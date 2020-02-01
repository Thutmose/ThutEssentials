package thut.essentials.events;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.Cancelable;

@Cancelable
/**
 * Cancel this event to allow the item to be used. These events are only called
 * in the case where these items are about to be denied of use
 */
public class DenyItemUseEvent extends EntityEvent
{
    private final ItemStack toUse;

    private final UseType type;

    public UseType getType()
    {
        return this.type;
    }

    public DenyItemUseEvent(final Entity user, @Nullable final ItemStack toUse, final UseType type)
    {
        super(user);
        this.toUse = toUse;
        this.type = type;
    }

    @Nullable
    public ItemStack getItem()
    {
        return this.toUse;
    }

    public static enum UseType
    {
        RIGHTCLICKBLOCK, LEFTCLICKBLOCK, RIGHTCLICKITEM;
    }
}
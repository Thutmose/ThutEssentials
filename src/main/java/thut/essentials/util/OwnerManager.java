package thut.essentials.util;

import javax.annotation.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class OwnerManager
{
    public static interface IOwnerChecker
    {
        @Nullable
        LivingEntity getOwner(final Entity in);
    }

    public static IOwnerChecker OWNERCHECK = (in) -> null;
}

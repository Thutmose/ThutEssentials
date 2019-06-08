package thut.essentials.areacontrol;

import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.server.permission.IPermissionHandler;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.context.PlayerContext;
import thut.essentials.util.Transporter;
import thut.essentials.util.Transporter.Vector3;

public class Requirement
{
    List<String> neededPerms = Lists.newArrayList();
    IRejection   rejection;

    public Requirement()
    {
    }

    public boolean fills(PlayerEntity player)
    {
        IPermissionHandler manager = PermissionAPI.getPermissionHandler();
        for (String node : neededPerms)
            if (!manager.hasPermission(player.getGameProfile(), node, new PlayerContext(player))) return false;
        return true;
    }

    public interface IRejection
    {
        void reject(PlayerEntity player);
    }

    public static class TeleportReject implements IRejection
    {
        BlockPos pos;
        int      dimension;

        public TeleportReject()
        {
        }

        public TeleportReject(BlockPos pos, int dimension)
        {
            this.pos = pos;
            this.dimension = dimension;
        }

        @Override
        public void reject(PlayerEntity player)
        {
            if (pos != null) Transporter.teleportEntity(player, new Vector3(pos), dimension);
        }
    }

    public static class PushReject implements IRejection
    {
        Vector3 dir;

        public PushReject()
        {
        }

        public PushReject(Vector3 dir)
        {
            this.dir = dir;
        }

        @Override
        public void reject(PlayerEntity player)
        {
            if (dir != null)
            {
                player.addVelocity(dir.x, dir.y, dir.z);
            }
        }

    }
}

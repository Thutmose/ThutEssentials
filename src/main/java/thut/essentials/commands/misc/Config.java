package thut.essentials.commands.misc;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.IPermissionHandler;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.context.PlayerContext;
import thut.essentials.ThutEssentials;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.ConfigManager;
import thut.essentials.util.Configure;

public class Config extends BaseCommand
{
    public static final String EDITPERM = "thutessentials.command.config.edit";

    ArrayList<String>          fields   = Lists.newArrayList();

    HashMap<String, Field>     fieldMap = Maps.newHashMap();

    public Config()
    {
        super("teconfig", 0);
        PermissionAPI.registerNode(EDITPERM, DefaultPermissionLevel.OP, "Can the player edit configs via the command.");
        populateFields();
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender)
    {
        return true;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        boolean op = isOp(sender);
        if (args.length == 0) { throw new CommandException("arguments error, press tab for options"); }
        boolean check = args.length <= 1;
        Field field = fieldMap.get(args[0]);

        if (field == null) { throw new CommandException("arguments error, press tab for options"); }
        try
        {
            String text = "";
            Object o = field.get(ThutEssentials.instance.config);
            if (o instanceof String[])
            {
                text += Arrays.toString((Object[]) o);
            }
            else if (o instanceof int[])
            {
                text += Arrays.toString((int[]) o);
            }
            else
            {
                text += o;
            }
            ITextComponent mess = new TextComponentTranslation("thutcore.command.settings.check", args[0], text);
            if (check)
            {
                sender.sendMessage(mess);
                return;
            }
            if (!op) { throw new CommandException("No permission to do that"); }
            try
            {
                String val = args[1];
                if (args.length > 2)
                {
                    for (int i = 2; i < args.length; i++)
                    {
                        val = val + " " + args[i];
                    }
                }
                ThutEssentials.instance.config.updateField(field, val);
            }
            catch (Exception e)
            {
                throw new CommandException("invalid options");
            }
            text = "";
            o = field.get(ThutEssentials.instance.config);
            if (o instanceof String[])
            {
                text += Arrays.toString((Object[]) o);
            }
            else if (o instanceof int[])
            {
                text += Arrays.toString((int[]) o);
            }
            else
            {
                text += o;
            }
            mess = new TextComponentTranslation("thutcore.command.settings.set", args[0], text);
            sender.sendMessage(mess);
            return;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new CommandException("unknown error");
        }
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "/" + getName() + "<option name> <optional:newvalue>";
    }

    @Override
    /** Return the required permission level for this command. */
    public int getRequiredPermissionLevel()
    {
        return 4;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos)
    {
        List<String> ret = new ArrayList<String>();
        if (args.length == 1)
        {
            String text = args[0];
            for (String name : fields)
            {
                if (name.contains(text))
                {
                    ret.add(name);
                }
            }
            Collections.sort(ret, new Comparator<String>()
            {
                @Override
                public int compare(String o1, String o2)
                {
                    return o1.compareToIgnoreCase(o2);
                }
            });
        }
        return ret;
    }

    private void populateFields()
    {
        Class<ConfigManager> me = ConfigManager.class;
        Configure c;
        for (Field f : me.getDeclaredFields())
        {
            c = f.getAnnotation(Configure.class);
            if (c != null)
            {
                f.setAccessible(true);
                fields.add(f.getName());
                fieldMap.put(f.getName(), f);
            }
        }
    }

    public static boolean isOp(ICommandSender sender)
    {
        if (sender instanceof EntityPlayerMP)
        {
            IPermissionHandler manager = PermissionAPI.getPermissionHandler();
            return manager.hasPermission(((EntityPlayerMP) sender).getGameProfile(), EDITPERM,
                    new PlayerContext((EntityPlayer) sender));
        }
        return sender.getName().equalsIgnoreCase("@") || sender.getName().equals("Server");
    }

}

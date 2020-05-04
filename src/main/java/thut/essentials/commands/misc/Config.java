package thut.essentials.commands.misc;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.config.Config.ConfigData;

public class Config
{
    protected static int execute(final ConfigData data, final CommandSource source, final String field)
    {
        try
        {
            final Field f = data.getClass().getField(field);
            final Object value = f.get(data);
            Essentials.config.sendFeedback(source, "thutcore.command.settings.check", false, field, value);
        }
        catch (final Exception e)
        {
            throw new CommandException(new StringTextComponent("Error with field name " + field));
        }

        return 0;
    }

    protected static int execute(final ConfigData data, final CommandSource source, final String field,
            final String message)
    {
        Field f = null;
        Object value = null;
        try
        {
            f = data.getClass().getField(field);
            value = f.get(data);
        }
        catch (final Exception e)
        {
            Essentials.LOGGER.error(e);
            throw new CommandException(new StringTextComponent("Error with field name " + field));
        }
        final String[] args = message.split(" ");
        String val = args[0];
        if (val.equals("!set"))
        {
            Config.handleSet(data, args, value, f);
            Essentials.config.sendFeedback(source, "thutcore.command.settings.array.set", true, field, value);
            return 0;
        }

        if (val.equals("!add"))
        {
            Config.handleAdd(data, args, value, f);
            Essentials.config.sendFeedback(source, "thutcore.command.settings.array.add", true, field, value);
            return 0;
        }

        if (val.equals("!remove"))
        {
            Config.handleRemove(data, args, value, f);
            Essentials.config.sendFeedback(source, "thutcore.command.settings.array.remove", true, field, value);
            return 0;
        }

        if (args.length > 1) for (int i = 1; i < args.length; i++)
            val = val + " " + args[i];
        try
        {
            data.updateField(f, val);
            value = f.get(data);
        }
        catch (final Exception e)
        {
            throw new CommandException(new StringTextComponent("Error with setting field name " + field));
        }
        Essentials.config.sendFeedback(source, "thutcore.command.settings.set", true, field, value);

        return 0;
    }

    static void handleAdd(final ConfigData data, final String[] args, final Object o, final Field field)
            throws CommandException
    {
        String value = args[1];
        for (int i = 3; i < args.length; i++)
            value = value + " " + args[i];
        Object toSet = null;
        if (o instanceof String[])
        {
            final int len = ((String[]) o).length;
            toSet = Arrays.copyOf((String[]) o, len + 1);
            ((String[]) toSet)[len] = value;
        }
        else if (o instanceof int[])
        {
            final int len = ((int[]) o).length;
            toSet = Arrays.copyOf((int[]) o, len + 1);
            ((int[]) toSet)[len] = Config.parseInt(value);
        }
        else throw new CommandException(new StringTextComponent("This can only by done for arrays."));
        try
        {
            data.updateField(field, toSet);
        }
        catch (final Exception e)
        {
            throw new CommandException(new StringTextComponent("Error with setting field name " + field));
        }
    }

    static void handleRemove(final ConfigData data, final String[] args, final Object o, final Field field)
            throws CommandException
    {
        String value = args[1];
        for (int i = 3; i < args.length; i++)
            value = value + " " + args[i];
        Object toSet = null;
        if (o instanceof String[])
        {
            final String[] arr = (String[]) o;
            final List<String> values = Lists.newArrayList(arr);
            final int index = values.indexOf(value);
            if (index != -1) values.remove(index);
            toSet = values.toArray(new String[values.size()]);
        }
        else if (o instanceof int[])
        {
            int[] arr = (int[]) o;
            final int arg = Config.parseInt(value);
            final List<Integer> values = Lists.newArrayList();
            for (final int element : arr)
                values.add(element);
            final int index = values.indexOf(arg);
            if (index != -1) values.remove(index);
            toSet = arr = new int[values.size()];
            for (int i = 0; i < values.size(); i++)
                arr[i] = values.get(i);
        }
        else throw new CommandException(new StringTextComponent("This can only by done for arrays."));
        try
        {
            data.updateField(field, toSet);
        }
        catch (final Exception e)
        {
            throw new CommandException(new StringTextComponent("Error with setting field name " + field));
        }
    }

    static void handleSet(final ConfigData data, final String[] args, final Object o, final Field field)
            throws CommandException
    {
        final int num = Config.parseInt(args[1]);
        String value = args[2];
        for (int i = 4; i < args.length; i++)
            value = value + " " + args[i];
        Object toSet = null;
        if (o instanceof String[])
        {
            ((String[]) o)[num] = value;
            toSet = ((String[]) o).clone();
        }
        else if (o instanceof int[])
        {
            ((int[]) o)[num] = Config.parseInt(value);
            toSet = ((int[]) o).clone();
        }
        else throw new CommandException(new StringTextComponent("This can only by done for arrays."));
        try
        {
            data.updateField(field, toSet);
        }
        catch (final Exception e)
        {
            throw new CommandException(new StringTextComponent("Error with setting field name " + field));
        }
    }

    public static SuggestionProvider<CommandSource> MakeProvider(final ConfigData data)
    {
        final List<String> values = Lists.newArrayList();
        for (final Field f : data.commonValues.keySet())
            values.add(f.getName());
        for (final Field f : data.serverValues.keySet())
            values.add(f.getName());
        for (final Field f : data.clientValues.keySet())
            values.add(f.getName());
        return (ctx, sb) -> net.minecraft.command.ISuggestionProvider.suggest(values, sb);
    }

    public static int parseInt(final String input) throws CommandException
    {
        try
        {
            return Integer.parseInt(input);
        }
        catch (final NumberFormatException var2)
        {
            throw new CommandException(Essentials.config.getMessage("commands.generic.num.invalid", new Object[] {
                    input }));
        }
    }

    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String prefix = "teconfig";
        String name = "";
        name = prefix;
        final String perm1 = "command." + name + ".check";
        final ConfigData data = Essentials.config;
        PermissionAPI.registerNode(perm1, DefaultPermissionLevel.OP, "Is the player allowed to check configs for "
                + data.MODID);

        LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm1)).then(Commands.argument("option", StringArgumentType.string()).suggests(Config.MakeProvider(
                        data)).executes(ctx -> Config.execute(data, ctx.getSource(), StringArgumentType.getString(ctx,
                                "option"))));
        commandDispatcher.register(command);

        final String perm2 = "command." + name + ".set";
        PermissionAPI.registerNode(perm2, DefaultPermissionLevel.OP, "Is the player allowed to set configs for "
                + data.MODID);

        command = Commands.literal(name).then(Commands.argument("option", StringArgumentType.string()).suggests(Config
                .MakeProvider(data)).then(Commands.argument("value", StringArgumentType.greedyString()).requires(
                        cs -> CommandManager.hasPerm(cs, perm2)).executes(ctx -> Config.execute(data, ctx.getSource(),
                                StringArgumentType.getString(ctx, "option"), StringArgumentType.getString(ctx,
                                        "value")))));
        commandDispatcher.register(command);
    }
}

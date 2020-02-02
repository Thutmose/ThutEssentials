package thut.essentials.commands;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.ClickEvent.Action;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.land.LandEventsHandler;
import thut.essentials.util.PlayerMover;

public class CommandManager
{

    public static class ClassFinder
    {

        private static final char DOT = '.';

        private static final char SLASH = '/';

        private static final String CLASS_SUFFIX = ".class";

        private static final String BAD_PACKAGE_ERROR = "Unable to get resources from path '%s'. Are you sure the package '%s' exists?";

        public static List<Class<?>> find(final String scannedPackage) throws UnsupportedEncodingException
        {
            final String scannedPath = scannedPackage.replace(ClassFinder.DOT, ClassFinder.SLASH);
            final URL scannedUrl = Thread.currentThread().getContextClassLoader().getResource(scannedPath);
            if (scannedUrl == null) throw new IllegalArgumentException(String.format(ClassFinder.BAD_PACKAGE_ERROR,
                    scannedPath, scannedPackage));
            File scannedDir = new File(java.net.URLDecoder.decode(scannedUrl.getFile(), Charset.defaultCharset()
                    .name()));

            final List<Class<?>> classes = new ArrayList<>();
            if (scannedDir.exists()) for (final File file : scannedDir.listFiles())
                classes.addAll(ClassFinder.findInFolder(file, scannedPackage));
            else if (scannedDir.toString().contains("file:") && scannedDir.toString().contains(".jar"))
            {
                String name = scannedDir.toString();
                final String pack = name.split("!")[1].replace(File.separatorChar, ClassFinder.SLASH).substring(1)
                        + ClassFinder.SLASH;
                name = name.replace("file:", "");
                name = name.replaceAll("(.jar)(.*)", ".jar");
                scannedDir = new File(name);
                try
                {
                    final ZipFile zip = new ZipFile(scannedDir);
                    final Enumeration<? extends ZipEntry> entries = zip.entries();
                    final int n = 0;
                    while (entries.hasMoreElements() && n < 10)
                    {
                        final ZipEntry entry = entries.nextElement();
                        final String s = entry.getName();
                        if (s.contains(pack) && s.endsWith(ClassFinder.CLASS_SUFFIX)) try
                        {
                            classes.add(Class.forName(s.replace(ClassFinder.CLASS_SUFFIX, "").replace(ClassFinder.SLASH,
                                    ClassFinder.DOT)));
                        }
                        catch (final ClassNotFoundException ignore)
                        {
                        }
                    }
                    zip.close();
                }
                catch (final Exception e)
                {
                    e.printStackTrace();
                }
            }
            return classes;
        }

        private static List<Class<?>> findInFolder(final File file, final String scannedPackage)
        {
            final List<Class<?>> classes = new ArrayList<>();
            final String resource = scannedPackage + ClassFinder.DOT + file.getName();
            if (file.isDirectory()) for (final File child : file.listFiles())
                classes.addAll(ClassFinder.findInFolder(child, resource));
            else if (resource.endsWith(ClassFinder.CLASS_SUFFIX))
            {
                final int endIndex = resource.length() - ClassFinder.CLASS_SUFFIX.length();
                final String className = resource.substring(0, endIndex);
                try
                {
                    classes.add(Class.forName(className));
                }
                catch (final ClassNotFoundException ignore)
                {
                }
            }
            return classes;
        }

    }

    public static GameProfile getProfile(final MinecraftServer server, final UUID id)
    {
        GameProfile profile = null;
        // First check profile cache.
        if (id != null) profile = server.getPlayerProfileCache().getProfileByUUID(id);
        if (profile == null) profile = new GameProfile(id, null);

        // Try to fill profile via secure method.
        LandEventsHandler.TEAMMANAGER.queueUpdate(profile);
        return profile;
    }

    public static GameProfile getProfile(final MinecraftServer server, final String arg)
    {
        UUID id = null;
        String name = null;

        // First check if arg is a UUID
        try
        {
            id = UUID.fromString(arg);
        }
        catch (final Exception e)
        {
            // If not a UUID, arg is the name.
            name = arg;
        }

        GameProfile profile = null;

        // First check profile cache.
        if (id != null) profile = server.getPlayerProfileCache().getProfileByUUID(id);
        if (profile == null) profile = new GameProfile(id, name);

        // Try to fill profile via secure method.
        LandEventsHandler.TEAMMANAGER.queueUpdate(profile);

        // Temporarily update the UUID from server player list if possible
        if (profile.getId() == null)
        {
            final PlayerEntity player = server.getPlayerList().getPlayerByUsername(profile.getName());
            profile = player.getGameProfile();
        }

        return profile;
    }

    public static boolean hasPerm(final CommandSource source, final String permission)
    {
        try
        {
            final ServerPlayerEntity player = source.asPlayer();
            return CommandManager.hasPerm(player, permission);
        }
        catch (final CommandSyntaxException e)
        {
            // TODO decide what to actually do here?
            return true;
        }
    }

    public static boolean hasPerm(final ServerPlayerEntity player, final String permission)
    { /*
       * Check if the node is registered, if not, register it as OP, and send
       * error message about this.
       */
        if (!PermissionAPI.getPermissionHandler().getRegisteredNodes().contains(permission))
        {
            final String message = "Autogenerated node, this is a bug and should be pre-registered.";
            PermissionAPI.getPermissionHandler().registerNode(permission, DefaultPermissionLevel.OP, message);
            System.err.println(message + ": " + permission);
        }
        return PermissionAPI.hasPermission(player, permission);
    }

    public static void register_commands(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        // We do this first, as commands might need it.
        MinecraftForge.EVENT_BUS.register(new PlayerMover());
        // Register commands.
        try
        {
            final List<Class<?>> foundClasses = ClassFinder.find(CommandManager.class.getPackage().getName());

            foundClasses.removeIf(c -> c.getName().startsWith("thut.essentials.commands.CommandManage"));

            final List<String> classNames = Lists.newArrayList();
            Method m;
            for (final Class<?> candidateClass : foundClasses)
                try
                {
                    if ((m = candidateClass.getDeclaredMethod("register", CommandDispatcher.class)) != null)
                    {
                        classNames.add(candidateClass.getName());
                        try
                        {
                            m.setAccessible(true);
                            m.invoke(null, commandDispatcher);
                        }
                        catch (final Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
                catch (final Exception e)
                {
                    Essentials.LOGGER.debug("No register found for " + candidateClass);
                }
            Collections.sort(classNames);

        }
        catch (final Exception e)
        {
            e.printStackTrace();
        }
    }

    public static ITextComponent makeFormattedComponent(final String text, final TextFormatting colour,
            final boolean bold, final Object[] args)
    {
        return new TranslationTextComponent(text, args).setStyle(new Style().setBold(bold).setColor(colour));
    }

    public static ITextComponent makeFormattedComponent(final String text, final TextFormatting colour,
            final Object[] args)
    {
        return new TranslationTextComponent(text, args).setStyle(new Style().setColor(colour));
    }

    public static ITextComponent makeFormattedCommandLink(final String text, final String command,
            final TextFormatting colour, final boolean bold, final Object[] args)
    {
        return new TranslationTextComponent(text, args).setStyle(new Style().setBold(bold).setColor(colour)
                .setClickEvent(new ClickEvent(Action.RUN_COMMAND, command)));
    }

    public static ITextComponent makeFormattedComponent(final String text, final TextFormatting colour,
            final boolean bold)
    {
        return new TranslationTextComponent(text).setStyle(new Style().setBold(bold).setColor(colour));
    }

    public static ITextComponent makeFormattedComponent(final String text, final TextFormatting colour)
    {
        return new TranslationTextComponent(text).setStyle(new Style().setColor(colour));
    }

    public static ITextComponent makeFormattedCommandLink(final String text, final String command,
            final TextFormatting colour, final boolean bold)
    {
        return new TranslationTextComponent(text).setStyle(new Style().setBold(bold).setColor(colour).setClickEvent(
                new ClickEvent(Action.RUN_COMMAND, command)));
    }

}

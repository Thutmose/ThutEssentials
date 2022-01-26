package thut.essentials.util;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.namespace.QName;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.Util;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;
import thut.essentials.xml.bind.Factory;
import thut.essentials.xml.bind.annotation.XmlAnyAttribute;
import thut.essentials.xml.bind.annotation.XmlElement;
import thut.essentials.xml.bind.annotation.XmlRootElement;

public class KitManager
{
    @XmlRootElement(name = "Kits")
    public static class Kits
    {
        @XmlElement(name = "Kit")
        public List<XMLStarterItems> kits = Lists.newArrayList();
    }

    public static class KitSet
    {
        public Integer cooldown = null;

        public List<ItemStack> stacks = null;
    }

    @XmlRootElement(name = "Items")
    public static class XMLStarterItems
    {
        @XmlAnyAttribute
        public Map<QName, String> values = Maps.newHashMap();

        @XmlElement(name = "Item")
        public List<Drop> drops = Lists.newArrayList();
    }

    @XmlRootElement(name = "Item")
    public static class Drop
    {
        @XmlAnyAttribute
        public Map<QName, String> values = Maps.newHashMap();
        @XmlElement(name = "tag")
        public String tag;
    }

    public static List<ItemStack> kit = Lists.newArrayList();
    public static Map<String, KitSet> kits = Maps.newHashMap();

    public static void registerPerms()
    {
        init();
    }

    public static void init()
    {
        final File kitsfile = FMLPaths.CONFIGDIR.get().resolve(Essentials.MODID).resolve("kits.xml").toFile();
        final boolean newKits = kitsfile.exists();
        if (!newKits) kitsfile.getParentFile().mkdirs();
        final QName ident = new QName("name");
        final QName cooldown = new QName("cooldown");
        KitManager.kits.clear();
        KitManager.kit.clear();

        // Load Kits
        if (newKits) try
        {
            final FileInputStream stream = new FileInputStream(kitsfile);
            final Kits database = Factory.make(stream, Kits.class);
            try
            {
                stream.close();
            }
            catch (final Exception e1)
            {
                e1.printStackTrace();
            }

            for (final XMLStarterItems items : database.kits) if (items.values.containsKey(ident))
            {
                final KitSet set = new KitSet();
                final String name = items.values.get(ident);
                if (KitManager.kits.containsKey(name)) Essentials.LOGGER.warn("Duplicate kit: " + name);
                final List<ItemStack> list = Lists.newArrayList();
                for (final Drop drop : items.drops)
                {
                    final ItemStack stack = KitManager.getStackFromDrop(drop);
                    if (!stack.isEmpty()) list.add(stack);
                }
                try
                {
                    set.cooldown = Integer.parseInt(items.values.get(cooldown));
                }
                catch (final Exception e)
                {
                    set.cooldown = Essentials.config.kitReuseDelay;
                }
                final String node = "thutessentials.kit." + name;
                PermNodes.registerNode(node, DefaultPermissionLevel.ALL, "Can get the Kit " + name);
                set.stacks = list;
                KitManager.kits.put(name, set);
            }
            else
            {
                final String node = "thutessentials.kit.default";
                PermNodes.registerNode(node, DefaultPermissionLevel.ALL, "Can get the default Kit");
                for (final Drop drop : items.drops)
                {
                    final ItemStack stack = KitManager.getStackFromDrop(drop);
                    if (!stack.isEmpty()) KitManager.kit.add(stack);
                }
            }
        }
        catch (final Exception e)
        {
            e.printStackTrace();
        }
        if (!newKits)
        {
            final Kits kit = new Kits();
            final XMLStarterItems items = new XMLStarterItems();
            final Drop init = new Drop();
            init.values.put(new QName("id"), "minecraft:stick");
            init.values.put(new QName("n"), "5");
            items.drops.add(init);
            kit.kits.add(items);
            // TODO serialize and write out the kit
        }

    }

    public static ItemStack getStackFromDrop(final Drop d)
    {
        final Map<QName, String> values = d.values;
        if (d.tag != null)
        {
            final QName name = new QName("tag");
            values.put(name, d.tag);
        }
        return KitManager.getStack(d.values);
    }

    public static boolean isSameStack(final ItemStack a, final ItemStack b)
    {
        return KitManager.isSameStack(a, b, false);
    }

    public static boolean isSameStack(final ItemStack a, final ItemStack b, final boolean strict)
    {
        // TODO determine if to use the tags?
        return ItemStack.isSameIgnoreDurability(a, b);
    }

    public static ItemStack getStack(final Map<QName, String> values)
    {
        String id = "";
        int size = 1;
        String tag = "";

        for (final QName key : values.keySet()) if (key.toString().equals("id")) id = values.get(key);
        else if (key.toString().equals("n")) size = Integer.parseInt(values.get(key));
        else if (key.toString().equals("tag")) tag = values.get(key).trim();
        if (id.isEmpty()) return ItemStack.EMPTY;
        final ResourceLocation loc = new ResourceLocation(id);
        ItemStack stack = ItemStack.EMPTY;
        Item item = ForgeRegistries.ITEMS.getValue(loc);
        if (item == null)
        {
            final Tag<Item> tags = ItemTags.getAllTags().getTagOrEmpty(loc);
            if (tags != null)
            {
                item = tags.getRandomElement(new Random(2));
                if (item != null) return new ItemStack(item);
            }
        }
        if (item == null) return ItemStack.EMPTY;
        if (stack.isEmpty()) stack = new ItemStack(item, 1);
        stack.setCount(size);
        if (!tag.isEmpty()) try
        {
            stack.setTag(TagParser.parseTag(tag));
        }
        catch (final CommandSyntaxException e)
        {
            Essentials.LOGGER.error("Error parsing items for " + values, e);
        }
        return stack;
    }

    public static void sendKitsList(final ServerPlayer player)
    {
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.kits.header"), Util.NIL_UUID);
        MutableComponent message;
        if (!KitManager.kit.isEmpty() && PermNodes.getBooleanPerm(player, "thutessentials.kit.default"))
        {
            message = CommandManager.makeFormattedComponent("thutessentials.kits.entry", null, false, "Default");
            message.setStyle(message.getStyle().withClickEvent(new ClickEvent(Action.RUN_COMMAND, "/kit Default")));
            player.sendMessage(message, Util.NIL_UUID);
        }
        for (String s : KitManager.kits.keySet())
        {
            if (!PermNodes.getBooleanPerm(player, "thutessentials.kit." + s)) continue;
            if (s.contains(" ")) s = "\"" + s + "\"";
            message = CommandManager.makeFormattedComponent("thutessentials.kits.entry", null, false, s);
            message.setStyle(message.getStyle().withClickEvent(new ClickEvent(Action.RUN_COMMAND, "/kit " + s)));
            player.sendMessage(message, Util.NIL_UUID);
        }
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.kits.footer"), Util.NIL_UUID);
    }
}
package thut.essentials.util;

import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.ClickEvent.Action;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.IPermissionHandler;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.context.PlayerContext;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;

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
        public Integer         cooldown = null;
        public List<ItemStack> stacks   = null;
    }

    @XmlRootElement(name = "Items")
    public static class XMLStarterItems
    {
        @XmlAnyAttribute
        public Map<QName, String> values = Maps.newHashMap();
        @XmlElement(name = "Item")
        private final List<Drop>  drops  = Lists.newArrayList();
    }

    @XmlRootElement(name = "Drop")
    public static class Drop
    {
        @XmlAnyAttribute
        public Map<QName, String> values = Maps.newHashMap();
        @XmlElement(name = "tag")
        public String             tag;
    }

    public static List<ItemStack>     kit  = Lists.newArrayList();
    public static Map<String, KitSet> kits = Maps.newHashMap();

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
            final JAXBContext jaxbContext = JAXBContext.newInstance(Kits.class);
            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            final FileReader reader = new FileReader(kitsfile);
            final Object obj = unmarshaller.unmarshal(reader);

            final Kits database = obj instanceof Kits ? (Kits) obj : new Kits();
            if (obj instanceof XMLStarterItems) database.kits.add((XMLStarterItems) obj);
            reader.close();

            for (final XMLStarterItems items : database.kits)
                if (items.values.containsKey(ident))
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
                    final IPermissionHandler manager = PermissionAPI.getPermissionHandler();
                    final String node = "thutessentials.kit." + name;
                    if (!manager.getRegisteredNodes().contains(node)) manager.registerNode(node,
                            DefaultPermissionLevel.ALL, "Can get the Kit " + name);
                    set.stacks = list;
                    KitManager.kits.put(name, set);
                }
                else
                {
                    final IPermissionHandler manager = PermissionAPI.getPermissionHandler();
                    final String node = "thutessentials.kit.default";
                    if (!manager.getRegisteredNodes().contains(node)) manager.registerNode(node,
                            DefaultPermissionLevel.ALL, "Can get the default Kit");
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
            try
            {
                final JAXBContext jaxbContext = JAXBContext.newInstance(Kits.class);
                final Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
                // output pretty printed
                jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                jaxbMarshaller.marshal(kit, kitsfile);
            }
            catch (final Exception e)
            {
                e.printStackTrace();
            }
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
        return ItemStack.areItemsEqualIgnoreDurability(a, b);
    }

    public static ItemStack getStack(final Map<QName, String> values)
    {
        String id = "";
        int size = 1;
        String tag = "";

        for (final QName key : values.keySet())
            if (key.toString().equals("id")) id = values.get(key);
            else if (key.toString().equals("n")) size = Integer.parseInt(values.get(key));
            else if (key.toString().equals("tag")) tag = values.get(key).trim();
        if (id.isEmpty()) return ItemStack.EMPTY;
        final ResourceLocation loc = new ResourceLocation(id);
        ItemStack stack = ItemStack.EMPTY;
        Item item = ForgeRegistries.ITEMS.getValue(loc);
        if (item == null)
        {
            final Tag<Item> tags = ItemTags.getCollection().get(loc);
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
            stack.setTag(JsonToNBT.getTagFromJson(tag));
        }
        catch (final CommandSyntaxException e)
        {
            Essentials.LOGGER.error("Error parsing items for " + values, e);
        }
        return stack;
    }

    public static void sendKitsList(final ServerPlayerEntity player)
    {
        final IPermissionHandler manager = PermissionAPI.getPermissionHandler();
        final PlayerContext context = new PlayerContext(player);
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.kits.header"));
        Style style = new Style();
        if (!KitManager.kit.isEmpty() && manager.hasPermission(player.getGameProfile(), "thutessentials.kit.default",
                context))
        {
            style = new Style();
            style.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/kit Default"));
            final ITextComponent message = CommandManager.makeFormattedComponent("thutessentials.kits.entry", null,
                    false, "Default");
            player.sendMessage(message.setStyle(style));
        }
        for (String s : KitManager.kits.keySet())
        {
            if (!manager.hasPermission(player.getGameProfile(), "thutessentials.kit." + s, context)) continue;
            style = new Style();
            if (s.contains(" ")) s = "\"" + s + "\"";
            style.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/kit " + s));
            final ITextComponent message = CommandManager.makeFormattedComponent("thutessentials.kits.entry", null,
                    false, s);
            player.sendMessage(message.setStyle(style));
        }
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.kits.footer"));
    }
}
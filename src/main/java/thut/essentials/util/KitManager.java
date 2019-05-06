package thut.essentials.util;

import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTException;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.IPermissionHandler;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.ThutEssentials;

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
        private List<Drop>        drops  = Lists.newArrayList();
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
        File file = new File(ConfigManager.INSTANCE.getConfigFile().getParentFile(), "kits.xml");

        boolean newKits = file.exists();
        QName ident = new QName("name");
        QName cooldown = new QName("10");
        kits.clear();
        kit.clear();

        // Load Kits
        if (newKits)
        {
            try
            {
                JAXBContext jaxbContext = JAXBContext.newInstance(Kits.class);
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                FileReader reader = new FileReader(file);
                Kits database = (Kits) unmarshaller.unmarshal(reader);
                reader.close();

                for (XMLStarterItems items : database.kits)
                {
                    if (items.values.containsKey(ident))
                    {
                        KitSet set = new KitSet();
                        String name = items.values.get(ident);
                        if (kits.containsKey(name)) ThutEssentials.logger.log(Level.WARNING, "Duplicate kit: " + name);
                        List<ItemStack> list = Lists.newArrayList();
                        for (Drop drop : items.drops)
                        {
                            ItemStack stack = getStackFromDrop(drop);
                            if (!stack.isEmpty()) list.add(stack);
                        }
                        try
                        {
                            set.cooldown = Integer.parseInt(items.values.get(cooldown));
                        }
                        catch (Exception e)
                        {
                            set.cooldown = 0;
                        }
                        IPermissionHandler manager = PermissionAPI.getPermissionHandler();
                        String node = "thutessentials.kit." + name;
                        if (!manager.getRegisteredNodes().contains(node))
                        {
                            manager.registerNode(node, DefaultPermissionLevel.ALL, "Can get the Kit " + name);
                        }
                        set.stacks = list;
                        kits.put(name, set);
                    }
                    else
                    {
                        IPermissionHandler manager = PermissionAPI.getPermissionHandler();
                        String node = "thutessentials.kit.default";
                        if (!manager.getRegisteredNodes().contains(node))
                        {
                            manager.registerNode(node, DefaultPermissionLevel.ALL, "Can get the default Kit");
                        }
                        for (Drop drop : items.drops)
                        {
                            ItemStack stack = getStackFromDrop(drop);
                            if (!stack.isEmpty()) kit.add(stack);
                        }
                    }

                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            // Update to new standard.
            file = new File(ConfigManager.INSTANCE.getConfigFile().getParentFile(), "kit.xml");
            if (file.exists())
            {
                newKits = true;
                try
                {
                    JAXBContext jaxbContext = JAXBContext.newInstance(XMLStarterItems.class);
                    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                    FileReader reader = new FileReader(file);
                    XMLStarterItems database = (XMLStarterItems) unmarshaller.unmarshal(reader);
                    reader.close();

                    for (Drop drop : database.drops)
                    {
                        ItemStack stack = getStackFromDrop(drop);
                        if (!stack.isEmpty()) kit.add(stack);
                    }
                    file.delete();
                    file = new File(ConfigManager.INSTANCE.getConfigFile().getParentFile(), "kits.xml");
                    Kits kits = new Kits();
                    kits.kits.add(database);
                    try
                    {
                        jaxbContext = JAXBContext.newInstance(Kits.class);
                        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
                        // output pretty printed
                        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                        jaxbMarshaller.marshal(kits, file);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
        if (!newKits)
        {
            file = new File(ConfigManager.INSTANCE.getConfigFile().getParentFile(), "kits.xml");
            XMLStarterItems items = new XMLStarterItems();
            Drop init = new Drop();
            init.values.put(new QName("id"), "minecraft:stick");
            init.values.put(new QName("n"), "5");
            items.drops.add(init);
            try
            {
                JAXBContext jaxbContext = JAXBContext.newInstance(XMLStarterItems.class);
                Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
                // output pretty printed
                jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                jaxbMarshaller.marshal(items, file);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

    }

    public static ItemStack getStackFromDrop(Drop d)
    {
        Map<QName, String> values = d.values;
        if (d.tag != null)
        {
            QName name = new QName("tag");
            values.put(name, d.tag);
        }
        return getStack(d.values);
    }

    public static boolean isSameStack(ItemStack a, ItemStack b)
    {
        if (!CompatWrapper.isValid(a) || !CompatWrapper.isValid(b)) return false;
        int[] aID = OreDictionary.getOreIDs(a);
        int[] bID = OreDictionary.getOreIDs(b);
        boolean check = a.getItem() == b.getItem();
        if (!check)
        {
            outer:
            for (int i : aID)
            {
                for (int i1 : bID)
                {
                    if (i == i1)
                    {
                        check = true;
                        break outer;
                    }
                }
            }
        }
        if (!check) { return false; }
        check = (!a.isItemStackDamageable() && a.getItemDamage() != b.getItemDamage());
        if (!a.isItemStackDamageable() && (a.getItemDamage() == OreDictionary.WILDCARD_VALUE
                || b.getItemDamage() == OreDictionary.WILDCARD_VALUE))
            check = false;
        if (check) return false;
        NBTBase tag;
        if (a.hasTagCompound() && ((tag = a.getTagCompound().getTag("ForgeCaps")) != null) && tag.hasNoTags())
        {
            a.getTagCompound().removeTag("ForgeCaps");
        }
        if (b.hasTagCompound() && ((tag = b.getTagCompound().getTag("ForgeCaps")) != null) && tag.hasNoTags())
        {
            b.getTagCompound().removeTag("ForgeCaps");
        }
        return ItemStack.areItemStackTagsEqual(a, b);
    }

    public static ItemStack getStack(Map<QName, String> values)
    {
        int meta = -1;
        String id = "";
        int size = 1;
        boolean resource = false;
        String tag = "";

        for (QName key : values.keySet())
        {
            if (key.toString().equals("id"))
            {
                id = values.get(key);
            }
            else if (key.toString().equals("d"))
            {
                meta = Integer.parseInt(values.get(key));
            }
            else if (key.toString().equals("n"))
            {
                size = Integer.parseInt(values.get(key));
            }
            else if (key.toString().equals("tag"))
            {
                tag = values.get(key);
            }
        }
        if (id.isEmpty()) return ItemStack.EMPTY;
        resource = id.contains(":");
        ItemStack stack = ItemStack.EMPTY;
        Item item = null;
        if (resource)
        {
            item = Item.REGISTRY.getObject(new ResourceLocation(id));
        }
        else
        {
            item = Item.REGISTRY.getObject(new ResourceLocation("minecraft:" + id));
        }
        if (item == null) return ItemStack.EMPTY;
        if (meta == -1) meta = 0;
        if (!CompatWrapper.isValid(stack)) stack = new ItemStack(item, 1, meta);
        CompatWrapper.setStackSize(stack, size);
        if (!tag.isEmpty())
        {
            try
            {
                stack.setTagCompound(JsonToNBT.getTagFromJson(tag));
            }
            catch (NBTException e)
            {
                e.printStackTrace();
            }
        }
        return stack;
    }
}

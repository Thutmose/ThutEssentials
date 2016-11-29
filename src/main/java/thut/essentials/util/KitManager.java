package thut.essentials.util;

import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Map;

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
import thut.lib.CompatWrapper;

public class KitManager
{
    @XmlRootElement(name = "Items")
    public static class XMLStarterItems
    {
        @XmlElement(name = "Item")
        private List<Drop> drops = Lists.newArrayList();
    }

    @XmlRootElement(name = "Drop")
    public static class Drop
    {
        @XmlAnyAttribute
        public Map<QName, String> values = Maps.newHashMap();
        @XmlElement(name = "tag")
        public String             tag;
    }

    static List<ItemStack> kit = Lists.newArrayList();

    public static void init()
    {
        File file = new File(ConfigManager.INSTANCE.getConfigFile().getParentFile(), "kit.xml");

        kit.clear();
        if (!file.exists())
        {
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
                if (stack != null) kit.add(stack);
            }
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
        if (id.isEmpty()) return CompatWrapper.nullStack;
        resource = id.contains(":");
        ItemStack stack = CompatWrapper.nullStack;
        Item item = null;
        if (resource)
        {
            item = Item.REGISTRY.getObject(new ResourceLocation(id));
        }
        else
        {
            item = Item.REGISTRY.getObject(new ResourceLocation("minecraft:" + id));
        }
        if (item == null) return CompatWrapper.nullStack;
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

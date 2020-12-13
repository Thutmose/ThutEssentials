package thut.essentials.util;

import java.util.IllegalFormatException;
import java.util.List;
import java.util.regex.Matcher;

import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ForgeI18n;
import thut.essentials.Essentials;

public class ChatManager
{
    public static void init()
    {
        MinecraftForge.EVENT_BUS.addListener(ChatManager::onChat);
    }

    @SubscribeEvent
    public static void onChat(final ServerChatEvent event)
    {
        if (!Essentials.config.useChatFormat) return;
        final String format = Essentials.config.chatFormat;
        final String raw = event.getMessage();
        final StringTextComponent comp = new StringTextComponent("");
        ChatManager.build(comp, format, event.getPlayer().getDisplayName(), raw);
        event.setComponent(comp);
    }

    private static void build(final StringTextComponent comp, final String format, final Object... args)
    {
        final List<ITextComponent> children = comp.getSiblings();
        final Matcher matcher = TranslationTextComponent.STRING_VARIABLE_PATTERN.matcher(format);
        try
        {
            int i = 0;

            int j;
            int l;
            for (j = 0; matcher.find(j); j = l)
            {
                final int k = matcher.start();
                l = matcher.end();
                if (k > j)
                {
                    final IFormattableTextComponent itextcomponent = new StringTextComponent(String.format(format
                            .substring(j, k)));
                    itextcomponent.setStyle(comp.getStyle());
                    children.add(itextcomponent);
                }

                final String s2 = matcher.group(2);
                final String s = format.substring(k, l);
                if ("%".equals(s2) && "%%".equals(s))
                {
                    final IFormattableTextComponent itextcomponent2 = new StringTextComponent("%");
                    itextcomponent2.setStyle(comp.getStyle());
                    children.add(itextcomponent2);
                }
                else
                {
                    if (!"s".equals(s2))
                    {
                        Essentials.LOGGER.error("Illegal chat format!");
                        return;
                    }
                    final String s1 = matcher.group(1);
                    final int i1 = s1 != null ? Integer.parseInt(s1) - 1 : i++;
                    if (i1 < args.length) children.add(ChatManager.getFormatArgumentAsComponent(i1, comp, args));
                }
            }

            if (j == 0) // if we failed to match above, lets try the
                        // messageformat
                // handler instead.
                j = ChatManager.handle(comp, children, args, format);
            if (j < format.length())
            {
                final IFormattableTextComponent itextcomponent1 = new StringTextComponent(String.format(format
                        .substring(j)));
                itextcomponent1.setStyle(comp.getStyle());
                children.add(itextcomponent1);
            }

        }
        catch (final IllegalFormatException illegalformatexception)
        {
            Essentials.LOGGER.error("Illegal chat format!");
        }
    }

    private static ITextComponent getFormatArgumentAsComponent(final int index, final TextComponent comp,
            final Object[] args)
    {
        if (index >= args.length)
        {
            Essentials.LOGGER.error("Illegal chat format!");
            return new StringTextComponent("");
        }
        else
        {
            final Object object = args[index];
            IFormattableTextComponent itextcomponent;
            if (object instanceof IFormattableTextComponent) itextcomponent = (IFormattableTextComponent) object;
            else
            {
                itextcomponent = new StringTextComponent(object == null ? "null" : object.toString());
                itextcomponent.setStyle(comp.getStyle());
            }
            return itextcomponent;
        }
    }

    public static int handle(final TextComponent parent, final List<ITextComponent> children, final Object[] formatArgs,
            final String format)
    {
        try
        {
            final StringTextComponent component = new StringTextComponent(ForgeI18n.parseFormat(format, formatArgs));
            component.setStyle(parent.getStyle());
            children.add(component);
            return format.length();
        }
        catch (final IllegalArgumentException ex)
        {
            return 0;
        }
    }

}

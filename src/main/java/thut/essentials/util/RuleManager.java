package thut.essentials.util;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.minecraft.util.text.TextFormatting;
import thut.essentials.Essentials;

public class RuleManager
{
    final static Map<String, TextFormatting> charCodeMap = Maps.newHashMap();

    static
    {
        try
        {
            for (final TextFormatting format : TextFormatting.values())
                try
                {
                    final char code = format.formattingCode;
                    RuleManager.charCodeMap.put(code + "", format);
                }
                catch (final Exception e)
                {
                    e.printStackTrace();
                }
        }
        catch (final Exception e)
        {
            e.printStackTrace();
        }
    }

    public static String format(String rule)
    {
        boolean done = false;
        int index = 0;
        final String arg = "\\n";
        rule = rule.replace(arg, System.getProperty("line.separator"));
        index = rule.indexOf('&', index);
        while (!done && index < rule.length() && index >= 0)
            try
            {
                done = !rule.contains("&");
                index = rule.indexOf('&', index);
                if (index < rule.length() - 1 && index >= 0)
                {
                    if (index > 0 && rule.substring(index - 1, index).equals("\\"))
                    {
                        index++;
                        continue;
                    }
                    final String toReplace = rule.substring(index, index + 2);
                    final String num = toReplace.replace("&", "");
                    final TextFormatting format = RuleManager.charCodeMap.get(num);
                    if (format != null) rule = rule.replaceAll(toReplace, format + "");
                    else index++;
                }
                else done = true;
            }
            catch (final Exception e)
            {
                done = true;
                e.printStackTrace();
            }
        return rule;
    }

    public static List<String> getRules()
    {
        return Lists.newArrayList(Essentials.config.rules);
    }
}
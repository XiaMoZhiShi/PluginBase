package xiamomc.pluginbase;

import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiamomc.pluginbase.Bindables.BindableList;

import java.util.List;

public class DummyPlugin extends JavaPlugin
{
    @Override
    public void onEnable()
    {
        super.onEnable();

        var logger = this.getSLF4JLogger();

        logger.info("Start testing BindableList...");
        var list1 = new BindableList<String>();
        var list2 = new BindableList<String>();

        list1.onListChanged((l, r) ->
        {
            logger.info("LIST1 >> " + r + " --> " + l);
            logger.info("LIST1 >> NOWHAVE: " + list1);
        });

        list2.onListChanged((l, r) ->
        {
            logger.info("LIST2 :: " + r + " ==> " + l);
            logger.info("LIST2 >> NOWHAVE: " + list2);
        });

        logger.info("Adding string to List1...");
        list1.add("Str1");
        list1.addAll(List.of("Str8", "Str3", "Str4"));

        logger.info("Adding string to List2...");
        list2.add("Str4");
        list2.addAll(List.of("Str5", "Str6", "Str7"));

        logger.info("Removing string from List1...");
        list1.remove("Str2");
        list1.removeAll(List.of("Str1", "Str3"));

        logger.info("Reseetting List1...");
        list1.clear();
        list1.addAll(List.of("Sttr1", "Stt32", "Stdr5", "Sttr5"));

        logger.info("Binding List2 to List 1");
        list2.bindTo(list1);

        logger.info("Removing from 1...");
        list1.remove("Sttr1");

        logger.info("Removing from 2...");
        list2.remove("Sttr32");

        logger.info("List1: " + list1);
        logger.info("List2: " + list2);
    }
}

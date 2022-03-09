package org.vessl.sql.plugin;

import org.vessl.bean.BeanOrder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class PluginManager {

    private static TreeSet<PluginInterceptor> executePlugins = new TreeSet<>(BeanOrder::order);
    private static TreeSet<PluginInterceptor> resultPlugins = new TreeSet<>(BeanOrder::order);

   static void addPlugin(PluginType pluginType, PluginInterceptor plugin) {
        if (pluginType.equals(PluginType.EXECUTE)) {
            executePlugins.add(plugin);
        }else{
            resultPlugins.add(plugin);
        }
    }

    public static List<PluginInterceptor> getPlugins(PluginType pluginType){
        if (pluginType.equals(PluginType.EXECUTE)) {
            return new ArrayList<>(executePlugins);
        }else{
            return new ArrayList<>(resultPlugins);
        }
    }

}

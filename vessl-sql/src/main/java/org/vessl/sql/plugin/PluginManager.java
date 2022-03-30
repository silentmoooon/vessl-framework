package org.vessl.sql.plugin;


import org.vessl.core.bean.BeanOrder;

import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

public class PluginManager {

    private static TreeSet<PluginInterceptor> executePlugins = new TreeSet<>(BeanOrder::order);
    private static TreeSet<PluginInterceptor> parameterPlugins = new TreeSet<>(BeanOrder::order);
    private static TreeSet<PluginInterceptor> resultPlugins = new TreeSet<>(BeanOrder::order);

   static void addPlugin(PluginType pluginType, PluginInterceptor plugin) {
       switch (pluginType) {
           case EXECUTE  -> executePlugins.add(plugin);
           case PARAMETER -> parameterPlugins.add(plugin);
           case RESULT -> resultPlugins.add(plugin);
       }

    }

    public static List<PluginInterceptor> getPlugins(PluginType pluginType){
        switch (pluginType) {
            case EXECUTE -> {
                return new LinkedList<>(executePlugins);
            }
            case PARAMETER -> {
                return new LinkedList<>(parameterPlugins);
            }
            case RESULT -> {
                return new LinkedList<>(resultPlugins);
            }
        }
        return new LinkedList<>();
    }

    public static int size() {
        return executePlugins.size() + parameterPlugins.size() + resultPlugins.size();
    }
}

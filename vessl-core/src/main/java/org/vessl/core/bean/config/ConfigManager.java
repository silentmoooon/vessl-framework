package org.vessl.core.bean.config;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private static Map<String, Object> configMap = new HashMap<>();

    public static void put(String key,Object value) {
        configMap.put(key, value);
    }

    public static void put(Map<String, Object> map) {
        map.forEach((s, o) -> {
            if(o instanceof Map map1){
                put(s,map1);
            }else{
                configMap.put(s, o);
            }
        });


    }
    private static void put(String key,Map<String, Object> map) {
        map.forEach((s, o) -> {
            if(o instanceof Map map1){
                put(map1);
            }else{
                configMap.put(key+"."+s, o);
            }
        });

    }

    public static Object get(String key) {

        return configMap.get(key);
    }
}

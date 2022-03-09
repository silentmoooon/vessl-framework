package org.vessl.sql.handle;

import lombok.AllArgsConstructor;
import org.vessl.sql.plugin.Plugin;
import org.vessl.sql.plugin.PluginInterceptor;

import java.lang.reflect.Method;
import java.util.List;

@AllArgsConstructor
public class MapperMethodInvoker {
    private List<PluginInterceptor> pluginInterceptors;
    private MapperMethodExecutor mapperMethodExecutor;
    private Method method;
    private Object[] args;

    public String getSql() {
        return mapperMethodExecutor.getSql();
    }

    public Object[] getArgs() {
        return args;
    }

    public Method method() {
        return method;
    }

    public Object invoke() throws Throwable {
        if (pluginInterceptors.size() == 0) {
            return mapperMethodExecutor.invoke(args);
        }
        PluginInterceptor plugin = pluginInterceptors.remove(0);
        return plugin.intercept(this);
    }
}

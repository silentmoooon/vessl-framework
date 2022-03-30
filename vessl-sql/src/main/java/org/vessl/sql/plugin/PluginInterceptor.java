package org.vessl.sql.plugin;

import org.vessl.sql.handle.MapperInvoker;

public interface PluginInterceptor {

    Object intercept(MapperInvoker invoker) throws Exception;
}

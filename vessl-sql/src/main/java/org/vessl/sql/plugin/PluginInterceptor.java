package org.vessl.sql.plugin;

import org.vessl.sql.bean.SqlMethodBean;
import org.vessl.sql.handle.MapperMethodInvoker;

import java.lang.reflect.Method;

public interface PluginInterceptor {

    Object intercept(MapperMethodInvoker invoker) throws Throwable;
}

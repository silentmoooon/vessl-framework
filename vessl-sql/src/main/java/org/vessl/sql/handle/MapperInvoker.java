package org.vessl.sql.handle;

import org.vessl.sql.bean.SqlMethodBean;
import org.vessl.sql.constant.SqlType;
import org.vessl.sql.plugin.PluginInterceptor;
import org.vessl.sql.plugin.PluginType;

import java.lang.reflect.Method;
import java.util.List;


public class MapperMethodInvoker {

    private SqlMethodBean methodBean;
    private Method method;
    private Object[] args;

    private PluginType pluginType = PluginType.EXECUTE;
    private List<PluginInterceptor> pluginInterceptors;
    private SqlProcessStep sqlProcessStep;

    public MapperMethodInvoker(SqlMethodBean methodBean, Method method, Object[] args) {
        this.methodBean = methodBean;
        this.method = method;
        this.args = args;
    }

    public void changePlugins(PluginType pluginType, SqlProcessStep sqlProcessStep, List<PluginInterceptor> pluginInterceptors) {
        this.pluginType = pluginType;
        this.sqlProcessStep = sqlProcessStep;
        this.pluginInterceptors = pluginInterceptors;
    }

    public String getSql() {
        return methodBean.getSql();
    }

    public Object[] getArgs() {
        return args;
    }

    public Method method() {
        return method;
    }

    public SqlType getSqlType() {
        return methodBean.getSqlType();
    }

    public Object invoke() throws Throwable {
        if (pluginInterceptors.size() == 0) {
            sqlProcessStep.execute(args);
        }

        PluginInterceptor plugin = pluginInterceptors.remove(0);
        return plugin.intercept(this);
    }
}

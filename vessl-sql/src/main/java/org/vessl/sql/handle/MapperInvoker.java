package org.vessl.sql.handle;

import org.vessl.sql.bean.SqlMethodBean;
import org.vessl.sql.constant.SqlType;
import org.vessl.sql.plugin.PluginInterceptor;

import java.lang.reflect.Method;
import java.util.List;


public class MapperInvoker {

    private SqlMethodBean methodBean;
    private Method method;
    private Object[] args;

    private List<PluginInterceptor> pluginInterceptors;
    private SqlProcessStep sqlProcessStep;

    MapperInvoker(SqlMethodBean methodBean, Method method, Object[] args) {
        this.methodBean = methodBean;
        this.method = method;
        this.args = args;
    }

    void changePlugins(SqlProcessStep sqlProcessStep, List<PluginInterceptor> pluginInterceptors) {
        this.sqlProcessStep = sqlProcessStep;
        this.pluginInterceptors = pluginInterceptors;
    }

    public String getSql() {
        return methodBean.getSql();
    }

    public Object[] getArgs() {
        return args;
    }

    public Method getMethod() {
        return method;
    }

    public Object getTarget() {
        return sqlProcessStep;
    }

    public SqlType getSqlType() {
        return methodBean.getSqlType();
    }

    public Object invoke() throws Exception {
        if (pluginInterceptors.size() == 0) {
            return sqlProcessStep.execute(this, args);
        }

        PluginInterceptor plugin = pluginInterceptors.remove(0);
        return plugin.intercept(this);
    }
}

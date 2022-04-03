package org.vessl.sql.handle;

import org.vessl.sql.bean.SqlMethodBean;
import org.vessl.sql.constant.SqlType;
import org.vessl.sql.plugin.PluginInterceptor;

import java.util.List;


public class MapperInvoker {

    private SqlMethodBean methodBean;
    private Object[] args;

    private List<PluginInterceptor> pluginInterceptors;
    private SqlProcessStep sqlProcessStep;


    MapperInvoker(SqlMethodBean methodBean, List<PluginInterceptor> pluginInterceptors, SqlProcessStep sqlProcessStep, Object[] args) {
        this.methodBean = methodBean;
        this.pluginInterceptors = pluginInterceptors;
        this.sqlProcessStep = sqlProcessStep;
        this.args = args;
    }


    public String getSql() {
        return methodBean.getSql();
    }

    public Object[] getArgs() {
        return args;
    }


    public SqlType getSqlType() {
        return methodBean.getSqlType();
    }

    public Object invoke() throws Exception {
        if (pluginInterceptors.size() == 0) {
            return sqlProcessStep.execute(methodBean, args);
        }

        PluginInterceptor plugin = pluginInterceptors.remove(0);
        return plugin.intercept(this);
    }

    public Object invoke(Object[] args) throws Exception {
        if (pluginInterceptors.size() == 0) {
            return sqlProcessStep.execute(methodBean, args);
        }

        PluginInterceptor plugin = pluginInterceptors.remove(0);
        return plugin.intercept(new MapperInvoker(methodBean, pluginInterceptors, sqlProcessStep, args));
    }
}

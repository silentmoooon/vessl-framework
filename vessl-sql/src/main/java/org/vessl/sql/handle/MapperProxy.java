package org.vessl.sql.handle;

import net.sf.cglib.core.ReflectUtils;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.vessl.sql.bean.MethodDesc;
import org.vessl.sql.plugin.PluginInterceptor;
import org.vessl.sql.plugin.PluginManager;
import org.vessl.sql.plugin.PluginType;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapperProxy implements MethodInterceptor,InvocationHandler {



    private Class<?> clazz;
    private List<MethodDesc> methodDescList;
    private final Map<String, MapperMethodExecutor> methodExecutorMap = new HashMap<>();

    protected MapperProxy(Class<?> clazz, List<MethodDesc> methodDescList){
        this.clazz=clazz;
        this.methodDescList=methodDescList;
        buildMethodExecutor();
    }
    private void buildMethodExecutor(){
        for (MethodDesc methodDesc : methodDescList) {
            String signature = ReflectUtils.getSignature(methodDesc.getMethod()).toString();
            methodExecutorMap.put(signature, new MapperMethodExecutor(methodDesc.getMethod(), methodDesc.getSqlMethodBean()));
        }
    }

    @Override
    public Object intercept(Object object, Method method, Object[] objects, MethodProxy proxy) throws Throwable {

        return execute(object,method, objects);
    }



    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return execute(proxy,method, args);

    }

    private Object execute(Object object,Method method, Object[] objects) throws Throwable {
        if (Object.class.equals(method.getDeclaringClass())) {
            return method.invoke(this, objects);
        }
         String signature = ReflectUtils.getSignature(method).toString();
        if(methodExecutorMap.containsKey(signature)){
            MapperMethodExecutor mapperMethodExecutor = methodExecutorMap.get(signature);
            List<PluginInterceptor> plugins = PluginManager.getPlugins(PluginType.EXECUTE);
            if (plugins.size() == 0) {
                return mapperMethodExecutor.invoke(objects);
            }
            PluginInterceptor plugin = plugins.remove(0);
           return plugin.intercept(new MapperMethodInvoker(plugins,mapperMethodExecutor,method,objects));
            //return methodExecutorMap.get(signature).invoke(objects);
        }
        return null;
    }

}

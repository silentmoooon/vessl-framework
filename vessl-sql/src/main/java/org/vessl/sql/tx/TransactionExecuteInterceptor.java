package org.vessl.sql.tx;

import org.vessl.core.aop.ExecuteInterceptor;
import org.vessl.core.aop.ProxyData;
import org.vessl.sql.handle.MapperManager;
import org.vessl.sql.handle.SqlSession;

import java.lang.annotation.Annotation;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionExecuteInterceptor implements ExecuteInterceptor {

    Map<String, String> datasourceNameMap = new ConcurrentHashMap<>();

    @Override
    public Class<? extends Annotation>[] targetAnnotation() {
        return new Class[]{Transactional.class};
    }

    @Override
    public void beforeHandle(ProxyData proxyData) {

        try {
            if (MapperManager.isSingle()) {
                SqlSession.beginTrans();
            }
            String signature = proxyData.getSignature().toString();
            if (!datasourceNameMap.containsKey(signature)) {
                Transactional annotation = proxyData.getMethod().getAnnotation(Transactional.class);
                String value = annotation.value();
                datasourceNameMap.put(proxyData.getSignature().toString(), value);
            }
            SqlSession.beginTrans(datasourceNameMap.get(signature));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void afterHandle(ProxyData proxyData) {
        try {
            SqlSession.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void afterReturn(ProxyData proxyData, Object result) {

    }

    @Override
    public void afterException(ProxyData proxyData, Throwable e) {
        try {
            SqlSession.rollback();
        } catch (SQLException e1) {
            e.printStackTrace();
        }
    }
}

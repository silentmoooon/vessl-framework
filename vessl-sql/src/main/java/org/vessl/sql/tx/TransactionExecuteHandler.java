package org.vessl.sql.tx;

import net.sf.cglib.core.Signature;
import org.vessl.bean.ClassExecuteHandler;
import org.vessl.sql.handle.SqlSession;

import java.lang.annotation.Annotation;
import java.sql.SQLException;

public class TransactionExecuteHandler implements ClassExecuteHandler {
    @Override
    public Class<? extends Annotation>[] targetAnnotation() {
        return new Class[]{Transactional.class};
    }

    @Override
    public void beforeHandle(Signature signature, Object[] args) {
        try {
            SqlSession.beginTrans();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void afterHandle(Signature signature) {
        try {
            SqlSession.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void afterReturn(Signature signature, Object[] args, Object result) {

    }

    @Override
    public void afterException(Signature signature, Object[] args, Throwable e) {
        try {
            SqlSession.rollback();
        } catch (SQLException e1) {
            e.printStackTrace();
        }
    }
}

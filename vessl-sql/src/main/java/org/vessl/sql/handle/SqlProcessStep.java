package org.vessl.sql.handle;

public interface SqlProcessStep {
    Object execute(MapperInvoker mapperInvoker, Object[] args) throws Exception;
}

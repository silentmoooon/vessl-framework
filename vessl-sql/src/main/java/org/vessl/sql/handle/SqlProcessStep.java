package org.vessl.sql.handle;

import org.vessl.sql.bean.SqlMethodBean;

public interface SqlProcessStep {
    Object execute(SqlMethodBean sqlMethodBean, Object[] args) throws Exception;
}

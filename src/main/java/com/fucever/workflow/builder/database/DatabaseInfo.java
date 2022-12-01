package com.fucever.workflow.builder.database;

import com.fucever.workflow.enums.DatabaseType;

public interface DatabaseInfo {

    DatabaseType getType();
    String getDriverClassName();
    String getIp();
    Integer getPort();
    String getPassword();
    String getUsername();
    String getSchema();
}

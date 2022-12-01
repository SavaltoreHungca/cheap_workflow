package com.fucever.workflow.builder.database;

import com.fucever.workflow.enums.DatabaseType;

public class H2DatabaseInfo implements DatabaseInfo{


    @Override
    public DatabaseType getType() {
        return DatabaseType.H2;
    }

    @Override
    public String getDriverClassName() {
        return null;
    }

    @Override
    public String getIp() {
        return null;
    }

    @Override
    public Integer getPort() {
        return null;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return null;
    }

    @Override
    public String getSchema() {
        return null;
    }
}

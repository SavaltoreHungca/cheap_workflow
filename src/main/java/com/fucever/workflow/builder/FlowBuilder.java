package com.fucever.workflow.builder;

import com.fucever.workflow.builder.database.DatabaseInfo;
import com.fucever.workflow.components.Configurations;
import com.fucever.workflow.db.DAO;
import com.fucever.workflow.db.impl.H2DAO;
import com.fucever.workflow.db.impl.Mysql57DAO;
import com.fucever.workflow.dto.Context;
import com.fucever.workflow.dto.LoginUser;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.fucever.workflow.components.Nullable.of;

public class FlowBuilder {

    private final Configurations configurations;
    private DAO dao;
    private DatabaseInfo databaseInfo;

    public FlowBuilder() {
        this.configurations = new Configurations();
    }

    public FlowBuilder setTablePrefix(String prefix) {
        this.configurations.tablePrefix = of(prefix).finalGetString("");
        return this;
    }

    public FlowBuilder setDatabase(DatabaseInfo database) {
        this.databaseInfo = database;
        this.configurations.databaseInfo = database;
        return this;
    }

    public FlowBuilder setGetLoginUser(LoginUser supplier){
        this.configurations.regisGetLoginUser(supplier);
        return this;
    }

    public FlowSession build() {
        switch (databaseInfo.getType()) {
            case H2: {
                this.dao = new H2DAO(this.configurations);
                break;
            }
            case MYSQL_57: {
                this.dao = new Mysql57DAO(this.configurations);
                break;
            }
        }
        return new FlowSessionImpl(this.dao, this.configurations);
    }
}

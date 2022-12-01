package com.fucever.workflow.db.impl;

import com.fucever.workflow.components.Configurations;
import com.fucever.workflow.db.DAO;
import com.fucever.workflow.enums.Tables;
import com.zaxxer.hikari.HikariDataSource;
import org.h2.Driver;
import org.jooq.SQLDialect;
import org.jooq.impl.SQLDataType;

import java.util.HashSet;
import java.util.Set;

public class H2DAO extends DAO {

    public Configurations configurations;

    public H2DAO(Configurations configurations) {
        this.configurations = configurations;

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(Driver.class.getName());
        dataSource.setJdbcUrl("jdbc:h2:~/h2db;USER=root;PASSWORD=123456;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=TRUE");
        dataSource.setMinimumIdle(2);
        dataSource.setMaximumPoolSize(5);
        dataSource.setConnectionTestQuery("select 1");
        dataSource.setUsername("root");
        dataSource.setPassword("123456");
        init(null, SQLDialect.H2, dataSource);

        initTables();
    }

    public void initTables() {
        Set<String> existsTables = new HashSet<>();
        this.openWithTransaction(dsl -> {
            dsl.fetch("show tables;").forEach(it -> {
                existsTables.add(it.getValue(0, String.class));
            });
            // 流程定义表
            if (!existsTables.contains(configurations.getTableName(Tables.FLOW_DEFINITIONS))) {
                dsl.createTable(configurations.getTableName(Tables.FLOW_DEFINITIONS))
                        .column("FLOW_ID", SQLDataType.BIGINT.nullable(false))
                        .column("ID", SQLDataType.BIGINT)
                        .column("NAME", SQLDataType.VARCHAR(200))
                        .column("NODE_TYPE", SQLDataType.VARCHAR(200))
                        .column("NEXT_NODE_TYPE", SQLDataType.VARCHAR(200))
                        .column("ALLOW", SQLDataType.BIGINT)
                        .column("NEXT_NODE", SQLDataType.BIGINT)
                        .column("REJECT", SQLDataType.BIGINT)
                        .column("DEFINITION_JSON", SQLDataType.BLOB.nullable(false))
                        .execute();
            }
            // 流程状态表
            if (!existsTables.contains(configurations.getTableName(Tables.FLOW))) {
                dsl.createTable(configurations.getTableName(Tables.FLOW))
                        .column("ID", SQLDataType.BIGINT.nullable(false).identity(true))
                        .column("NAME", SQLDataType.VARCHAR(255))
                        .column("TYPE", SQLDataType.VARCHAR(255))
                        .column("CURRENT_NODE_ID", SQLDataType.BIGINT)
                        .column("CREATION_TIME", SQLDataType.TIMESTAMP)
                        .column("IS_CLOSED", SQLDataType.INTEGER.defaultValue(0))
                        .column("IS_PAUSED", SQLDataType.INTEGER.defaultValue(0))
                        .column("IS_FINISHED", SQLDataType.INTEGER.defaultValue(0))
                        .column("IS_STARTED", SQLDataType.INTEGER.defaultValue(0))
                        .execute();
            }
            // 流程数据表 即 flowData
            if (!existsTables.contains(configurations.getTableName(Tables.FLOW_DATA))) {
                dsl.createTable(configurations.getTableName(Tables.FLOW_DATA))
                        .column("FLOW_ID", SQLDataType.BIGINT.nullable(false))
                        .column("KEY", SQLDataType.VARCHAR(255).nullable(false))
                        .column("VALUE", SQLDataType.VARCHAR(255))
                        .execute();
            }
            // 流程审核权限表
            if (!existsTables.contains(configurations.getTableName(Tables.FLOW_AUDITOR))) {
                dsl.createTable(configurations.getTableName(Tables.FLOW_AUDITOR))
                        .column("FLOW_ID", SQLDataType.BIGINT.nullable(false))
                        .column("NODE_ID", SQLDataType.BIGINT.nullable(false))
                        .column("AUDITOR", SQLDataType.VARCHAR(255))
                        .execute();
            }
            // 流程处理历史表
            if (!existsTables.contains(configurations.getTableName(Tables.FLOW_HISTORY))) {
                dsl.createTable(configurations.getTableName(Tables.FLOW_HISTORY))
                        .column("FLOW_ID", SQLDataType.BIGINT.nullable(false))
                        .column("OPERATION", SQLDataType.BLOB.nullable(false))
                        .column("HISTORY_FORM", SQLDataType.BLOB.nullable(false))
                        .column("CREATION_TIME", SQLDataType.TIMESTAMP)
                        .execute();
            }
        });
    }
}

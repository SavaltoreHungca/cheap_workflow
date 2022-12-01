package com.fucever.workflow.db.impl;

import com.fucever.workflow.builder.database.DatabaseInfo;
import com.fucever.workflow.components.Configurations;
import com.fucever.workflow.db.DAO;
import com.fucever.workflow.enums.Tables;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.SQLDialect;
import org.jooq.impl.SQLDataType;

import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import static com.fucever.workflow.enums.Tables.*;

public class Mysql57DAO extends DAO {
    public Configurations configurations;

    public Mysql57DAO(Configurations configurations) {
        this.configurations = configurations;
        DatabaseInfo databaseInfo = configurations.databaseInfo;
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(databaseInfo.getDriverClassName());
        dataSource.setJdbcUrl(String.format("jdbc:mysql://%s:%s/%s?autoReconnect=true" +
                        "&useUnicode=true" +
                        "&characterEncoding=utf-8" +
                        "&useSSL=false" +
                        "&zeroDateTimeBehavior=convertToNull" +
                        "&generateSimpleParameterMetadata=true" +
                        "&serverTimezone=%s" +
                        "", databaseInfo.getIp(),
                databaseInfo.getPort(),
                databaseInfo.getSchema(), TimeZone.getDefault().getID()));
        dataSource.setMinimumIdle(2);
        dataSource.setMaximumPoolSize(5);
        dataSource.setConnectionTestQuery("select 1");
        dataSource.setUsername(databaseInfo.getUsername());
        dataSource.setPassword(databaseInfo.getPassword());
        init(null, SQLDialect.MYSQL_5_7, dataSource);

        initTables();
    }

    public String tb(Tables tables) {
        return configurations.getTableName(tables);
    }

    public void initTables() {
        Set<String> existsTables = new HashSet<>();
        this.openWithTransaction(dsl -> {
            dsl.fetch("show tables;").forEach(it -> {
                existsTables.add(it.getValue(0, String.class).toLowerCase());
            });
            // 流程定义表
            if (!existsTables.contains(configurations.getTableName(Tables.FLOW_DEFINITIONS).toLowerCase())) {
                dsl.execute("create table `" + tb(FLOW_DEFINITIONS) + "` ("
                        + "`auto_id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增id',"
                        + "`ID` bigint NOT NULL COMMENT '节点id，非mysql主键',"
                        + "FLOW_ID bigint not null,"
                        + "NAME varchar(200) default null,"
                        + "NODE_TYPE varchar(200) default null,"
                        + "NEXT_NODE_TYPE varchar(200) default null,"
                        + "ALLOW bigint,"
                        + "NEXT_NODE bigint,"
                        + "REJECT bigint,"
                        + "DEFINITION_JSON blob,"
                        + "primary key (`auto_id`)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
                );
            }
            // 流程状态表
            if (!existsTables.contains(configurations.getTableName(Tables.FLOW).toLowerCase())) {
                dsl.execute("create table `" + tb(FLOW) + "` ("
                        + "`ID` bigint NOT NULL AUTO_INCREMENT COMMENT '自增id',"
                        + "NAME varchar(200) null,"
                        + "TYPE varchar(200) null,"
                        + "CURRENT_NODE_ID bigint,"
                        + "CREATION_TIME datetime,"
                        + "IS_CLOSED int default 0 not null,"
                        + "IS_PAUSED int default 0 not null,"
                        + "IS_FINISHED int default 0 not null,"
                        + "IS_STARTED int default 0 not null,"
                        + "primary key (`ID`)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
                );
            }
            // 流程数据表 即 flowData
            if (!existsTables.contains(configurations.getTableName(Tables.FLOW_DATA).toLowerCase())) {
                dsl.execute("create table `" + tb(FLOW_DATA) + "` ("
                        + "`ID` bigint NOT NULL AUTO_INCREMENT COMMENT '自增id',"
                        + "FLOW_ID bigint not null,"
                        + "`KEY` varchar(200) default '' not null,"
                        + "VALUE blob,"
                        + "primary key (`ID`)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
                );
            }
            // 流程审核权限表
            if (!existsTables.contains(configurations.getTableName(Tables.FLOW_AUDITOR).toLowerCase())) {
                dsl.execute("create table `" + tb(FLOW_AUDITOR) + "` ("
                        + "`ID` bigint NOT NULL AUTO_INCREMENT COMMENT '自增id',"
                        + "FLOW_ID bigint not null,"
                        + "NODE_ID bigint not null,"
                        + "AUDITOR varchar(500) default '' not null,"
                        + "primary key (`ID`)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
                );
            }
            // 流程处理历史表
            if (!existsTables.contains(configurations.getTableName(Tables.FLOW_HISTORY).toLowerCase())) {
                dsl.execute("create table `" + tb(FLOW_HISTORY) + "` ("
                        + "`ID` bigint NOT NULL AUTO_INCREMENT COMMENT '自增id',"
                        + "FLOW_ID bigint not null,"
                        + "OPERATION blob,"
                        + "HISTORY_FORM blob,"
                        + "CREATION_TIME datetime,"
                        + "primary key (`ID`)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
                );
            }
        });
    }
}

package com.fucever.workflow.db;

import com.fucever.workflow.components.Utils;
import org.jooq.*;
import org.jooq.conf.RenderMapping;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;


public abstract class DAO {

    protected DataSource dataSource;
    protected ThreadLocal<ConnectionLevel> connectionLocal = new ThreadLocal<>();
    protected String defaultSchema;
    protected SQLDialect sqlDialect;

    public void init(String defaultSchema, SQLDialect sqlDialect, DataSource dataSource) {
        this.defaultSchema = defaultSchema;
        this.dataSource = dataSource;
        this.sqlDialect = sqlDialect;
    }

    public <T> T createSession(Function<DSLContext, T> supplier) {

        ConnectionLevel level = null;
        try {
            level = connectionLocal.get();
            if (level == null) {
                level = new ConnectionLevel(dataSource.getConnection());
                connectionLocal.set(level);
            }
            level.level += 1;
            return supplier.apply(level.dslContext);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            if (level != null) {
                level.level -= 1;
                if (level.level <= 0) {
                    level.close();
                    connectionLocal.remove();
                }
            }
        }
    }

    public void createSession(Consumer<DSLContext> supplier) {
        createSession(dsl -> {
            supplier.accept(dsl);
            return null;
        });
    }

    public void openWithTransaction(Consumer<DSLContext> supplier) {
        openWithTransaction(dsl -> {
            supplier.accept(dsl);
            return null;
        });
    }

    public Condition fetchCondition(Collection<? extends Condition> conditions) {
        Condition result = DSL.trueCondition();
        for (Condition condition : conditions)
            if (condition != null)
                result = result.and(condition);
        return result;
    }

    public Integer getCount(String sql, Object... params) {
        return openWithTransaction(dsl -> {
            return Utils.getOne(dsl.fetch(String.format("select count(1) from (%s) xijfjisjie", sql), params)
                    .into(Integer.class));
        });
    }

    public <T> T openWithTransaction(Function<DSLContext, T> supplier) {
        ConnectionLevel level = null;
        try {
            level = connectionLocal.get();
            if (level == null) {
                level = new ConnectionLevel(dataSource.getConnection());
                level.setAutoCommit(false);
                connectionLocal.set(level);
            }
            level.level += 1;

            return supplier.apply(level.dslContext);
        } catch (Throwable e) {
            if (null != level) level.rollback();
            throw new RuntimeException(e);
        } finally {
            if (level != null) {
                level.level -= 1;
                if (level.level <= 0) {
                    level.commit();
                    level.close();
                    connectionLocal.remove();
                }
            }
        }
    }

    // 直接进行批量插入
    public <R extends Record> void batchInsert(Table<R> table, Collection<R> collection) {
        batchInsert(table, collection, Query::execute);
    }

    // 适合需要高级操作的批量插入， 如获取生成的 id
    public <R extends Record> void batchInsert(Table<R> table, Collection<R> collection, Consumer<InsertValuesStepN<R>> consumer) {
        if (collection == null || collection.isEmpty()) return;
        openWithTransaction(dsl -> {
            Field<?>[] fields = table.fields();
            InsertValuesStepN<R> insertValuesStepN = dsl.insertInto(table).columns(fields);
            for (R r : collection) {
                List<Object> values = new ArrayList<>();
                for (Field<?> field : fields) {
                    values.add(r.getValue(field));
                }
                insertValuesStepN.values(values.toArray(new Object[]{}));
            }

            consumer.accept(insertValuesStepN);
        });
    }

    public class ConnectionLevel {
        public ConnectionLevel(Connection connection) {
            this.connection = connection;
            if(defaultSchema != null){
                Settings settings = new Settings();
                settings.withRenderMapping(new RenderMapping()
                        .withDefaultSchema(defaultSchema)
                );
                this.dslContext = DSL.using(connection, sqlDialect, settings);
            }else{
                this.dslContext = DSL.using(connection, sqlDialect);
            }
            this.level = 0;
        }

        Connection connection;
        int level;
        DSLContext dslContext;

        public void setAutoCommit(boolean autoCommit) {
            try {
                this.connection.setAutoCommit(autoCommit);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void commit() {
            try {
                this.connection.commit();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void rollback() {
            try {
                this.connection.rollback();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void close() {
            try {
                this.dslContext.close();
            } catch (Exception e) {
                // pass
            }
            try {
                this.connection.close();
            } catch (Exception e) {
                // pass
            }
        }
    }
}

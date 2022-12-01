package com.fucever.workflow.db;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fucever.workflow.components.*;
import com.fucever.workflow.db.domain.*;
import com.fucever.workflow.dto.*;
import com.fucever.workflow.enums.DatabaseType;
import com.fucever.workflow.enums.NodeType;
import com.fucever.workflow.enums.Tables;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;

public class Repository {

    DAO dao;
    Configurations configurations;

    public Repository(DAO dao, Configurations configurations) {
        this.dao = dao;
        this.configurations = configurations;
    }

    public Flow getFlowById(Long id) {
        return dao.createSession(dsl -> {
            return dsl.selectFrom(configurations.getTableName(Tables.FLOW))
                    .where("ID=?", id)
                    .fetchOneInto(Flow.class);
        });
    }

    public void justOpenTransaction(Runnable runnable) {
        dao.openWithTransaction(dsl -> {
            runnable.run();
        });
    }

    private void updateTableValue(String tableName, String filed, String value, String where) {
        dao.openWithTransaction(dsl -> {
            if (value == null) {
                dsl.execute(String.format("update `%s` set `%s`=null where %s",
                        tableName, filed, where));
            } else {
                dsl.execute(String.format("update `%s` set `%s`='%s' where %s",
                        tableName, filed, value, where));
            }
        });
    }

    private String genInsertSql(Map<String, Object> fieldValues, String tableName) {
        StringBuilder sb = new StringBuilder("insert into `").append(tableName).append("` ");
        StringBuilder fields = new StringBuilder();
        StringBuilder values = new StringBuilder();
        fieldValues.forEach((k, v) -> {
            fields.append("`").append(k).append("`,");
            values.append("'").append(Nullable.of(v).finalGetString()).append("',");
        });
        fields.deleteCharAt(fields.length() - 1);
        values.deleteCharAt(values.length() - 1);
        sb.append("(").append(fields).append(") values ")
                .append("(").append(values).append(")");
        return sb.toString();
    }

    private String genBatchInsertSql(Collection<String> fields, List<Map<String, Object>> values, String tableName) {
        StringBuilder sb = new StringBuilder("insert into `").append(tableName).append("` ");
        StringBuilder fieldsSb = new StringBuilder();
        StringBuilder valuesSb = new StringBuilder("(");

        for (String field : fields) {
            fieldsSb.append("`").append(field).append("`,");
        }
        fieldsSb.deleteCharAt(fieldsSb.length() - 1);

        values.forEach(map -> {
            for (String field : fields) {
                Object v = map.get(field);
                if (v == null) {
                    valuesSb.append("null").append(",");
                } else {
                    valuesSb.append("'").append(
                            Utils.decodeSpecialCharsWhenLikeUseBackslash(Nullable.of(v).finalGetString())
                    ).append("',");
                }
            }
            valuesSb.deleteCharAt(valuesSb.length() - 1).append("),(");
        });
        valuesSb.deleteCharAt(valuesSb.length() - 1)
                .deleteCharAt(valuesSb.length() - 1);

        sb.append("(").append(fieldsSb).append(") values ").append(valuesSb);
        return sb.toString();
    }

    private String genJoined(Collection<?> items) {
        StringBuilder sb = new StringBuilder();
        for (Object item : items) {
            sb.append("'")
                    .append(item)
                    .append("',");
        }
        return sb.substring(0, sb.length() - 1);
    }

    public void setFlowFinished(Long id) {
        updateTableValue(configurations.getTableName(Tables.FLOW), "IS_FINISHED", "1", String.format("ID='%s'", id));
    }

    public void setFlowClosed(Long id) {
        updateTableValue(configurations.getTableName(Tables.FLOW), "IS_CLOSED", "1", String.format("ID='%s'", id));
    }

    public void setFlowPaused(Long id) {
        updateTableValue(configurations.getTableName(Tables.FLOW), "IS_PAUSED", "1", String.format("ID='%s'", id));
    }

    public void cancelFlowPaused(Long id) {
        updateTableValue(configurations.getTableName(Tables.FLOW), "IS_PAUSED", "0", String.format("ID='%s'", id));
    }

    public void setFlowStarted(Long id) {
        updateTableValue(configurations.getTableName(Tables.FLOW), "IS_STARTED", "1", String.format("ID='%s'", id));
    }

    public void setFlowCurrentNode(Long id, Long nodeId) {
        updateTableValue(configurations.getTableName(Tables.FLOW), "CURRENT_NODE_ID", String.valueOf(nodeId), String.format("ID='%s'", id));
    }

    public Long insertFlow(String name, String type, String definitionsJson) {
        Long id = IdGenerator.genId();

        List<Map<String, Object>> definitions = new ArrayList<>();

        List<Map<String, Object>> auditor = new ArrayList<>();

        dao.openWithTransaction(dsl -> {
            JSONArray array = JSONArray.parseArray(definitionsJson);

            Long startNodeId = null;
            for (int i = 0; i < array.size(); i++) {
                JSONObject ob = array.getJSONObject(i);
                definitions.add(Utils.asMap(
                        "FLOW_ID", id,
                        "ID", ob.getLong("id"),
                        "NAME", ob.getString("name"),
                        "NODE_TYPE", ob.getString("type"),
                        "NEXT_NODE_TYPE", ob.getString("nextNodeType"),
                        "ALLOW", ob.getLong("allow"),
                        "NEXT_NODE", ob.getLong("nextNode"),
                        "REJECT", ob.getLong("reject"),
                        "DEFINITION_JSON", configurations.databaseInfo.getType().equals(DatabaseType.H2) ?
                                HexConvertor.toHexString(ob.toJSONString()) : ob.toJSONString()
                ));
                switch (NodeType.valueOf(ob.getString("type"))) {
                    case START: {
                        startNodeId = ob.getLong("id");
                        break;
                    }
                    case AUDIT: {
                        JSONArray auditorArray = ob.getJSONArray("auditor");
                        for (int j = 0; j < auditorArray.size(); j++) {
                            auditor.add(Utils.asMap(
                                    "FLOW_ID", id,
                                    "NODE_ID", ob.getLong("id"),
                                    "AUDITOR", auditorArray.getString(j)
                            ));
                        }
                        break;
                    }
                }
            }

            dsl.execute(genInsertSql(Utils.asMap(
                    "ID", id,
                    "NAME", name,
                    "TYPE", type,
                    "CURRENT_NODE_ID", startNodeId,
                    "CREATION_TIME", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
                    "IS_CLOSED", 0,
                    "IS_PAUSED", 0,
                    "IS_FINISHED", 0,
                    "IS_STARTED", 0
            ), configurations.getTableName(Tables.FLOW)));

            dsl.execute(genBatchInsertSql(Utils.asList("FLOW_ID", "ID", "NAME", "NODE_TYPE", "NEXT_NODE_TYPE",
                    "ALLOW", "NEXT_NODE", "REJECT", "DEFINITION_JSON"),
                    definitions, configurations.getTableName(Tables.FLOW_DEFINITIONS)));

            if (!Utils.isEmpty(auditor))
                dsl.execute(genBatchInsertSql(Utils.asList("FLOW_ID", "NODE_ID", "AUDITOR"), auditor, configurations.getTableName(Tables.FLOW_AUDITOR)));

        });
        return id;
    }

    public Node getNode(Long flowId, Long id) {
        return dao.openWithTransaction(dsl -> {
            return dsl.selectFrom(configurations.getTableName(Tables.FLOW_DEFINITIONS))
                    .where("FLOW_ID=?", flowId)
                    .and("ID=?", id)
                    .fetchOneInto(Node.class);
        });
    }

    public Node getNodeByName(Long flowId, String nodeName) {
        return dao.openWithTransaction(dsl -> {
            return dsl.selectFrom(configurations.getTableName(Tables.FLOW_DEFINITIONS))
                    .where("FLOW_ID=?", flowId)
                    .and("NAME=?", nodeName)
                    .fetchOneInto(Node.class);
        });
    }

    public List<FlowAuditor> getFlowAuditors(Long flowId) {
        return dao.openWithTransaction(dsl -> {
            return dsl.fetch(String.format("select fa.*, fd.name as node_name from %s fa left join %s fd on fd.id = fa.node_id " +
                            "where fa.flow_id = ? and fa.node_id is not null",
                    configurations.getTableName(Tables.FLOW_AUDITOR),
                    configurations.getTableName(Tables.FLOW_DEFINITIONS)), flowId)
                    .into(FlowAuditor.class);
        });
    }

    public Node getCurrentNode(Long flowId) {
        return dao.openWithTransaction(dsl -> {
            Flow flow = this.getFlowById(flowId);
            return this.getNode(flowId, flow.currentNodeId);
        });
    }

    public String getDefinitionsJson(Long flowId) {
        return dao.openWithTransaction(dsl -> {
            List<Node> nodes = dsl.selectFrom(configurations.getTableName(Tables.FLOW_DEFINITIONS))
                    .where("FLOW_ID=?", flowId)
                    .fetchInto(Node.class);
            StringBuilder sb = new StringBuilder("[");
            for (Node node : nodes) {
                sb.append(node.definitionJson).append(",");
            }
            return sb.deleteCharAt(sb.length() - 1).append("]").toString();
        });
    }

    public void changeAuditor(Long flowId, Long nodeId, Collection<String> auditor) {
        dao.openWithTransaction(dsl -> {
            Node node = getNode(flowId, nodeId);
            if (!node.nodeType.equals(NodeType.AUDIT)) {
                throw new WorkflowException();
            }

            JSONObject ob = JSONArray.parseObject(node.definitionJson);
            ob.put("auditor", auditor);

            updateTableValue(configurations.getTableName(Tables.FLOW_DEFINITIONS), "DEFINITION_JSON",
                    configurations.databaseInfo.getType().equals(DatabaseType.H2) ?
                            HexConvertor.toHexString(ob.toJSONString())
                            : ob.toJSONString(), String.format("FLOW_ID='%s' and ID='%s'", flowId, nodeId));

            dsl.execute(String.format("delete from `%s` where FLOW_ID='%s' and NODE_ID='%s'",
                    configurations.getTableName(Tables.FLOW_AUDITOR), flowId, nodeId));

            List<Map<String, Object>> auditors = new ArrayList<>();
            for (String s : auditor) {
                auditors.add(Utils.asMap(
                        "FLOW_ID", flowId,
                        "NODE_ID", nodeId,
                        "AUDITOR", s
                ));
            }
            if (!Utils.isEmpty(auditors)) {
                dsl.execute(genBatchInsertSql(Utils.asList("FLOW_ID", "NODE_ID", "AUDITOR"), auditors, configurations.getTableName(Tables.FLOW_AUDITOR)));
            }
        });
    }

    public Set<String> getNodeAuditor(Long flowId, Long nodeId) {
        Set<String> ans = new HashSet<>();
        dao.openWithTransaction(dsl -> {
            List<String> rlt = dsl.fetch(String.format("select `AUDITOR` from %s where FLOW_ID='%s' and NODE_ID='%s'",
                    configurations.getTableName(Tables.FLOW_AUDITOR), flowId, nodeId))
                    .into(String.class);
            ans.addAll(Utils.nullSafe(rlt));
        });
        return ans;
    }

    public Map<String, String> getFlowData(Long flowId) {
        return dao.openWithTransaction(dsl -> {
            List<FlowData> flowDataList = dsl.select().from(configurations.getTableName(Tables.FLOW_DATA))
                    .where("FLOW_ID=?", flowId)
                    .fetchInto(FlowData.class);

            Map<String, String> ans = new HashMap<>();
            Utils.nullSafe(flowDataList).forEach(it -> ans.put(it.key, it.value));
            return ans;
        });
    }

    public Map<Long, Map<String, String>> batchGetFlowData(Collection<Long> flowIds) {
        Map<Long, Map<String, String>> ans = new HashMap<>();
        flowIds.forEach(id -> ans.put(id, new HashMap<>()));
        if (!Utils.isEmpty(flowIds)) {
            dao.openWithTransaction(dsl -> {
                dsl.fetch(String.format("select * from %s where `FLOW_ID` in ( %s )", configurations.getTableName(Tables.FLOW_DATA), genJoined(flowIds)))
                        .forEach(record -> {
                            ans.computeIfAbsent(record.get("FLOW_ID", Long.class), _k -> new HashMap<>())
                                    .put(record.get("KEY", String.class), record.get("VALUE", String.class));
                        });
            });
        }
        return ans;
    }

    public void setFlowData(Long flowId, Map<String, String> data) {
        dao.openWithTransaction(dsl -> {
            dsl.execute(String.format("delete from `%s` where `FLOW_ID`='%s'", configurations.getTableName(Tables.FLOW_DATA), flowId));
            List<Map<String, Object>> values = new ArrayList<>();
            data.forEach((k, v) -> {
                values.add(Utils.asMap(
                        "FLOW_ID", flowId,
                        "KEY", k,
                        "VALUE", v
                ));
            });
            dsl.execute(genBatchInsertSql(Utils.asList("FLOW_ID", "KEY", "VALUE"), values,
                    configurations.getTableName(Tables.FLOW_DATA)));
        });
    }

    public void setFlowVisitor(Long flowId, Collection<String> visitors) {
        dao.openWithTransaction(dsl -> {
            dsl.execute(genBatchInsertSql(Utils.asList(
                    "FLOW_ID", "AUDITOR"
            ), Utils.mapToList(visitors, visitor -> Utils.asMap(
                    "FLOW_ID", flowId,
                    "AUDITOR", visitor
            )), configurations.getTableName(Tables.FLOW_AUDITOR)));
        });
    }

    public Set<String> getFlowVisitor(Long flowId) {
        return dao.openWithTransaction(dsl -> {
            return new HashSet<String>(Utils.nullSafe(dsl.fetch(String.format("select AUDITOR from %s where NODE_ID is null and FLOW_ID = ?",
                    configurations.getTableName(Tables.FLOW_AUDITOR)), flowId)
                    .into(String.class)));
        });
    }

    private void insertHistory(Long flowId, Object operation, Map<String, String> historyForm) {
        dao.openWithTransaction(dsl -> {
            dsl.execute(genInsertSql(Utils.asMap(
                    "FLOW_ID", flowId,
                    "OPERATION", configurations.databaseInfo.getType().equals(DatabaseType.H2) ?
                            HexConvertor.toHexString(JSONArray.toJSONString(operation))
                            : JSONArray.toJSONString(operation),
                    "HISTORY_FORM", configurations.databaseInfo.getType().equals(DatabaseType.H2) ?
                            HexConvertor.toHexString(JSONArray.toJSONString(historyForm))
                            : JSONArray.toJSONString(historyForm),
                    "CREATION_TIME", Nullable.of(new Date()).finalGetStrDate()
            ), configurations.getTableName(Tables.FLOW_HISTORY)));
        });
    }

    public void addHistory(Long flowId, String preNodeName, String currentNodeName, String operation) {
        insertHistory(flowId, Utils.asMap(
                "preNodeName", preNodeName,
                "currentNodeName", currentNodeName,
                "operation", operation,
                "operator", configurations.loginUser.user()
        ), getFlowData(flowId));
    }

    public List<FlowHistory> getHistory(Long flowId) {
        return dao.openWithTransaction(dsl -> {
            return dsl.fetch(String.format("select * from %s where flow_id=?",
                    configurations.getTableName(Tables.FLOW_HISTORY)), flowId)
                    .into(FlowHistory.class);
        });
    }

    /**
     * 查询参与的流程id
     */
    public Set<Long> getFlowsByUserAndDepartment(String flowType) {
        Collection<String> loginUser = configurations.loginUser.getAuthority();
        Set<Long> ans = new HashSet<>();
        dao.openWithTransaction(dsl -> {
            List<Long> ids = new ArrayList<>();
            if (!Utils.isEmptyObject(loginUser)) {
                String sql = null;

                if (flowType == null) {
                    sql = String.format("select `FLOW_ID` from %s where `AUDITOR` in (%s)", configurations.getTableName(Tables.FLOW_AUDITOR),
                            Utils.strjoin(",", Utils.mapToList(loginUser, it -> String.format("'%s'", it))));
                } else {
                    sql = String.format("select a.`FLOW_ID` from %s a " +
                                    "left join %s b on b.ID = a.FLOW_ID " +
                                    "where b.TYPE='%s' and a.`AUDITOR` in (%s)",
                            configurations.getTableName(Tables.FLOW_AUDITOR),
                            configurations.getTableName(Tables.FLOW),
                            flowType,
                            Utils.strjoin(",", Utils.mapToList(loginUser, it -> String.format("'%s'", it))));
                }

                ids = dsl.fetch(sql)
                        .into(Long.class);
                ans.addAll(ids);
            }
        });

        return ans;
    }

    /**
     * 获取我参与且已完成的
     */
    public Set<Long> getIFinished(String flowType) {
        Collection<String> loginUser = configurations.loginUser.getAuthority();

        Set<Long> ans = new HashSet<>();
        dao.openWithTransaction(dsl -> {

            List<Long> ids = new ArrayList<>();
            if (!Utils.isEmptyObject(loginUser)) {
                String sql = null;

                if (flowType == null) {
                    sql = String.format("select c.FLOW_ID from %s c " +
                                    "left join %s a on a.ID = c.FLOW_ID " +
                                    "left join %s b on b.ID = a.CURRENT_NODE_ID and b.FLOW_ID = a.ID " +
                                    "where c.AUDITOR in (%s) and b.NODE_TYPE = 'END'",
                            configurations.getTableName(Tables.FLOW_AUDITOR),
                            configurations.getTableName(Tables.FLOW),
                            configurations.getTableName(Tables.FLOW_DEFINITIONS),
                            Utils.strjoin(",", Utils.mapToList(loginUser, it -> String.format("'%s'", it)))
                    );
                } else {
                    sql = String.format("select c.FLOW_ID from %s c " +
                                    "left join %s a on a.ID = c.FLOW_ID " +
                                    "left join %s b on b.ID = a.CURRENT_NODE_ID and b.FLOW_ID = a.ID " +
                                    "where c.AUDITOR  in (%s) and b.NODE_TYPE = 'END' and a.TYPE='%s'",
                            configurations.getTableName(Tables.FLOW_AUDITOR),
                            configurations.getTableName(Tables.FLOW),
                            configurations.getTableName(Tables.FLOW_DEFINITIONS),
                            Utils.strjoin(",", Utils.mapToList(loginUser, it -> String.format("'%s'", it))),
                            flowType
                    );
                }

                ids = dsl.fetch(sql)
                        .into(Long.class);
                ans.addAll(ids);
            }
        });
        return ans;
    }

    /**
     * 查询当前待处理的流程id; flowType 可以为 空; specialCode 可以为空
     */
    public PageInfo<Long> getFlowsCommissions(String flowType,
                                              String currentNodeName,
                                              FlowDataQuery dataQuery,
                                              PageInfo<Long> pageInfo,
                                              Long specialCode) {
        Collection<String> loginUser = configurations.loginUser.getAuthority();

        Supplier<String> whereBuilder = () -> {
            String ans = String.format(" (a.`AUDITOR` in (%s) ", Utils.strjoin(",", Utils.mapToList(loginUser, it -> String.format("'%s'", it))))
                    + (flowType == null ? "" : "and b.TYPE='" + flowType + "'")
                    + " and b.CURRENT_NODE_ID is not null)";

            if (!Utils.isEmptyObject(specialCode)) {
                ans += String.format(" or (NODE_ID = '%s' and AUDITOR in (%s)) ", specialCode,
                        Utils.strjoin(",", Utils.mapToList(loginUser, it -> String.format("'%s'", it))));
            }
            ans = String.format("(%s)", ans);

            if (!Utils.isEmptyString(currentNodeName)) {
                ans += String.format(" and fd.name='%s' ", currentNodeName);
            }

            if (dataQuery == null || dataQuery.isAllEmpty()) {
                return " where " + ans;
            } else {
                return " and " + ans;
            }
        };

        Supplier<String> limitBuilder = () -> {
            if (pageInfo.shouldPageable()) {
                return String.format(" limit %s, %s", pageInfo.getOffset(), pageInfo.getPageSize());
            }
            return "";
        };

        dao.openWithTransaction(dsl -> {
            if (!Utils.isEmptyObject(loginUser)) {
                String sql = String.format("select distinct a.`FLOW_ID` from %s a " +
                                " left join `%s` b on a.`FLOW_ID`= b.`ID` and b.`CURRENT_NODE_ID` = a.`NODE_ID`" +
                                " left join %s fd on fd.FLOW_ID = b.ID and b.CURRENT_NODE_ID = fd.ID ",
                        configurations.getTableName(Tables.FLOW_AUDITOR),
                        configurations.getTableName(Tables.FLOW),
                        configurations.getTableName(Tables.FLOW_DEFINITIONS)
                ) + (dataQuery == null ? "" : dataQuery.getStatement("b", configurations)) + whereBuilder.get();

                List<Long> ids = dsl.fetch(sql + limitBuilder.get()).into(Long.class);
                pageInfo.setRecords(Utils.nullSafe(ids));
                pageInfo.setTotal(dao.getCount(sql));
            }
        });
        pageInfo.setRecords(Utils.nullSafe(pageInfo.getRecords()));
        return pageInfo;
    }

    /**
     * 根据自定义数据查询
     */
    public Set<Long> getFlowByData(Map<String, String> data, String flowType, Boolean isFinished) {
        Map<Long, Map<String, String>> ans = new HashMap<>();
        dao.openWithTransaction(dsl -> {
            String sql = null;
            if (isFinished) {
                sql = String.format("select a.* from %s a " +
                                "left join %s b on b.ID = a.FLOW_ID " +
                                "left join %s c on c.FLOW_ID = b.ID and b.CURRENT_NODE_ID = c.ID " +
                                "where c.NODE_TYPE = 'END' and b.TYPE = '%s' and a.`KEY` in (%s) and a.`VALUE` in (%s)",
                        configurations.getTableName(Tables.FLOW_DATA),
                        configurations.getTableName(Tables.FLOW),
                        configurations.getTableName(Tables.FLOW_DEFINITIONS),
                        flowType,
                        genJoined(data.keySet()),
                        genJoined(data.values()));
            } else {
                sql = String.format("select a.* from %s a " +
                                "left join %s b on b.ID = a.FLOW_ID " +
                                "where b.TYPE = '%s' and a.`KEY` in (%s) and a.`VALUE` in (%s)",
                        configurations.getTableName(Tables.FLOW_DATA),
                        configurations.getTableName(Tables.FLOW),
                        flowType,
                        genJoined(data.keySet()),
                        genJoined(data.values()));
            }

            dsl.fetch(sql).forEach(record -> {
                ans.computeIfAbsent(record.get("FLOW_ID", Long.class), _k -> new HashMap<>())
                        .put(record.get("KEY", String.class), record.get("VALUE", String.class));
            });
        });
        Set<Long> rlt = new HashSet<>();
        ans.forEach((id, map) -> {
            if (map.size() == data.size()) {
                rlt.add(id);
            }
        });
        return rlt;
    }

    /**
     * flowType 可以为空, pageInfo 可以为空
     */
    public PageInfo<Long> getAllFlow(String flowType,
                                     String currentNodeName,
                                     FlowDataQuery dataQuery,
                                     PageInfo<Long> pageInfo) {
        return dao.openWithTransaction(dsl -> {
            PageInfo<Long> fageInfo = pageInfo == null ? new PageInfo<>() : pageInfo;
            Supplier<String> whereBuilder = () -> {
                List<String> conditions = new ArrayList<>();
                if (!Utils.isEmptyObject(flowType)) {
                    conditions.add(String.format(" f.type='%s' ", flowType));
                }
                if (!Utils.isEmptyObject(currentNodeName)) {
                    conditions.add(String.format(" fd.name='%s' ", currentNodeName));
                }
                if (conditions.isEmpty()) {
                    return " ";
                } else {
                    if (dataQuery == null || dataQuery.isAllEmpty()) {
                        return " where " + Utils.strjoin(" and ", conditions);
                    } else {
                        return " and " + Utils.strjoin(" and ", conditions);
                    }
                }
            };

            Supplier<String> limitBuilder = () -> {
                return String.format(" limit %s, %s ", fageInfo.getOffset(), fageInfo.getPageSize());
            };

            String sql = String.format("select distinct f.id from %s f left join %s fd on fd.FLOW_ID = f.ID and f.CURRENT_NODE_ID = fd.ID " +
                            (dataQuery == null ? "" : dataQuery.getStatement("f", configurations)),
                    configurations.getTableName(Tables.FLOW),
                    configurations.getTableName(Tables.FLOW_DEFINITIONS))
                    + whereBuilder.get() + (pageInfo == null ? "" : limitBuilder.get());
            List<Long> rlt = dsl.fetch(sql).into(Long.class);
            fageInfo.setRecords(new HashSet<>(Utils.nullSafe(rlt)))
                    .setTotal(dao.getCount(sql));
            return fageInfo;
        });
    }

    public Map<Long, Map<String, String>> getFlowByDataKey(Collection<String> keySet, String flowType, Boolean isFinished) {
        Map<Long, Map<String, String>> ans = new HashMap<>();
        dao.openWithTransaction(dsl -> {
            String sql = null;
            if (isFinished) {
                sql = String.format("select a.* from %s a " +
                                "left join %s b on b.ID = a.FLOW_ID " +
                                "left join %s c on c.FLOW_ID = b.ID and b.CURRENT_NODE_ID = c.ID " +
                                "where c.NODE_TYPE = 'END' and b.TYPE = '%s' and a.`KEY` in (%s)",
                        configurations.getTableName(Tables.FLOW_DATA),
                        configurations.getTableName(Tables.FLOW),
                        configurations.getTableName(Tables.FLOW_DEFINITIONS),
                        flowType,
                        genJoined(keySet));
            } else {
                sql = String.format("select * from %s where `KEY` in (%s)",
                        configurations.getTableName(Tables.FLOW_DATA),
                        genJoined(keySet));
            }

            dsl.fetch(sql).forEach(record -> {
                ans.computeIfAbsent(record.get("FLOW_ID", Long.class), _k -> new HashMap<>())
                        .put(record.get("KEY", String.class), record.get("VALUE", String.class));
            });
        });
        Utils.mapRemove(ans, (id, map) -> map.size() != keySet.size());
        return ans;
    }


    /**
     * 查询当前特殊处理的流程id
     */
    public Set<Long> getFlowsBySpecialCode(String flowType, Long specialCode) {
        Collection<String> loginUser = configurations.loginUser.getAuthority();

        Set<Long> ans = new HashSet<>();
        dao.openWithTransaction(dsl -> {
            List<Long> ids = new ArrayList<>();

            if (!Utils.isEmptyObject(loginUser)) {
                String sql = null;

                if (flowType == null) {
                    sql = String.format("select a.`FLOW_ID` from %s a " +
                                    " left join `%s` b on a.`FLOW_ID`= b.`ID` " +
                                    " where a.`AUDITOR` in (%s) and a.NODE_ID='%s' and b.IS_FINISHED = '0'",
                            configurations.getTableName(Tables.FLOW_AUDITOR),
                            configurations.getTableName(Tables.FLOW),
                            Utils.strjoin(",", Utils.mapToList(loginUser, it -> String.format("'%s'", it))),
                            specialCode
                    );
                } else {
                    sql = String.format("select a.`FLOW_ID` from %s a " +
                                    " left join `%s` b on a.`FLOW_ID`= b.`ID` " +
                                    " where a.`AUDITOR` in (%s) and b.TYPE='%s' and a.NODE_ID='%s' and b.IS_FINISHED = '0'",
                            configurations.getTableName(Tables.FLOW_AUDITOR),
                            configurations.getTableName(Tables.FLOW),
                            Utils.strjoin(",", Utils.mapToList(loginUser, it -> String.format("'%s'", it))),
                            flowType,
                            specialCode
                    );
                }

                ids = dsl.fetch(sql)
                        .into(Long.class);
                ans.addAll(ids);
            }


        });

        return ans;
    }

    /**
     * 根据 flowId 获取该流程的特殊权限所对应的审核人
     */
    public Map<Long, Set<String>> getNodeSpecialAuditor(Long flowId) {
        Map<Long, Set<String>> ans = new HashMap<>();
        dao.openWithTransaction(dsl -> {
            dsl.fetch(String.format("select * from %s where FLOW_ID='%s' and NODE_ID<0",
                    configurations.getTableName(Tables.FLOW_AUDITOR), flowId))
                    .forEach(record -> {
                        ans.computeIfAbsent(record.get("NODE_ID", Long.class), _k -> new HashSet<>())
                                .add(record.get("AUDITOR", String.class));
                    });
        });
        return ans;
    }

    /**
     * 设置流程的特殊审核权限
     */
    public void setFlowSpecialAuditor(Long flowId, Long specialCode, Collection<String> visitors) {
        if (specialCode >= 0) {
            throw new RuntimeException("specialCode must lower than 0");
        }
        dao.openWithTransaction(dsl -> {
            dsl.execute(String.format("delete from %s where FLOW_ID = '%s' and NODE_ID = '%s'",
                    configurations.getTableName(Tables.FLOW_AUDITOR),
                    flowId,
                    specialCode
            ));

            dsl.execute(genBatchInsertSql(Utils.asList(
                    "FLOW_ID", "AUDITOR", "NODE_ID"
            ), Utils.mapToList(visitors, visitor -> Utils.asMap(
                    "FLOW_ID", flowId,
                    "AUDITOR", visitor,
                    "NODE_ID", specialCode
            )), configurations.getTableName(Tables.FLOW_AUDITOR)));
        });
    }

    public void directSetCurrentNode(Long flowId, Long nodeId, NodeType nodeType) {
        justOpenTransaction(() -> {
            dao.openWithTransaction(dsl -> {
                dsl.execute(String.format("update %s set current_node_id = '%s' where id='%s'",
                        configurations.getTableName(Tables.FLOW), nodeId, flowId));
                switch (nodeType) {
                    case END: {
                        setFlowFinished(flowId);
                        break;
                    }
                    case START:
                    default: {
                        setFlowStarted(flowId);
                        break;
                    }
                }
            });
        });
    }

    public void deleteFlow(List<Long> flowIds) {
        justOpenTransaction(() -> {
            dao.openWithTransaction(dsl -> {
                dsl.execute(String.format("delete from %s where flow_id in (%s)",
                        configurations.getTableName(Tables.FLOW_DATA), genJoined(flowIds)));

                dsl.execute(String.format("delete from %s where flow_id in (%s)",
                        configurations.getTableName(Tables.FLOW_HISTORY), genJoined(flowIds)));

                dsl.execute(String.format("delete from %s where flow_id in (%s)",
                        configurations.getTableName(Tables.FLOW_AUDITOR), genJoined(flowIds)));

                dsl.execute(String.format("delete from %s where flow_id in (%s)",
                        configurations.getTableName(Tables.FLOW_DEFINITIONS), genJoined(flowIds)));

                dsl.execute(String.format("delete from %s where ID in (%s)",
                        configurations.getTableName(Tables.FLOW), genJoined(flowIds)));
            });
        });
    }
}

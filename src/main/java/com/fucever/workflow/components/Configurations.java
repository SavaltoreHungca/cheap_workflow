package com.fucever.workflow.components;

import com.fucever.workflow.builder.database.DatabaseInfo;
import com.fucever.workflow.dto.Context;
import com.fucever.workflow.dto.LoginUser;
import com.fucever.workflow.enums.ListenerType;
import com.fucever.workflow.enums.Tables;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Configurations {

    public String tablePrefix = "";
    public LoginUser loginUser;
    public DatabaseInfo databaseInfo;

    public Map<String, Map<String, Map<ListenerType, Consumer<Context>>>> listeners = new HashMap<>();
    public Map<String, Map<ListenerType, Consumer<Context>>> flowListeners = new HashMap<>();


    public void addIntoNodeListener(String flowType, String nodeName, Consumer<Context> consumer) {
        listeners.computeIfAbsent(flowType, k -> new HashMap<>())
                .computeIfAbsent(nodeName, k -> new HashMap<>())
                .put(ListenerType.INTO_NODE, consumer);
    }

    public void addNodeOperateListener(String flowType, String nodeName, Consumer<Context> consumer) {
        listeners.computeIfAbsent(flowType, k -> new HashMap<>())
                .computeIfAbsent(nodeName, k -> new HashMap<>())
                .put(ListenerType.OPERATE_NODE, consumer);
    }

    public void addFlowStartedListener(String flowType, Consumer<Context> consumer) {
        flowListeners.computeIfAbsent(flowType, k -> new HashMap<>())
                .put(ListenerType.FLOW_STARTED, consumer);
    }

    public void addFlowFinishedListener(String flowType, Consumer<Context> consumer) {
        flowListeners.computeIfAbsent(flowType, k -> new HashMap<>())
                .put(ListenerType.FLOW_FINISHED, consumer);
    }

    public void regisGetLoginUser(LoginUser supplier) {
        this.loginUser = supplier;
    }

    public String getTableName(Tables tables) {
        if (tablePrefix == null || tablePrefix.trim().equals("")) {
            return tables.name();
        }
        return tablePrefix + tables.name();
    }
}

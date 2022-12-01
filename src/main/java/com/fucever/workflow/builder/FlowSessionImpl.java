package com.fucever.workflow.builder;

import com.fucever.workflow.Workflow;
import com.fucever.workflow.components.Configurations;
import com.fucever.workflow.components.ValueContainer;
import com.fucever.workflow.db.DAO;
import com.fucever.workflow.dto.Context;
import com.fucever.workflow.dto.FlowDataQuery;
import com.fucever.workflow.dto.FlowInfo;
import com.fucever.workflow.dto.PageInfo;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FlowSessionImpl implements FlowSession {

    private final Workflow workflow;


    public FlowSessionImpl(DAO dao, Configurations configurations) {
        this.workflow = new Workflow(dao, configurations);
    }

    public WFlow getFlow(Long flowId) {
        return new WFlow(workflow, flowId);
    }

    public Long createFlow(String flowName, String flowType, String jsonDefinition) {
        return this.workflow.repository.insertFlow(flowName, flowType, jsonDefinition);
    }

    public FlowSession addIntoNodeListener(String flowType, String nodeName, Consumer<Context> consumer) {
        this.workflow.configurations.addIntoNodeListener(flowType, nodeName, consumer);
        return this;
    }

    public FlowSession addNodeOperateListener(String flowType, String nodeName, Consumer<Context> consumer) {
        this.workflow.configurations.addNodeOperateListener(flowType, nodeName, consumer);
        return this;
    }

    public FlowSession addFlowStartedListener(String flowType, Consumer<Context> consumer) {
        this.workflow.configurations.addFlowStartedListener(flowType, consumer);
        return this;
    }

    public FlowSession addFlowFinishedListener(String flowType, Consumer<Context> consumer) {
        this.workflow.configurations.addFlowFinishedListener(flowType, consumer);
        return this;
    }

    /**
     * 查询我参与且已完成的流程, 返回 flowId
     **/
    public Set<Long> queryIFinished(String flowType) {
        return this.workflow.repository.getIFinished(flowType);
    }

    /**
     * 查询我参与的流程，返回 flowId
     **/
    public Set<Long> queryJoinedFlow(String flowType) {
        return this.workflow.repository.getFlowsByUserAndDepartment(flowType);
    }

    /**
     * 查询当前需要我处理的流程, 返回 flowId; flowType 可为空
     **/
    public Set<Long> queryCommissions(String flowType) {
        return new HashSet<>(this.workflow.repository.getFlowsCommissions(flowType,
                null, null, new PageInfo<Long>(), null).getRecords());
    }

    /**
     * 查询当前需要我处理的流程, 返回 flowId; flowType 可为空
     **/
    public PageInfo<Long> queryCommissions(String flowType, PageInfo<Long> pageInfo) {
        return this.workflow.repository.getFlowsCommissions(flowType, null, null, pageInfo, null);
    }

    /**
     * 查询当前需要我处理的流程, 返回 flowId; flowType 可为空
     **/
    public PageInfo<Long> queryCommissions(String flowType, PageInfo<Long> pageInfo, Long specialCode) {
        return this.workflow.repository.getFlowsCommissions(flowType, null, null, pageInfo, specialCode);
    }

    public PageInfo<Long> queryCommissions(String flowType, String currentNodeName, FlowDataQuery dataQuery, PageInfo<Long> pageInfo, Long specialCode) {
        return this.workflow.repository.getFlowsCommissions(flowType, currentNodeName, dataQuery, pageInfo, specialCode);
    }

    /**
     * 查询具有特殊审核权限的流程id, 返回 flowId
     **/
    public Set<Long> queryFlowsBySpecialCode(Long specialCode, String flowType) {
        return this.workflow.repository.getFlowsBySpecialCode(flowType, specialCode);
    }

    /**
     * 根据flowData查询，返回 flowId
     **/
    public Set<Long> queryByFlowData(Map<String, String> data, String flowType, Boolean isFinished) {
        return this.workflow.repository.getFlowByData(data, flowType, isFinished);
    }

    public PageInfo<Long> queryAllFlow(String flowType, PageInfo<Long> pageInfo) {
        return this.workflow.repository.getAllFlow(flowType, null, null, pageInfo);
    }

    public PageInfo<Long> queryAllFlow(String flowType, String currentNodeName, FlowDataQuery dataQuery, PageInfo<Long> pageInfo) {
        return this.workflow.repository.getAllFlow(flowType, currentNodeName, dataQuery, pageInfo);
    }

    @Override
    public <T> T openSession(Supplier<T> supplier) {
        ValueContainer<T> j = new ValueContainer<>();
        this.workflow.repository.justOpenTransaction(() -> {
            j.set(supplier.get());
        });
        return j.get();
    }

    @Override
    public void openSession(Runnable runnable) {
        this.workflow.repository.justOpenTransaction(runnable);
    }


    @Override
    public void setSpecialAuditor(Long flowId, Collection<String> auditors, Long specialCode) {
        getFlow(flowId).setSpecialAuditor(auditors, specialCode);
    }

    /**
     * 根据flowData 的键查询，返回 flowId
     **/
    public Map<Long, Map<String, String>> queryByFlowDataKey(Collection<String> keySet, String flowType, Boolean isFinished) {
        return this.workflow.repository.getFlowByDataKey(keySet, flowType, isFinished);
    }

    /**
     * 根据 flowId 批量获取 flowData
     **/
    public Map<Long, Map<String, String>> batchGetFlowData(Collection<Long> flowIds) {
        return this.workflow.repository.batchGetFlowData(flowIds);
    }

    public void changeAuditor(Long flowId, String nodeName, Collection<String> auditors) {
        getFlow(flowId).changeAuditor(nodeName, auditors);
    }

    public List<FlowInfo> batchGetFlowInfo(Collection<Long> flowIds) {
        List<FlowInfo> ans = new ArrayList<>();
        this.workflow.repository.justOpenTransaction(() -> {
            for (Long flowId : flowIds) {
                ans.add(getFlow(flowId).getInfo());
            }
        });
        return ans;
    }

}

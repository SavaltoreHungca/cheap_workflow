package com.fucever.workflow.builder;

import com.fucever.workflow.dto.Context;
import com.fucever.workflow.dto.FlowDataQuery;
import com.fucever.workflow.dto.FlowInfo;
import com.fucever.workflow.dto.PageInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface FlowSession {
    public WFlow getFlow(Long flowId);

    public Long createFlow(String flowName, String flowType, String jsonDefinition);

    public FlowSession addIntoNodeListener(String flowType, String nodeName, Consumer<Context> consumer);

    public FlowSession addNodeOperateListener(String flowType, String nodeName, Consumer<Context> consumer);

    public FlowSession addFlowStartedListener(String flowType, Consumer<Context> consumer);

    public FlowSession addFlowFinishedListener(String flowType, Consumer<Context> consumer);

    public Set<Long> queryJoinedFlow(String flowType);

    public Set<Long> queryIFinished(String flowType);

    public Set<Long> queryCommissions(String flowType);

    public PageInfo<Long> queryCommissions(String flowType, PageInfo<Long> pageInfo);
    public PageInfo<Long> queryCommissions(String flowType, PageInfo<Long> pageInfo, Long specialCode);
    public PageInfo<Long> queryCommissions(String flowType, String currentNodeName, FlowDataQuery dataQuery, PageInfo<Long> pageInfo, Long specialCode);

    public Set<Long> queryFlowsBySpecialCode(Long specialCode, String flowType);

    public void setSpecialAuditor(Long flowId, Collection<String> auditors, Long specialCode);

    public Map<Long, Map<String, String>> batchGetFlowData(Collection<Long> flowIds);

    public void changeAuditor(Long flowId, String nodeName, Collection<String> auditors);

    public List<FlowInfo> batchGetFlowInfo(Collection<Long> flowIds);

    public PageInfo<Long> queryAllFlow(String flowType, PageInfo<Long> pageInfo);
    public PageInfo<Long> queryAllFlow(String flowType, String currentNodeName, FlowDataQuery dataQuery, PageInfo<Long> pageInfo);

    public <T> T openSession(Supplier<T> supplier);
    public void openSession(Runnable runnable);
}

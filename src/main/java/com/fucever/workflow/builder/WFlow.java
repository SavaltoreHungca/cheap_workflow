package com.fucever.workflow.builder;

import com.fucever.workflow.Workflow;
import com.fucever.workflow.components.Utils;
import com.fucever.workflow.db.domain.Flow;
import com.fucever.workflow.db.domain.FlowAuditor;
import com.fucever.workflow.db.domain.FlowHistory;
import com.fucever.workflow.db.domain.Node;
import com.fucever.workflow.dto.*;
import com.fucever.workflow.dto.tasks.AuditTasks;
import com.fucever.workflow.dto.tasks.EndTasks;
import com.fucever.workflow.dto.tasks.StartTasks;
import com.fucever.workflow.dto.tasks.Tasks;
import com.fucever.workflow.enums.ListenerType;
import com.fucever.workflow.enums.NodeType;

import java.util.*;

import static com.fucever.workflow.components.Nullable.of;

public class WFlow {

    private final Workflow workflow;
    public final Long flowId;

    public WFlow(Workflow workflow, Long flowId) {
        this.workflow = workflow;
        this.flowId = flowId;
    }

    public String getDefinitionsJson() {
        return this.workflow.repository.getDefinitionsJson(flowId);
    }

    public Tasks getCurrentTasks() {
        Node node = this.workflow.repository.getCurrentNode(flowId);
        switch (node.nodeType) {
            case AUDIT: {
                return new AuditTasks(this.workflow, this, node);
            }
            case START: {
                return new StartTasks(this.workflow, this, node);
            }
            case END: {
                return new EndTasks(this.workflow, this, node);
            }
        }
        return null;
    }

    public void start() {
        workflow.repository.justOpenTransaction(() -> {
            Tasks tasks = this.getCurrentTasks();
            if (tasks.nodeType().equals(NodeType.START)) {
                tasks.complete();
            }
        });
    }

    public void pause() {
        workflow.repository.justOpenTransaction(() -> {
            workflow.repository.setFlowPaused(flowId);
            Flow flow = workflow.repository.getFlowById(flowId);
            Node currentNode = workflow.repository.getNode(flowId, flow.currentNodeId);
            of(this.workflow.configurations.flowListeners.get(flow.type))
                    .get(m -> m.get(ListenerType.FLOW_PAUSED))
                    .process(consumer -> consumer.accept(new Context()
                            .setFlowId(flowId)
                            .setFlowName(flow.name)
                            .setFlowType(flow.type)
                            .setNodeId(currentNode.id)
                            .setNodeName(currentNode.name)
                    ));
        });
    }

    public void close() {
        workflow.repository.justOpenTransaction(() -> {
            workflow.repository.setFlowClosed(flowId);
            Flow flow = workflow.repository.getFlowById(flowId);
            Node currentNode = workflow.repository.getNode(flowId, flow.currentNodeId);
            of(this.workflow.configurations.flowListeners.get(flow.type))
                    .get(m -> m.get(ListenerType.FLOW_CLOSED))
                    .process(consumer -> consumer.accept(new Context()
                            .setFlowId(flowId)
                            .setFlowName(flow.name)
                            .setFlowType(flow.type)
                            .setNodeId(currentNode.id)
                            .setNodeName(currentNode.name)
                    ));
        });
    }

    public void setData(Map<String, String> data) {
        this.workflow.setFlowData(flowId, data);
    }

    public void setData(String key, String value) {
        this.workflow.setFlowData(flowId, Utils.asMap(key, value));
    }

    public Map<String, String> getData() {
        return this.workflow.repository.getFlowData(flowId);
    }

    public void setVisitors(Collection<String> visitors) {
        workflow.repository.setFlowVisitor(flowId, visitors);
    }

    public void setSpecialAuditor(Collection<String> auditors, Long specialCode) {
        workflow.repository.setFlowSpecialAuditor(flowId, specialCode, auditors);
    }

    public void changeAuditor(String nodeName, Collection<String> auditors){
        Node n = this.workflow.repository.getNodeByName(this.flowId, nodeName);
        this.workflow.repository.changeAuditor(this.flowId, n.id, auditors);
    }

    public List<HistoryDisplay> getHistory(){
        return Utils.mapToList(this.workflow.repository.getHistory(this.flowId), FlowHistory::getHistoryDisplay);
    }

    public FlowInfo getInfo() {
        FlowInfo ans = new FlowInfo();
        this.workflow.repository.justOpenTransaction(() -> {
            Node node = this.workflow.repository.getCurrentNode(flowId);
            Flow flow = this.workflow.repository.getFlowById(flowId);
            List<FlowAuditor> flowAuditors = this.workflow.repository.getFlowAuditors(flowId);
            Map<String, Set<String>> nodeAuditors = new HashMap<>();
            for (FlowAuditor flowAuditor : flowAuditors) {
                nodeAuditors.computeIfAbsent(String.format("%s[%s]", flowAuditor.getNodeName(),
                        flowAuditor.getNodeId()), _k -> new HashSet<>())
                        .add(flowAuditor.getAuditor());
            }
            ans.setCurrentNode(node.name)
                    .setCurrentNodeId(node.id)
                    .setFlowData(getData())
                    .setSpecialCodes(this.workflow.repository.getNodeSpecialAuditor(flowId))
                    .setFlowId(this.flowId)
                    .setName(flow.name)
                    .setVisitors(this.workflow.repository.getFlowVisitor(flowId))
                    .setType(flow.type)
                    .setNodeAuditors(nodeAuditors)
                    .setStatus(flow.getStatus());
        });
        return ans;
    }

    /** 不会触发监听事件 */
    public void directSetCurrentNode(Long nodeId){
        Node node = this.workflow.repository.getNode(this.flowId, nodeId);
        this.workflow.repository.directSetCurrentNode(this.flowId, nodeId, node.nodeType);
    }

    public void directSetCurrentNode(String nodeName){
        Node node = this.workflow.repository.getNodeByName(this.flowId, nodeName);
        this.workflow.repository.directSetCurrentNode(this.flowId, node.id, node.nodeType);
    }

    public void delete() {
        this.workflow.repository.deleteFlow(Utils.asList(this.flowId));
    }
}

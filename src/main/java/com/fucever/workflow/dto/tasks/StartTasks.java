package com.fucever.workflow.dto.tasks;

import com.fucever.workflow.Workflow;
import com.fucever.workflow.builder.WFlow;
import com.fucever.workflow.db.Repository;
import com.fucever.workflow.db.domain.Flow;
import com.fucever.workflow.db.domain.Node;
import com.fucever.workflow.dto.*;
import com.fucever.workflow.enums.ListenerType;
import com.fucever.workflow.enums.NodeType;

import java.util.Collection;

import static com.fucever.workflow.components.Nullable.of;

public class StartTasks implements Tasks {

    private final Workflow workflow;
    public WFlow wFlow;
    private final Long flowId;
    private final Node currentNode;

    public StartTasks(Workflow workflow, WFlow wFlow, Node currentNode) {
        this.workflow = workflow;
        this.wFlow = wFlow;
        this.flowId = wFlow.flowId;
        this.currentNode = currentNode;
    }

    @Override
    public String nodeName() {
        return this.currentNode.name;
    }

    @Override
    public NodeType nodeType() {
        return this.currentNode.nodeType;
    }
    @Override
    public AllowAuditObject allowAudit() {
        return workflow.getAllowAudit(wFlow.flowId, currentNode.id);
    }

    @Override
    public void complete() {
        Repository repository = workflow.repository;
        repository.justOpenTransaction(() -> {
            Flow flow = workflow.repository.getFlowById(flowId);

            if (flow.isStarted == 0) {
                repository.setFlowCurrentNode(flowId, currentNode.nextNode);
                repository.setFlowStarted(flowId);

                Node nextNode = repository.getNode(flowId, currentNode.nextNode);
                of(this.workflow.configurations.flowListeners.get(flow.type))
                        .get(m -> m.get(ListenerType.FLOW_STARTED))
                        .process(consumer -> consumer.accept(new Context()
                                .setFlowId(flowId)
                                .setFlowName(flow.name)
                                .setFlowType(flow.type)
                                .setNodeId(currentNode.id)
                                .setNodeName(currentNode.name)
                        ));

                if (currentNode.nextNodeType.equals(NodeType.END)) {
                    repository.setFlowFinished(flowId);
                    repository.setFlowCurrentNode(flowId, currentNode.nextNode);
                    repository.addHistory(flowId, currentNode.name, nextNode.name, "结束流程");
                    of(this.workflow.configurations.flowListeners.get(flow.type))
                            .get(m -> m.get(ListenerType.FLOW_FINISHED))
                            .process(consumer -> consumer.accept(new Context()
                                    .setFlowId(flowId)
                                    .setFlowName(flow.name)
                                    .setFlowType(flow.type)
                                    .setNodeId(currentNode.id)
                                    .setNodeName(currentNode.name)
                            ));
                } else {
                    repository.addHistory(flowId, currentNode.name, nextNode.name, "开始流程");
                    of(this.workflow.configurations.listeners.get(flow.type))
                            .get(m -> m.get(nextNode.name))
                            .get(m -> m.get(ListenerType.INTO_NODE))
                            .process(consumer -> consumer.accept(new Context()
                                    .setFlowId(flowId)
                                    .setFlowName(flow.name)
                                    .setFlowType(flow.type)
                                    .setNodeId(nextNode.id)
                                    .setNodeName(nextNode.name)
                                    .setPreviousNodeId(currentNode.id)
                                    .setPreviousNodeName(currentNode.name)
                            ));
                }
            } else if (flow.isPaused == 1) {
                repository.cancelFlowPaused(flowId);
                of(this.workflow.configurations.flowListeners.get(flow.type))
                        .get(m -> m.get(ListenerType.FLOW_CANCEL_PAUSE))
                        .process(consumer -> consumer.accept(new Context()
                                .setFlowId(flowId)
                                .setFlowName(flow.name)
                                .setFlowType(flow.type)
                                .setNodeId(currentNode.id)
                                .setNodeName(currentNode.name)
                        ));
            }
        });
    }

    @Override
    public void reject() {

    }

    @Override
    public void changeAuditor(Collection<String> auditor) {

    }
}

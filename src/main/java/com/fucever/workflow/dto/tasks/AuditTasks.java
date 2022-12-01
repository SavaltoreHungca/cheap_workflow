package com.fucever.workflow.dto.tasks;

import com.fucever.workflow.Workflow;
import com.fucever.workflow.builder.WFlow;
import com.fucever.workflow.components.WorkflowException;
import com.fucever.workflow.db.Repository;
import com.fucever.workflow.db.domain.Flow;
import com.fucever.workflow.db.domain.Node;
import com.fucever.workflow.dto.*;
import com.fucever.workflow.enums.ListenerType;
import com.fucever.workflow.enums.NodeType;

import java.util.Collection;

import static com.fucever.workflow.components.Nullable.of;

public class AuditTasks implements Tasks {

    private final Workflow workflow;
    public WFlow wFlow;
    private final Node currentNode;

    public AuditTasks(Workflow workflow, WFlow wFlow, Node currentNode) {
        this.workflow = workflow;
        this.wFlow = wFlow;
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

    public void complete() {
        workflow.repository.justOpenTransaction(() -> {
            Flow flow = workflow.repository.getFlowById(wFlow.flowId);
            if (workflow.canContinue(flow)) {
                switch (currentNode.nextNodeType) {
                    case AUDIT: {
                        workflow.repository.setFlowCurrentNode(wFlow.flowId, currentNode.nextNode);
                        Node nextNode = this.workflow.repository.getNode(wFlow.flowId, currentNode.nextNode);
                        workflow.repository.addHistory(wFlow.flowId, currentNode.name, nextNode.name, "审批完成");
                        of(this.workflow.configurations.listeners.get(flow.type))
                                .get(m -> m.get(nextNode.name))
                                .get(m -> m.get(ListenerType.INTO_NODE))
                                .process(consumer -> consumer.accept(new Context()
                                        .setFlowId(wFlow.flowId)
                                        .setFlowName(flow.name)
                                        .setFlowType(flow.type)
                                        .setNodeId(nextNode.id)
                                        .setNodeName(nextNode.name)
                                        .setPreviousNodeId(currentNode.id)
                                        .setPreviousNodeName(currentNode.name)
                                ));
                        break;
                    }
                    case EXCLUDE_GATEWAY: {
                        workflow.repository.setFlowCurrentNode(wFlow.flowId, currentNode.allow);
                        Node allowNode = workflow.repository.getNode(wFlow.flowId, currentNode.allow);
                        if (allowNode.nodeType.equals(NodeType.END)) {
                            workflow.repository.setFlowFinished(wFlow.flowId);
                            of(this.workflow.configurations.flowListeners.get(flow.type))
                                    .get(m -> m.get(ListenerType.FLOW_FINISHED))
                                    .process(consumer -> consumer.accept(new Context()
                                            .setFlowId(wFlow.flowId)
                                            .setFlowName(flow.name)
                                            .setFlowType(flow.type)
                                            .setNodeId(allowNode.id)
                                            .setNodeName(allowNode.name)
                                            .setPreviousNodeId(currentNode.id)
                                            .setPreviousNodeName(currentNode.name)
                                    ));
                        }else {
                            of(this.workflow.configurations.listeners.get(flow.type))
                                    .get(m -> m.get(allowNode.name))
                                    .get(m -> m.get(ListenerType.INTO_NODE))
                                    .process(consumer -> consumer.accept(new Context()
                                            .setFlowId(wFlow.flowId)
                                            .setFlowName(flow.name)
                                            .setFlowType(flow.type)
                                            .setNodeId(allowNode.id)
                                            .setNodeName(allowNode.name)
                                            .setPreviousNodeId(currentNode.id)
                                            .setPreviousNodeName(currentNode.name)
                                    ));
                        }
                        workflow.repository.addHistory(wFlow.flowId, currentNode.name, allowNode.name, "同意");
                        break;
                    }
                    case END: {
                        workflow.repository.setFlowFinished(wFlow.flowId);
                        workflow.repository.setFlowCurrentNode(wFlow.flowId, currentNode.nextNode);
                        Node nextNode = this.workflow.repository.getNode(wFlow.flowId, currentNode.nextNode);
                        workflow.repository.addHistory(wFlow.flowId, currentNode.name, nextNode.name, "流程结束");
                        of(this.workflow.configurations.flowListeners.get(flow.type))
                                .get(m -> m.get(ListenerType.FLOW_FINISHED))
                                .process(consumer -> consumer.accept(new Context()
                                        .setFlowId(wFlow.flowId)
                                        .setFlowName(flow.name)
                                        .setFlowType(flow.type)
                                        .setNodeId(nextNode.id)
                                        .setNodeName(nextNode.name)
                                        .setPreviousNodeId(currentNode.id)
                                        .setPreviousNodeName(currentNode.name)
                                ));
                        break;
                    }
                }
            }
        });
    }

    public void reject() {
        Repository repository = workflow.repository;
        Long flowId = this.wFlow.flowId;

        repository.justOpenTransaction(() -> {
            Flow flow = workflow.repository.getFlowById(wFlow.flowId);
            if (workflow.canContinue(flow)) {
                if (!currentNode.nodeType.equals(NodeType.AUDIT) || !currentNode.nextNodeType.equals(NodeType.EXCLUDE_GATEWAY)) {
                    throw new WorkflowException();
                }
                repository.setFlowCurrentNode(flowId, currentNode.reject);
                Node rejectNode = repository.getNode(flowId, currentNode.reject);
                if (rejectNode.nodeType.equals(NodeType.END)) {
                    repository.setFlowFinished(flowId);
                    of(this.workflow.configurations.flowListeners.get(flow.type))
                            .get(m -> m.get(ListenerType.FLOW_FINISHED))
                            .process(consumer -> consumer.accept(new Context()
                                    .setFlowId(wFlow.flowId)
                                    .setFlowName(flow.name)
                                    .setFlowType(flow.type)
                                    .setNodeId(currentNode.id)
                                    .setNodeName(currentNode.name)
                            ));
                    workflow.repository.addHistory(wFlow.flowId, currentNode.name, rejectNode.name, "流程结束");
                } else {
                    of(this.workflow.configurations.listeners.get(flow.type))
                            .get(m -> m.get(rejectNode.name))
                            .get(m -> m.get(ListenerType.INTO_NODE))
                            .process(consumer -> consumer.accept(new Context()
                                    .setFlowId(wFlow.flowId)
                                    .setFlowName(flow.name)
                                    .setFlowType(flow.type)
                                    .setNodeId(rejectNode.id)
                                    .setNodeName(rejectNode.name)
                                    .setPreviousNodeId(currentNode.id)
                                    .setPreviousNodeName(currentNode.name)
                            ));
                    workflow.repository.addHistory(wFlow.flowId, currentNode.name, rejectNode.name, "拒绝");
                }
            }
        });
    }

    @Override
    public void changeAuditor(Collection<String> auditor) {
        Repository repository = workflow.repository;
        repository.justOpenTransaction(() -> {
            Flow flow = workflow.repository.getFlowById(wFlow.flowId);
            if (workflow.canContinue(flow)) {
                this.workflow.repository.changeAuditor(this.wFlow.flowId, currentNode.id, auditor);
            }
        });
    }
}

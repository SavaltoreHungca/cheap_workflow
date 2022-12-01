package com.fucever.workflow.dto.tasks;

import com.fucever.workflow.Workflow;
import com.fucever.workflow.builder.WFlow;
import com.fucever.workflow.dto.AllowAuditObject;
import com.fucever.workflow.db.domain.Node;
import com.fucever.workflow.enums.NodeType;

import java.util.Collection;

public class EndTasks  implements Tasks {

    private final Workflow workflow;
    public WFlow wFlow;
    private final Node currentNode;

    public EndTasks(Workflow workflow, WFlow wFlow, Node currentNode) {
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
    @Override
    public void complete() {

    }

    @Override
    public void reject() {

    }

    @Override
    public void changeAuditor(Collection<String> auditor) {

    }
}

package com.fucever.workflow.dto;

import com.fucever.workflow.enums.ExecutionType;
import lombok.Getter;

@Getter
public class Context {
    Long flowId;
    String flowType;
    String flowName;
    Long nodeId;
    String nodeName;
    Long previousNodeId;
    String previousNodeName;
    ExecutionType executionType;


    public Context setExecutionType(ExecutionType executionType) {
        this.executionType = executionType;
        return this;
    }

    public Context setFlowId(Long flowId) {
        this.flowId = flowId;
        return this;
    }

    public Context setNodeName(String nodeName) {
        this.nodeName = nodeName;
        return this;
    }

    public Context setFlowType(String flowType) {
        this.flowType = flowType;
        return this;
    }

    public Context setFlowName(String flowName) {
        this.flowName = flowName;
        return this;
    }

    public Context setNodeId(Long nodeId) {
        this.nodeId = nodeId;
        return this;
    }

    public Context setPreviousNodeId(Long previousNodeId) {
        this.previousNodeId = previousNodeId;
        return this;
    }

    public Context setPreviousNodeName(String previousNodeName) {
        this.previousNodeName = previousNodeName;
        return this;
    }
}

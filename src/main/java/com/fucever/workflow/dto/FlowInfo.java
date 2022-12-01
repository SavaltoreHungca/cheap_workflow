package com.fucever.workflow.dto;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Getter
public class FlowInfo {
    String type;
    Long flowId;
    String name;
    String currentNode;
    Long currentNodeId;
    Map<Long, Set<String>> specialCodes = new HashMap<>();
    Map<String, String> flowData;
    Set<String> visitors;
    Map<String, Set<String>> nodeAuditors = new HashMap<>(); // 各节点审核人
    String status;

    public FlowInfo setName(String name) {
        this.name = name;
        return this;
    }

    public FlowInfo setType(String type) {
        this.type = type;
        return this;
    }

    public FlowInfo setFlowId(Long flowId) {
        this.flowId = flowId;
        return this;
    }

    public FlowInfo setCurrentNode(String currentNode) {
        this.currentNode = currentNode;
        return this;
    }

    public FlowInfo setCurrentNodeId(Long currentNodeId) {
        this.currentNodeId = currentNodeId;
        return this;
    }

    public FlowInfo setSpecialCodes(Map<Long, Set<String>> specialCodes) {
        this.specialCodes = specialCodes;
        return this;
    }

    public FlowInfo setFlowData(Map<String, String> flowData) {
        this.flowData = flowData;
        return this;
    }

    public FlowInfo setVisitors(Set<String> visitors) {
        this.visitors = visitors;
        return this;
    }

    public FlowInfo setNodeAuditors(Map<String, Set<String>> nodeAuditors) {
        this.nodeAuditors = nodeAuditors;
        return this;
    }

    public FlowInfo setStatus(String status) {
        this.status = status;
        return this;
    }
}

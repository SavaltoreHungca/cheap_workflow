package com.fucever.workflow.db.domain;

import lombok.Data;

@Data
public class FlowAuditor {
    public Long flowId;
    public Long nodeId;
    public String nodeName;
    public String auditor;

}

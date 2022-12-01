package com.fucever.workflow.dto.tasks;

import com.fucever.workflow.dto.AllowAuditObject;
import com.fucever.workflow.enums.NodeType;

import java.util.Collection;
import java.util.List;

public interface Tasks {

    String nodeName();
    NodeType nodeType();
    AllowAuditObject allowAudit();
    void complete();
    void reject();
    void changeAuditor(Collection<String> auditor);
}

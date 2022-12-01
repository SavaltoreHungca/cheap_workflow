package com.fucever.workflow.db.domain;

import com.fucever.workflow.enums.NodeType;
import lombok.Data;

import java.util.List;

@Data
public class Node {
    public Long id;
    public String name;
    public NodeType nodeType;
    public NodeType nextNodeType;
    public Long allow;
    public Long nextNode;
    public Long reject;
    public String definitionJson;
}

package com.fucever.workflow.dto;

import java.util.Date;
import java.util.Map;

public class HistoryDisplay {
    public Long id;
    public Long flowId;
    public String preNodeName;
    public String currentNodeName;
    public String operation;
    public String operator;
    public Map<String, String> historyForm;
    public Date creationTime;
}

package com.fucever.workflow.db.domain;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fucever.workflow.components.Utils;
import com.fucever.workflow.dto.HistoryDisplay;
import lombok.Data;

import java.util.Date;
import java.util.Map;

@Data
public class FlowHistory {
    public Long id;
    public Long flowId;
    public String operation;
    public String historyForm;
    public Date creationTime;


    public HistoryDisplay getHistoryDisplay() {
        HistoryDisplay display = new HistoryDisplay();
        if (!Utils.isEmptyObject(historyForm)) {
            display.historyForm = JSONArray.parseObject(historyForm, Map.class);
        }
        if (!Utils.isEmptyObject(operation)) {
            JSONObject map = JSONArray.parseObject(operation);

            display.preNodeName = map.getString("preNodeName");
            display.currentNodeName = map.getString("currentNodeName");
            display.operation = map.getString("operation");
            display.operator = map.getString("operator");
        }
        display.id = id;
        display.flowId = flowId;
        display.creationTime = creationTime;
        return display;
    }
}

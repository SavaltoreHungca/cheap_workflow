package com.fucever.workflow.dto;

import com.fucever.workflow.components.Configurations;
import com.fucever.workflow.components.Utils;
import com.fucever.workflow.enums.QueryOperation;
import com.fucever.workflow.enums.Tables;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class FlowDataQuery {
    public FlowDataQuery next;

    public String fieldName;
    public String value;
    public QueryOperation operation;

    public boolean isEmpty() {
        if (Utils.isEmptyString(value) || operation == null || Utils.isEmptyString(fieldName)) {
            return true;
        }
        return false;
    }

    public boolean isAllEmpty() {
        FlowDataQuery query = this;
        boolean ans = true;
        while (query != null) {
            if (!query.isEmpty()) {
                ans = false;
            }
            query = query.next;
        }
        return ans;
    }

    public FlowDataQuery setNext(FlowDataQuery next) {
        this.next = next;
        return this;
    }

    public FlowDataQuery setFieldName(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    public FlowDataQuery setValue(String value) {
        this.value = value;
        return this;
    }

    public FlowDataQuery setOperation(QueryOperation operation) {
        this.operation = operation;
        return this;
    }

    public String getOperationStr() {
        switch (operation) {
            case eq: {
                return " = ";
            }
            case gt: {
                return " > ";
            }
            case lt: {
                return " < ";
            }
            case ge: {
                return " >= ";
            }
            case le: {
                return " <= ";
            }
        }
        throw new RuntimeException();
    }

    public String getStatement(String flowTableName, Configurations configurations) {
        if (this.isAllEmpty()) {
            return "";
        }
        String joi = "";
        List<String> whe = new ArrayList<>();
        FlowDataQuery query = this;
        while (query != null) {
            if (query.isEmpty()) {
                query = query.next;
                continue;
            }
            String t = "a" + UUID.randomUUID().toString().replaceAll("-", "");
            joi += String.format(" left join %s on %s.FLOW_ID = " + flowTableName + ".ID and %s.`key` = '%s' ",
                    configurations.getTableName(Tables.FLOW_DATA) + " " + t,
                    t, t, query.fieldName);
            whe.add(String.format(" %s.value %s '%s' ", t, query.getOperationStr(), query.value));
            query = query.next;
        }
        if (joi.equals("")) {
            return "";
        }
        return joi + " where " + Utils.strjoin(" and ", whe);
    }
}

package com.fucever.workflow;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fucever.workflow.components.Configurations;

import com.fucever.workflow.dto.AllowAuditObject;
import com.fucever.workflow.db.domain.Flow;
import com.fucever.workflow.db.DAO;
import com.fucever.workflow.db.Repository;

import java.util.Collection;
import java.util.Map;

public class Workflow {

    public Configurations configurations;
    public Repository repository;

    public Workflow(DAO dao, Configurations configurations) {
        this.configurations = configurations;
        this.repository = new Repository(dao, configurations);
    }

    public boolean canContinue(Flow flow) {
        return flow.isClosed != 1 && flow.isPaused != 1 && flow.isFinished != 1;
    }

    public void setFlowData(Long flowId, Map<String, String> data) {
        repository.justOpenTransaction(() -> {
            Map<String, String> exists = repository.getFlowData(flowId);
            data.forEach(exists::put);
            repository.setFlowData(flowId, exists);
        });
    }

    public AllowAuditObject getAllowAudit(Long flowId, Long nodeId){
        AllowAuditObject allowAuditObject = new AllowAuditObject();
        repository.justOpenTransaction(()->{
            allowAuditObject.auditors = repository.getNodeAuditor(flowId, nodeId);
            allowAuditObject.specialCodesForAuditor = repository.getNodeSpecialAuditor(flowId);
        });
        return allowAuditObject;
    }

    public static String changeDefinitionAuditor(String definition, String nodeName, Collection<String> auditors) {
        JSONArray array = JSONArray.parseArray(definition);
        for (int i = 0; i < array.size(); i++) {
            JSONObject ob = array.getJSONObject(i);
            if (nodeName.equals(ob.getString("name"))) {
                ob.put("auditor", auditors);
                break;
            }
        }
        return array.toJSONString();
    }
}

package com.fucever.workflow.dto;

import java.util.*;

public class AllowAuditObject {

    public Set<String> auditors = new HashSet<>();
    /**
     * 返回值 Map<权限code, 可审核人的id>
     */
    public Map<Long, Set<String>> specialCodesForAuditor = new HashMap<>();

}

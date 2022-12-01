package com.fucever.workflow.test;

import com.alibaba.fastjson.JSONArray;
import com.fucever.workflow.Workflow;
import com.fucever.workflow.builder.FlowBuilder;
import com.fucever.workflow.builder.FlowSession;
import com.fucever.workflow.builder.database.H2DatabaseInfo;
import com.fucever.workflow.components.Configurations;
import com.fucever.workflow.db.impl.H2DAO;
import com.fucever.workflow.dto.FlowInfo;
import com.fucever.workflow.dto.LoginUser;
import com.fucever.workflow.dto.PageInfo;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class Main {

    static String FLOW_DEFINITION = "[\n" +
            "    {\n" +
            "        \"id\": 2156800624188949000,\n" +
            "        \"name\": \"start\",\n" +
            "        \"type\": \"START\",\n" +
            "        \"nextNodeType\": \"AUDIT\",\n" +
            "        \"nextNode\": 14737642928429818000,\n" +
            "        \"offsetLeft\": \"74\",\n" +
            "        \"offsetTop\": \"144\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"id\": 14737642928429818000,\n" +
            "        \"name\": \"资金申请\",\n" +
            "        \"type\": \"AUDIT\",\n" +
            "        \"nextNodeType\": \"AUDIT\",\n" +
            "        \"offsetLeft\": \"326\",\n" +
            "        \"offsetTop\": \"144\",\n" +
            "        \"auditor\": [],\n" +
            "        \"nextNode\": 69960664860100810000\n" +
            "    },\n" +
            "    {\n" +
            "        \"id\": 69960664860100810000,\n" +
            "        \"name\": \"主管审核\",\n" +
            "        \"type\": \"AUDIT\",\n" +
            "        \"nextNodeType\": \"EXCLUDE_GATEWAY\",\n" +
            "        \"offsetLeft\": \"330\",\n" +
            "        \"offsetTop\": \"380\",\n" +
            "        \"auditor\": [],\n" +
            "        \"allow\": 4127634792803358000,\n" +
            "        \"reject\": 14737642928429818000,\n" +
            "        \"excludeGateway\": {\n" +
            "            \"offsetLeft\": \"640\",\n" +
            "            \"offsetTop\": \"140\",\n" +
            "            \"id\": 44484742982885440,\n" +
            "            \"name\": \"\",\n" +
            "            \"updot\": \"reject\",\n" +
            "            \"downdot\": \"allow\"\n" +
            "        }\n" +
            "    },\n" +
            "    {\n" +
            "        \"id\": 3276004840815968000,\n" +
            "        \"name\": \"end\",\n" +
            "        \"type\": \"END\",\n" +
            "        \"offsetLeft\": \"908\",\n" +
            "        \"offsetTop\": \"233\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"id\": 4127634792803358000,\n" +
            "        \"name\": \"财务审核\",\n" +
            "        \"type\": \"AUDIT\",\n" +
            "        \"nextNodeType\": \"END\",\n" +
            "        \"offsetLeft\": \"651\",\n" +
            "        \"offsetTop\": \"353\",\n" +
            "        \"auditor\": [],\n" +
            "        \"nextNode\": 3276004840815968000\n" +
            "    }\n" +
            "]";

    public static class FakeThreadSession {
        public static Collection<String> authority;
        public static String user;

        public static Collection<String> getAuthority() {
            return authority;
        }

        public static String getUser() {
            return user;
        }
    }

    public static void main(String[] args) {

        // 创建流程引擎
        FlowSession flowSession = new FlowBuilder()
                .setDatabase(new H2DatabaseInfo()) // 指定数据类型
                .setGetLoginUser(new LoginUser() {
                    @Override
                    public Collection<String> getAuthority() {
                        return FakeThreadSession.getAuthority();
                    }

                    @Override
                    public String user() {
                        return FakeThreadSession.getUser();
                    }
                })
                .build();

        // 创建流程
        Long flowId = flowSession.createFlow("小明的项目资金申请", "项目资金申请", FLOW_DEFINITION);


        // 设置结点的审批人，这个可以在前端绘制流程引擎时指定，也可在这里重新指定
        flowSession.changeAuditor(flowId, "资金申请", Arrays.asList("小明"));
        flowSession.changeAuditor(flowId, "主管审核", Arrays.asList("主管"));
        flowSession.changeAuditor(flowId, "财务审核", Arrays.asList("财务"));

        // 启动
        flowSession.getFlow(flowId).start();

        // 查询流程进度
        printCurrentNode(flowSession, flowId);

        // 特殊权限指：用在任务列表查询处。
        // 用来实现控制获得处在非自己所管理结点的流程，以实现全局查看，全局修改，任意结点审批等。
        // -1 只是个编码，可以有任意多个特殊权限，看需求，specialCode 规定只能是负数
        // 这里将 -1 绑定到 "主管审核" 上，用有 主管审核 权限的人 同时拥有 -1 code
        flowSession.setSpecialAuditor(flowId, Arrays.asList("主管审核"), -1L);

        flowSession.openSession(() -> { // 确保局部数据安全
            FakeThreadSession.user = "小明";
            FakeThreadSession.authority = Arrays.asList("资金申请");

            Map<String, String> form = new HashMap<>(2);
            form.put("reason", "用于项目"); // 理由
            form.put("amount", "1000"); // 金额多少
            flowSession.getFlow(flowId)
                    .setData(form); // 小明提交表单

            flowSession.getFlow(flowId)
                    .getCurrentTasks()
                    .complete(); // 小明提交申请
        });

        // 查询流程进度
        printCurrentNode(flowSession, flowId);


        flowSession.openSession(() -> { // 确保局部数据安全
            FakeThreadSession.user = "主管";
            FakeThreadSession.authority = Arrays.asList("主管审核");

            Map<String, String> form = new HashMap<>(1);
            form.put("master_suggest", "理由不明确"); // 主管填写表单
            flowSession.getFlow(flowId)
                    .setData(form);

            flowSession.getFlow(flowId)
                    .getCurrentTasks()
                    .reject(); // 拒绝
        });

        // 查询流程进度
        printCurrentNode(flowSession, flowId);


        flowSession.openSession(() -> {
            FakeThreadSession.user = "主管";
            FakeThreadSession.authority = Arrays.asList("主管审核");

            // 当前任务结点应是小明重新填写表单
            // 现在登录人是主管，按理说主管不能获取当前结点的流程
            // 假设现在他想帮小明填写表单，可通过 specialCode 来获取进程
            PageInfo<Long> p = flowSession.queryCommissions("项目资金申请", new PageInfo<>(0, 10), -1L);

            Map<String, String> form = new HashMap<>(1);
            form.put("reason", "我是主管，我代小明补充细节"); // 通过specialCode主管代小明填写表单
            flowSession.getFlow(p.getRecordsList().get(0)) // 这里演示直接取0了，如果有多个流程，主要区分
                    .setData(form);

            flowSession.getFlow(p.getRecordsList().get(0))
                    .getCurrentTasks()
                    .complete(); // 主管替小明提交

            // 查询流程进度
            printCurrentNode(flowSession, flowId);

            flowSession.getFlow(p.getRecordsList().get(0))
                    .getCurrentTasks()
                    .complete(); // 主管给自己审批同意
        });

        // 查询流程进度
        printCurrentNode(flowSession, flowId);

        flowSession.openSession(() -> { // 确保局部数据安全
            FakeThreadSession.user = "财务";
            FakeThreadSession.authority = Arrays.asList("财务审核");

            Map<String, String> form = new HashMap<>(1);
            form.put("financial_department", "无误，批了"); // 填写表单
            flowSession.getFlow(flowId)
                    .setData(form);

            flowSession.getFlow(flowId)
                    .getCurrentTasks()
                    .complete(); // 同意
        });

        // 查询流程进度
        printCurrentNode(flowSession, flowId);

        // 打印审批历史
        System.out.println("审批历史：" + JSONArray.toJSONString(flowSession.getFlow(flowId).getHistory(), true));
    }

    public static void printCurrentNode(FlowSession flowSession, Long flowId){
        FlowInfo info = flowSession.getFlow(flowId).getInfo();
        System.out.println("当前结点：" + info.getCurrentNode());
        System.out.println("表单：" + JSONArray.toJSONString(info.getFlowData()));
    }
}

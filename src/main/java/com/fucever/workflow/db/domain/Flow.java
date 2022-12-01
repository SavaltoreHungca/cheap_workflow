package com.fucever.workflow.db.domain;

public class Flow {
    public Long id;
    public String name;
    public String type;
    public String creationTime;

    public Integer isClosed = 0;
    public Integer isPaused = 0;
    public Integer isFinished = 0;
    public Integer isStarted = 0;

    public Long currentNodeId;

    public String getStatus(){
        if (isClosed == 1){
            return "已关闭";
        }
        if (isPaused == 1){
            return "已停止";
        }
        if (isFinished == 1){
            return "已结束";
        }
        if (isStarted == 1){
            return "已开始";
        }
        return "未开始";
    }
}

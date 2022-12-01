package com.fucever.workflow.dto;

import lombok.Data;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;

import java.util.Collection;
import java.util.List;

@Data
public class PageInfo<T> {
    private Integer pageIndex;
    private Integer pageSize = 10;
    private Integer total = 0;
    private Collection<T> records;

    public PageInfo(){}
    public PageInfo(Integer pageIndex,Integer pageSize){
        this.pageIndex = pageIndex;
        this.pageSize = pageSize;
    }

    public PageInfo(Integer pageIndex,Integer pageSize, Integer total, Collection<T> records){
        this.pageIndex = pageIndex;
        this.pageSize = pageSize;
        this.total=total;
        this.records=records;
    }

    public PageInfo<T> setRecords(Collection<T> records){
        this.records = records;
        return this;
    }

    public List<Long> getRecordsList(){
        return (List) this.records;
    }

    public Integer getOffset() {
        if (shouldPageable()) {
            return pageIndex * pageSize;
        } else {
            return 0;
        }
    }

    public boolean shouldPageable() {
        return !(pageIndex == null || pageSize == null);
    }
}

package com.threattrace.domain;
import java.time.Instant;
public final class ProcessEvent extends ForensicEvent {
    public ProcessEvent(Integer id,Integer caseId,Instant at,String source,String description,String evidence,String certainty){super(id,caseId,at,source,description,evidence,certainty);}
    @Override public String getCategory(){return "PROCESS";} @Override public void validate(){if(getDescription().isBlank())throw new IllegalArgumentException("Process event description is required");}
}

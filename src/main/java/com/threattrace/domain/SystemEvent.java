package com.threattrace.domain;
import java.time.Instant;
public final class SystemEvent extends ForensicEvent {
    public SystemEvent(Integer id,Integer caseId,Instant at,String source,String description,String evidence,String certainty){super(id,caseId,at,source,description,evidence,certainty);}
    @Override public String getCategory(){return "SYSTEM";} @Override public void validate(){if(getDescription().isBlank())throw new IllegalArgumentException("System event description is required");}
}

package com.threattrace.domain;
import java.time.Instant;
public final class IdentityEvent extends ForensicEvent {
    public IdentityEvent(Integer id,Integer caseId,Instant at,String source,String description,String evidence,String certainty){super(id,caseId,at,source,description,evidence,certainty);}
    @Override public String getCategory(){return "IDENTITY";} @Override public void validate(){if(getOccurredAt()==null)throw new IllegalArgumentException("Event time is required");}
}

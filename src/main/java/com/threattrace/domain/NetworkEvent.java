package com.threattrace.domain;
import java.time.Instant;
public final class NetworkEvent extends ForensicEvent {
    public NetworkEvent(Integer id,Integer caseId,Instant at,String source,String description,String evidence,String certainty){super(id,caseId,at,source,description,evidence,certainty);}
    @Override public String getCategory(){return "NETWORK";} @Override public void validate(){if(getSource().isBlank())throw new IllegalArgumentException("Network source is required");}
}

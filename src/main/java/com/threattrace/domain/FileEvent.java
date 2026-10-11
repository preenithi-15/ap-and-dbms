package com.threattrace.domain;
import java.time.Instant;
public final class FileEvent extends ForensicEvent {
    public FileEvent(Integer id,Integer caseId,Instant at,String source,String description,String evidence,String certainty){super(id,caseId,at,source,description,evidence,certainty);}
    @Override public String getCategory(){return "FILE";} @Override public void validate(){if(getEvidenceRef().isBlank())throw new IllegalArgumentException("File event evidence reference is required");}
}

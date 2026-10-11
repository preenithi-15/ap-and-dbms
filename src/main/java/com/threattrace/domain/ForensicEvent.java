package com.threattrace.domain;

import java.time.Instant;

public abstract class ForensicEvent {
    private final Integer id;
    private final Integer caseId;
    private final Instant occurredAt;
    private final String source;
    private final String description;
    private final String evidenceRef;
    private final String certainty;

    protected ForensicEvent(Integer id, Integer caseId, Instant occurredAt, String source, String description, String evidenceRef, String certainty) {
        this.id=id; this.caseId=caseId; this.occurredAt=occurredAt; this.source=source;
        this.description=description; this.evidenceRef=evidenceRef; this.certainty=certainty;
    }
    public Integer getId(){return id;} public Integer getCaseId(){return caseId;} public Instant getOccurredAt(){return occurredAt;}
    public String getSource(){return source;} public String getDescription(){return description;} public String getEvidenceRef(){return evidenceRef;}
    public String getCertainty(){return certainty;} public abstract String getCategory(); public abstract void validate();
}

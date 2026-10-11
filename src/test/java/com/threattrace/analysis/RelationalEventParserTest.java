package com.threattrace.analysis;

import com.threattrace.domain.ProcessEvent;
import org.junit.jupiter.api.Test;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RelationalEventParserTest {
    @Test void parsesProcessRowsToValidatedSubtype() {
        var row=Map.<String,Object>of("id",101,"caseId",1,"occurredAt",Timestamp.from(Instant.parse("2026-04-14T09:00:07Z")),"type","PROCESS","source","Endpoint sensor","description","Shell spawned","evidenceRef","EV-101","certainty","OBSERVED");
        var event=new RelationalEventParser().parse(row);
        assertInstanceOf(ProcessEvent.class,event);
        assertEquals("PROCESS",event.getCategory());
        assertEquals(101,event.getId());
    }

    @Test void parsesMysqlLocalDateTimeRowsAsUtcInstants() {
        var row=Map.<String,Object>of("id",102,"caseId",1,"occurredAt",LocalDateTime.of(2026,4,14,9,0,7,123_000_000),"type","PROCESS","source","Endpoint sensor","description","Shell spawned","evidenceRef","EV-102","certainty","OBSERVED");
        var event=new RelationalEventParser().parse(row);
        assertEquals(Instant.parse("2026-04-14T09:00:07.123Z"),event.getOccurredAt());
    }
}

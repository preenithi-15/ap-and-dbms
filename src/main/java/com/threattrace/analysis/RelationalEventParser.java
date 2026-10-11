package com.threattrace.analysis;
import com.threattrace.domain.*;
import org.springframework.stereotype.Component;
import java.util.Map;
@Component
public class RelationalEventParser implements EventParser {
    @Override public ForensicEvent parse(Map<String,Object> r){Integer id=((Number)r.get("id")).intValue(),caseId=((Number)r.get("caseId")).intValue();Object time=r.get("occurredAt");java.time.Instant at=EventTime.toInstant(time);String type=String.valueOf(r.get("type")),source=String.valueOf(r.get("source")),description=String.valueOf(r.get("description")),evidence=String.valueOf(r.get("evidenceRef")),certainty=String.valueOf(r.get("certainty"));ForensicEvent event=switch(type){case "IDENTITY"->new IdentityEvent(id,caseId,at,source,description,evidence,certainty);case "PROCESS"->new ProcessEvent(id,caseId,at,source,description,evidence,certainty);case "NETWORK"->new NetworkEvent(id,caseId,at,source,description,evidence,certainty);case "FILE"->new FileEvent(id,caseId,at,source,description,evidence,certainty);default->new SystemEvent(id,caseId,at,source,description,evidence,certainty);};event.validate();return event;}
}

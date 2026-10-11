package com.threattrace.web;

import com.threattrace.analysis.AnalysisJobService;
import com.threattrace.analysis.EventParser;
import com.threattrace.persistence.CaseRepository;
import com.threattrace.privacy.EvidencePassportService;
import com.threattrace.security.InputChecks;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final CaseRepository repository;private final AnalysisJobService analysis;private final EventParser eventParser;private final EvidencePassportService passport;private final ObjectMapper mapper;
    public ApiController(CaseRepository repository,AnalysisJobService analysis,EventParser eventParser,EvidencePassportService passport,ObjectMapper mapper){this.repository=repository;this.analysis=analysis;this.eventParser=eventParser;this.passport=passport;this.mapper=mapper;}
    @GetMapping("/cases") public List<Map<String,Object>> cases(){return repository.cases();}
    @GetMapping("/cases/{id}/timeline") public List<Map<String,Object>> timeline(@PathVariable int id,@RequestParam(defaultValue="ALL") String category,@RequestParam(defaultValue="ALL") String certainty){return repository.timeline(id,category,certainty).stream().map(row->{var event=eventParser.parse(row);Map<String,Object> mapped=new LinkedHashMap<>(row);mapped.put("type",event.getCategory());return mapped;}).toList();}
    @GetMapping("/events/{id}") public Map<String,Object> event(@PathVariable int id){return repository.event(id);}
    @GetMapping("/cases/{id}/hypotheses") public List<com.threattrace.domain.Hypothesis> hypotheses(@PathVariable int id){return repository.hypothesisRecords(id);}
    @GetMapping("/hypotheses/{id}/evidence") public List<Map<String,Object>> hypothesisEvidence(@PathVariable int id){return repository.evidenceForHypothesis(id);}
    @GetMapping("/cases/{id}/twin") public Map<String,Object> twin(@PathVariable int id){return Map.of("nodes",repository.assets(id),"edges",repository.edges(id),"strategies",repository.strategies());}
    @PostMapping("/cases/{id}/simulate") public Map<String,Object> simulate(@PathVariable int id,@RequestBody Map<String,Object> body){int strategyId=((Number)body.get("strategyId")).intValue();var strategy=repository.strategy(strategyId);String label=(String)strategy.get("label");var base=repository.assets(id).size();int reduction=((Number)strategy.get("riskReduction")).intValue(),disruption=((Number)strategy.get("disruption")).intValue(),evidenceImpact=((Number)strategy.get("evidenceImpact")).intValue(),minutes=((Number)strategy.get("minutes")).intValue();var output=Map.of("baselineReachableAssets",base,"scenarioReachableAssets",Math.max(1,base-Math.round(base*reduction/100f)),"riskReductionEstimate",reduction,"businessDisruption",disruption,"evidenceImpact",evidenceImpact,"estimatedMinutes",minutes,"riskChange","Illustrative risk reduction based on the selected mitigation rule","assumptions",List.of("Synthetic graph only","Reachability reduction scales with the strategy's demo risk-reduction value","Unknown links may change reachability"),"modelVersion","twin-1.0");try{repository.saveSimulation(id,label,mapper.writeValueAsString(output));}catch(Exception e){throw new IllegalStateException("Could not save simulation",e);}return output;}
    @PostMapping("/compare") public Map<String,Object> compare(@RequestBody Map<String,Integer> body){int a=body.getOrDefault("caseA",0),b=body.getOrDefault("caseB",0);if(a==b||a<1||b<1)throw new IllegalArgumentException("Choose two different cases");var result=analysis.compare(a,b).data();var fa=repository.analyticsFeatures(a);var fb=repository.analyticsFeatures(b);try{String digest=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(Map.of("featuresA",fa,"featuresB",fb))));result.put("comparisonId",repository.saveComparison(result,digest,fa,fb));for(Object item:(List<?>)result.get("dimensions")){Map<?,?> dimension=(Map<?,?>)item;enrichTimestamps((List<Map<String,Object>>)dimension.get("matchedEvents"));enrichTimestamps((List<Map<String,Object>>)dimension.get("caseBMatchedEvents"));}}catch(Exception e){throw new IllegalStateException("Could not preserve reproducible comparison record",e);}return result;}
    private void enrichTimestamps(List<Map<String,Object>> rows){for(var row:rows)row.put("occurredAt",repository.event(((Number)row.get("id")).intValue()).get("occurredAt"));}
    @GetMapping("/packages") public List<Map<String,Object>> packages(){return repository.packages();}
    @PostMapping("/packages") public Map<String,Object> create(@RequestBody Map<String,Object> body){int caseId=((Number)body.get("caseId")).intValue();String purpose=InputChecks.required((String)body.get("purpose"),"Purpose",300),recipient=InputChecks.required((String)body.get("recipient"),"Recipient",120);List<Integer> ids=((List<?>)body.get("eventIds")).stream().map(v->((Number)v).intValue()).toList();String mode=InputChecks.required((String)body.getOrDefault("redactionMode","PSEUDONYMIZE"),"Redaction mode",32);if(!Set.of("PSEUDONYMIZE","GENERALIZE_TIME").contains(mode))throw new IllegalArgumentException("Unsupported redaction mode");return repository.createPackage(caseId,purpose,recipient,InputChecks.ids(ids),mode);}
    @GetMapping("/packages/{id}/preview") public Map<String,Object> preview(@PathVariable int id){return passport.preview(id);}
    @GetMapping("/packages/{id}/export") public Map<String,Object> export(@PathVariable int id){return passport.releaseArtifact(id);}
    @PostMapping("/packages/{id}/submit") public Map<String,Object> submit(@PathVariable int id){repository.submitPackage(id);return Map.of("status","PENDING_REVIEW");}
    @PostMapping("/packages/{id}/decision") public Map<String,Object> decide(@PathVariable int id,@RequestBody Map<String,String> body){repository.decidePackage(id,InputChecks.required(body.get("decision"),"Decision",16),InputChecks.required(body.get("reviewer"),"Reviewer",120),InputChecks.required(body.get("reason"),"Reason",300));return Map.of("status",body.get("decision").equals("APPROVE")?"RELEASED":"REJECTED");}
    @GetMapping("/audit") public List<Map<String,Object>> audit(){return repository.audit();}
    @ExceptionHandler(Exception.class) public ResponseEntity<Map<String,String>> error(Exception e){return ResponseEntity.badRequest().body(Map.of("error",Optional.ofNullable(e.getMessage()).orElse("Request failed")));}
}

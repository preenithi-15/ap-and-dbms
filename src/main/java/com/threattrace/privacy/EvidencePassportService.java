package com.threattrace.privacy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threattrace.domain.EventTime;
import com.threattrace.persistence.CaseRepository;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;

@Service
public class EvidencePassportService {
    private final CaseRepository repository;private final ObjectMapper mapper;private final String pseudonymKey;
    public EvidencePassportService(CaseRepository repository,ObjectMapper mapper,@Value("${threattrace.pseudonym-key:threattrace-synthetic-demo-key}") String pseudonymKey){this.repository=repository;this.mapper=mapper;this.pseudonymKey=pseudonymKey;}
    public Map<String,Object> preview(int packageId){
        Map<String,Object> source=repository.packagePreview(packageId);
        List<Map<String,Object>> rows=(List<Map<String,Object>>)source.get("items");List<Map<String,Object>> sanitized=new ArrayList<>();
        for(Map<String,Object> row:rows){Map<String,Object> out=new LinkedHashMap<>(row);Object id=row.get("id");out.put("source", "Source " + pseudonym(String.valueOf(row.get("source"))));out.put("host","Asset " + pseudonym(String.valueOf(row.get("host"))));
            out.put("description",String.valueOf(row.get("description")).replaceAll("(?i)(WS|FILE|ID|NET)-[A-Z0-9-]+","[ASSET]").replaceAll("\\b[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}\\b","[EMAIL]"));
            Object date=row.get("occurredAt");out.put("occurredAt",EventTime.toUtcHour(date));
            out.put("sourceEventId","EV-"+id);out.remove("id");out.remove("redactionMode");sanitized.add(out);}
        source.put("items",sanitized);source.put("redactionManifest",List.of("Host and source labels replaced with stable pseudonyms","Asset-like identifiers masked in descriptions","Timestamps generalized to the hour","Original evidence remains in the source case"));source.put("warning","Redaction reduces direct identifiers but does not guarantee anonymity. Human review required.");return source;
    }
    public Map<String,Object> releaseArtifact(int id){return releaseArtifact(id,"JSON");}
    public Map<String,Object> releaseArtifact(int id,String format){
        if(!"JSON".equalsIgnoreCase(format))throw new IllegalArgumentException("Only JSON passport export is supported");
        Map<String,Object> preview=preview(id);if(!"RELEASED".equals(preview.get("status")))throw new IllegalStateException("Only an approved RELEASED package can be exported");
        try{preview.remove("manifestHash");preview.put("hashAlgorithm","SHA-256");preview.put("packageVersion","1.0");byte[] content=mapper.writeValueAsBytes(preview);String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));repository.setManifestHash(id,hash);Map<String,Object> result=new LinkedHashMap<>(preview);result.put("manifestHash",hash);return result;}catch(Exception e){throw new IllegalStateException("Could not generate package integrity hash",e);}
    }
    private String pseudonym(String raw){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(pseudonymKey.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(raw.getBytes(StandardCharsets.UTF_8))).substring(0,8).toUpperCase(Locale.ROOT);}catch(Exception e){throw new IllegalStateException(e);}}
}

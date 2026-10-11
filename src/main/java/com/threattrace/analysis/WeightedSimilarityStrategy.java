package com.threattrace.analysis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

@Service
public class WeightedSimilarityStrategy implements SimilarityStrategy {
    private final String analyticsUrl; private final ObjectMapper mapper;
    public WeightedSimilarityStrategy(@Value("${analytics.url}") String analyticsUrl,ObjectMapper mapper){this.analyticsUrl=analyticsUrl;this.mapper=mapper;}
    @Override public Map<String,Object> compare(Map<String,Object> featuresA,Map<String,Object> featuresB){
        return compare(featuresA,featuresB,Map.of("sequence",.25,"timing",.20,"target",.15,"behavior",.25,"progression",.15));
    }
    public Map<String,Object> compare(Map<String,Object> featuresA,Map<String,Object> featuresB,Map<String,Double> weights){
        try { var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            var body=mapper.writeValueAsString(Map.of("featuresA",featuresA,"featuresB",featuresB,"weights",weights));
            var req=HttpRequest.newBuilder(URI.create(analyticsUrl+"/compare")).timeout(Duration.ofSeconds(5)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            var response=client.send(req,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200) throw new IllegalStateException("Analytics service returned "+response.statusCode());
            return mapper.readValue(response.body(),new TypeReference<>(){});
        } catch(Exception e){throw new IllegalStateException("Analytics service unavailable: "+e.getMessage(),e);}
    }
}

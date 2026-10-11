package com.threattrace.domain;
public record Hypothesis(Integer id,Integer caseId,String title,String status,Double confidence,String rationale,String missingTelemetry) { }

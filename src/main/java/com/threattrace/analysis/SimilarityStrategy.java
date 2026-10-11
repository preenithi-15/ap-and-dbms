package com.threattrace.analysis;
import java.util.Map;
public interface SimilarityStrategy { Map<String,Object> compare(Map<String,Object> featuresA,Map<String,Object> featuresB); }

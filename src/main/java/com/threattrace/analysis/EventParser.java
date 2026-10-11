package com.threattrace.analysis;
import com.threattrace.domain.ForensicEvent;
import java.util.Map;
public interface EventParser { ForensicEvent parse(Map<String,Object> row); }

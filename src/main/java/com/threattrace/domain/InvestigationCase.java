package com.threattrace.domain;

import java.util.List;

public record InvestigationCase(Integer id, String name, String classification, List<ForensicEvent> events) { }

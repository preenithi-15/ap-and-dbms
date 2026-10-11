package com.threattrace.security;
import java.util.*;
public final class InputChecks {
    private InputChecks(){}
    public static String required(String value,String name,int max){if(value==null||value.isBlank())throw new IllegalArgumentException(name+" is required");String s=value.trim();if(s.length()>max)throw new IllegalArgumentException(name+" exceeds "+max+" characters");return s;}
    public static List<Integer> ids(List<Integer> ids){if(ids==null||ids.isEmpty()||ids.size()>50)throw new IllegalArgumentException("Select between 1 and 50 evidence events");return List.copyOf(new LinkedHashSet<>(ids));}
}

package com.threattrace.domain;
public record Result<T>(boolean ok, T data, String error) {
    public static <T> Result<T> success(T data){return new Result<>(true,data,null);}
    public static <T> Result<T> failure(String error){return new Result<>(false,null,error);}
}

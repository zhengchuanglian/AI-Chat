package com.example.ai.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
//@AllArgsConstructor
public class Result {

    private Integer ok;
    private String message;


    private Result(Integer ok, String message){
        this.ok = ok;
        this.message = message;
    }

    public static Result ok(){
        return new Result(1, "ok");
    }

    public static Result fail(String message){
        return new Result(0, message);
    }
}

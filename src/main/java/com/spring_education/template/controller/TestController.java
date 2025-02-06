package com.spring_education.template.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/sample")
    public String helloWorld(){
        return "hello world!";
    }
}

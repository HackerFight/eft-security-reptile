package com.efreight.security.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * @author fu yuan hui
 * @date 2023-10-07 16:16:13 Saturday
 */
@RestController
public class HelloController {


    @GetMapping("/hello")
    public String hello(){
        return "Hello: " + LocalDateTime.now();
    }

    @GetMapping("/test")
    public String test(){
        return "test: " + LocalDateTime.now();
    }
}

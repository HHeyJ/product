package com.hyq.product.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @RequestMapping("/status")
    public String checkHealth(){
        return "SUCCESS";
    }

}

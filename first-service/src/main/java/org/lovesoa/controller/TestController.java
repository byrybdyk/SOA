package org.lovesoa.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/test")
    public String test() {
        return "Spring MVC is working!";
    }

    @GetMapping("/movies/hello")
    public String helloMovies() {
        return "Movies endpoint is working!";
    }
}
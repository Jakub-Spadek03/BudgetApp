package pk.js.pasir_spadek_jakub.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/test")
    public String test() {
        return "Hello, World!";
    }
    @GetMapping("/api/info")
    public java.util.Map<String, String> getAppInfo() {
        java.util.Map<String, String> info = new java.util.HashMap<>();


        info.put("appName", "PASiR - Budget App");
        info.put("message", "Połączenie z backendem działa!!!!");
        info.put("author", "Jakub Spadek");

        return info;}
}
package com.fpolizzi.smartlead.controller;

import com.fpolizzi.smartlead.service.AiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/ai")
    public String generateText(@RequestParam String userInput) {
        return aiService.generateText(userInput);
    }
}

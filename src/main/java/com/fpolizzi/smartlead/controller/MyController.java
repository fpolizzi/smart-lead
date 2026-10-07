package com.fpolizzi.smartlead.controller;

import com.fpolizzi.smartlead.service.HuggingFaceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by fpolizzi on 10/7/26
 */
@RestController
public class MyController {

    private final HuggingFaceService huggingFaceService;

    public MyController(HuggingFaceService huggingFaceService) {
        this.huggingFaceService = huggingFaceService;
    }

    @GetMapping("/ai")
    String generation(String userInput) {
        return huggingFaceService.generateText(userInput);
    }
}

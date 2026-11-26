package com.fpolizzi.smartlead.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeWebController {

    @GetMapping("/ui")
    public String home() {
        return "redirect:/ui/messages/new";
    }
}

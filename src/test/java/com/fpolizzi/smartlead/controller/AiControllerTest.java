package com.fpolizzi.smartlead.controller;

import com.fpolizzi.smartlead.exception.AiUnavailableException;
import com.fpolizzi.smartlead.service.AiService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AiControllerTest {

    @Test
    void returns200WithText() throws Exception {
        AiService service = mock(AiService.class);
        when(service.generateText("hello")).thenReturn("Hi there");

        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AiController(service)).build();

        mvc.perform(get("/ai").param("userInput", "hello"))
            .andExpect(status().isOk())
            .andExpect(content().string("Hi there"));
    }

    @Test
    void returns503WhenServiceUnavailable() throws Exception {
        AiService service = mock(AiService.class);
        when(service.generateText("test")).thenThrow(new AiUnavailableException());

        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AiController(service)).build();

        mvc.perform(get("/ai").param("userInput", "test"))
            .andExpect(status().isServiceUnavailable());
    }

    @Test
    void returns400WhenMissingUserInput() throws Exception {
        AiService service = mock(AiService.class);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AiController(service)).build();

        mvc.perform(get("/ai"))
            .andExpect(status().isBadRequest());
    }
}

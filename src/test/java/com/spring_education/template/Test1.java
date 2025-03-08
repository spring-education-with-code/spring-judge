package com.spring_education.template;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@AutoConfigureMockMvc
@SpringBootTest
public class Test1 {

    @Autowired
    MockMvc mockMvc;

    @Test
    public void case_1(){
        assertTrue(true);
    }

    @Test
    public void case_2(){
        assertTrue(true);
    }

    @Test
    public void case_3() throws Exception{
        mockMvc.perform(get("/api/sample"))
                .andExpect(content().string("hello world!"));
    }
}


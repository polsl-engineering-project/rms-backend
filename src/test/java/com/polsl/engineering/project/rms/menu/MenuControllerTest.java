package com.polsl.engineering.project.rms.menu;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

@WebMvcTest(MenuController.class)
@AutoConfigureMockMvc(addFilters = false)
public class MenuControllerTest {
}

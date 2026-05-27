package com.animetracker.config;

import com.animetracker.controller.AdminController;
import com.animetracker.controller.HealthCheckController;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
@ComponentScan(basePackageClasses = {
        AdminController.class,
        HealthCheckController.class
})
public class WebMvcConfig {
}

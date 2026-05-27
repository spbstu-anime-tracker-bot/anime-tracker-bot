package com.animetracker.config;

import com.animetracker.controller.AdminController;
import com.animetracker.controller.HealthCheckController;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class WebMvcConfigTest {

    @Test
    void webContextRegistersRestControllersWithoutSpringBoot() throws Exception {
        try (AnnotationConfigApplicationContext rootContext = new AnnotationConfigApplicationContext(AppConfig.class);
             AnnotationConfigWebApplicationContext webContext = new AnnotationConfigWebApplicationContext()) {

            webContext.setParent(rootContext);
            webContext.setServletContext(new MockServletContext());
            webContext.register(WebMvcConfig.class);
            webContext.refresh();

            DispatcherServlet dispatcherServlet = new DispatcherServlet(webContext);
            dispatcherServlet.init(new MockServletConfig(webContext.getServletContext(), "dispatcherServlet"));

            assertNotNull(webContext.getBean(AdminController.class));
            assertNotNull(webContext.getBean(HealthCheckController.class));
            assertNotNull(webContext.getBean("requestMappingHandlerMapping"));

            dispatcherServlet.destroy();
        }
    }
}

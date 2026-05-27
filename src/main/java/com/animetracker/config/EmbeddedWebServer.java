package com.animetracker.config;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import java.nio.file.Files;
import java.nio.file.Path;

public final class EmbeddedWebServer implements AutoCloseable {

    private final Tomcat tomcat;

    private EmbeddedWebServer(Tomcat tomcat) {
        this.tomcat = tomcat;
    }

    public static EmbeddedWebServer start(ApplicationContext parentContext, int port) throws Exception {
        Path baseDir = Files.createTempDirectory("anime-tracker-tomcat");

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.setBaseDir(baseDir.toString());
        tomcat.getConnector();

        Context servletContext = tomcat.addContext("", baseDir.toString());

        AnnotationConfigWebApplicationContext webContext = new AnnotationConfigWebApplicationContext();
        webContext.setParent(parentContext);
        webContext.register(WebMvcConfig.class);

        DispatcherServlet dispatcherServlet = new DispatcherServlet(webContext);
        Tomcat.addServlet(servletContext, "dispatcherServlet", dispatcherServlet).setLoadOnStartup(1);
        servletContext.addServletMappingDecoded("/", "dispatcherServlet");

        tomcat.start();
        return new EmbeddedWebServer(tomcat);
    }

    @Override
    public void close() throws Exception {
        tomcat.stop();
        tomcat.destroy();
    }
}

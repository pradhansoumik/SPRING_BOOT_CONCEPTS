package com.example.autoconfigdemo.app;

import com.example.autoconfigdemo.autoconfigure.Greeter;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.DispatcherServlet;

@Component
public class StartupInspector implements ApplicationRunner {

    private final ApplicationContext ctx;

    public StartupInspector(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void run(ApplicationArguments args) {
        System.out.println();
        System.out.println("========== after refresh() (ApplicationRunner) ==========");
        probeClass("com.google.gson.Gson");
        probeClass("org.springframework.web.servlet.DispatcherServlet");
        System.out.println();
        probeBeanByName("Gson (Boot auto-config, ConditionalOnClass)", "com.google.gson.Gson");
        probeBean("DispatcherServlet (starter-web on classpath)", DispatcherServlet.class);
        probeBean("Greeter (OUR AutoConfiguration.imports)", Greeter.class);
        System.out.println("========================================================");
        System.out.println();
    }

    private void probeClass(String fqcn) {
        try {
            Class.forName(fqcn);
            System.out.println("[classpath] " + fqcn + " → PRESENT (Maven put the JAR here)");
        } catch (ClassNotFoundException e) {
            System.out.println("[classpath] " + fqcn + " → ABSENT");
        }
    }

    private void probeBeanByName(String label, String fqcn) {
        try {
            probeBean(label, Class.forName(fqcn));
        } catch (ClassNotFoundException e) {
            System.out.println("[bean]      " + label + " → NOT created (class not on classpath)");
        }
    }

    private void probeBean(String label, Class<?> type) {
        String[] names = ctx.getBeanNamesForType(type);
        if (names.length == 0) {
            System.out.println("[bean]      " + label + " → NOT created (condition failed or missing JAR)");
        } else {
            System.out.println("[bean]      " + label + " → " + String.join(", ", names));
        }
    }
}

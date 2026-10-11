package com.zaalima.orchestrator.config;

import java.util.Properties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BuildPropertiesConfig {

    @Bean
    public BuildProperties buildProperties() {
        Properties properties = new Properties();
        properties.setProperty("version", "0.0.1-SNAPSHOT");
        return new BuildProperties(properties);
    }
}

package com.datastax.demo;

import com.datastax.oss.driver.api.core.CqlSession;
import com.dtsx.astra.sdk.cassio.MetadataVectorCassandraTable;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Main class to start the Spring Boot application.
 */
@SpringBootApplication
public class GenerativeAiApplication {

    /**
     * Main operation.
     *
     * @param args
     *      command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(GenerativeAiApplication.class, args);
    }
}

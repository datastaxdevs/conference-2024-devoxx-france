package com.datastax.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration;

/**
 * Main class to start the Spring Boot application.
 */
@SpringBootApplication(exclude = {
        CassandraDataAutoConfiguration.class,
        CassandraAutoConfiguration.class }
)
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

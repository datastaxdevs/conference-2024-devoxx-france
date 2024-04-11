package com.datastax.demo;

import com.datastax.astra.sdk.AstraClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ConnectionTest {

    @Autowired
    AstraClient astraClient;

    @Test
    public void shouldBeConnectedTest() {
        System.out.println("---------------------------");
        System.out.println("---         SUCCESS      --");
        System.out.println("---------------------------");
        System.out.println("Connected to " + astraClient.apiDevops().getOrganization().getName());
    }

}

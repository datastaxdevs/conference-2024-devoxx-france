package devoxx.demo;

import com.datastax.oss.driver.api.core.CqlSession;
import com.dtsx.astra.sdk.AstraDBAdmin;
import com.dtsx.astra.sdk.cassio.CassIO;
import com.dtsx.astra.sdk.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.UUID;

@Slf4j
public class CassIOConnection {

    static private String token =  System.getenv("ASTRA_DB_APPLICATION_TOKEN");

    @Test
    public void shouldConnect() {

        // Create db if not exists
        UUID databaseId = new AstraDBAdmin(token).createDatabase("devoxx");
        log.info("Database ID: {}", databaseId);

        // Initializing CqlSession
        try (CqlSession cqlSession = CassIO.init(token, databaseId,  "us-east1", "default_keyspace")) {
            log.info("Connected to AstraDB");

        }
    }
}

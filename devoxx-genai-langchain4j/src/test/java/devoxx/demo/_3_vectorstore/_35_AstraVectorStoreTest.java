package devoxx.demo._3_vectorstore;

import com.datastax.astra.client.DataAPIClient;
import devoxx.demo.utils.AbstractDevoxxTestSupport;
import org.junit.jupiter.api.Test;

/**
 * astra db list
 *
 */
public class _35_AstraVectorStoreTest extends AbstractDevoxxTestSupport {

    @Test
    public void testConnect() {
        System.out.println("Test connect to AstraDB");
        DataAPIClient client = new DataAPIClient(astraToken);
        client.getDatabase(astraEndpoint).listCollectionNames().forEach(System.out::println);


    }
}

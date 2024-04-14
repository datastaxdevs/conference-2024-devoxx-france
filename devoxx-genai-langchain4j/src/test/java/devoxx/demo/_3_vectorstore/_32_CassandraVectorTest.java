package devoxx.demo._3_vectorstore;

import com.datastax.oss.driver.api.core.CqlSession;
import com.dtsx.astra.sdk.cassio.CassIO;
import com.dtsx.astra.sdk.cassio.MetadataVectorRecord;
import com.dtsx.astra.sdk.cassio.MetadataVectorTable;
import com.fasterxml.jackson.databind.ObjectMapper;
import devoxx.demo.utils.AbstractDevoxxTestSupport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class _32_CassandraVectorTest extends AbstractDevoxxTestSupport {

    @SuppressWarnings("unchecked")
    private LinkedHashMap<String, List<?>> loadQuotes(String filePath) throws IOException {
        File inputFile = new File(_32_CassandraVectorTest.class.getClassLoader().getResource(filePath).getFile());
        LinkedHashMap<String, Object> sampleQuotes = new ObjectMapper().readValue(inputFile, LinkedHashMap.class);
        System.out.println("Quotes by Author:");
        ((LinkedHashMap<?,?>) sampleQuotes.get("quotes")).forEach((k,v) ->
                System.out.println("   " + k + " (" + ((ArrayList<?>)v).size() + ") "));
        log.info("Sample Quotes");
        ((LinkedHashMap<?, ?>) sampleQuotes.get("quotes"))
                .entrySet().stream().limit(2)
                .forEach(e -> {
                    System.out.println("   " + e.getKey() + " : ");
                    Map<String, Object> entry = (Map<String,Object>) ((ArrayList<?>)e.getValue()).get(0);
                    System.out.println("      " + ((String) entry.get("body")).substring(0, 50) + "... (tags: " + entry.get("tags") + ")");
                    entry = (Map<String,Object>) ((ArrayList<?>)e.getValue()).get(1);
                    System.out.println("      " + ((String) entry.get("body")).substring(0, 50) + "... (tags: " + entry.get("tags") + ")");
                });
        return  ((LinkedHashMap<String, List<?>>) sampleQuotes.get("quotes"));
    }

    @SuppressWarnings("unchecked")
    private MetadataVectorRecord mapQuote(AtomicInteger quote_idx, String author, Object q) {
        MetadataVectorRecord record = new MetadataVectorRecord();
        Map<String, Object> quote = (Map<String, Object>) q;
        String body = (String) quote.get("body");
        record.setBody(body);
        record.getMetadata().put("author", author);
        ((ArrayList<String>) quote.get("tags")).forEach(tag -> record.getMetadata().put(tag, "true"));
        record.setVector(getEmbeddingModelGecko().embed(body).content().vectorAsList());
        record.setRowId("q_" + author + "_" + quote_idx.getAndIncrement());
        System.out.print("◾");
        return record;
    }

    /**
     * Load quotes into the Vector Store
     * Insert quotes into vector store.
     * <p>
     * You will compute the embeddings for the quotes and save them into the Vector Store,
     * along with the text itself and the metadata planned for later use. Note that the author is added
     * as a metadata field along with the "tags" already found with the quote itself.
     * </p>
     * To optimize speed and reduce the calls, you'll perform batched calls to the embedding OpenAI service,
     * with one batch per author.
     */
    @Test
    public void shouldIngestDocuments() throws IOException {

        // Connection to the Cassandra
        try (CqlSession cqlSession = CassIO.init(astraToken, astraDatabaseId, astraDatabaseRegion, astraDatabaseKeyspace)) {
            MetadataVectorTable vectorTable = new MetadataVectorTable(cqlSession, astraDatabaseKeyspace, "vector_store", 768);
            log.info("Loading Dataset");
            loadQuotes("philo_quotes.json").forEach((author, quoteList) -> {
                log.info("Processing '{}' :", author);
                AtomicInteger quote_idx = new AtomicInteger(0);
                quoteList.stream().map(quote -> mapQuote(quote_idx, author, quote)).forEach(vectorTable::put);
                log.info(" Done (inserted " + quote_idx.get() + " quotes).");
            });
            log.info("Finished inserting.");

        }
    }

}

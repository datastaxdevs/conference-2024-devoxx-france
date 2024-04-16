package devoxx.demo._5_vectorsearch;

import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.cassio.AnnQuery;
import dev.langchain4j.store.cassio.AnnResult;
import dev.langchain4j.store.cassio.MetadataVectorRecord;
import dev.langchain4j.store.cassio.MetadataVectorTable;
import devoxx.demo.devoxx.Quote;
import devoxx.demo.utils.AbstractDevoxxTestSupport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.langchain4j.store.cassio.SimilarityMetric.COSINE;
import static devoxx.demo.devoxx.Utilities.ASTRA_KEYSPACE;
import static devoxx.demo.devoxx.Utilities.EMBEDDING_DIMENSION;
import static devoxx.demo.devoxx.Utilities.TABLE_NAME;
import static devoxx.demo.devoxx.Utilities.loadQuotes;

@Slf4j
public class _53_CassIOVectorDbTest extends AbstractDevoxxTestSupport {

    @Test
    @Disabled
    public void shouldIngestDocuments() throws IOException {
        getVectorTable().create();
        getVectorTable().clear();
        loadQuotes("philo_quotes.json")       // extraction
                .stream().parallel()                  // no chunking (single sentences)
                .map(this::mapQuoteToCassandraRecord) // bean-> db record
                .forEach(getVectorTable()::put);      // persist
    }

    @Test
    public void shouldFindSimilarQuotes() {
        // Encode question
        List<Float> vector = getEmbeddingModelGecko()
                .embed("We struggle all our life for nothing")
                .content()
                .vectorAsList();

        // Build Query
        AnnQuery query = AnnQuery.builder()
                .metric(COSINE)
                .topK(3).threshold(.8)
                .embeddings(vector)  // add vector here
                .build();

        // Execute query
        Stream<MetadataVectorRecord> results = getVectorTable()
                .similaritySearch(query)
                .stream()
                .map(AnnResult::getEmbedded);

        // Display Results
        results.map(MetadataVectorRecord::getBody)
               .forEach(System.out::println);
    }

    @Test
    public void shouldFindSimilarQuotesWithFilter() {
        log.info("Sample Search");
        findSimilarQuotes("We struggle all our life for nothing", 3, .8, null)
                .map(Quote::body).forEach(System.out::println);

        log.info("Search with filter");
        findSimilarQuotes("We struggle all our life for nothing", 2, .5, "politics")
                .map(Quote::body).forEach(System.out::println);
    }

    @Test
    public void shouldGenerateQuotes() {
        log.info("Generate Quotes");
        MetadataVectorTable vectorTable = getVectorTable();

        PromptTemplate promptTemplate = PromptTemplate.from("""
                Generate a single short philosophical quote on the given topic,
                similar in spirit and form to the provided actual example quotes.
                Do not exceed 20-30 words in your quote.
                REFERENCE TOPIC: \n {{topic}} \n
                ACTUAL EXAMPLES:\n{{examples}}
                """);

        Map<String, Object> variables = new HashMap<>();
        variables.put("topic", "politics");
        variables.put("examples", findSimilarQuotes("We struggle all our life for nothing", 2, .8, "politics")
                .map(Quote::body)
                .collect(Collectors.joining(",")));

        Prompt prompt = promptTemplate.apply(variables);
        System.out.println(prompt.toSystemMessage().text());
        Response<String> response = getLanguageModelTextBison().generate(prompt);
        System.out.println(response.content());
    }

    private Stream<Quote> findSimilarQuotes(String quote, int topK, double threshold, String topic) {
        AnnQuery.AnnQueryBuilder builder = AnnQuery.builder()
                .metric(COSINE)
                .topK(topK)
                .threshold(threshold)
                .metaData(topic != null ? Map.of(topic, "true") : null)
                .embeddings(getEmbeddingModelGecko()
                        .embed(quote)
                        .content().vectorAsList());
        return getVectorTable().similaritySearch(builder.build()).stream()
                .map(AnnResult::getEmbedded)
                .map(embedded -> (MetadataVectorRecord) embedded)
                .map(this::mapCassandraRowToQuote);
    }

    private MetadataVectorRecord mapQuoteToCassandraRecord(Quote quote) {
        System.out.println("◾ " + quote );
        MetadataVectorRecord record = new MetadataVectorRecord();
        record.setBody(quote.body());
        record.getMetadata().put("author", quote.author());
        quote.tags().forEach(tag -> record.getMetadata().put(tag, "true"));
        record.setVector(getEmbeddingModelGecko().embed(quote.body()).content().vectorAsList());
        record.setRowId(quote.rowId());
        return record;
    }

    private Quote mapCassandraRowToQuote(MetadataVectorRecord r) {
        // Removing the brackets and trimming unnecessary spaces
        List<String> tags = new ArrayList<>();
        String myTags = r.getMetadata().get("tags");
        if (myTags != null) {
            String trimmed =myTags.substring(1, myTags.length() - 1).trim();
            tags.addAll(Arrays.stream(trimmed.split(",")).map(String::trim).toList());
        }
        return new Quote(
                r.getRowId(),
                r.getMetadata().get("author"),
                tags ,
                r.getBody());
    }

    private MetadataVectorTable getVectorTable() {
        return  new MetadataVectorTable(getCassandraSession(), ASTRA_KEYSPACE, TABLE_NAME, EMBEDDING_DIMENSION);
    }

}

package devoxx.demo._3_vectorstore;

import com.datastax.oss.driver.api.core.cql.Row;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
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
import static devoxx.demo.devoxx.Utilities.loadQuotes;

@Slf4j
public class _33_CassIOVectorDbTest extends AbstractDevoxxTestSupport {

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
                .embeddings(vector)
                .topK(3).threshold(.8)
                .build();

        // Execute query
        Stream<MetadataVectorRecord> results = getVectorTable()
                .similaritySearch(query)
                .stream()
                .map(AnnResult::getEmbedded);

        results.map(MetadataVectorRecord::getBody).forEach(System.out::println);
    }

    @Test
    public void shouldFindSimilarQuotesWithFilter() {
        findSimilarQuotes("We struggle all our life for nothing", 3, .8)
                .map(Quote::body).forEach(System.out::println);
    }

    private Stream<Quote> findSimilarQuotes(String quote, int topK, double threshold) {
        return getVectorTable().similaritySearch(AnnQuery.builder()
                        .metric(COSINE)
                        .topK(topK)
                        .threshold(threshold)
                        //.metaData(Map.of("author", "nietzsche"))
                        //.metaData(Map.of("tag", "politics"))
                        .embeddings(getEmbeddingModelGecko()
                                .embed(quote)
                                .content().vectorAsList())
                        .build()).stream()
                .map(AnnResult::getEmbedded)
                .map(embedded -> (MetadataVectorRecord) embedded)
                .map(this::mapCassandraRowToQuote);
    }


    @Test
    public void shouldGenerateQuotes() {
        log.info("Generate Quotes");
        MetadataVectorTable vectorTable = getVectorTable();

        PromptTemplate promptTemplate = PromptTemplate.from("""
                Generate a single short philosophical quote on the given topic,
                similar in spirit and form to the provided actual example quotes.
                Do not exceed 20-30 words in your quote.
                REFERENCE TOPIC: \n {topic} \n
                ACTUAL EXAMPLES:\n{examples}
                """);

        Map<String, Object> variables = new HashMap<>();
        variables.put("topic", "Java Developer");
        variables.put("examples", findSimilarQuotes("We struggle all our life for nothing", 2, .8)
                .map(Quote::body).collect(Collectors.joining(",")));
        System.out.println(variables);

        Prompt p = promptTemplate.apply(variables);
        System.out.println(p.toSystemMessage().text());
        /*

        ChatCompletionRequest req = ChatCompletionRequest.builder()
                .model(llmModelNameChatCompletion)
                .messages(Collections.singletonList(new ChatMessage("user", promptTemplate)))
                .temperature(0.7)
                .maxTokens(320)
                .n(n)
                .build();

        return openAIClient.createChatCompletion(req)
                .getChoices().stream()
                .map(ChatCompletionChoice::getMessage)
                .map(ChatMessage::getContent)
                .collect(Collectors.toList());*/
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

    private Quote mapCassandraRowToQuote(Row row) {
        return new Quote(
                row.getString("row_id"),
                row.getString("author"),
                row.getList("tags", String.class),
                row.getString("body"));
    }

    private MetadataVectorTable getVectorTable() {
        return  new MetadataVectorTable(getCassandraSession(), ASTRA_KEYSPACE, "vector_store", 768);
    }


}

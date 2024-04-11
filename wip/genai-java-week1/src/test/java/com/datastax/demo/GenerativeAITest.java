package com.datastax.demo;

import com.datastax.oss.driver.api.core.CqlSession;
import com.dtsx.astra.sdk.cassio.MetadataVectorCassandraTable;
import com.dtsx.astra.sdk.cassio.SimilarityMetric;
import com.dtsx.astra.sdk.cassio.SimilaritySearchQuery;
import com.dtsx.astra.sdk.cassio.SimilaritySearchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.service.OpenAiService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * This app is a tentative to implement an equivalent of the CassIO example
 * direct usage with Java.
 */
@SpringBootTest
public class GenerativeAITest {

    /**
     * Logger.
     */
    static Logger log = LoggerFactory.getLogger(GenerativeAITest.class);

    /**
     * Keys in application.yml
     */
    @Value("${generative-ai.llm-model.embeddings}")
    public String llmModelNameEmbeddings;

    @Value("${generative-ai.llm-model.chat-completion}")
    public String llmModelNameChatCompletion;

    /**
     * @see GenerativeAiConfiguration
     */

    @Autowired
    public OpenAiService openAIClient;

    @Autowired
    public CqlSession cqlSession;

    @Autowired
    public MetadataVectorCassandraTable vectorTable;

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
    public void shouldIngestDocuments()
    throws IOException {
        log.info("Loading Dataset");
        loadQuotes("philo_quotes.json").forEach((author, quoteList) -> {
            log.info("Processing '{}' :", author);
            AtomicInteger quote_idx = new AtomicInteger(0);
            quoteList.stream()
                    .map(quote -> mapQuote(quote_idx, author, quote))
                    .forEach(vectorTable::put);
            System.out.println();
            log.info(" Done (inserted " + quote_idx.get() + " quotes).");
        });
        log.info("Finished inserting.");
    }

    @Test
    public void shouldSimilaritySearchQuotes() {
        List<String> quotes1 = findQuotes(vectorTable, "We struggle all our life for nothing", 3);
        logQuotes(quotes1);
    }

    @Test
    public void shouldSimilaritySearchQuotesFilteredByAuthor() {
        List<String> quotes2 =  findQuotesWithAuthor(vectorTable, "We struggle all our life for nothing", 2, "nietzsche");
        logQuotes(quotes2);
    }

    @Test
    public void shouldSimilaritySearchQuotesFilteredByTags() {
        List<String> quotes3 =  findQuotesWithATags(vectorTable, "We struggle all our life for nothing", 2, "politics");
        logQuotes(quotes3);
    }

    @Test
    public void shouldSimilaritySearchQuotesWithThreshold() {
        List<String> quotes4 = findQuotesWithThreshold(vectorTable, "Animals are our equals", 8, 0.8);
        logQuotes(quotes4);
    }

    @Test
    public void shouldGenerateQuotesWithRag() {
        List<String> generatedQuotes = generateQuotes(vectorTable, "politics and virtue", 4, "nietzsche");
        logQuotes(generatedQuotes);
    }

    // -- utilities --

    /**
     * Generate Query based on a retrieval.
     *
     * @param v_table
     *      current table
     * @param topic
     *      current topic
     * @param author
     *      current author
     * param n
     *      number of generated quotes
     * @return
     *      generated
     */
    private List<String> generateQuotes(MetadataVectorCassandraTable v_table, String topic, int n, String author) {
        log.info("Generate Quotes");
        String promptTemplate =
                "Generate a single short philosophical quote on the given topic,\n" +
                "similar in spirit and form to the provided actual example quotes.\n" +
                "Do not exceed 20-30 words in your quote.\n" +
                "REFERENCE TOPIC: \n{topic}" +
                "\nACTUAL EXAMPLES:\n{examples}"
                        .replace("{topic}", topic)
                        .replace("{examples}", String.join(",", findQuotesWithAuthor(v_table, topic, 4, author)));

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
                .collect(Collectors.toList());
    }

    /**
     * Search for quotes
     * You'll now search for quotes using the vector store. You'll use the ann_search method
     * to find the closest quotes to a given query. You'll use the author as a filter to
     * restrict the search to a given author.
     *
     * @param vTable
     *      current table
     * @param  query
     *      similarity query
     * @param recordCount
     *      record count
     */
    private List<String> findQuotes(MetadataVectorCassandraTable vTable, String query, int recordCount) {
        log.info("Search for quotes:");
        return findQuotesDetailed(vTable, query, null, recordCount, null,  (String[]) null);
    }

    private void logQuotes(List<String> quotes) {
        if (quotes != null) {
            quotes.forEach(System.out::println);
        }
    }

    /**
     * The vector similarity search generally returns the vectors that are closest to the query, even if that
     *  means results that might be somewhat irrelevant if there's nothing better.
     * To keep this issue under control, you can get the actual "distance" between the query and each result,
     * and then set a cutoff on it, effectively discarding results that are beyond that threshold. Tuning
     * this threshold correctly is not an easy problem: here, we'll just show you the way.
     * To get a feeling on how this works, try the following query and play with the choice of quote and
     * threshold to compare the results.
     * Note (for the mathematically inclined): this "distance" is exactly the cosine difference between the
     * vectors, i.e. the scalar product divided by the product of the norms of the two vectors. As such,
     * it is a number ranging from -1 to +1. Elsewhere, (e.g. in the "CQL" version of this example) you
     * will see this quantity rescaled to fit the [0, 1] interval, which means the numerical values and
     * adequate thresholds will be slightly different.
     */
    private List<String> findQuotesWithThreshold(MetadataVectorCassandraTable vTable, String query,int recordCount, double threshold) {
        log.info(" Cutting out irrelevant results:");
        return findQuotesDetailed(vTable, query, threshold, recordCount, null, (String[]) null);
    }

    private List<String> findQuotesWithAuthor(MetadataVectorCassandraTable vTable, String query, int recordCount, String author) {
        log.info("Search restricted to an author:");
        return findQuotesDetailed(vTable, query, null, recordCount, author, (String[])  null);
    }

    private List<String> findQuotesWithATags(MetadataVectorCassandraTable vTable, String query, int recordCount, String... tags) {
        log.info("Search constrained to a tag (out of those saved earlier with the quotes");
       return findQuotesDetailed(vTable, query, null, recordCount, null, tags);
    }

    private List<Float> computeOpenAIEmbeddings(String sentence) {
        return  openAIClient
                // Invoke OpenAi API
                .createEmbeddings(EmbeddingRequest.builder()
                        .model(llmModelNameEmbeddings)
                        .input(Collections.singletonList(sentence))
                        .build()).getData().get(0)
                // Mapping as a List<Float>
                .getEmbedding().stream()
                .map(Double::floatValue)
                .collect(Collectors.toList());
    }

    /**
     * Search for quotes
     * You'll now search for quotes using the vector store. You'll use the ann_search method
     * to find the closest quotes to a given query. You'll use the author as a filter to
     * restrict the search to a given author.
     *
     * @param vTable
     *      current table
     * @param  query
     *      similarity query
     * @param threshold
     *      threshold
     * @param recordCount
     *      record count
     * @param author
     *      author
     * @param tags
     *      tags
     */
    private List<String> findQuotesDetailed(MetadataVectorCassandraTable vTable, String query, Double threshold, int recordCount, String author, String... tags) {
        // Build the query
        SimilaritySearchQuery.SimilaritySearchQueryBuilder queryBuilder =
                SimilaritySearchQuery.builder()
                .distance(SimilarityMetric.DOT_PRODUCT)
                .recordCount(recordCount)
                .embeddings(computeOpenAIEmbeddings(query));

        if (threshold != null) {
            queryBuilder.threshold(threshold);
        }
        Map<String, String> metaData = new LinkedHashMap<>();
        if (author != null) {
            metaData.put("author", author);
        }
        if (tags != null) {
            for (String tag : tags) {
                metaData.put(tag, "true");
            }
        }
        if (!metaData.isEmpty()) {
            queryBuilder.metaData(metaData);
        }

        return vTable.similaritySearch(queryBuilder.build())
                     .stream()
                     .map(SimilaritySearchResult::getEmbedded)
                     .map(MetadataVectorCassandraTable.Record::getBody)
                     .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private LinkedHashMap<String, List<?>> loadQuotes(String filePath) throws IOException {
        File inputFile = new File(GenerativeAITest.class.getClassLoader().getResource(filePath).getFile());
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
    private MetadataVectorCassandraTable.Record mapQuote(AtomicInteger quote_idx, String author, Object q) {
        MetadataVectorCassandraTable.Record record = new MetadataVectorCassandraTable.Record();
        Map<String, Object> quote = (Map<String, Object>) q;
        String body = (String) quote.get("body");
        record.setBody(body);
        record.getMetadata().put("author", author);
        ((ArrayList<String>) quote.get("tags"))
                .forEach(tag -> record.getMetadata().put(tag, "true"));
        record.setVector(openAIClient.createEmbeddings(EmbeddingRequest
                        .builder()
                        .model(llmModelNameEmbeddings)
                        .input(Collections.singletonList(body))
                        .build())
                .getData().get(0)
                .getEmbedding().stream()
                .map(Double::floatValue)
                .collect(Collectors.toList()));
        record.setRowId("q_" + author + "_" + quote_idx.getAndIncrement());
        System.out.print("◾");
        return record;
    }

}

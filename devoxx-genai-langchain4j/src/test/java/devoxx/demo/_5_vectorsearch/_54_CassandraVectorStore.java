package devoxx.demo._5_vectorsearch;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.cassandra.CassandraCassioEmbeddingStore;
import devoxx.demo.utils.AbstractDevoxxTestSupport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;
import static devoxx.demo.devoxx.Utilities.EMBEDDING_DIMENSION;
import static devoxx.demo.devoxx.Utilities.TABLE_NAME;

@Slf4j
public class _54_CassandraVectorStore extends AbstractDevoxxTestSupport {

    @Test
    public void langchain4jEmbeddingStore() {
        // I have to create a EmbeddingModel
        EmbeddingModel embeddingModel = getEmbeddingModelGecko();

        // Embed the question
        Embedding questionEmbedding = embeddingModel
                .embed("We struggle all our life for nothing")
                .content();

        // We need the store
        EmbeddingStore<TextSegment> embeddingStore = new CassandraCassioEmbeddingStore(
                getCassandraSession(), TABLE_NAME, EMBEDDING_DIMENSION);

        // Query (1)
        log.info("Querying the store");
        embeddingStore
                .findRelevant(questionEmbedding, 3, 0.8d)
                .stream().map(r -> r.embedded().text())
                .forEach(System.out::println);

        // Query with a filter(2)
        log.info("Querying with filter");
        embeddingStore.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(questionEmbedding)
                .filter(metadataKey("author").isEqualTo("nietzsche"))
                .maxResults(3).minScore(0.8d).build())
                .matches()
                .stream().map(r -> r.embedded().text())
                .forEach(System.out::println);

    }

}

package devoxx.demo._4_rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.cohere.CohereScoringModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.model.vertexai.VertexAiChatModel;
import dev.langchain4j.model.vertexai.VertexAiEmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.ContentAggregator;
import dev.langchain4j.rag.content.aggregator.ReRankingContentAggregator;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import devoxx.demo.utils.Assistant;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static devoxx.demo.devoxx.Utilities.GCP_PROJECT_ENDPOINT;
import static devoxx.demo.devoxx.Utilities.GCP_PROJECT_ID;
import static devoxx.demo.devoxx.Utilities.GCP_PROJECT_LOCATION;
import static devoxx.demo.devoxx.Utilities.GCP_PROJECT_PUBLISHER;
import static devoxx.demo.utils.Utils.startConversationWith;
import static devoxx.demo.utils.Utils.toPath;


public class _10_Advanced_RAG_with_ReRanking_Example {

    /**
     * Please refer to {@link _07_Naive_RAG_Example} for a basic context.
     * <p>
     * Advanced RAG in LangChain4j is described here: https://github.com/langchain4j/langchain4j/pull/538
     * <p>
     * This example illustrates the implementation of a more advanced RAG application
     * using a technique known as "re-ranking".
     * <p>
     * Frequently, not all results retrieved by {@link ContentRetriever} are truly relevant to the user query.
     * This is because, during the initial retrieval stage, it is often preferable to use faster
     * and more cost-effective models, particularly when dealing with a large volume of data.
     * The trade-off is that the retrieval quality may be lower.
     * Providing irrelevant information to the LLM can be costly and, in the worst case, lead to hallucinations.
     * Therefore, in the second stage, we can perform re-ranking of the results obtained in the first stage
     * and eliminate irrelevant results using a more advanced model (e.g., Cohere Rerank).
     */

    public static void main(String[] args) {

        Assistant assistant = createAssistant("documents/miles-of-smiles-terms-of-use.txt");

        // First, say "Hi". Observe how all segments retrieved in the first stage were filtered out.
        // Then, ask "Can I cancel my reservation?" and observe how all but one segment were filtered out.
        startConversationWith(assistant);
    }

    private static Assistant createAssistant(String documentPath) {

        Document document = loadDocument(toPath(documentPath), new TextDocumentParser());

        //EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();


        EmbeddingModel embeddingModel = VertexAiEmbeddingModel.builder()
                .project(GCP_PROJECT_ID)
                .endpoint(GCP_PROJECT_ENDPOINT)
                .location(GCP_PROJECT_LOCATION)
                .publisher(GCP_PROJECT_PUBLISHER)
                .modelName("textembedding-gecko@001")
                .maxRetries(3)
                .build();
        ChatLanguageModel chatLanguageModel= VertexAiChatModel.builder()
                .project(GCP_PROJECT_ID)
                .location(GCP_PROJECT_LOCATION)
                .modelName("gemini-pro")
                .build();

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(300, 0))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(document);

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5) // let's get more results
                .build();

        // To register and get a free API key for Cohere, please visit the following link:
        // https://dashboard.cohere.com/welcome/register
        ScoringModel scoringModel = CohereScoringModel.withApiKey(System.getenv("COHERE_API_KEY"));

        ContentAggregator contentAggregator = ReRankingContentAggregator.builder()
                .scoringModel(scoringModel)
                .minScore(0.8) // we want to present the LLM with only the truly relevant segments for the user's query
                .build();

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .contentAggregator(contentAggregator)
                .build();

        return AiServices.builder(Assistant.class)
                .chatLanguageModel(chatLanguageModel)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}
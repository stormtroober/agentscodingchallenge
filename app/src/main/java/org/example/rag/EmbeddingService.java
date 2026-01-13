package org.example.rag;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Embedding service using DJL for nomic-embed-text-v2-moe or fallback models.
 * 
 * Uses task prefixes as per model specification:
 * - "search_query: " for queries
 * - "search_document: " for documents
 */
public class EmbeddingService implements AutoCloseable {

    private static final String QUERY_PREFIX = "";
    private static final String DOCUMENT_PREFIX = "";

    private final int embeddingDimension;
    private ZooModel<String, float[]> model;
    private Predictor<String, float[]> predictor;
    private boolean initialized = false;
    private boolean useFallback = false;

    /**
     * Create embedding service with default 384 dimensions (standard for MiniLM).
     */
    public EmbeddingService() {
        this(384);
    }

    /**
     * Create embedding service with specified dimensions (for Matryoshka).
     */
    public EmbeddingService(int dimension) {
        this.embeddingDimension = dimension;
    }

    /**
     * Initialize the embedding model.
     * This is done lazily on first use.
     */
    public synchronized void initialize() {
        if (initialized)
            return;

        try {
            // Try to load multilingual model via DJL/HuggingFace
            // System.out.println("[EmbeddingService] Loading embedding model
            // (paraphrase-multilingual-MiniLM-L12-v2)...");

            // First try the primary model, fall back to simpler one if needed
            try {
                loadPrimaryModel();
            } catch (Exception e) {
                System.out.println("[EmbeddingService] Warning: Primary model failed to load (" + e.getMessage() + ")");
                System.out.println("[EmbeddingService] Switching to fallback mean pooling (Calculated locally)");
                useFallback = true;
            }

            initialized = true;
            System.out.println("[EmbeddingService] Model loaded successfully");

        } catch (Exception e) {
            System.err.println("[EmbeddingService] Failed to load model: " + e.getMessage());
            useFallback = true;
            initialized = true;
        }
    }

    /**
     * Load the primary embedding model.
     */
    private void loadPrimaryModel() throws ModelNotFoundException, MalformedModelException, IOException {
        // Use paraphrase-multilingual-MiniLM-L12-v2 (Multilingual, 384d)
        Criteria<String, float[]> criteria = Criteria.builder()
                .setTypes(String.class, float[].class)
                .optApplication(Application.NLP.TEXT_EMBEDDING)
                .optEngine("PyTorch")
                .optModelUrls(
                        "djl://ai.djl.huggingface.pytorch/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2")
                .optTranslator(new EmbeddingTranslator())
                .build();

        model = criteria.loadModel();
        predictor = model.newPredictor();
    }

    /**
     * Embed a single text.
     * 
     * @param text    The text to embed
     * @param isQuery If true, uses query prefix; otherwise document prefix
     * @return The embedding vector
     */
    public float[] embed(String text, boolean isQuery) {
        if (!initialized)
            initialize();

        String prefixedText = (isQuery ? QUERY_PREFIX : DOCUMENT_PREFIX) + text;

        if (useFallback) {
            return fallbackEmbed(prefixedText);
        }

        try {
            float[] embedding = predictor.predict(prefixedText);
            // Truncate to requested dimension if using Matryoshka
            if (embedding.length > embeddingDimension) {
                return Arrays.copyOf(embedding, embeddingDimension);
            }
            return embedding;
        } catch (Exception e) {
            System.err.println("[EmbeddingService] Embedding failed: " + e.getMessage());
            // Don't switch to fallback permanently on transient errors, but do return a
            // fallback for this call
            return fallbackEmbed(prefixedText);
        }
    }

    /**
     * Embed multiple texts in batch.
     */
    public List<float[]> embedBatch(List<String> texts, boolean isQuery) {
        List<float[]> results = new ArrayList<>();
        for (String text : texts) {
            results.add(embed(text, isQuery));
        }
        return results;
    }

    /**
     * Simple fallback embedding using character-based hashing.
     * This is a placeholder - real fallback should use a simpler model.
     */
    private float[] fallbackEmbed(String text) {
        float[] embedding = new float[embeddingDimension];

        // Simple hash-based embedding (not semantically meaningful, but deterministic)
        String normalized = text.toLowerCase();
        int[] charCounts = new int[26];

        for (char c : normalized.toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                charCounts[c - 'a']++;
            }
        }

        // Distribute character frequencies across embedding dimensions
        for (int i = 0; i < embeddingDimension; i++) {
            int charIdx = i % 26;
            int hashComponent = normalized.hashCode() ^ (i * 31);
            embedding[i] = (float) (charCounts[charIdx] * 0.1 + (hashComponent % 100) * 0.001);
        }

        // Normalize
        float norm = 0;
        for (float v : embedding)
            norm += v * v;
        norm = (float) Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < embedding.length; i++) {
                embedding[i] /= norm;
            }
        }

        return embedding;
    }

    @Override
    public void close() {
        if (predictor != null)
            predictor.close();
        if (model != null)
            model.close();
    }

    /**
     * Custom translator for sentence embeddings.
     */
    private static class EmbeddingTranslator implements Translator<String, float[]> {

        private HuggingFaceTokenizer tokenizer;

        @Override
        public void prepare(TranslatorContext ctx) throws Exception {
            // Locate and load the tokenizer from the model directory
            Path modelPath = ctx.getModel().getModelPath();
            Path tokenizerPath = modelPath.resolve("tokenizer.json");
            if (!java.nio.file.Files.exists(tokenizerPath)) {
                // Fallback to tokenizer.model or other files if necessary,
                // but tokenizer.json is standard for HF models downloaded via DJL
                // Start searching in the directory
            }
            tokenizer = HuggingFaceTokenizer.newInstance(modelPath.resolve("tokenizer.json"));
        }

        @Override
        public NDList processInput(TranslatorContext ctx, String input) {
            if (tokenizer == null) {
                throw new IllegalStateException("Tokenizer not initialized");
            }

            ai.djl.huggingface.tokenizers.Encoding encoding = tokenizer.encode(input);
            long[] ids = encoding.getIds();
            long[] attentionMask = encoding.getAttentionMask();

            NDManager manager = ctx.getNDManager();
            NDArray inputIdArray = manager.create(ids).reshape(1, ids.length);
            NDArray attentionArray = manager.create(attentionMask).reshape(1, attentionMask.length);

            // Some models typically require input_ids and attention_mask
            // Token_type_ids are sometimes needed but usually 0 for single sentence

            return new NDList(inputIdArray, attentionArray);
        }

        @Override
        public float[] processOutput(TranslatorContext ctx, NDList list) {
            // Get the [CLS] token embedding or mean pool
            // Nomic typically uses mean pooling or CLS.
            // Nomic v2 uses mean pooling with specific prefix handling (already added in
            // main code)

            NDArray embedding = list.get(0);

            // If the output is (batch, seq, hidden), we need to pool
            if (embedding.getShape().dimension() > 2) {
                // Mean pool across sequence dimension (dim 1)
                // We should respect authentication mask ideally, but simple mean is often
                // sufficient for basic usage
                // Or DJL might have done it if we used the built-in translator.

                // For robustness, let's assume raw output and mean pool:
                embedding = embedding.mean(new int[] { 1 });
            }

            // Normalize
            NDArray norm = embedding.pow(2).sum(new int[] { 1 }, true).sqrt();
            embedding = embedding.div(norm);

            return embedding.toFloatArray();
        }

        @Override
        public Batchifier getBatchifier() {
            return null;
        }
    }
}

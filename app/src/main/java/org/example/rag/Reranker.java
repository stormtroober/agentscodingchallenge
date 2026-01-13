package org.example.rag;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Reranker using cross-encoder scoring.
 * 
 * For production, this would use a cross-encoder model like:
 * - ms-marco-MiniLM-L-6-v2
 * - bge-reranker-base
 * 
 * Currently implements a simplified TF-IDF based reranking as fallback.
 */
public class Reranker {

    /**
     * Rerank candidates based on relevance to query.
     * 
     * @param query      The search query
     * @param candidates List of candidate chunks to rerank
     * @param topK       Number of results to return
     * @return Reranked list with updated scores
     */
    public List<ScoredChunk> rerank(String query, List<ScoredChunk> candidates, int topK) {
        if (candidates.isEmpty())
            return candidates;

        // Simplified reranker: Just return top K from previous stage (RRF/Vector)
        // since the lexical reranking fails for multilingual queries (term mismatch).
        // In a real production system, use a Cross-Encoder here.
        return candidates.stream()
                .limit(topK)
                .collect(Collectors.toList());
    }

}

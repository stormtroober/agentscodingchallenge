package org.example.rag;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.util.*;

/**
 * BM25 search index using Apache Lucene.
 * Provides lexical/keyword-based retrieval.
 */
public class BM25Index {

    private final Directory directory;
    private final StandardAnalyzer analyzer;
    private IndexWriter writer;
    private DirectoryReader reader;
    private IndexSearcher searcher;

    public BM25Index() {
        this.directory = new ByteBuffersDirectory();
        this.analyzer = new StandardAnalyzer();

        try {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            this.writer = new IndexWriter(directory, config);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create Lucene index", e);
        }
    }

    /**
     * Add a document chunk to the index.
     */
    public void addDocument(String id, String content, String source) {
        try {
            Document doc = new Document();
            doc.add(new StringField("id", id, Field.Store.YES));
            doc.add(new TextField("content", content, Field.Store.YES));
            doc.add(new StringField("source", source, Field.Store.YES));
            writer.addDocument(doc);
        } catch (IOException e) {
            throw new RuntimeException("Failed to add document to index", e);
        }
    }

    /**
     * Add multiple chunks to the index.
     */
    public void addChunks(List<Chunk> chunks) {
        for (Chunk chunk : chunks) {
            addDocument(chunk.id(), chunk.content(), chunk.source());
        }
        commit();
    }

    /**
     * Commit changes and refresh searcher.
     */
    public void commit() {
        try {
            writer.commit();
            refreshSearcher();
        } catch (IOException e) {
            throw new RuntimeException("Failed to commit index", e);
        }
    }

    /**
     * Refresh the searcher after index updates.
     */
    private void refreshSearcher() throws IOException {
        if (reader != null) {
            reader.close();
        }
        reader = DirectoryReader.open(directory);
        searcher = new IndexSearcher(reader);
    }

    /**
     * Search for documents matching the query.
     * 
     * @param queryText The search query
     * @param topK      Number of results to return
     * @return List of scored chunks
     */
    public List<ScoredChunk> search(String queryText, int topK) {
        if (searcher == null) {
            try {
                refreshSearcher();
            } catch (IOException e) {
                return Collections.emptyList();
            }
        }

        try {
            // Escape special Lucene characters and parse query
            String escapedQuery = QueryParser.escape(queryText);
            QueryParser parser = new QueryParser("content", analyzer);
            Query query = parser.parse(escapedQuery);

            TopDocs topDocs = searcher.search(query, topK);
            List<ScoredChunk> results = new ArrayList<>();

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.storedFields().document(scoreDoc.doc);
                results.add(new ScoredChunk(
                        doc.get("id"),
                        doc.get("content"),
                        doc.get("source"),
                        scoreDoc.score));
            }

            return results;

        } catch (Exception e) {
            System.err.println("Search failed: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Search with source filter.
     */
    public List<ScoredChunk> search(String queryText, int topK, String sourceFilter) {
        if (searcher == null) {
            try {
                refreshSearcher();
            } catch (IOException e) {
                return Collections.emptyList();
            }
        }

        try {
            String escapedQuery = QueryParser.escape(queryText);
            QueryParser parser = new QueryParser("content", analyzer);
            Query contentQuery = parser.parse(escapedQuery);

            // Add source filter
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            builder.add(contentQuery, BooleanClause.Occur.MUST);
            builder.add(new TermQuery(new Term("source", sourceFilter)), BooleanClause.Occur.MUST);

            TopDocs topDocs = searcher.search(builder.build(), topK);
            List<ScoredChunk> results = new ArrayList<>();

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.storedFields().document(scoreDoc.doc);
                results.add(new ScoredChunk(
                        doc.get("id"),
                        doc.get("content"),
                        doc.get("source"),
                        scoreDoc.score));
            }

            return results;

        } catch (Exception e) {
            System.err.println("Search failed: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get total number of indexed documents.
     */
    public int getDocumentCount() {
        try {
            if (reader == null) {
                refreshSearcher();
            }
            return reader.numDocs();
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Close the index.
     */
    public void close() {
        try {
            if (reader != null)
                reader.close();
            if (writer != null)
                writer.close();
        } catch (IOException e) {
            // Ignore close errors
        }
    }
}

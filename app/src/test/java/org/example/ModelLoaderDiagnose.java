package org.example;

import ai.djl.Application;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

public class ModelLoaderDiagnose {
    @Test
    public void diagnose() {
        // Filter logic: Skip if language-specific filters are active
        String itFilter = System.getProperty("test.id.it");
        String enFilter = System.getProperty("test.id.en");
        String generalFilter = System.getProperty("test.id");

        boolean languageFilterActive = (itFilter != null && !itFilter.equals("null") && !itFilter.isEmpty()) ||
                (enFilter != null && !enFilter.equals("null") && !enFilter.isEmpty()) ||
                (generalFilter != null && !generalFilter.equals("null") && !generalFilter.isEmpty());

        String ragFilter = System.getProperty("test.rag");
        boolean ragFilterActive = ragFilter != null && !ragFilter.equals("null") && !ragFilter.isEmpty();

        if (languageFilterActive && !ragFilterActive) {
            System.out.println(">>> SKIPPING ModelLoaderDiagnose (Language filter active)");
            Assumptions.assumeFalse(true, "Skipping ModelLoaderDiagnose because language filter is active");
        }

        System.out.println("Diagnosing model download failure...");
        try {
            Criteria<String, float[]> criteria = Criteria.builder()
                    .setTypes(String.class, float[].class)
                    .optApplication(Application.NLP.TEXT_EMBEDDING)
                    .optEngine("PyTorch")
                    .optModelUrls(
                            "djl://ai.djl.huggingface.pytorch/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2")
                    .build();

            System.out.println("Attempting to load model...");
            ZooModel<String, float[]> model = criteria.loadModel();
            System.out.println("Model loaded successfully!");
            model.close();
        } catch (Exception e) {
            System.out.println("\nERROR DETAILS:");
            e.printStackTrace();
        }
    }
}

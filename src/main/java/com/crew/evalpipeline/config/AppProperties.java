package com.crew.evalpipeline.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Evaluation evaluation = new Evaluation();
    private final Suggestion suggestion = new Suggestion();
    private final Judge judge = new Judge();

    public Evaluation getEvaluation() {
        return evaluation;
    }

    public Suggestion getSuggestion() {
        return suggestion;
    }

    public Judge getJudge() {
        return judge;
    }

    public static class Evaluation {
        private int maxJobClaimSize = 10;
        private long jobPollDelayMs = 5000;
        private int maxRetries = 3;
        private long latencyThresholdMs = 1000;
        private double lowConfidenceThreshold = 0.65;
        private double annotationAgreementThreshold = 0.60;
        private double divergenceThreshold = 0.25;
        private String evaluatorVersion = "v1";

        public int getMaxJobClaimSize() {
            return maxJobClaimSize;
        }

        public void setMaxJobClaimSize(int maxJobClaimSize) {
            this.maxJobClaimSize = maxJobClaimSize;
        }

        public long getJobPollDelayMs() {
            return jobPollDelayMs;
        }

        public void setJobPollDelayMs(long jobPollDelayMs) {
            this.jobPollDelayMs = jobPollDelayMs;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public long getLatencyThresholdMs() {
            return latencyThresholdMs;
        }

        public void setLatencyThresholdMs(long latencyThresholdMs) {
            this.latencyThresholdMs = latencyThresholdMs;
        }

        public double getLowConfidenceThreshold() {
            return lowConfidenceThreshold;
        }

        public void setLowConfidenceThreshold(double lowConfidenceThreshold) {
            this.lowConfidenceThreshold = lowConfidenceThreshold;
        }

        public double getAnnotationAgreementThreshold() {
            return annotationAgreementThreshold;
        }

        public void setAnnotationAgreementThreshold(double annotationAgreementThreshold) {
            this.annotationAgreementThreshold = annotationAgreementThreshold;
        }

        public double getDivergenceThreshold() {
            return divergenceThreshold;
        }

        public void setDivergenceThreshold(double divergenceThreshold) {
            this.divergenceThreshold = divergenceThreshold;
        }

        public String getEvaluatorVersion() {
            return evaluatorVersion;
        }

        public void setEvaluatorVersion(String evaluatorVersion) {
            this.evaluatorVersion = evaluatorVersion;
        }
    }

    public static class Suggestion {
        private int minimumPatternFrequency = 2;

        public int getMinimumPatternFrequency() {
            return minimumPatternFrequency;
        }

        public void setMinimumPatternFrequency(int minimumPatternFrequency) {
            this.minimumPatternFrequency = minimumPatternFrequency;
        }
    }

    public static class Judge {
        private String provider = "mock";
        private final OpenAi openai = new OpenAi();

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public OpenAi getOpenai() {
            return openai;
        }
    }

    public static class OpenAi {
        private String baseUrl;
        private String model;
        private String apiKey;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }
}

package org.grouplens.samantha.server.config;

public enum ConfigKey {

    SAMANTHA_BASE("samantha"),
    ELASTICSEARCH_HOST("samantha.elasticSearch.host"),
    ELASTICSEARCH_PORT("samantha.elasticSearch.port"),
    ELASTICSEARCH_CLUSTER_NAME("samantha.elasticSearch.clusterName"),
    REDIS_HOST("samantha.redis.host"),
    REDIS_PORT("samantha.redis.port"),
    REDIS_DBID("samantha.redis.dbid"),
    ENGINES_ENABLED("samantha.engines.enabled"),
    ENGINE_TYPE("engineType"),
    ENGINE_COMPONENT_NAME("name"),
    ENGINE_COMPONENT_CONFIG_CLASS("configClass"),
    PREDICTOR_FEATURIZER_CONFIG("featurizer"),
    BASIC_FEATURE_EXTRACTOR_LIST_CONFIG("featureExtractorList"),
    FEATURE_EXTRACTOR_CONFIG_CLASS("extractorConfigClass"),
    DAO_CONFIG_CLASS("daoConfigClass"),
    ENTITY_DAOS_CONFIG("entityDaosConfig"),
    METRIC_CONFIG_CLASS("metricConfigClass"),
    REQUEST_CONTEXT("requestContext"),
    EVALUATOR_ENGINE_NAME("engine"),
    EVALUATOR_COMPONENT_PARA("componentPara"),
    EVALUATOR_METRIC_NAME("metricName"),
    EVALUATOR_METRIC_PARA("metricPara"),
    EVALUATOR_METRIC_VALUE("metricValue"),
    EXPANDERS_CONFIG("expandersConfig"),
    EXPANDER_CLASS("expanderClass"),
    STATE_PROBABILITY_NAME("prob"),
    TRANSITIONER_CLASS("transitionerClass"),
    RANKER_PAGE("page"),
    RANKER_PAGE_SIZE("pageSize"),
    RANKER_OFFSET("offset"),
    RANKER_LIMIT("limit"),
    MODEL_OPERATION_KEY("modelOperation"),
    MODEL_NAME_KEY("modelName"),
    METHOD_CLASS("methodClass"),
    ;
    private final String key;

    ConfigKey(String key) {
        this.key = key;
    }

    public String get() {
        return key;
    }
}

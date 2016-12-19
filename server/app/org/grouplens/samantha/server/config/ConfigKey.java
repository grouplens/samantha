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
    CSV_FILE_SERVICE_SEPARATOR("samantha.csvFileService.separator"),
    CSV_FILE_SERVICE_DATA_DIRS("samantha.csvFileService.dataDirs"),
    CSV_FILE_SERVICE_DIR_PATTERN("samantha.csvFileService.dirPattern"),
    CSV_FILE_SERVICE_MAX_WRITER("samantha.csvFileService.maxWriter"),
    ENGINE_NAME("engine"),
    ENGINE_TYPE("engineType"),
    ENGINE_COMPONENT_NAME("name"),
    ENGINE_COMPONENT_TYPE("componentType"),
    ENGINE_COMPONENT_CONFIG("config"),
    ENGINE_COMPONENT_CONFIG_CLASS("configClass"),
    ENGINE_COMPONENT_VERBOSE("verbose"),
    PREDICTOR_FEATURIZER_CONFIG("featurizer"),
    BASIC_FEATURE_EXTRACTOR_LIST_CONFIG("featureExtractorList"),
    FEATURE_EXTRACTOR_CONFIG_CLASS("extractorConfigClass"),
    DAO_CONFIG_CLASS("daoConfigClass"),
    ENTITY_DAOS_CONFIG("entityDaosConfig"),
    METRIC_CONFIG_CLASS("metricConfigClass"),
    REQUEST_CONTEXT("requestContext"),
    EVALUATOR_ENGINE_NAME("engine"),
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
    MODEL_OPERATION("modelOperation"),
    MODEL_NAME("modelName"),
    MODEL_FILE("modelFile"),
    MODEL_EVALUATING_PREFIX("evaluatingModel"),
    METHOD_CLASS("methodClass"),
    OBJECTIVE_CLASS("objectiveClass"),
    DATA_SUBSCRIBERS("dataSubscribers")
    ;
    private final String key;

    ConfigKey(String key) {
        this.key = key;
    }

    public String get() {
        return key;
    }
}

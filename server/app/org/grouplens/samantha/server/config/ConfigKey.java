/*
 * Copyright (c) [2016-2017] [University of Minnesota]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
    ENGINE_BEFORE_START_SCHEDULERS("beforeStartSchedulers"),
    PREDICTOR_FEATURIZER_CONFIG("featurizer"),
    BASIC_FEATURE_EXTRACTOR_LIST_CONFIG("featureExtractorList"),
    FEATURE_EXTRACTOR_CONFIG_CLASS("extractorConfigClass"),
    DAO_CONFIG_CLASS("daoConfigClass"),
    ENTITY_DAOS_CONFIG("entityDaosConfig"),
    ENTITY_DAO_NAME_KEY("entityDaoName"),
    REQUEST_ENTITY_DAO_NAME("RequestEntityDAO"),
    REQUEST_ENTITY_DAO_ENTITIES_KEY("entities"),
    METRIC_CONFIG_CLASS("metricConfigClass"),
    REQUEST_CONTEXT("requestContext"),
    EVALUATOR_ENGINE_NAME("engine"),
    EVALUATOR_METRIC_NAME("metricName"),
    EVALUATOR_METRIC_PARA("metricPara"),
    EVALUATOR_METRIC_VALUE("metricValue"),
    EVALUATOR_METRIC_SUPPORT("metricSupport"),
    EXPANDERS_CONFIG("expandersConfig"),
    POST_EXPANDERS_CONFIG("postExpandersConfig"),
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
    DATA_SUBSCRIBERS("dataSubscribers"),
    DATA_OPERATION("dataOperation"),
    LABEL_INDEX_NAME("CLASS"),
    INDEX_DATA("indexData"),
    ;
    private final String key;

    ConfigKey(String key) {
        this.key = key;
    }

    public String get() {
        return key;
    }
}

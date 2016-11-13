package org.grouplens.samantha.server.ranker;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.predictor.Predictor;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class PredictorBasedRankerConfig implements RankerConfig {
    private final Injector injector;
    private final int pageSize;
    private final String predictorName;
    private final List<Configuration> expandersConfig;
    private final Configuration config;

    private PredictorBasedRankerConfig(Configuration config, int pageSize, String predictorName,
                                       List<Configuration> expandersConfig, Injector injector) {
        this.pageSize = pageSize;
        this.predictorName = predictorName;
        this.injector = injector;
        this.expandersConfig = expandersConfig;
        this.config = config;
    }

    public static RankerConfig getRankerConfig(Configuration rankerConfig,
                                               Injector injector) {
        int pageSize = RankerUtilities.defaultPageSize;
        if (rankerConfig.asMap().containsKey(ConfigKey.RANKER_PAGE_SIZE.get())) {
            pageSize = rankerConfig.getInt(ConfigKey.RANKER_PAGE_SIZE.get());
        }
        String predictorName = rankerConfig.getString("predictor");
        List<Configuration> expanders = ExpanderUtilities.getEntityExpandersConfig(rankerConfig);
        return new PredictorBasedRankerConfig(rankerConfig, pageSize, predictorName, expanders, injector);
    }

    public Ranker getRanker(RequestContext requestContext) {
        SamanthaConfigService configService = injector.instanceOf(
                SamanthaConfigService.class);
        Predictor predictor = configService.getPredictor(predictorName, requestContext);
        JsonNode reqBody = requestContext.getRequestBody();
        int page = JsonHelpers.getOptionalInt(reqBody,
                ConfigKey.RANKER_PAGE.get(), 1);
        int offset = JsonHelpers.getOptionalInt(reqBody,
                ConfigKey.RANKER_OFFSET.get(), (page - 1) * pageSize);
        int limit = JsonHelpers.getOptionalInt(reqBody, ConfigKey.RANKER_LIMIT.get(), pageSize);
        List<EntityExpander> entityExpanders = ExpanderUtilities.getEntityExpanders(requestContext,
                expandersConfig, injector);
        return new PredictorBasedRanker(predictor, pageSize, offset, limit, entityExpanders, config);
    }
}

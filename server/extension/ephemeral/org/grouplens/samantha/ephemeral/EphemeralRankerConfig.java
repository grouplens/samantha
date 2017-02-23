package org.grouplens.samantha.ephemeral;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.common.ModelService;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.exception.ConfigurationException;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.ranker.Ranker;
import org.grouplens.samantha.server.ranker.RankerConfig;
import play.Configuration;
import play.inject.Injector;

import java.util.*;
import java.util.stream.Collectors;

public class EphemeralRankerConfig implements RankerConfig {
    private final Configuration config;
    private final Injector injector;
    private final String svdfeaturePredictor;
    private final String svdfeatureModel;
    private final Map<Integer, Double> preferenceWeights;
    private final Map<Integer, List<SelectionCriteria>> selectionCriteriaMap;
    private final Map<Integer, Integer> numRoundsToExclude;

    public Ranker getRanker(RequestContext requestContext) {
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        ModelService modelService = injector.instanceOf(ModelService.class);
        configService.getPredictor(svdfeaturePredictor, requestContext);
        SVDFeature svdFeature = (SVDFeature) modelService.getModel(requestContext.getEngineName(), svdfeatureModel);

        // Get the experimental conditions and seed value from the request
        JsonNode reqBody = requestContext.getRequestBody();
        Map<String, Integer> expt = JsonHelpers.getRequiredStringToIntegerMap(reqBody, "expt");
        long seed = JsonHelpers.getRequiredLong(reqBody, "seed");

        return new EphemeralRanker(config,
                svdFeature,
                preferenceWeights,
                selectionCriteriaMap,
                numRoundsToExclude,
                expt,
                seed);
    }

    private EphemeralRankerConfig(Configuration config,
                                  Injector injector,
                                  String svdfeaturePredictor,
                                  String svdfeatureModel,
                                  Map<Integer, Double> preferenceWeights,
                                  Map<Integer, List<SelectionCriteria>> selectionCriteriaMap,
                                  Map<Integer, Integer> numRoundsToExclude) {
        this.injector = injector;
        this.config = config;
        this.svdfeatureModel = svdfeatureModel;
        this.svdfeaturePredictor = svdfeaturePredictor;
        this.preferenceWeights = preferenceWeights;
        this.selectionCriteriaMap = selectionCriteriaMap;
        this.numRoundsToExclude = numRoundsToExclude;
    }

    public static RankerConfig getRankerConfig(Configuration rankerConfig,
                                               Injector injector) {

        String svdfeaturePredictor = rankerConfig.getString("svdfeaturePredictor");
        String svdfeatureModel = rankerConfig.getString("svdfeatureModel");

        /*
         * PREFERENCE WEIGHTS & NUM ROUNDS TO EXCLUDE
         */
        // TODO: Check ranker round parsing...
        List<Integer> preferences = rankerConfig.getIntList("preferences");

        Configuration preferenceWeightsConfig = rankerConfig.getConfig("preferenceWeights");
        Map<Integer, Double> preferenceWeights = preferences.stream().collect(Collectors.toMap(
                p -> p,
                p -> preferenceWeightsConfig.getDouble(p.toString(), 0.0)
        ));

        Configuration numRoundsToExcludeConfig = rankerConfig.getConfig("numRoundsToExclude");
        Map<Integer, Integer> numRoundsToExclude = preferences.stream().collect(Collectors.toMap(
                p -> p,
                p -> numRoundsToExcludeConfig.getInt(p.toString(), -1)
        ));

        /*
         * SELECTION CRITERIA
         */
        Map<Integer, List<SelectionCriteria>> selectionCriteriaMap = new HashMap<>();
        Configuration selectionCriteriaMapConfig = rankerConfig.getConfig("selectionCriteriaByRound");

        for (String key: selectionCriteriaMapConfig.keys()) {
            List<SelectionCriteria> selectionCriteriaList = new ArrayList<>();
            List<Configuration> selectionCriteriaListConfig = selectionCriteriaMapConfig.getConfigList(key);

            for (Configuration selectionCriteriaConfig : selectionCriteriaListConfig) {
                int limit = selectionCriteriaConfig.getInt("limit");
                int n = selectionCriteriaConfig.getInt("n");
                double ratedDropout = selectionCriteriaConfig.getDouble("ratedDropout", 0.0);
                double dropout = selectionCriteriaConfig.getDouble("dropout", 0.0);

//                // TODO: These could be a Map<String, Double> that we don't have to manually specify
//                double itemBiasWeighting = selectionCriteriaConfig.getDouble("itemBiasWeighting", 0.0);
//                double logSupportWeighting = selectionCriteriaConfig.getDouble("logSupportWeighting", 0.0);
//                double avgRatingWeighting = selectionCriteriaConfig.getDouble("avgRatingWeighting", 0.0);
//                double logNumRatingsWeighting = selectionCriteriaConfig.getDouble("logNumRatingsWeighting", 0.0);
//                double halflife15YearsWeighting = selectionCriteriaConfig.getDouble("halflife15YearsWeighting", 0.0);

                selectionCriteriaList.add(new SelectionCriteria(limit, n, ratedDropout, dropout));
            }

            if (key.equals("default")) {
                selectionCriteriaMap.put(-1, selectionCriteriaList);
            } else {
                try {
                    int intKey = Integer.parseInt(key);
                    if (intKey < 1) { throw new NumberFormatException(); }
                    selectionCriteriaMap.put(intKey, selectionCriteriaList);
                } catch (NumberFormatException e) {
                    throw new ConfigurationException("selectionCriteriaByRound keys must be positive integers or \"default\"");
                }

            }
        }

        return new EphemeralRankerConfig(rankerConfig,
                injector,
                svdfeaturePredictor,
                svdfeatureModel,
                preferenceWeights,
                selectionCriteriaMap,
                numRoundsToExclude);
    }
}

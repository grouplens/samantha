package org.grouplens.samantha.ephemeral;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.common.ModelService;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.exception.ConfigurationException;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.predictor.Predictor;
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
    private final double revertToMeanConstant;
    private final double revertToMeanFraction;
    private final int minNumRecentRatings;
    private final int maxNumRecentRatings;
    private final double excludeRecentRatingsBelow;
    private final Map<Integer, Double> preferenceWeights;
    private final Map<Integer, List<SelectionCriteria>> selectionCriteriaMap;
    private final Map<Integer, Integer> numRoundsToExclude;

    public Ranker getRanker(RequestContext requestContext) {
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        ModelService modelService = injector.instanceOf(ModelService.class);
        Predictor predictor = configService.getPredictor(svdfeaturePredictor, requestContext);
        SVDFeature svdFeature = (SVDFeature) modelService.getModel(requestContext.getEngineName(), svdfeatureModel);

        // Get the experimental conditions and seed value from the request
        JsonNode reqBody = requestContext.getRequestBody();
        Map<String, Integer> expt = JsonHelpers.getRequiredStringToIntegerMap(reqBody, "expt");
        long seed = JsonHelpers.getRequiredLong(reqBody, "seed");

        return new EphemeralRanker(config,
                predictor,
                svdFeature,
                revertToMeanConstant,
                revertToMeanFraction,
                minNumRecentRatings,
                maxNumRecentRatings,
                excludeRecentRatingsBelow,
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
                                  Double revertToMeanConstant,
                                  Double revertToMeanFraction,
                                  Integer minNumRecentRatings,
                                  Integer maxNumRecentRatings,
                                  Double excludeRecentRatingsBelow,
                                  Map<Integer, Double> preferenceWeights,
                                  Map<Integer, List<SelectionCriteria>> selectionCriteriaMap,
                                  Map<Integer, Integer> numRoundsToExclude) {
        this.injector = injector;
        this.config = config;
        this.svdfeatureModel = svdfeatureModel;
        this.svdfeaturePredictor = svdfeaturePredictor;
        this.revertToMeanConstant = revertToMeanConstant;
        this.revertToMeanFraction = revertToMeanFraction;
        this.minNumRecentRatings = minNumRecentRatings;
        this.maxNumRecentRatings = maxNumRecentRatings;
        this.excludeRecentRatingsBelow = excludeRecentRatingsBelow;
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
        Double revertToMeanConstant = rankerConfig.getDouble("revertToMeanConstant", 0.0);
        Double revertToMeanFraction = rankerConfig.getDouble("revertToMeanFraction", 0.0);
        Integer minNumRecentRatings = rankerConfig.getInt("minNumRecentRatings", 1);
        Integer maxNumRecentRatings = rankerConfig.getInt("maxNumRecentRatings", 5);
        Double excludeRecentRatingsBelow = rankerConfig.getDouble("excludeRecentRatingsBelow", 4.0);

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

            int nTotal = 0;
            for (Configuration selectionCriteriaConfig : selectionCriteriaListConfig) {
                int n = selectionCriteriaConfig.getInt("n");
                nTotal += n;

                String diversityMetric = selectionCriteriaConfig.getString("diversityMetric");
                String similarityMetric = selectionCriteriaConfig.getString("similarityMetric");
                double excludeBelow = selectionCriteriaConfig.getDouble("excludeBelow", 0.0);
                int limit = selectionCriteriaConfig.getInt("limit", 0);
                double ratedDropout = selectionCriteriaConfig.getDouble("ratedDropout", 0.0);
                double dropout = selectionCriteriaConfig.getDouble("dropout", 0.0);
                int nthMostDistant = selectionCriteriaConfig.getInt("nthMostDistant", 1);

                selectionCriteriaList.add(new SelectionCriteria(n, similarityMetric, diversityMetric, excludeBelow, limit, ratedDropout, dropout, nthMostDistant));
            }

            if (nTotal != 10) {
                throw new ConfigurationException("round " + key + " must select 10 items total, not " + nTotal);
            }

            try {
                int intKey = Integer.parseInt(key);
                if (intKey < 0) { throw new NumberFormatException(); }
                selectionCriteriaMap.put(intKey, selectionCriteriaList);
            } catch (NumberFormatException e) {
                throw new ConfigurationException("selectionCriteriaByRound keys must be positive integers");
            }
        }
        if (!selectionCriteriaMap.containsKey(1)) {
            throw new ConfigurationException("selectionCriteriaByRound must contain key 1");
        }

        return new EphemeralRankerConfig(rankerConfig,
                injector,
                svdfeaturePredictor,
                svdfeatureModel,
                revertToMeanConstant,
                revertToMeanFraction,
                minNumRecentRatings,
                maxNumRecentRatings,
                excludeRecentRatingsBelow,
                preferenceWeights,
                selectionCriteriaMap,
                numRoundsToExclude);
    }
}

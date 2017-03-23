package org.grouplens.samantha.ephemeral;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Ordering;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureKey;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.exception.ConfigurationException;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.predictor.Prediction;
import org.grouplens.samantha.server.ranker.AbstractRanker;
import org.grouplens.samantha.server.ranker.RankedResult;
import org.grouplens.samantha.server.retriever.RetrievedResult;
import org.grouplens.samantha.server.retriever.RetrieverUtilities;
import play.Configuration;
import play.Logger;
import play.libs.Json;

import java.text.DecimalFormat;
import java.text.Format;
import java.util.*;
import java.util.stream.Collectors;

public class EphemeralRanker extends AbstractRanker {
    private final SVDFeature svdFeature;
    private final double revertToMeanConstant;
    private final double revertToMeanFraction;
    private final Map<Integer, Double> preferenceWeights;
    private final Map<Integer, List<SelectionCriteria>> selectionCriteriaMap;
    private final Map<Integer, Integer> numRoundsToExclude;
    private final Map<String, Integer> expt;
    private Random random;
    private Format d = new DecimalFormat("#.###");
    private RealVector averageUserVector = null;

    public EphemeralRanker(
            Configuration config,
            SVDFeature svdFeature,
            double revertToMeanConstant,
            double revertToMeanFraction,
            Map<Integer, Double> preferenceWeights,
            Map<Integer, List<SelectionCriteria>> selectionCriteriaMap,
            Map<Integer, Integer> numRoundsToExclude,
            Map<String, Integer> expt,
            long seed) {
        super(config);
        this.svdFeature = svdFeature;
        this.revertToMeanConstant = revertToMeanConstant;
        this.revertToMeanFraction = revertToMeanFraction;
        this.preferenceWeights = preferenceWeights;
        this.selectionCriteriaMap = selectionCriteriaMap;
        this.numRoundsToExclude = numRoundsToExclude;
        this.expt = expt;
        this.random = new Random(seed);
    }


    private RealVector getUserVector(int movieId, RealVector defaultValue) {
        try {
            return getUserVector(movieId);
        } catch (BadRequestException e) {
            return defaultValue;
        }
    }
    private RealVector getUserVector(int userId) {
        return getVector("userId", userId);
    }
    private RealVector getMovieVector(int movieId, RealVector defaultValue) {
        try {
            return getMovieVector(movieId);
        } catch (BadRequestException e) {
            return defaultValue;
        }
    }
    private RealVector getMovieVector(int movieId) {
        return getVector("movieId", movieId);
    }
    private RealVector getVector(String type, int id) {
        String name = SVDFeatureKey.FACTORS.get();
        String key = FeatureExtractorUtilities.composeKey(
                type,
                Integer.toString(id)
        );

        if (!svdFeature.containsKey(name, key)) {
            throw new BadRequestException("No vector for " + type + " " + id + " in dataset");
        }

        int idx = svdFeature.getIndexForKey(name, key);
        return svdFeature.getVectorVarByNameIndex(name, idx);
    }

//    private double getUserBias(int userId) {
//        return getBias("userId", userId);
//    }
//    private double getMovieBias(int movieId) {
//        return getBias("movieId", movieId);
//    }
//    private double getBias(String type, int id) {
//        String name = SVDFeatureKey.BIASES.get();
//        String key = FeatureExtractorUtilities.composeKey(
//                type,
//                Integer.toString(id)
//        );
//
//        if (!svdFeature.containsKey(name, key)) {
//            throw new BadRequestException("No bias for " + type + " " + id + " in dataset");
//        }
//
//        int idx = svdFeature.getIndexForKey(name, key);
//        return svdFeature.getScalarVarByNameIndex(name, idx);
//    }
//
//    private double getUserSupport(int userId) {
//        return getSupport("userId", userId);
//    }
//    private double getMovieSupport(int movieId) {
//        return getSupport("movieId", movieId);
//    }
//    private double getSupport(String type, int id) {
//        String name = SVDFeatureKey.BIASES.get();
//        String key = FeatureExtractorUtilities.composeKey(
//                type,
//                Integer.toString(id)
//        );
//
//
//        if (!svdFeature.containsKey(name, key)) {
//            throw new BadRequestException("No support for " + type + " " + id + " in dataset");
//        }
//
//        int idx = svdFeature.getIndexForKey(name, key);
//        return svdFeature.getScalarVarByNameIndex(SVDFeatureKey.SUPPORT.get(), idx);
//    }

//    private RealVector computeDesiredVec2(RealVector seed, List<Map<Integer, List<Integer>>> roundsList) {
//
//        // Iterate over each round, adding the user's preferences to the
//        // "desired" vector and more heavily weighting more recent rounds.
//        int i = 0;
//
//        for (Map<Integer, List<Integer>> round : roundsList) {
//            double roundCount = 0;
//            RealVector roundVec = new ArrayRealVector(seed.getDimension());
//
//            Logger.info("Round {} Intermediate Vector Magnitude: {}", i, seed.getNorm());
//            i++;
//
//            // Each entry in round is a map from preference to list of movie ids
//            for (Map.Entry<Integer, List<Integer>> entry : round.entrySet()) {
//
//                // Get the weight for this preference
//                Double w = preferenceWeights.getOrDefault(entry.getKey(), 0.0);
//
//                // If the weight is zero, don't bother adding up the vectors.
//                if (w == 0.0) { continue; }
//
//                List<Integer> movies = entry.getValue();
//                RealVector prefVec = movies.stream()
//                        .map(x -> getMovieVector(x))
//                        .reduce(
//                                new ArrayRealVector(seed.getDimension()),
//                                (x, y) -> x.add(y)
//                        );
//
//                // Multiply by the configured weight for this preference value...
//                roundVec = roundVec.add(prefVec.mapMultiply(w));
//                roundCount += Math.abs(w) * movies.size();
//            }
//
//            // Average over all the movie vectors
//            roundVec.mapDivideToSelf(roundCount);
//
//            // Discount previous rounds by the following amount.
//            double discount;
//            if (halflife < 0.0) { // All rounds weighted equally,
//                discount = 1.0;
//            } else if (halflife == 0.0) { // Ignore any previous rounds
//                discount = 0.0;
//            } else { // Discount previous rounds according to the given halflife
//                discount = Math.pow(0.5, roundCount / halflife);
//            }
//
//            seed = seed.mapMultiply(discount).add(roundVec);
//
//        }
//        Logger.info("Desired Vector Magnitude: {}", seed.getNorm());
//
//        // Normalize vector to keep dot products reasonable.
//        seed.mapDivideToSelf(seed.getNorm());
//        return seed;
//    }
//
//    private RealVector computeDesiredVec1(RealVector seed, List<Map<Integer, List<Integer>>> roundsList) {
//        seed = seed.mapDivideToSelf(seed.getNorm()); // make seed into unit vector
//
//        // Iterate over each round, adding the user's preferences to the
//        // "desired" vector and more heavily weighting more recent rounds.
//        for (Map<Integer, List<Integer>> round : roundsList) {
//            double totalWeight = 0.0;
//            int numMovies = 0;
//            final RealVector roundSeed = seed;
//            RealVector roundVec = new ArrayRealVector(seed.getDimension(), 0.0);
//
//
//            double maxNorm = round.values().stream()
//                    .flatMap(x -> x.stream())
//                    .map(x -> getMovieVector(x))
//                    .map(v -> v.mapDivide(v.dotProduct(roundSeed)).subtract(roundSeed))
//                    .mapToDouble(v -> v.getNorm())
//                    .max().orElse(-1);
//            if (maxNorm < 0.0) { throw new BadRequestException(); }
//
//            // Each entry in round is a map from preference to list of movie ids
//            for (Map.Entry<Integer, List<Integer>> entry : round.entrySet()) {
//
//                // Get the weight for this preference. If it's zero, skip it.
//                double w = preferenceWeights.getOrDefault(entry.getKey(), 0.0);
//
//                // Scale movie vectors to be on the plane tangent to the unit sphere
//                // at the seed vector. Then take the difference between this and the
//                // seed vector. This gives us a vector parallel to the plane.
//                List<Integer> movies = entry.getValue();
//
//
//                RealVector prefVec;
//                if (w > 0.0) {
//                    // Project positive vectors on to the plane perpendicular
//                    // to the target vector, then average.
//                    prefVec = movies.stream()
//                            .map(x -> getMovieVector(x))
//                            .map(v -> v.mapDivide(v.dotProduct(roundSeed)).subtract(roundSeed))
//                            .reduce(
//                                    new ArrayRealVector(seed.getDimension()),
//                                    (x, y) -> x.add(y)
//                            );
//                }
//                else if (w < 0.0) {
//                    // Project negative vectors on to the plane perpendicular
//                    // to the target vector, scale to half of the average
//                    // length, then average.
//                    prefVec = movies.stream()
//                            .map(x -> getMovieVector(x))
//                            .map(v -> v.mapDivide(v.dotProduct(roundSeed)).subtract(roundSeed))
//                            .map(v -> v.mapMultiply(maxNorm / (v.getNorm())))
//                            .reduce(
//                                    new ArrayRealVector(seed.getDimension()),
//                                    (x, y) -> x.add(y)
//                            );
//                }
//                else {
//                    // If the preference group's weight is 0, we skip it!
//                    continue;
//                }
//
//                numMovies += movies.size();
//
//                // Whether each preference group's vector should be averaged.
//                // If it is, changes in the number of movies in a preference
//                // group shouldn't dramatically affect the magnitude.
//                if (averagePreferenceGroups) {
//                    roundVec = roundVec.add(prefVec.mapMultiply(w / movies.size()));
//                    totalWeight += Math.abs(w);
//                } else {
//                    roundVec = roundVec.add(prefVec.mapMultiply(w));
//                    totalWeight += Math.abs(w) * movies.size();
//                }
//                // Multiply by the configured weight for this preference value...
//
//            }
//
//            // TODO: Is it better to average rounds, or should we average each preference class, according to the number of movies in it?
//            // TODO: e.g. apply [numMovies / (7.0 / 9.0 * numMovies + 20.0 / 9.0)] to each pref class, then add them to the seed vector...?
//            if (!averageRounds) {
//                throw new UnsupportedOperationException("rounds must be averaged");
//            } else if (totalWeight > 0.0) { // If the user didn't provide any information, we go on to the next round.
//                // TODO: Should this be by numMovie, or by totalWeight?
//                roundVec.mapDivideToSelf(totalWeight); // Average
//                roundVec.mapMultiplyToSelf(numMovies / (8.0 / 9.0 * numMovies + 10.0 / 9.0)); // Weight 0.33–1.00
//
//                seed = seed.add(roundVec);
//                seed = seed.mapDivideToSelf(seed.getNorm());
//            }
//
//            Logger.info("cosine between this round and previous round: {}", seed.cosine(roundSeed));
//        }
//
//        return seed;
//    }

//    private RealVector computeVecFromPreferenceGroup(Map<Integer, List<Integer>> round) {
//        RealVector numerator = new ArrayRealVector(svdFeature.getFactDim(), 0.0);
//        Double denominator = 0.0;
//
//        for (Map.Entry<Integer, List<Integer>> entry : round.entrySet()) {
//            double w = preferenceWeights.getOrDefault(entry.getKey(), 0.0);
//            double ww = w * Math.sqrt(entry.getValue().size());
//            if (ww == 0) {
//                continue;
//            }
//
//            List<RealVector> vecs = entry.getValue().stream()
//                    .map(x -> getMovieVector(x))
//                    .collect(Collectors.toList());
//
//            RealVector prefVec = vecs.stream()
//                    .map(v -> v.mapMultiply(ww))
//                    .reduce((v1, v2) -> v1.add(v2))
//                    .get();
//
//            numerator = numerator.add(prefVec);
//            denominator += vecs.stream().mapToDouble(v -> v.getNorm() * Math.abs(ww)).sum();
//        }
//
//        if (denominator == 0.0) { return null;}
//        return numerator.mapDivide(denominator);
//    }

    private String realVectorToString(RealVector vec) {
        String[] arr = new String[vec.getDimension()];
        for (int i=0; i<vec.getDimension(); i++) {
            arr[i] = d.format(Double.valueOf(vec.getEntry(i)));
        }
        return StringUtils.join(arr, ",");
    }

    private RealVector getAverageUserVector() {
        if (averageUserVector == null) {
            List<Integer> indices = new ArrayList<>();

            int size = svdFeature.getKeyMapSize(SVDFeatureKey.FACTORS.get());
            for (int i = 0; i < size; i++) {
                String key = (String) svdFeature.getKeyForIndex(SVDFeatureKey.FACTORS.get(), i);
                if (key.startsWith("userId")) {
                    indices.add(i);
                } else if (!key.startsWith("movieId")) {
                    throw new ConfigurationException("encountered vector variable that didn't start with userId or movieId");
                }
            }

            if (indices.isEmpty()) {
                throw new ConfigurationException("No userId vectors found");
            }

            averageUserVector = indices.stream()
                    .map(index -> svdFeature.getVectorVarByNameIndex(SVDFeatureKey.FACTORS.get(), index))
                    .reduce((v1, v2) -> v1.add(v2))
                    .get().mapDivide(indices.size());

        }
        return averageUserVector;
    }

    private RealVector computeDesiredVec(RealVector initialVec, List<Map<Integer, List<Integer>>> roundsList) {
//        // For neutral starting points...
//        int i = 0;
//        while (initialVec == null && i < roundsList.size()) {
//            RealVector numerator = new ArrayRealVector(svdFeature.getFactDim(), 0.0);
//            Double denominator = 0.0;
//
//            for (Map.Entry<Integer, List<Integer>> entry : roundsList.get(i).entrySet()) {
//                double w = preferenceWeights.getOrDefault(entry.getKey(), 0.0);
//                double ww = w * Math.sqrt(entry.getValue().size());
//                if (ww == 0) { continue; }
//
//                List<RealVector> vecs = entry.getValue().stream()
//                        .map(x -> getMovieVector(x))
//                        .collect(Collectors.toList());
//
//                RealVector prefVec = vecs.stream()
//                        .map(v -> v.mapMultiply(ww))
//                        .reduce((v1, v2) -> v1.add(v2))
//                        .get();
//
//                numerator = numerator.add(prefVec);
//                denominator += vecs.stream().mapToDouble(v -> v.getNorm() * Math.abs(ww)).sum();
//            }
//            i++;
//
//            if (numerator == null || denominator == 0.0) {
//                Logger.info("Using a neutral starting point. Unable to construct intialSeed from round {}.", i);
//                continue;
//            }
//
//            initialVec = numerator.mapDivide(denominator);
//            Logger.info("Using neutral starting point. Constructed initialVec with magnitude {} after {} rounds.", d.format(initialVec.getNorm()), i);
//
//        }
//
//        if (initialVec == null) {
//            Logger.info("Using a neutral starting point. Unable to construct intialSeed after {} rounds.", i);
//            return null;
//        }

        RealVector currentVec = initialVec.mapDivide(initialVec.getNorm());
        RealVector defaultVec = getAverageUserVector();
        defaultVec = defaultVec.mapDivide(defaultVec.getNorm());

        // Iterate over each round, adding the user's preferences to the
        // "desired" vector and more heavily weighting more recent rounds.
        int i = 0;
        for (Map<Integer, List<Integer>> round : roundsList.subList(i, roundsList.size())) {
            i++;
            final RealVector roundSeed = currentVec;

            Map<Integer, RealVector> dVecMap = round.values().stream()
                    .flatMap(x -> x.stream())
                    .collect(Collectors.toMap(
                            x -> x,
                            x -> getMovieVector(x)
                                    .mapDivide(getMovieVector(x).dotProduct(roundSeed))
                                    .subtract(roundSeed)
                    ));

            // Get minimum and maximum vector magnitudes.
            List<Double> vectorNorms = dVecMap.values().stream()
                    .map(v -> v.getNorm())
                    .collect(Collectors.toList());

            double selectedNorm = 0.0;
            double totalNorm = vectorNorms.stream().mapToDouble(x -> x).sum();
            double maxNorm = vectorNorms.stream().mapToDouble(x -> x).max().orElse(-1.0);
            double minNorm = vectorNorms.stream().mapToDouble(x -> x).min().orElse(-1.0);
            if (maxNorm < 0.0 || minNorm < 0.0) { throw new BadRequestException(); }

            double distanceNumerator = 0.0;
            double distanceDenominator = 0.0;
            double movementNumerator = 0.0;
            double movementDenominator = 0.0;
            RealVector vNumerator = new ArrayRealVector(currentVec.getDimension(), 0.0);
            double vDenominator = 0.0;

            for (Map.Entry<Integer, List<Integer>> entry : round.entrySet()) {
                double w = preferenceWeights.getOrDefault(entry.getKey(), 0.0);
                double ww = w * Math.sqrt(entry.getValue().size());

                if (ww == 0.0) { continue; } // skip if weight is zero...

                // Construct differential vectors
                List<RealVector> dVecs = entry.getValue().stream()
                        .map(x -> dVecMap.get(x))
                        .collect(Collectors.toList());
                List<Double> dNorms = dVecs.stream().map(v -> v.getNorm()).collect(Collectors.toList());

                // Weight differential vectors and add them to the numerator
                RealVector prefVec = dVecs.stream()
                        .map(v -> v.mapMultiply(ww))
                        .reduce((v1, v2) -> v1.add(v2))
                        .get();
                vNumerator = vNumerator.add(prefVec);

                // Add the absolute value of the preferenceWeights to the denominator
                vDenominator += dNorms.stream().mapToDouble(n -> n * Math.abs(ww)).sum();


                // Calculate the movement pressure.
                if (ww > 0.0) {
                    // All positive vectors are given an equal weight of 1.
                    movementNumerator += Math.abs(w) * dNorms.stream().mapToDouble(x -> x).sum();
                    movementDenominator += Math.abs(w) * dVecs.size();

                    distanceNumerator += Math.abs(ww) * dNorms.stream().mapToDouble(x -> x).sum();
                    distanceDenominator += Math.abs(ww) * dVecs.size();
                } else { // ww < 0.0
                    // When minNorm is close to maxNorm, the weight of negative
                    // vectors will be close to 1, but all vectors will have
                    // similar magnitudes. When minNorm is substantially
                    // smaller than maxNorm, vectors will have a range of
                    // magnitudes, and the weight of distant negative vectors
                    // will be smaller than that of nearby negative vectors.
                    double totalNegWeight = dNorms.stream().mapToDouble(n -> Math.pow(0.1, (n - minNorm) / (2 * minNorm))).sum();
                    movementNumerator += Math.abs(w) * maxNorm * totalNegWeight;
                    movementDenominator += Math.abs(w) * totalNegWeight;

                    distanceNumerator += Math.abs(ww) * dNorms.stream().mapToDouble(x -> maxNorm + minNorm - x).sum();
                    distanceDenominator += Math.abs(ww) * dVecs.size();
                }

                // Add up the selected vector norms, so we can calculate a percentage...
                selectedNorm += dNorms.stream().mapToDouble(x -> x).sum();
            }

            if (vDenominator !=0) {
                RealVector direction = vNumerator.mapDivide(vDenominator);

                // TODO: Move more for earlier rounds?
                double movement = movementNumerator / movementDenominator;
                double confidence = Math.pow(selectedNorm / totalNorm, 0.25);
                double distance = distanceNumerator / distanceDenominator;


//                RealVector delta = direction.mapMultiply(movement * confidence);
//                RealVector delta = direction.mapMultiply(distance);
                RealVector delta = direction.mapMultiply(distance * confidence);

                currentVec = currentVec.add(delta);
                currentVec = currentVec.mapDivide(currentVec.getNorm());
            }

            double revertToMeanMultiplier = revertToMeanConstant;
            if (revertToMeanFraction != 0) {
                // getDistanceMetric("cosine", ...) will return a value between 0 and 1, where:
                // >>> 0 indicates the vectors are in the same direction;
                // >>> 0.5 indicates orthogonal vectors; and
                // >>> 1 indicates vectors in the opposite direction.
                revertToMeanMultiplier += getDistanceMetricScore("cosine", currentVec, defaultVec) * revertToMeanFraction;
            }

            // Add in a little bit of the average user vector and renormalize.
            if (revertToMeanMultiplier != 0) {
                Logger.info("before: Cosine of {} from avg user vec to current vec", d.format(currentVec.cosine(defaultVec)));
                currentVec = currentVec.mapMultiply(1.0 - revertToMeanMultiplier).add(defaultVec.mapMultiply(revertToMeanMultiplier));
                currentVec = currentVec.mapDivide(currentVec.getNorm());
                Logger.info("after: Cosine of {} from avg user vec to current vec", d.format(currentVec.cosine(defaultVec)));
            }

            if (i == 1) {
                Logger.info("Cosine of {} from user seed to round 1", d.format(currentVec.cosine(roundSeed)));
            } else {
                Logger.info("Cosine of {} from round {} to {}", d.format(currentVec.cosine(roundSeed)), i - 1, i);
            }
        }

        if (i > 1) {
            Logger.info("Cosine of {} from user seed to round {}", d.format(currentVec.cosine(initialVec)), i);
        }

        return currentVec;
    }

    private List<Map<Integer, List<Integer>>> parseRoundsList(JsonNode allRawRounds) {
        List<Map<Integer, List<Integer>>> allRounds = new ArrayList<>(allRawRounds.size());

        for (final JsonNode rawRound : allRawRounds) {
            Map<Integer, List<Integer>> round = preferenceWeights.keySet().stream()
                    .collect(Collectors.toMap(i -> i, i -> new ArrayList<>()));

            for (Iterator<Map.Entry<String, JsonNode>> it = rawRound.fields(); it.hasNext(); ) {
                try {
                    Map.Entry<String, JsonNode> entry = it.next();

                    int movieId = Integer.parseInt(entry.getKey());
                    int pref = entry.getValue().asInt();

                    if (!round.containsKey(pref)) {
                        throw new BadRequestException("preferences must be in " + round.keySet().toString());
                    }

                    round.get(pref).add(movieId);
                } catch (NumberFormatException e) {
                    throw new BadRequestException("movieIds must be integers");
                }
            }

            allRounds.add(round);
        }

        return allRounds;
    }

    private Set<Integer> getExclusions(List<Map<Integer, List<Integer>>> roundsList) {
        Set<Integer> exclusions = new HashSet<>();
        for (int i=0; i < roundsList.size(); i++) {
            for (Map.Entry<Integer, List<Integer>> entry : roundsList.get(i).entrySet()) {
                Integer back = numRoundsToExclude.get(entry.getKey());

                if (back < 0 || i >= roundsList.size() - back) {
                    exclusions.addAll(entry.getValue());
                }
            }
        }
        return exclusions;
    }


    public RankedResult rank(RetrievedResult retrievedResult, RequestContext requestContext) {
        /*
         * STEP 1: Extract parameters from the request and transform them into
         * usable data structures.
         */

        JsonNode reqBody = requestContext.getRequestBody();

        int userId = JsonHelpers.getRequiredInt(reqBody, "userId");

        List<Map<Integer, List<Integer>>> roundsList = new ArrayList<>();
        try {
            JsonNode rawRoundList = JsonHelpers.getRequiredArray(reqBody, "rounds");
            roundsList = parseRoundsList(rawRoundList);
        } catch (BadRequestException e) {
            Logger.info("request does not contain any rounds");
        }
        Logger.info("Considering {} rounds", roundsList.size());


        List<Integer> ratedMovieIds = new ArrayList<>();
        List<RealVector> recentlyRated = new ArrayList<>(3);
        try {
            JsonNode ratedMoviesNode = JsonHelpers.getRequiredJson(reqBody, "ratedMovies");
            if (!ratedMoviesNode.isArray()) { throw new BadRequestException("ratedMovies must be an array"); }
            for (JsonNode node : (ArrayNode) ratedMoviesNode) {
                try {
                    int movieId = JsonHelpers.getRequiredInt(node, "movieId");
                    ratedMovieIds.add(movieId);

                    double rating = JsonHelpers.getRequiredDouble(node, "rating");
                    if (recentlyRated.size() < 5 && rating > 3.0) {
                        recentlyRated.add(getMovieVector(movieId));
                    }
                } catch (BadRequestException e) { continue; }
            }
        } catch (BadRequestException e) {
            Logger.info("request does not contain any rated movies");
        }
        Logger.info("User has rated {} movies", ratedMovieIds.size());

        List<Integer> ignoredMovieIds = new ArrayList<>();
        try {
            ignoredMovieIds = JsonHelpers.getRequiredListOfInteger(reqBody, "ignoredMovieIds");
        } catch (BadRequestException e) {
            Logger.info("request does not contain any ignored movies");
        }
        Logger.info("Ignoring {} movies", ignoredMovieIds.size());

//        JsonNode movieNodes = JsonHelpers.getRequiredJson(reqBody, "movieNodes");
//        if (!movieNodes.isArray()) { throw new BadRequestException("movieNodes must be an array"); }
//        Map<Integer, JsonNode> movieDetails = new HashMap<>();
//        for (JsonNode node : (ArrayNode) movieNodes) {
//            movieDetails.put(node.get("movieId").asInt(), node);
//        }
//
//        Logger.info("Using {} most popular movies", movieDetails.size());

        // Ensure that each round uses a different, but reproducible seed.

        // TODO: Support different algorithm conditions
        if (expt.get("algorithm") == 2) {
            // random
        } else if (expt.get("algorithm") == 1) {
            // top-n
        } else if (expt.get("algorithm") == 0) {
            // real algorithm
        } else {
            throw new BadRequestException("algorithm must be 0, 1, or 2");
        }


        /*
         * STEP 2: Compute a "desired" vector based on the user's chosen
         * starting point and their subsequent elicited preferences.
         */
        RealVector initialVec = null;
        if (expt.get("origin") == 1){
            try {
                initialVec = getUserVector(userId);
                Logger.info("Using a starting point personalized to long-term preference");
            } catch (BadRequestException e) {
                Logger.error("User had no user vector to use for initial vector");
                initialVec = null; // Go with condition 0
            }
        } else if (expt.get("origin") == 2) {
            if (!recentlyRated.isEmpty()) {
                initialVec = recentlyRated.stream()
                        .reduce((v1, v2) -> v1.add(v2))
                        .get()
                        .mapDivide(recentlyRated.size());
                Logger.info("Using a starting point personalized to short-term preference with {} movies", recentlyRated.size());
            } else {
                Logger.error("User had no positively rated movies to construct initial vector from");
                initialVec = null; // Go with condition 0
            }
        } else {
            initialVec = getAverageUserVector();
        }
        final RealVector desiredVec = computeDesiredVec(initialVec, roundsList);

        /*
         * STEP 3: Filter the candidate movies and select movies from the resulting set.
         */

        // Get the selection criteria for the current round, defaulting to
        // previous rounds as necessary.
        final int currentRoundNum = roundsList.size() + 1;
        int selectionCriteriaNum = selectionCriteriaMap.keySet().stream()
                .filter(x -> x <= currentRoundNum)
                .mapToInt(x -> x)
                .max().orElse(1);
        if (selectionCriteriaNum <= 0) {
            throw new ConfigurationException("selectionCriteriaByRound must contain key 1");
        }
        List<SelectionCriteria> selectionCriteria = selectionCriteriaMap.get(selectionCriteriaNum);


        // Get the set of movies to exclude...
        Set<Integer> exclusions = getExclusions(roundsList);
        Logger.info("Excluding {} movies shown in previous rounds", exclusions.size());
        exclusions.addAll(ignoredMovieIds); // add any exclusions specified in the request


        // Get all items (excluding those in previous rounds) and score them
        List<ObjectNode> universe = retrievedResult.getEntityList().stream()
                .filter(obj -> !exclusions.contains(obj.get("movieId").asInt()))
                .collect(Collectors.toList());
        scoreItems(desiredVec, universe);


        List<ObjectNode> selected = new ArrayList<>();
        for (SelectionCriteria criteria : selectionCriteria) {
            selectItems(selected, universe, ratedMovieIds, criteria);
        }

//        // Get the list of candidate movies to select among.
//        List<ObjectNode> candidates = new ArrayList<>();
//        Map<Integer, RealVector> movieVectorMap = new HashMap<>();
//        for (ObjectNode entity : retrievedResult.getEntityList()) {
//            int movieId = entity.get("movieId").asInt();
//            try {
//                movieVectorMap.put(movieId, getMovieVector(movieId));
//                candidates.add(entity);
//            } catch (BadRequestException e) {
//                Logger.warn("movieId {} not in model", movieId);
//                continue;
//            }
//        }
//
//        Logger.info("universe contains {} movies", candidates.size());
//        Logger.info("vector element range [{}, {}, {}]",
//                movieVectorMap.values().stream().mapToDouble(x -> x.getMinValue()).min().getAsDouble(),
//                movieVectorMap.values().stream().mapToDouble(x -> x.getL1Norm() / x.getDimension()).average().getAsDouble(),
//                movieVectorMap.values().stream().mapToDouble(x -> x.getMaxValue()).max().getAsDouble());
//
////        if (desiredVec == null) {
////            selectionCriteria = selectionCriteriaMap.get(0); // TODO: is this a good approach?
////            // No starting point yet, so we use a more general approach to movie selection.
////            int limit = getLimit(selectionCriteria, ratedMovieIds);
////            candidates = filterItems(limit, candidates, selectionCriteria, exclusions);
////        } else {
////            int limit = getLimit(selectionCriteria, ratedMovieIds);
////            candidates = filterItems(desiredVec, limit, candidates, selectionCriteria, exclusions);
////        }
//
//        // Prepare the data structures needed for item selection.
//        List<Integer> candidateIds = candidates.stream()
//                .map(o -> o.get("movieId").asInt())
//                .collect(Collectors.toList());
//
//        List<Integer> selected = new ArrayList<>();
//        for (SelectionCriteria criteria : selectionCriteria) {
//            // TODO: Fix logging to use new criteria parameters...
//            Logger.info("*** Selecting {} of top ??{}?? movies, dropout={}, ratedDropout={} ***",
//                    criteria.n, criteria.limit, criteria.dropout, criteria.ratedDropout);
//
//            Ordering<ObjectNode> ordering = RetrieverUtilities.jsonFieldOrdering("");
//            ordering.sortedCopy()
//
//            selectItems(selected, movieVectorMap, candidateIds, ratedMovieIds, criteria, "manhattanDistance");
//        }
//
////        if (desiredVec == null) {
////            Collections.shuffle(candidateIds);
////            selected = candidateIds.subList(0, 10);
////        }
//        /*
//         * STEP 4: Return selected movies in the correct format.
//         */
//
//        final List<Integer> selectedCopy = selected;
//        List<ObjectNode> entityList = candidates.stream()
//                .filter(o -> selectedCopy.contains(o.get("movieId").asInt()))
//                .collect(Collectors.toList());
//        //Collections.shuffle(entityList, random);

        if (desiredVec != null) {
            Logger.info("Final selected cosine scores: {}", selected.stream()
                    .map(o -> d.format(o.get("cosine").asDouble()))
                    .collect(Collectors.toList())
                    .toString());
        }

        // Return the chosen set of movies in the correct format.
        List<Prediction> ranking = selected.stream()
                .map((entity) -> new Prediction(entity, null, 0.0))
                .collect(Collectors.toList());

        ObjectNode params = null;

        // Change this user's experimental condition...
        if (initialVec == null) {
            params = Json.newObject();
            params.put("origin", 0);
        }

        return new RankedResult(ranking, 0, ranking.size(), ranking.size(), params);
    }

//    private int getLimit(List<SelectionCriteria> selectionCriteria, Collection<Integer> ratedMovieIds) {
//        // Figure out a good maximum number of items to consider, given the specified dropout parameters
//        final int numRated = ratedMovieIds.size();
//        int maxLimit = (int) selectionCriteria.stream()
//                .mapToDouble(x -> 1.25 * x.limit * (1 / (1 - x.dropout)) + 0.25 * x.ratedDropout * numRated)
//                .max().getAsDouble();
//        return maxLimit;
//    }

//    // TODO: Use reddis integration to filter these items...
//    // TODO: Filter this by all time pop, pop last year, and pop last month, release date, etc.
//    // TODO: The goal is to make this list change reasonably often, while still displaying well known items...
//    private List<ObjectNode> filterItems(int limit, List<ObjectNode> universe, List<SelectionCriteria> selectionCriteria, Collection<Integer> exclusions) {
//        return universe.stream()
//                .filter(x -> !exclusions.contains(x.get("movieId").asInt()))
//                .limit(limit)
//                .collect(Collectors.toList());
//    }

    /////////////////
    // SCORE ITEMS //
    /////////////////

    private void scoreItems(RealVector desiredVec, List<ObjectNode> universe) {
        for (ObjectNode entity : universe) {
            RealVector movieVec = getMovieVector(entity.get("movieId").asInt());

            // Score the entity
            double cosine = getSimilarityMetricScore("cosine", desiredVec, movieVec);
            double dotProduct = getSimilarityMetricScore("dotProduct", desiredVec, movieVec);
            double magnitude = movieVec.getNorm();
            double score1 = dotProduct * Math.pow(Math.abs(cosine), 1);
            double score2 = dotProduct * Math.pow(Math.abs(cosine), 2);
            double score3 = dotProduct * Math.pow(Math.abs(cosine), 3);
            double score4 = dotProduct * Math.pow(Math.abs(cosine), 3);
            double score5 = dotProduct * Math.pow(Math.abs(cosine), 3);

            entity.put("cosine", cosine);
            entity.put("dotProduct", dotProduct);
            entity.put("magnitude", magnitude);
            entity.put("score1", score1);
            entity.put("score2", score2);
            entity.put("score3", score3);
            entity.put("score4", score4);
            entity.put("score5", score5);
        }

        for (String key : Arrays.asList("score1", "score2", "score3", "cosine", "dotProduct", "magnitude")) {
            double min = universe.stream().mapToDouble(x -> x.get(key).asDouble()).min().orElse(1.0 / 0.0);
            double max = universe.stream().mapToDouble(x -> x.get(key).asDouble()).max().orElse(-1.0 / 0.0);
            Logger.info("{}: min{}, max {}", key, d.format(min), d.format(max));
        }
    }

    //////////////////
    // FILTER ITEMS //
    //////////////////


//    private List<ObjectNode> filterItems(RealVector desiredVec, int limit, List<ObjectNode> universe, List<SelectionCriteria> selectionCriteria, Collection<Integer> exclusions) {
//        // Get the topN movies that have the largest dot product with the
//        // "desired" vector that we constructed above, ignoring exclusions.
//        TopNAccumulator<ObjectNode> accum = new TopNAccumulator<>(limit);
//        List<ObjectNode> allEntities = new ArrayList<>();
//
//        int cnt = 0;
//        for (ObjectNode entity : universe) {
//            int movieId = entity.get("movieId").asInt();
//            if (exclusions.contains(movieId)) { continue; }
//
//            // Score the entity
//            double cosine = getSimilarityMetricScore("cosine", desiredVec, getMovieVector(movieId));
//            double dotProduct = getSimilarityMetricScore("dotProduct", desiredVec, getMovieVector(movieId));
//            double magnitude = getMovieVector(movieId).getNorm();
//            double score1 = dotProduct * Math.pow(Math.abs(cosine), 1);
//            double score2 = dotProduct * Math.pow(Math.abs(cosine), 2);
//            double score3 = dotProduct * Math.pow(Math.abs(cosine), 3);
//            entity.put("cosine", cosine);
//            entity.put("dotProduct", dotProduct);
//            entity.put("magnitude", magnitude);
//
//            double score = score3;
//            entity.put("score", score);
//
//            // Save a list of all entities considered for logging purposes
//            allEntities.add(entity);
//
//            //if (cosine < 0.80) { continue; }
//            if (score < 0.0) { continue; } // must have a positive score
//            accum.put(entity, score);
//            cnt++;
//        }
//
//        List<ObjectNode> topEntities = accum.finishList();
//
//        // Log the cosine, dot product, and score of both the entities
//        // considered and the entities selected.
//        Logger.info("Chose top {} movies ({} requested, {} available)", topEntities.size(), limit, cnt);
//        for (String key : Arrays.asList("score", "cosine", "dotProduct", "magnitude")) {
//            double min = allEntities.stream().mapToDouble(x -> x.get(key).asDouble()).min().orElse(1.0 / 0.0);
//            double minSelected = topEntities.stream().mapToDouble(x -> x.get(key).asDouble()).min().orElse(1.0 / 0.0);
//            double max = allEntities.stream().mapToDouble(x -> x.get(key).asDouble()).max().orElse(-1.0 / 0.0);
//            double maxSelected = topEntities.stream().mapToDouble(x -> x.get(key).asDouble()).max().orElse(-1.0 / 0.0);
//
//            Logger.info("{}: min {}, min selected {}, max selected {}, max {}", key,
//                    d.format(min), d.format(minSelected), d.format(maxSelected), d.format(max));
//        }
//
//        return topEntities;
//    }

//    private double weightedGeometricMean(List<WeightedDouble> values) {
//        if (values.isEmpty()) { return -1.0; }
//        if (values.stream().filter(x -> x.value < 0.0 || x.weight <= 0.0).limit() > 0) {
//            throw new IllegalArgumentException("values must be nonnegative and their preferenceWeights must be positive");
//        }
//
//        double product = values.stream()
//                .map(x -> Math.pow(x.value, x.weight))
//                .reduce(1.0, (x, y) -> x * y);
//        double root = values.stream()
//                .map(x -> x.weight)
//                .reduce(0.0, (x, y) -> x + y);
//        return Math.pow(product, 1.0 / root);
//    }
//    private double geometricMean(List<Double> values) {
//        if (values.isEmpty()) { return -1.0; }
//        if (values.stream().filter(x -> x < 0.0).limit() > 0) {
//            throw new IllegalArgumentException("values must be nonnegative");
//        }
//
//        double product = values.stream()
//                .reduce(1.0, (x, y) -> x * y);
//        return Math.pow(product, 1.0 / values.size());
//    }

    private double getAvgDistanceToVectors(String metric, Collection<RealVector> vectors, RealVector target) {
        return vectors.stream()
                .mapToDouble(v -> getDistanceMetricScore(metric, v, target))
                .average()
                .orElse(-1.0);
    }

    private double getMinDistanceToVectors(String metric, Collection<RealVector> vectors, RealVector target) {
        return vectors.stream()
                .mapToDouble(v -> getDistanceMetricScore(metric, v, target))
                .min()
                .orElse(-1.0);
    }

//    private double getHarmonicMeanDistanceToVectors(String metric, Collection<RealVector> vectors, RealVector target) {
//        double sumInverses = vectors.stream()
//                .mapToDouble(v -> 1 / getDistanceMetricScore(metric, v, target))
//                .sum();
//        if (sumInverses != 0.0) { return vectors.size() / sumInverses; }
//        else { return -1.0; }
//    }

    private void selectItems(List<ObjectNode> selected,
                             List<ObjectNode> universe,
                             List<Integer> ratedMovieIds,
                             SelectionCriteria criteria) {

        Logger.info("Selecting {} of {} movies: ratedDropout={}, dropout={}",
                criteria.n, criteria.limit, criteria.ratedDropout, criteria.dropout);

        Ordering<ObjectNode> ordering = RetrieverUtilities.jsonFieldOrdering(criteria.similarityMetric);
        List<ObjectNode> sortedEntities = ordering.reverse().immutableSortedCopy(universe);
        List<ObjectNode> candidates = sortedEntities.stream()
                .filter(obj -> obj.get(criteria.similarityMetric).asDouble() > criteria.excludeBelow)
                .collect(Collectors.toList());

        int startingSize = selected.size();
        while (selected.size() < startingSize + criteria.n) {
            TopNAccumulator<ObjectNode> top = new TopNAccumulator<>(1);
            List<RealVector> selectedVectors = selected.stream()
                    .map(obj -> getMovieVector(obj.get("movieId").asInt()))
                    .collect(Collectors.toList());
            Set<Integer> selectedIds = selected.stream()
                    .map(obj -> obj.get("movieId").asInt())
                    .collect(Collectors.toSet());


            double minValue = - 1.0 / 0.0;
            double maxValue = 1.0 / 0.0;
            int numIterated = 0;
            for (ObjectNode obj : candidates) {
                int id = obj.get("movieId").asInt();
                if (selectedIds.contains(id)) { continue; }

                numIterated++;
                if (random.nextDouble() < criteria.dropout) { continue; }
                if (ratedMovieIds.contains(id) && random.nextDouble() < criteria.ratedDropout) { continue; }

                double distance = getMinDistanceToVectors(criteria.diversityMetric, selectedVectors, getMovieVector(id));
                if (distance < 0) { distance = distance * random.nextDouble(); } // Randomize first choice
                top.put(obj, distance);

                if (top.count() == 1) {
                    maxValue = obj.get(criteria.similarityMetric).asDouble();
                }
                if (top.count() >= criteria.limit) {
                    minValue = obj.get(criteria.similarityMetric).asDouble();
                    break;
                }
            }

            ObjectNode chosen = top.max();
            double distance = getMinDistanceToVectors(
                    criteria.diversityMetric,
                    selectedVectors,
                    getMovieVector(chosen.get("movieId").asInt())
            );
            Logger.info("Selected item with {}={} after comparing {} of {} items with {}=[{}, {}]",
                    criteria.diversityMetric, d.format(distance),
                    top.count(), numIterated,
                    criteria.similarityMetric, d.format(minValue), d.format(maxValue));
            selected.add(chosen);
        }




//        // Consider the given number of limit items
//        int limit = 0;
//        for (ObjectNode obj : sortedEntities.subList(primary.size(), sortedEntities.size())) {
//            if (obj.get(criteria.similarityMetric).asDouble() < criteria.excludeBelow) { break; }
//            if (limit >= criteria.limit) { break; }
//            candidates.add(obj);
//        }
//
//
//        int ratedBound = (int) Math.round(criteria.ratedDropout * ratedMovieIds.size());
//        int candidateBound = (int) Math.round(criteria.dropout * candidates.size());
//
//
//        int startingSize = selected.size();
//        while (selected.size() < startingSize + criteria.n) {
//            TopNAccumulator<ObjectNode> top = new TopNAccumulator<>(1);
//            List<RealVector> selectedVectors = selected.stream()
//                    .map(x -> movieVectorMap.get(x))
//                    .collect(Collectors.toList());
//
//            // Randomize the candidates and rated movies we'll be dropping
//            Collections.shuffle(candidates, random);
//            Collections.shuffle(ratedMovieIds, random);
//
//            Set<Integer> exclusions = selected.stream()
//                    .map(obj -> obj.get("movieId").asInt())
//                    .collect(Collectors.toSet());
//            exclusions.addAll(ratedMovieIds.subList(0, ratedBound));
//
//            List<ObjectNode> subCandidates = candidates.stream()
//                    .filter(obj -> !exclusions.contains(obj.get("movieId").asInt()))
//                    .collect(Collectors.toList());
//            int subCandidateBound = (int) Math.round(criteria.dropout * subCandidates.size());
//
//
//
//            for (ObjectNode obj : subCandidates.subList(subCandidateBound, subCandidates.size())) {
//                double distance = getMinDistanceToVectors(
//                        criteria.diversityMetric,
//                        selectedVectors,
//                        movieVectorMap.get(obj.get("movieId").asInt())
//                );
//                if (distance < 0) { // Randomize first choice
//                    distance = distance * random.nextDouble();
//                }
//                top.put(obj, distance);
//            }
//
//            // Select the top choice
//            selected.add(top.max());
//        }

    }

    /*
     * Calculates the specified distance metric for two vectors. Return values
     * are guaranteed to be nonnegative.
     */
    private double getDistanceMetricScore(String metric, RealVector vec1, RealVector vec2) {
        switch (metric) {
            case "euclideanCosine": return (0.5 - (vec1.cosine(vec2) / 2)) * vec1.getDistance(vec2);
            case "manhattanCosine": return (0.5 - (vec1.cosine(vec2) / 2)) * vec1.getL1Distance(vec2);
            case "cosine": return 0.5 - (vec1.cosine(vec2) / 2); // angular distance
            case "euclideanDistance": return vec1.getDistance(vec2);
            case "manhattanDistance": return vec1.getL1Distance(vec2);
            case "maxDistance": return vec1.getLInfDistance(vec2);
            case "minDistance": return vec1.subtract(vec2).map(x -> Math.abs(x)).getMinValue();
            default: throw new ConfigurationException("metric not regonized");
        }
    }

    /*
     * Calculates the specified similarity metric for two vectors. Return
     * values are guaranteed to be nonnegative. Can be asked to use distance
     * metrics, in which case the method falls back to using 0.5^[distance].
     * This results in a similarity value between 0 and 1, like with cosine.
     */
    private double getSimilarityMetricScore(String metric, RealVector vec1, RealVector vec2) {
        switch (metric) {
            case "dotProduct": return vec1.dotProduct(vec2);
            case "cosine": return vec1.cosine(vec2);
            default: return Math.pow(0.5, getDistanceMetricScore(metric, vec1, vec2));

        }
    }

//    private double getMetricScore(String metric, RealVector vec1, RealVector vec2, RealVector weightVec) {
//        switch (metric) {
//            case "dotProduct": return vec1.dotProduct(vec2.ebeMultiply(weightVec));
//            case "cosine": return vec1.cosine(vec2.ebeMultiply(weightVec));
//            case "euclideanDistance": return -vec1.ebeMultiply(weightVec).getDistance(vec2.ebeMultiply(weightVec));
//            case "manhattanDistance": return -vec1.ebeMultiply(weightVec).getL1Distance(vec2.ebeMultiply(weightVec));
//            case "maxDistance": return -vec1.ebeMultiply(weightVec).getLInfDistance(vec2.ebeMultiply(weightVec));
//            case "minDistance": return -vec1.subtract(vec2).ebeMultiply(weightVec).map(x -> Math.abs(x)).getMinValue();
//            default: throw new ConfigurationException("metric not regonized");
//        }
//    }
//
//    private int findMostDistantVector(Map<Integer, RealVector> basket, Map<Integer, RealVector> choices, RealVector vecElemWeights, WeightedValue... preferenceWeights) {
//        TopNAccumulator<Integer> top1 = new TopNAccumulator<>(1);
//
//        double maxDistance = -1000000.0;
//
//        for (Map.Entry<Integer, RealVector> entry : choices.entrySet()) {
//
//            double totalDistance = 0.0;
//            for (RealVector b : basket.values()) {
//                double score;
//                if (weightDiversityByUserVector) {
//                    score = getDiversityMetricScore(b, entry.getValue(), vecElemWeights);
//                } else {
//                    score = getDiversityMetricScore(b, entry.getValue());
//                }
//
//                totalDistance = totalDistance + score;
//            }
//
//
//            double avgDistance = totalDistance / basket.size();
//
////            // Weight the distance by other values.
////            double root = 1.0;
////            for ( WeightedValue w : preferenceWeights ) {
////                avgDistance *= Math.pow(w.value, w.weight);
////                root += w.weight;
////            }
////            avgDistance = Math.pow(avgDistance, 1.0 / root);
//
//            maxDistance = Math.max(maxDistance, avgDistance);
//            top1.put(entry.getKey(), avgDistance);
//
//        }
//        Logger.info("Chose distant movie with diversity score: {}", maxDistance);
//        return top1.min();
//    }
}
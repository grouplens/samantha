package org.grouplens.samantha.ephemeral;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import play.Configuration;
import play.Logger;

import java.text.DecimalFormat;
import java.text.Format;
import java.util.*;
import java.util.stream.Collectors;

public class EphemeralRanker extends AbstractRanker {
    private final SVDFeature svdFeature;
    private final Map<Integer, Double> preferenceWeights;
    private final Map<Integer, List<SelectionCriteria>> selectionCriteriaMap;
    private final Map<Integer, Integer> numRoundsToExclude;
    private final Map<String, Integer> expt;
    private Random random;

    public EphemeralRanker(
            Configuration config,
            SVDFeature svdFeature,
            Map<Integer, Double> preferenceWeights,
            Map<Integer, List<SelectionCriteria>> selectionCriteriaMap,
            Map<Integer, Integer> numRoundsToExclude,
            Map<String, Integer> expt,
            long seed) {
        super(config);
        this.svdFeature = svdFeature;
        this.preferenceWeights = preferenceWeights;
        this.selectionCriteriaMap = selectionCriteriaMap;
        this.numRoundsToExclude = numRoundsToExclude;
        this.expt = expt;
        this.random = new Random(seed);
    }

    private RealVector getUserVector(int userId) {
        return getVector("userId", userId);
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

    private RealVector computeDesiredVec(RealVector initialSeed, List<Map<Integer, List<Integer>>> roundsList) {
        Format d = new DecimalFormat("#.###");

        initialSeed = initialSeed.mapDivide(initialSeed.getNorm()); // normalize seed to unit vector
        RealVector currentVec = initialSeed; // make a working copy we can modify

        // Iterate over each round, adding the user's preferences to the
        // "desired" vector and more heavily weighting more recent rounds.
        int i = 0;
        for (Map<Integer, List<Integer>> round : roundsList) {
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
                }

                // Add up the selected vector norms, so we can calculate a percentage...
                selectedNorm += dNorms.stream().mapToDouble(x -> x).sum();
            }

            RealVector direction = vNumerator.mapDivide(vDenominator);
            double movement = movementNumerator / movementDenominator;
            double confidence = Math.pow(selectedNorm / totalNorm, 0.25);

            RealVector delta = direction.mapMultiply(movement * confidence);
            currentVec = currentVec.add(delta);
            currentVec = currentVec.mapDivide(currentVec.getNorm());

            if (i == 1) {
                Logger.info("Cosine of {} from user seed to round 1", d.format(currentVec.cosine(roundSeed)));
            } else {
                Logger.info("Cosine of {} from round {} to {}", d.format(currentVec.cosine(roundSeed)), i - 1, i);
            }
        }

        if (i > 1) {
            Logger.info("Cosine of {} from user seed to round {}", d.format(currentVec.cosine(initialSeed)), i);
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
        try {
            ratedMovieIds = JsonHelpers.getRequiredListOfInteger(reqBody, "ratedMovieIds");
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

        /*
         * STEP 2: Compute a "desired" vector based on the user's chosen
         * starting point and their subsequent elicited preferences.
         */

        final RealVector userVec = getUserVector(userId);
        final RealVector desiredVec = computeDesiredVec(userVec, roundsList);

        /*
         * STEP 3: Compute the set of n candidate movies that we will return to the user.
         */

        // Get the set of movies to exclude...
        Set<Integer> exclusions = getExclusions(roundsList);
        Logger.info("Excluding {} movies shown in previous rounds", exclusions.size());
        exclusions.addAll(ignoredMovieIds); // add any exclusions specified in the request

        // Get the selection criteria for the current round, defaulting to -1.
        List<SelectionCriteria> selectionCriteria = selectionCriteriaMap.getOrDefault(
                roundsList.size() + 1,
                selectionCriteriaMap.get(-1)
        );


        // Figure out a good maximum number of items to consider, given the specified dropout parameters
        final int numRated = ratedMovieIds.size();
        int maxLimit = (int) selectionCriteria.stream()
                .mapToDouble(x -> 1.25 * x.limit * (1 / (1 - x.dropout)) + 0.25 * x.ratedDropout * numRated)
                .max().getAsDouble();

        // Get the topN movies that have the largest dot product with the
        // "desired" vector that we constructed above, ignoring exclusions.
        TopNAccumulator<ObjectNode> accum = new TopNAccumulator<ObjectNode>(maxLimit);
        List<ObjectNode> allEntities = new ArrayList<>();

        for (ObjectNode entity : retrievedResult.getEntityList()) {
            int movieId = entity.get("movieId").asInt();
            if (exclusions.contains(movieId)) { continue; }

            // Score the entity
            double cosine = getSimilarityMetricScore("cosine", desiredVec, getMovieVector(movieId));
            double dotProduct = getSimilarityMetricScore("dotProduct", desiredVec, getMovieVector(movieId));
            double score = dotProduct * Math.pow(Math.abs(cosine), 2);
            entity.put("cosine", cosine);
            entity.put("dotProduct", dotProduct);
            entity.put("score", score);

            // Save a list of all entities considered for logging purposes
            allEntities.add(entity);

            if (score < 0.0) { continue; } // must have a positive score
            accum.put(entity, score);
        }

        // Retrieve the topn entities
        List<ObjectNode> topEntities = accum.finishList();

        // Log the cosine, dot product, and score of both the entities
        // considered and the entities selected.
        Format d = new DecimalFormat("#.###");
        Logger.info("Chose top {} movies ({} requested)", topEntities.size(), maxLimit);
        for (String key : Arrays.asList("score", "cosine", "dotProduct")) {
            double min = allEntities.stream().mapToDouble(x -> x.get(key).asDouble()).min().orElse(1.0 / 0.0);
            double minSelected = topEntities.stream().mapToDouble(x -> x.get(key).asDouble()).min().orElse(1.0 / 0.0);
            double max = allEntities.stream().mapToDouble(x -> x.get(key).asDouble()).max().orElse(-1.0 / 0.0);
            double maxSelected = topEntities.stream().mapToDouble(x -> x.get(key).asDouble()).max().orElse(-1.0 / 0.0);

            Logger.info("{}: min {}, min selected {}, max selected {}, max {}", key,
                    d.format(min), d.format(minSelected), d.format(maxSelected), d.format(max));
        }

        // Prepare the data structures needed for item selection.
        List<Integer> topMovieIds = topEntities.stream()
                .map(o -> o.get("movieId").asInt())
                .collect(Collectors.toList());
        Map<Integer, RealVector> movieVectorMap = topMovieIds.stream()
                .collect(Collectors.toMap(x -> x, x -> getMovieVector(x)));
        List<Integer> candidates = new ArrayList<>();

        // Select items according to the specified criteria
        for (SelectionCriteria criteria : selectionCriteria) {
            Logger.info("*** Selecting {} of top {} movies, dropout={}, ratedDropout={} ***",
                    criteria.n, criteria.limit, criteria.dropout, criteria.ratedDropout);

            selectItems(candidates, movieVectorMap, topMovieIds, ratedMovieIds, criteria);
        }

        /*
         * STEP 4: Return candidate movies in the correct format.
         */


        List<ObjectNode> entityList = topEntities.stream()
                .filter(o -> candidates.contains(o.get("movieId").asInt()))
                .collect(Collectors.toList());
        Collections.shuffle(entityList, random);

//        // TODO: Use the code above to randomize
//        Map<Integer, ObjectNode> objectMapper = topEntities.stream().collect(Collectors.toMap(
//                o -> o.get("movieId").asInt(),
//                o -> o
//        ));
//        List<ObjectNode> entityList = candidates.stream()
//                .map(x -> objectMapper.get(x))
//                .collect(Collectors.toList());


        Logger.info("Final candidates cosine scores: {}", entityList.stream()
                .map(o ->  d.format(o.get("cosine").asDouble()))
                .collect(Collectors.toList())
                .toString());

        // Return the chosen set of movies in the correct format.
        List<Prediction> ranking = entityList.stream()
                .map((entity) -> new Prediction(entity, null, entity.get("score").asDouble()))
                .collect(Collectors.toList());
        return new RankedResult(ranking, 0, ranking.size(), ranking.size());
    }


//    private double weightedGeometricMean(List<WeightedDouble> values) {
//        if (values.isEmpty()) { return -1.0; }
//        if (values.stream().filter(x -> x.value < 0.0 || x.weight <= 0.0).count() > 0) {
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
//        if (values.stream().filter(x -> x < 0.0).count() > 0) {
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

    private void selectItems(List<Integer> candidates,
                             Map<Integer, RealVector> movieVectorMap,
                             List<Integer> topMovieIds,
                             List<Integer> ratedMovieIds,
                             SelectionCriteria criteria) {

        int i = 0;
        while (i < criteria.n) {
            TopNAccumulator<Integer> top = new TopNAccumulator<>(1);

            List<RealVector> candidateVectors = candidates.stream()
                    .map(x -> movieVectorMap.get(x))
                    .collect(Collectors.toList());

            int numIterated = 0;
            int numCompared = 0;
            for (Integer movieId : topMovieIds) {
                if (candidates.contains(movieId)) { continue; } // exclude selected movies...

                numIterated++;

                if (random.nextDouble() < criteria.dropout) { continue; } // dropout some proportion of movies...
                if (random.nextDouble() < criteria.ratedDropout && ratedMovieIds.contains(movieId)) { continue; } // dropout some proportion of rated movies...

//                List<WeightedDouble> values = new ArrayList<>();
//
//                if (!candidates.isEmpty()) {
//                    double distance = getMinDistanceToVectors(candidateVectors, movieVectorMap.get(movieId));
//                    if (diversityMetric.equals("cosine")) { // TODO: Remove, or put in diversity metric code?
//                        distance += 1; // cosine is between -1 and 0, so we can increment it by one to get a nonnegative value
//                    }
//                    values.add(new WeightedDouble(distance, 1.0));
//                }
//
//                if (criteria.itemBiasWeighting > 0.0) { // quality
//                    double sigmoidMovieBias = sigmoid.value(getMovieBias(movieId));
//                    values.add(new WeightedDouble(sigmoidMovieBias, criteria.itemBiasWeighting));
//                }
//
//                if (criteria.logSupportWeighting > 0.0) { // popularity
//                    double logSupport = Math.log10(getMovieSupport(movieId));
//                    values.add(new WeightedDouble(logSupport, criteria.logSupportWeighting));
//                }
//
//                if (criteria.avgRatingWeighting > 0.0) { // quality
//                    double avgRating = movieDetails.get(movieId).get("avgRating").asDouble();
//                    values.add(new WeightedDouble(avgRating, criteria.avgRatingWeighting));
//                }
//
//                if (criteria.logNumRatingsWeighting > 0.0) { // popularity
//                    double logNumRatings = Math.log(movieDetails.get(movieId).get("numRatings").asInt());
//                    values.add(new WeightedDouble(logNumRatings, criteria.logNumRatingsWeighting));
//                } else if (criteria.logNumRatingsWeighting < 0.0) {
//                    double logNumRatings = 1.0 / Math.log(movieDetails.get(movieId).get("numRatings").asInt());
//                    values.add(new WeightedDouble(logNumRatings, Math.abs(criteria.logNumRatingsWeighting)));
//                }
//
//                if (criteria.halflife15YearsWeighting > 0.0) { // recency
//                    double halflife15Years;
//                    try {
//                        halflife15Years = Math.pow(0.5, movieDetails.get(movieId).get("daysOld").asInt() / (365.0 * 15.0));
//                    } catch (Throwable e) {
//                        // Assume movies with no release date are 50 years old.
//                        halflife15Years = Math.pow(0.5, 50.0 / 15.0);
//                    }
//                    values.add(new WeightedDouble(halflife15Years, criteria.halflife15YearsWeighting));
//                }


                // Calculate distance between the current movie's vector and
                // those we've already selected. Returns -1, when no vectors.

                double score = getMinDistanceToVectors("euclideanDistance", candidateVectors, movieVectorMap.get(movieId));
                if (score < 0) { score = score * random.nextDouble(); } // Randomize first choice
                top.put(movieId, score);
                numCompared++;

                // Only compare up to criteria.limit number of candidates.
                if (numCompared >= criteria.limit) { break; }

            }

            // Get the top choice movie.
            int chosenMovieId = top.max();

            // Log the top choice movie's distance and the maximum possible distance.
            double chosenMinD = getMinDistanceToVectors("euclideanDistance", candidateVectors, movieVectorMap.get(chosenMovieId));
            double maxMinD = topMovieIds.stream()
                    .filter(x -> !candidates.contains(x))
                    .limit(numIterated)
                    .mapToDouble(v -> getMinDistanceToVectors("euclideanDistance", candidateVectors, movieVectorMap.get(v)))
                    .max().orElse(-1.0);
            Format d = new DecimalFormat("#.###");
            Logger.info("Iterated over {} (of {}) movies to select movieId={} with min dist {} (max {}).",
                    numIterated, topMovieIds.size(), chosenMovieId, d.format(chosenMinD), d.format(maxMinD));

            candidates.add(chosenMovieId);
            i++;
        }

    }


    /*
     * Calculates the specified distance metric for two vectors. Return values
     * are guaranteed to be nonnegative.
     */
    private double getDistanceMetricScore(String metric, RealVector vec1, RealVector vec2) {
        switch (metric) {
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
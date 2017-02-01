package org.grouplens.samantha.modeler.featurizer;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.space.IndexSpace;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * The interface for feature extraction.
 *
 * A feature extractor takes in the data point JSON representation and convert it to lists of numerical feature
 * representation. This is the very unit of a {@link Featurizer}. A feature extractor can output multiple lists of
 * features by differentiating them using keys in Map.
 */
public interface FeatureExtractor extends Serializable {

    /**
     * Extract features from the given JSON data point. A feature is an integer index and a double value. The index
     * should come from {@link IndexSpace} by mapping the attribute/field/variable name into a consecutive index space,
     * which will be further used to access {@link org.grouplens.samantha.modeler.space.VariableSpace VariableSpace}.
     * The value could just be 1.0 which means the corresponding feature is hit by the data point. This usually happens
     * when a categorical feature is dealt with. It can be better understood if you think about the conversion from
     * factor variable to dummy variables in R if you know about data analysis in R. For numerical features, the value will
     * just be the actual value in the JSON.
     *
     * @param entity the JSON representation of the data point, with multiple key-values.
     * @param update whether update the indexSpace if the variable name is not present. While learning a model, this should
     *               be true. While using a model to make predictions, this should be false.
     * @param indexSpace the index space used for variable name to integer index mapping. this usually is part of a
     *                   {@link org.grouplens.samantha.modeler.space.SpaceModel SpaceModel}
     * @return a map of feature lists from the feature name to the actual list of features.
     */
    Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                       IndexSpace indexSpace);
}

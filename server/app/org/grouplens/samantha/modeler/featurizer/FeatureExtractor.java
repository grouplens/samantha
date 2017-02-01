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

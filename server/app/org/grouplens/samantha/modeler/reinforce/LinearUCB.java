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

package org.grouplens.samantha.modeler.reinforce;

import com.fasterxml.jackson.databind.JsonNode;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.featurizer.Featurizer;
import org.grouplens.samantha.modeler.featurizer.StandardFeaturizer;
import org.grouplens.samantha.modeler.featurizer.StandardLearningInstance;
import org.grouplens.samantha.modeler.solver.*;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.VariableSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class LinearUCB extends AbstractLearningModel implements Featurizer {
    private static Logger logger = LoggerFactory.getLogger(LinearUCB.class);
    private final StandardFeaturizer featurizer;
    private final double lambda;
    private final double alpha;
    private final int numMainFeatures;
    private final List<String> features;

    /*
     * Directly calling this is discouraged. Use {@link LinearUCBProducer} instead.
     * Note that the featureExtractors must have main features first and interaction features next, because
     *   by default the weight of interaction features is set to zero using this order and numMainFeatures.
     * This model is dense input only.
     */
    public LinearUCB(double lambda,
                     double alpha,
                     List<String> features,
                     int numMainFeatures,
                     String labelName,
                     String weightName,
                     List<FeatureExtractor> featureExtractors,
                     IndexSpace indexSpace,
                     VariableSpace variableSpace) {
        super(indexSpace, variableSpace);
        this.featurizer = new StandardFeaturizer(indexSpace, featureExtractors,
                features, null, labelName, weightName);
        this.lambda = lambda;
        this.alpha = alpha;
        this.numMainFeatures = numMainFeatures;
        this.features = features;
        ensureScalarVarSpace();
        ensureVectorVarSpace();
    }

    public List<StochasticOracle> getStochasticOracle(List<LearningInstance> instances) {
        List<StochasticOracle> oracles = new ArrayList<>(instances.size());
        for (LearningInstance ins : instances) {
            StochasticOracle orc = new StochasticOracle();
            StandardLearningInstance instance = (StandardLearningInstance) ins;
            orc.setValues(-instance.getLabel(), instance.getLabel(), instance.getWeight());
            int dim = features.size();
            RealVector x = extractDenseVector(dim, ins);
            RealMatrix increA = x.outerProduct(x);
            RealVector increB = x.mapMultiplyToSelf(instance.getLabel());
            for (int i = 0; i < dim; i++) {
                orc.addScalarOracle(LinearUCBKey.B.get(), i, -increB.getEntry(i));
                orc.addVectorOracle(LinearUCBKey.A.get(), i, increA.getRowVector(i).mapMultiplyToSelf(-1.0));
            }
            oracles.add(orc);
        }
        return oracles;
    }

    public ObjectiveFunction getObjectiveFunction() {
        return new IdentityFunction();
    }

    private RealVector extractDenseVector(int dim, LearningInstance instance) {
        Int2DoubleMap features = ((StandardLearningInstance) instance).getFeatures();
        RealVector x = MatrixUtils.createRealVector(new double[dim]);
        for (Int2DoubleMap.Entry entry : features.int2DoubleEntrySet()) {
            x.setEntry(entry.getIntKey(), entry.getDoubleValue());
        }
        return x;
    }

    public double[] predict(LearningInstance instance) {
        RealMatrix A = variableSpace.getMatrixVarByName(LinearUCBKey.A.get());
        RealVector B = variableSpace.getScalarVarByName(LinearUCBKey.B.get());
        RealMatrix invA = new LUDecomposition(A).getSolver().getInverse();
        RealVector theta = invA.operate(B);
        RealVector x = extractDenseVector(theta.getDimension(), instance);
        double bound = Math.sqrt(x.dotProduct(invA.operate(x)));
        double mean = x.dotProduct(theta);
        double pred = mean + alpha * bound;
        if (Double.isNaN(pred)) {
            logger.error("Prediction is NaN, model parameter A probably goes wrong.");
            pred = 0.0;
        }
        double[] preds = new double[1];
        preds[0] = pred;
        return preds;
    }

    private void ensureScalarVarSpace() {
        int dim = features.size();
        int size = variableSpace.getScalarVarSizeByName(LinearUCBKey.B.get());
        if (size < dim) {
            variableSpace.ensureScalarVar(LinearUCBKey.B.get(), numMainFeatures, 1.0 / numMainFeatures, false);
            variableSpace.ensureScalarVar(LinearUCBKey.B.get(), dim, 0.0, false);
        }
    }

    private void ensureVectorVarSpace() {
        int dim = features.size();
        int size = variableSpace.getVectorVarSizeByName(LinearUCBKey.A.get());
        if (size < dim) {
            variableSpace.ensureVectorVar(LinearUCBKey.A.get(),
                    dim, dim, 0, false, false);
            for (int i=size; i<dim; i++) {
                RealVector vector = MatrixUtils.createRealVector(new double[dim]);
                vector.setEntry(i, lambda);
                variableSpace.setVectorVarByNameIndex(LinearUCBKey.A.get(), i, vector);
            }
        }
    }

    public LearningInstance featurize(JsonNode entity, boolean update) {
        StandardLearningInstance instance = (StandardLearningInstance) featurizer.featurize(entity, true);
        return instance;
    }
}

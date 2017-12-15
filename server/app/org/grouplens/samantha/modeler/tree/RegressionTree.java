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

package org.grouplens.samantha.modeler.tree;

import com.fasterxml.jackson.databind.JsonNode;
import it.unimi.dsi.fastutil.ints.IntList;
import org.apache.commons.math3.linear.RealVector;
import org.grouplens.samantha.modeler.featurizer.Feature;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.featurizer.StandardLearningInstance;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.VariableSpace;

import java.util.List;

public class RegressionTree extends AbstractDecisionTree {
    private static final long serialVersionUID = 1L;
    private final SplittingCriterion criterion;
    private final String treeName;
    static final public int nodeSize = 6;
    static final public String indexName = TreeKey.TREE.get();

    /**
     * Directly calling this is discouraged. Use {@link RegressionTreeProducer} instead.
     */
    public RegressionTree(String treeName,
                          SplittingCriterion criterion,
                          IndexSpace indexSpace, VariableSpace variableSpace,
                          List<String> features,
                          List<FeatureExtractor> featureExtractors,
                          String labelName, String weightName) {
        super(indexSpace, variableSpace, features, featureExtractors, labelName, weightName);
        this.criterion = criterion;
        this.treeName = treeName;
    }

    public SplittingCriterion createSplittingCriterion() {
        return criterion.create();
    }

    /**
     * A regression node: split fea index, split fea value, leftNode, rightNode, mean, sumWeights/numInstances
     */
    public int createNode(int parentNode, boolean left, IntList relevant,
                           List<double[]> respList, Feature bestSplit) {
        int node = variableSpace.getVectorVarSizeByName(treeName);
        variableSpace.ensureVectorVar(treeName, node + 1, nodeSize, 0.0, false, false);
        if (parentNode >= 0) {
            RealVector nodeVec = variableSpace.getVectorVarByNameIndex(treeName, parentNode);
            if (left) {
                nodeVec.setEntry(2, node);
            } else {
                nodeVec.setEntry(3, node);
            }
            variableSpace.setVectorVarByNameIndex(treeName, parentNode, nodeVec);
        }
        RealVector nodeVec = variableSpace.getVectorVarByNameIndex(treeName, node);
        double sumValue = 0.0;
        double sumWeight = 0.0;
        for (int i=0; i<relevant.size(); i++) {
            double[] resp = respList.get(relevant.getInt(i));
            sumValue += (resp[0] * resp[1]);
            sumWeight += resp[1];
        }
        nodeVec.setEntry(0, bestSplit.getIndex());
        nodeVec.setEntry(1, bestSplit.getValue());
        nodeVec.setEntry(2, -1);
        nodeVec.setEntry(3, -1);
        double mean = 0.0;
        if (sumWeight > 0.0) {
            mean = sumValue / sumWeight;
        }
        nodeVec.setEntry(4, mean);
        nodeVec.setEntry(5, sumWeight);
        variableSpace.setVectorVarByNameIndex(treeName, node, nodeVec);
        return node;
    }

    public double[] predict(LearningInstance instance) {
        double[] preds = new double[1];
        if (variableSpace.getVectorVarSizeByName(treeName) > 0) {
            StandardLearningInstance ins = (StandardLearningInstance) instance;
            int node = 0;
            do {
                RealVector nodeVec = variableSpace.getVectorVarByNameIndex(treeName, node);
                int splitIdx = (int)nodeVec.getEntry(0);
                if (splitIdx == -1) {
                    preds[0] = nodeVec.getEntry(4);
                    return preds;
                }
                double splitVal = nodeVec.getEntry(1);
                double feaVal = 0.0;
                if (ins.getFeatures().containsKey(splitIdx)) {
                    feaVal = ins.getFeatures().get(splitIdx);
                }
                if (feaVal <= splitVal) {
                    node = (int)nodeVec.getEntry(2);
                } else {
                    node = (int)nodeVec.getEntry(3);
                }
                if (node == -1) {
                    preds[0] = nodeVec.getEntry(4);
                    return preds;
                }
            } while (node != -1);
        }
        preds[0] = 0.0;
        return preds;
    }

    private int predictLeaf(LearningInstance instance) {
        int predNode = -1;
        if (variableSpace.getVectorVarSizeByName(treeName) > 0) {
            StandardLearningInstance ins = (StandardLearningInstance) instance;
            int node = 0;
            do {
                predNode = node;
                RealVector nodeVec = variableSpace.getVectorVarByNameIndex(treeName, node);
                int splitIdx = (int)nodeVec.getEntry(0);
                if (splitIdx == -1) {
                    return predNode;
                }
                double splitVal = nodeVec.getEntry(1);
                double feaVal = 0.0;
                if (ins.getFeatures().containsKey(splitIdx)) {
                    feaVal = ins.getFeatures().get(splitIdx);
                }
                if (feaVal <= splitVal) {
                    node = (int)nodeVec.getEntry(2);
                } else {
                    node = (int)nodeVec.getEntry(3);
                }
                if (node == -1) {
                    return predNode;
                }
            } while (node != -1);
        }
        return predNode;
    }

    public int predictLeaf(JsonNode entity) {
        return predictLeaf(featurize(entity, false));
    }
}

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

import it.unimi.dsi.fastutil.ints.*;

import org.grouplens.samantha.modeler.featurizer.Feature;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.featurizer.StandardLearningInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class ExactGreedy extends AbstractTreeLearningMethod {
    private static Logger logger = LoggerFactory.getLogger(ExactGreedy.class);
    private final int minNodeSplit;
    private final int maxTreeDepth;

    public ExactGreedy(int minNodeSplit, int maxTreeDepth) {
        this.maxTreeDepth = maxTreeDepth;
        this.minNodeSplit = minNodeSplit;
    }

    @Inject
    public ExactGreedy() {
        this.maxTreeDepth = 3;
        this.minNodeSplit = 50;
    }

    private double testWhetherBetterSplit(SplittingCriterion leftSplit, SplittingCriterion rightSplit,
                                          List<double[]> respList, double bestGain, Feature split,
                                          double beforeValue, double splitPoint) {
        double gain = rightSplit.getSplittingGain(beforeValue, leftSplit, rightSplit, respList);
        if (gain >= bestGain) {
            split.setValue(splitPoint);
            return gain;
        }
        return bestGain;
    }

    private double findBestFeatureSplit(List<double[]> insVals, List<double[]> respList,
                                        Feature split, IntList relevant, DecisionTree tree) {
        insVals.sort(SortingUtilities.pairDoubleSecondComparator());
        SplittingCriterion leftSplit = tree.createSplittingCriterion();
        SplittingCriterion rightSplit = tree.createSplittingCriterion();
        rightSplit.add(relevant, respList);
        double beforeValue = rightSplit.getValue(respList);
        int insert = 0;
        IntList zeroInts = new IntArrayList();
        if (insVals.size() < relevant.size()) {
            IntOpenHashSet nonZeroInts = new IntOpenHashSet();
            for (int i = 0; i < insVals.size(); i++) {
                double[] insVal = insVals.get(i);
                nonZeroInts.add((int) insVal[0]);
                if (0.0 > insVal[1]) {
                    insert++;
                }
            }
            for (int i = 0; i < relevant.size(); i++) {
                int rel = relevant.getInt(i);
                if (!nonZeroInts.contains(rel)) {
                    zeroInts.add(rel);
                }
            }
        }
        double bestGain = 0.0;
        split.setValue(insVals.get(0)[1]);
        for (int i=0; i<insVals.size(); i++) {
            double[] former = insVals.get(i);
            double[] latter = null;
            if (i + 1 < insVals.size()) {
                latter = insVals.get(i + 1);
            }
            if (zeroInts.size() > 0 && insert == i) {
                leftSplit.add(zeroInts, respList);
                if (0.0 < former[1]) {
                    rightSplit.remove(zeroInts, respList);
                    bestGain = testWhetherBetterSplit(leftSplit, rightSplit, respList, bestGain, split,
                            beforeValue, (0.0 + former[1]) / 2);
                } else if (0.0 == former[1]) {
                    rightSplit.remove(zeroInts, respList);
                }
            }
            leftSplit.add((int)former[0], respList);
            rightSplit.remove((int)former[0], respList);
            if (zeroInts.size() > 0 && insert == i + 1 && former[1] < 0.0) {
                bestGain = testWhetherBetterSplit(leftSplit, rightSplit, respList, bestGain, split,
                        beforeValue, (0.0 + former[1]) / 2);
            } else if (latter != null) {
                if (former[1] < latter[1]) {
                    bestGain = testWhetherBetterSplit(leftSplit, rightSplit, respList, bestGain, split,
                            beforeValue, (latter[1] + former[1]) / 2);
                }
            }
        }
        return bestGain;
    }

    private void learnTreeNode(DecisionTree tree, int parentNode, boolean left,
                               int depth, List<double[]> respList,
                               IntList relevant, Int2ObjectOpenHashMap<List<double[]>> fea2sub) {
        if (relevant.size() == 0) {
            return;
        }
        Feature bestSplit = new Feature(-1, 0.0);
        if (relevant.size() > minNodeSplit && depth < maxTreeDepth && fea2sub.size() > 0) {
            double bestGain = 0.0;
            Int2DoubleMap fea2gain = new Int2DoubleOpenHashMap(fea2sub.size());
            Int2ObjectMap<Feature> fea2split = new Int2ObjectOpenHashMap<>(fea2sub.size());
            fea2sub.int2ObjectEntrySet().parallelStream().forEach(entry -> {
                int feature = entry.getIntKey();
                List<double[]> insVals = entry.getValue();
                Feature splitFea = new Feature(feature, 0.0);
                double feaGain = findBestFeatureSplit(insVals, respList, splitFea, relevant, tree);
                synchronized (fea2gain) {
                    fea2gain.put(feature, feaGain);
                    fea2split.put(feature, splitFea);
                }
            });
            for (int feature : fea2gain.keySet()) {
                if (fea2gain.get(feature) > bestGain) {
                    bestSplit = fea2split.get(feature);
                    bestGain = fea2gain.get(feature);
                }
            }
            if (bestGain <= 0.0) {
                tree.createNode(parentNode, left, relevant, respList, bestSplit);
                return;
            }
            IntOpenHashSet leftRel = new IntOpenHashSet();
            IntOpenHashSet rightRel = new IntOpenHashSet();
            List<double[]> insVals = fea2sub.get(bestSplit.getIndex());
            double splitPoint = bestSplit.getValue();
            for (int i = 0; i < insVals.size(); i++) {
                double[] insVal = insVals.get(i);
                if (insVal[1] <= splitPoint) {
                    leftRel.add((int) insVal[0]);
                } else {
                    rightRel.add((int) insVal[0]);
                }
            }
            for (int i=0; i<relevant.size(); i++) {
                int rel = relevant.getInt(i);
                if (!leftRel.contains(rel) && !rightRel.contains(rel)) {
                    if (0.0 <= splitPoint) {
                        leftRel.add(rel);
                    } else if (0.0 > splitPoint) {
                        rightRel.add(rel);
                    }
                }
            }
            Int2ObjectOpenHashMap<List<double[]>> leftFea2sub = new Int2ObjectOpenHashMap<>();
            Int2ObjectOpenHashMap<List<double[]>> rightFea2sub = new Int2ObjectOpenHashMap<>();
            for (Int2ObjectOpenHashMap.Entry<List<double[]>> entry : fea2sub.int2ObjectEntrySet()) {
                int feature = entry.getIntKey();
                insVals = entry.getValue();
                List<double[]> leftInsVals = new ArrayList<>();
                List<double[]> rightInsVals = new ArrayList<>();
                for (int i=0; i<insVals.size(); i++) {
                    double[] insVal = insVals.get(i);
                    if (leftRel.contains((int)insVal[0])) {
                        leftInsVals.add(insVal);
                    } else if (rightRel.contains((int)insVal[0])) {
                        rightInsVals.add(insVal);
                    }
                }
                if (leftInsVals.size() > 0) {
                    leftFea2sub.put(feature, leftInsVals);
                }
                if (rightInsVals.size() > 0) {
                    rightFea2sub.put(feature, rightInsVals);
                }
            }
            insVals.clear();
            fea2sub.clear();
            fea2sub.trim();
            IntList leftIntList = new IntArrayList(leftRel.toIntArray());
            leftRel.clear();
            leftRel.trim();
            IntList rightIntList = new IntArrayList(rightRel.toIntArray());
            rightRel.clear();
            rightRel.trim();

            int node = tree.createNode(parentNode, left, relevant, respList, bestSplit);
            relevant.clear();
            learnTreeNode(tree, node, true, depth + 1, respList, leftIntList, leftFea2sub);
            learnTreeNode(tree, node, false, depth + 1, respList, rightIntList, rightFea2sub);
        } else {
            tree.createNode(parentNode, left, relevant, respList, bestSplit);
        }
    }

    public void learn(DecisionTree tree, LearningData learningData) {
        List<double[]> respList = new ArrayList<>();
        Int2ObjectOpenHashMap<List<double[]>> fea2sub = new Int2ObjectOpenHashMap<>();
        int cnt = 0;
        IntList relevant = new IntArrayList();
        learningData.startNewIteration();
        List<LearningInstance> instances;
        while ((instances = learningData.getLearningInstance()).size() > 0) {
            for (LearningInstance ins : instances) {
                StandardLearningInstance treeIns = tree.getLearningInstance(ins);
                double[] resp = {treeIns.getLabel(), treeIns.getWeight()};
                respList.add(resp);
                for (Int2DoubleMap.Entry feature : treeIns.getFeatures().int2DoubleEntrySet()) {
                    int index = feature.getIntKey();
                    List<double[]> feaList;
                    if (fea2sub.containsKey(index)) {
                        feaList = fea2sub.get(index);
                    } else {
                        feaList = new ArrayList<>();
                        fea2sub.put(index, feaList);
                    }
                    double[] insVal = {cnt, feature.getDoubleValue()};
                    feaList.add(insVal);
                }
                relevant.add(cnt);
                cnt++;
                if (cnt % 10000 == 0) {
                    logger.info("Loaded {} instances.", cnt);
                }
            }
        }
        learnTreeNode(tree, -1, true, 0, respList, relevant, fea2sub);
    }
}

package org.grouplens.samantha.modeler.solver;

import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;

import java.util.List;

public class ExpectationMaximization extends AbstractOptimizationMethod {
    private OptimizationMethod method;
    
    public ExpectationMaximization() {
        super(1.0, 50, 2);
        method = new StochasticGradientDescent(3, 2, 0.0, 0.01, 10);
    }

    public ExpectationMaximization(double tol, int maxIter, int minIter,
                                   double subTol, int subMaxIter, int subMinIter,
                                   double l2coef, double learningRate) {
        super(tol, maxIter, minIter);
        method = new StochasticGradientDescent(subMaxIter, subMinIter, l2coef, learningRate, subTol);
    }

    public double minimize(LearningModel learningModel, LearningData learningData, LearningData validData) {
        LatentLearningModel model = (LatentLearningModel)learningModel;
        TerminationCriterion termCrit = new TerminationCriterion(tol, maxIter, minIter);
        double objVal = 0;
        while (termCrit.keepIterate()) {
            objVal = 0;
            learningData.startNewIteration();
            List<LearningInstance> instances;
            while ((instances = learningData.getLearningInstance()).size() > 0) {
                for (LearningInstance ins : instances) {
                    objVal += model.expectation(ins);
                }
            }
            termCrit.addIteration(objVal);
            LearningModel subModel = model.maximization();
            if (subModel != null) {
                method.minimize(subModel, learningData, validData);
            }
        }
        return objVal;
    }
}

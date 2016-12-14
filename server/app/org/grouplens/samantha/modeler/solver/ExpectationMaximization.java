package org.grouplens.samantha.modeler.solver;

import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;

import java.util.List;

public class ExpectationMaximization extends AbstractOptimizationMethod {
    private OptimizationMethod method;
    
    public ExpectationMaximization() {
        super(1.0, 50);
        method = new StochasticGradientDescent(3, 0.0, 0.01, 10);
    }

    public ExpectationMaximization(double tol, int maxIter, double subTol, int subMaxIter,
                                   double l2coef, double learningRate) {
        super(tol, maxIter);
        method = new StochasticGradientDescent(subMaxIter, l2coef, learningRate, subTol);
    }

    public double minimize(LearningModel learningModel, LearningData learningData, LearningData validData) {
        LatentLearningModel model = (LatentLearningModel)learningModel;
        TerminationCriterion termCrit = new TerminationCriterion(tol, maxIter);
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

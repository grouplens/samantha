package org.grouplens.samantha.modeler.solver;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.space.VariableSpace;

import javax.inject.Inject;
import java.util.List;

public class ProximalGradientMethod extends AbstractOptimizationMethod {
    final private double l1coef;
    final private double l2coef;
    final private double ro;
    @Inject private VariableSpace variableSpace;

    public ProximalGradientMethod() {
        super(5.0, 50);
        this.l1coef = 4.0;
        this.l2coef = 0.1;
        this.ro = 5.0;
    }

    protected double update(LearningModel model, LearningData learningData) {
        List<String> allScalarVarNames = model.getAllScalarVarNames();
        for (String name : allScalarVarNames) {
            variableSpace.requestScalarVar(name, 0, 0.0, false);
        }
        List<String> allVectorVarNames = model.getAllVectorVarNames();
        for (String name : allVectorVarNames) {
            int dim = model.getVectorVarDimensionByName(name);
            variableSpace.requestVectorVar(name, 0, dim, 0.0, false, false);
        }
        double objVal = 0.0;
        List<LearningInstance> instances;
        ObjectiveFunction objective = model.getObjectiveFunction();
        while ((instances = learningData.getLearningInstance()).size() > 0) {
            List<StochasticOracle> oracles = model.getStochasticOracle(instances);
            oracles = objective.wrapOracle(oracles);
            for (StochasticOracle oracle : oracles) {
                objVal += oracle.getObjectiveValue();
                for (int i = 0; i < oracle.scalarNames.size(); i++) {
                    String name = oracle.scalarNames.get(i);
                    int idx = oracle.scalarIndexes.getInt(i);
                    double grad = oracle.scalarGrads.getDouble(i);
                    variableSpace.ensureScalarVar(name, idx + 1, 0.0, false);
                    variableSpace.setScalarVarByNameIndex(name, idx,
                            variableSpace.getScalarVarByNameIndex(name, idx) + grad);
                }
                for (int i = 0; i < oracle.vectorNames.size(); i++) {
                    String name = oracle.vectorNames.get(i);
                    int idx = oracle.vectorIndexes.getInt(i);
                    RealVector grad = oracle.vectorGrads.get(i);
                    variableSpace.ensureVectorVar(name, idx + 1, grad.getDimension(),
                            0.0, false, false);
                    variableSpace.setVectorVarByNameIndex(name, idx, grad.combineToSelf(1.0, 1.0,
                            variableSpace.getVectorVarByNameIndex(name, idx)));
                }
            }
        }
        for (String name : allScalarVarNames) {
            for (int i=0; i<variableSpace.getScalarVarSizeByName(name); i++) {
                double eta = variableSpace.getScalarVarByNameIndex(name, i) -
                        model.getScalarVarByNameIndex(name, i) * ro;
                double value = 0.0;
                if (eta > l1coef) {
                    value = (l1coef - eta) / (l2coef + ro);
                } else if (eta < -l1coef) {
                    value = (-eta - l1coef) / (l2coef + ro);
                }
                model.setScalarVarByNameIndex(name, i, value);
            }
        }
        for (String name : allVectorVarNames) {
            int dim = variableSpace.getVectorVarDimensionByName(name);
            for (int i=0; i<variableSpace.getVectorVarSizeByName(name); i++) {
                RealVector etas = variableSpace.getVectorVarByNameIndex(name, i)
                        .combineToSelf(1.0, -ro, model.getVectorVarByNameIndex(name, i));
                RealVector value = MatrixUtils.createRealVector(new double[dim]);
                for (int j=0; j<dim; j++) {
                    double eta = etas.getEntry(j);
                    if (eta > l1coef) {
                        value.setEntry(j, (l1coef - eta) / (l2coef + ro));
                    } else if (eta < -l1coef) {
                        value.setEntry(j, (-eta - l1coef) / (l2coef + ro));
                    }
                }
                model.setVectorVarByNameIndex(name, i, value);
            }
        }
        return objVal;
    }
}

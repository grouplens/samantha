package org.grouplens.samantha.modeler.svdfeature;

import org.grouplens.samantha.modeler.solver.*;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class SVDFeatureModelBuildTest {

    @Test
    public void testModelBuildFromEntityDAO() throws IOException {
        /*
        String insFile = "/Users/qian/Study/yahoointern/MFTree/data/trainIns.txt";
        String validFile = "/Users/qian/Study/yahoointern/MFTree/data/validIns.txt";
        String modelFile = "/Users/qian/Study/yahoointern/MFTree/data/svdModel.txt";

        int biasSize = 48500;
        int factSize = 48500;
        int factDim = 20;

        //ObjectiveFunction loss = new LogisticLoss();
        ObjectiveFunction loss = new L2NormLoss();
        OptimizationMethod method = new StochasticGradientDescent(50, 1.0, 0.001, 5.0);
        //OptimizationMethod method = new StochasticGradientDescent(50, 0.0, 0.00000001, 5.0);
        SVDFeatureInstanceDAO insDao = new SVDFeatureInstanceDAO(new File(insFile), " ");
        SVDFeatureInstanceDAO validDao = new SVDFeatureInstanceDAO(new File(validFile), " ");

        SVDFeatureModel model = new SVDFeatureModel(biasSize, factSize, factDim, loss);
        method.minimize(model, insDao, validDao);
        model.dump(new File(modelFile));
        */
    }
}

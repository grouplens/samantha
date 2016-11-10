package org.grouplens.samantha.server.retriever;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Ordering;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.SpaceProducer;
import org.grouplens.samantha.modeler.space.VariableSpace;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureModel;
import org.grouplens.samantha.modeler.svdfeature.FeatureKnnModel;
import org.grouplens.samantha.server.common.ModelService;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.exception.InvalidRequestException;
import org.grouplens.samantha.server.io.RequestContext;
import play.inject.Injector;

import javax.annotation.Nullable;
import java.io.*;
import java.util.List;

import static org.grouplens.samantha.modeler.tree.SortingUtilities.compareValues;

public class RetrieverUtilities {

    private RetrieverUtilities() {}

    static public FeatureKnnModel getFeatureKnnModel(SamanthaConfigService configService, ModelService modelService,
                                                     RequestContext requestContext, boolean toBuild,
                                                     String knnModelName, String predictorName,
                                                     String predictorModelName, List<String> itemAttrs,
                                                     int numNeighbors, boolean reverse, int minSupport,
                                                     Injector injector) {
        String engineName = requestContext.getEngineName();
        if (!toBuild && modelService.hasModel(engineName, knnModelName)) {
            return (FeatureKnnModel)modelService.getModel(engineName, knnModelName);
        } else {
            configService.getPredictor(predictorName, requestContext);
            SVDFeatureModel svdFeatureModel = (SVDFeatureModel) modelService.getModel(engineName,
                    predictorModelName);
            SpaceProducer spaceProducer = injector.instanceOf(SpaceProducer.class);
            IndexSpace indexSpace = spaceProducer.getIndexSpace(knnModelName);
            VariableSpace variableSpace = spaceProducer.getVariableSpace(knnModelName);
            FeatureKnnModel knnModel = new FeatureKnnModel(knnModelName, itemAttrs,
                    numNeighbors, reverse, minSupport, svdFeatureModel, indexSpace, variableSpace);
            if (toBuild) {
                knnModel.buildModel();
            }
            modelService.setModel(engineName, knnModelName, knnModel);
            return knnModel;
        }
    }

    static public void dumpModel(Object model, String modelFile) {
        try {
            String tmpFile = modelFile + ".tmp";
            ObjectOutputStream fout = new ObjectOutputStream(new FileOutputStream(tmpFile));
            fout.writeObject(model);
            fout.close();
            File file = new File(tmpFile);
            file.renameTo(new File(modelFile));
        } catch (IOException e) {
            throw new InvalidRequestException(e);
        }
    }

    static public Object loadModel(ModelService modelService, String engineName, String modelName,
                                      String modelFile) {
        try {
            ObjectInputStream fin = new ObjectInputStream(new FileInputStream(modelFile));
            Object model = fin.readObject();
            fin.close();
            modelService.setModel(engineName, modelName, model);
            return model;
        } catch (IOException | ClassNotFoundException e) {
            throw new InvalidRequestException(e);
        }
    }

    static public Ordering<ObjectNode> jsonFieldOrdering(String field) {
        return new Ordering<ObjectNode>() {
            private String orderField = field;
            @Override
            public int compare(@Nullable ObjectNode left, @Nullable ObjectNode right) {
                if (left.has(orderField)) {
                    double leftValue = left.get(orderField).asDouble();
                    double rightValue = right.get(orderField).asDouble();
                    return compareValues(leftValue, rightValue);
                } else {
                    return 0;
                }
            }
        };
    }
}

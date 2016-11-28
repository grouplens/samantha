package org.grouplens.samantha.server.retriever;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Ordering;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.SpaceProducer;
import org.grouplens.samantha.modeler.space.VariableSpace;
import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.modeler.knn.FeatureKnnModel;
import org.grouplens.samantha.server.common.ModelService;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import play.inject.Injector;

import javax.annotation.Nullable;
import java.util.List;

import static org.grouplens.samantha.modeler.tree.SortingUtilities.compareValues;

public class RetrieverUtilities {

    private RetrieverUtilities() {}

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

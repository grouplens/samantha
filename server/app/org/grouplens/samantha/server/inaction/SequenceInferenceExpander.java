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

package org.grouplens.samantha.server.inaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.grouplens.samantha.modeler.common.PredictiveModel;
import org.grouplens.samantha.modeler.dao.CSVFileDAO;
import org.grouplens.samantha.modeler.featurizer.Featurizer;
import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.modeler.tensorflow.TensorFlowModel;
import org.grouplens.samantha.server.common.ModelService;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SequenceInferenceExpander implements EntityExpander {
    final private List<String> nameAttrs;
    final private List<String> valueAttrs;
    final private Set<String> excludedLabels;
    final private List<String> historyAttrs;
    final private String tstampAttr;
    final private int splitTstamp;
    final private String separator;
    final private String joiner;
    final private Integer maxStepNum;
    final private boolean backward;
    final private EntityExpander inferenceExpander;

    public SequenceInferenceExpander(List<String> nameAttrs, List<String> valueAttrs,
                                     List<String> excludedLabels, List<String> historyAttrs, String separator,
                                     String joiner, Integer maxStepNum, boolean backward,
                                     String tstampAttr, int splitTstamp, EntityExpander inferenceExpander) {
        this.nameAttrs = nameAttrs;
        this.valueAttrs = valueAttrs;
        if (excludedLabels != null) {
            this.excludedLabels = new HashSet<>(excludedLabels);
        } else {
            this.excludedLabels = new HashSet<>();
        }
        this.historyAttrs = historyAttrs;
        this.joiner = joiner;
        this.separator = separator;
        this.maxStepNum = maxStepNum;
        this.backward = backward;
        this.splitTstamp = splitTstamp;
        this.tstampAttr = tstampAttr;
        this.inferenceExpander = inferenceExpander;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        ModelService modelService = injector.instanceOf(ModelService.class);
        String predictorName = expanderConfig.getString("predictorName");
        String modelName = expanderConfig.getString("modelName");
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        configService.getPredictor(predictorName, requestContext);
        Object rawModel = modelService.getModel(
                requestContext.getEngineName(), modelName);
        PredictiveModel model = (PredictiveModel) rawModel;
        Featurizer featurizer = (Featurizer) rawModel;

        String tfPredictorName = "tensorFlowInteractionPredictor";
        String tfModelName = "tensorFlowInteractionPredictorModel";
        String ratingPredictorName = "inactionRatingFMPredictor";
        String ratingModelName = "inactionRatingFMPredictorModel";
        String ratedPredictorName = "inactionRatedFMPredictor";
        String ratedModelName = "inactionRatedFMPredictorModel";
        String clickPredictorName = "inactionClickFMPredictor";
        String clickModelName = "inactionClickFMPredictorModel";
        String wishlistPredictorName = "inactionWishlistFMPredictor";
        String wishlistModelName = "inactionWishlistFMPredictorModel";
        String itemInfoFile = "/opt/samantha/learning/historyMovieData.tsv";

        configService.getPredictor(tfPredictorName, requestContext);
        configService.getPredictor(ratingPredictorName, requestContext);
        configService.getPredictor(ratedPredictorName, requestContext);
        configService.getPredictor(clickPredictorName, requestContext);
        configService.getPredictor(wishlistPredictorName, requestContext);
        TensorFlowModel tfModel = (TensorFlowModel) modelService.getModel(
                requestContext.getEngineName(), tfModelName);
        SVDFeature ratingModel = (SVDFeature) modelService.getModel(
                requestContext.getEngineName(), ratingModelName);
        SVDFeature ratedModel = (SVDFeature) modelService.getModel(
                requestContext.getEngineName(), ratedModelName);
        SVDFeature clickModel = (SVDFeature) modelService.getModel(
                requestContext.getEngineName(), clickModelName);
        SVDFeature wishlistModel = (SVDFeature) modelService.getModel(
                requestContext.getEngineName(), wishlistModelName);
        String itemAttr = "movieId";
        List<JsonNode> item2info = new ArrayList<>();
        CSVFileDAO dao = new CSVFileDAO("\t", itemInfoFile);
        while (dao.hasNextEntity()) {
            ObjectNode item = dao.getNextEntity();
            int idx = item.get(itemAttr).asInt();
            while (item2info.size() < idx) {
                item2info.add(null);
            }
            item2info.add(item);
        }
        dao.close();

        EntityExpander inferenceExpander = new InferenceExpander(
                expanderConfig.getString("labelAttr"),
                expanderConfig.getInt("numClass"),
                expanderConfig.getString("joiner"),
                model, featurizer, tfModel,
                ratingModel, ratedModel, clickModel, wishlistModel, item2info);

        Boolean backward = expanderConfig.getBoolean("backward");
        if (backward == null) {
            backward = false;
        }
        Integer splitTstamp = expanderConfig.getInt("splitTstamp");
        if (splitTstamp == null) {
            splitTstamp = 0;
        }
        return new SequenceInferenceExpander(
                expanderConfig.getStringList("nameAttrs"),
                expanderConfig.getStringList("valueAttrs"),
                expanderConfig.getStringList("excludedLabels"),
                expanderConfig.getStringList("historyAttrs"),
                expanderConfig.getString("separator"),
                expanderConfig.getString("joiner"),
                expanderConfig.getInt("maxStepNum"), backward,
                expanderConfig.getString("tstampAttr"), splitTstamp,
                inferenceExpander);
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        List<ObjectNode> expanded = new ArrayList<>();
        for (ObjectNode entity : initialResult) {
            List<String[]> values = new ArrayList<>();
            int size = 0;
            for (String nameAttr : nameAttrs) {
                String[] splitted = entity.get(nameAttr).asText().split(separator, -1);
                size = splitted.length;
                values.add(splitted);
            }
            int start = 0;
            int end = size;
            if (tstampAttr != null) {
                String[] fstamp = entity.get(tstampAttr).asText().split(separator, -1);
                int i;
                for (i=0; i<size; i++) {
                    String tstamp = fstamp[i];
                    int istamp = Integer.parseInt(tstamp);
                    if (istamp >= splitTstamp) {
                        break;
                    }
                }
                if (backward) {
                    end = i;
                } else {
                    start = i;
                }
                size = end - start;
            }
            if (maxStepNum != null && maxStepNum < size) {
                if (backward) {
                    start = end - maxStepNum;
                } else {
                    end = start + maxStepNum;
                }
            }
            for (int i=start; i<end; i++) {
                if (!excludedLabels.contains(values.get(0)[i])) {
                    ObjectNode newEntity = entity.deepCopy();
                    for (int j = 0; j < values.size(); j++) {
                        newEntity.put(valueAttrs.get(j), values.get(j)[i]);
                        newEntity.put(historyAttrs.get(j), StringUtils.join(
                                ArrayUtils.subarray(values.get(j), 0, i), joiner));
                    }
                    List<ObjectNode> inferredList = new ArrayList<>();
                    inferredList.add(newEntity);
                    inferredList = inferenceExpander.expand(inferredList, requestContext);
                    for (ObjectNode inferred : inferredList) {
                        for (int j=0; j<values.size(); j++) {
                            inferred.remove(historyAttrs.get(j));
                        }
                    }
                    expanded.addAll(inferredList);
                }
            }
        }
        return expanded;
    }
}

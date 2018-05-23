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
import org.apache.commons.lang3.StringUtils;
import org.grouplens.samantha.modeler.common.LearningInstance;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InferenceExpander implements EntityExpander {
    private final String labelAttr;
    private final int numClass;
    private final String joiner;
    private final PredictiveModel predictiveModel;
    private final Featurizer featurizer;
    private final TensorFlowModel model;
    private final SVDFeature ratingModel;
    private final SVDFeature ratedModel;
    private final SVDFeature clickModel;
    private final SVDFeature wishlistModel;
    private final List<JsonNode> item2info;

    public InferenceExpander(String labelAttr, int numClass, String joiner,
                             PredictiveModel predictiveModel,
                             Featurizer featurizer,
                             TensorFlowModel model,
                             SVDFeature ratingModel, SVDFeature ratedModel,
                             SVDFeature clickModel, SVDFeature wishlistModel,
                             List<JsonNode> item2info) {
        this.labelAttr = labelAttr;
        this.numClass = numClass;
        this.joiner = joiner;
        this.predictiveModel = predictiveModel;
        this.featurizer = featurizer;
        this.model = model;
        this.ratedModel = ratedModel;
        this.ratingModel = ratingModel;
        this.clickModel = clickModel;
        this.wishlistModel = wishlistModel;
        this.item2info = item2info;
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
        return new InferenceExpander(
                expanderConfig.getString("labelAttr"),
                expanderConfig.getInt("numClass"),
                expanderConfig.getString("joiner"),
                model, featurizer, tfModel,
                ratingModel, ratedModel, clickModel, wishlistModel, item2info);
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        String userAttr = "userId";
        String itemAttr = "movieId";
        String tstampAttr = "tstamp";
        String[] historyAttrs = InactionUtilities.historyAttrs;
        Map<String, String[]> attr2seq = new HashMap<>();
        for (ObjectNode entity : initialResult) {
            List<String> predArr = new ArrayList<>();
            for (int i=0; i<numClass; i++) {
                predArr.add(Double.valueOf(0.0).toString());
            }
            entity.put("prevTstamp", -1);
            entity.put("prevSessionId", "None");
            entity.put("prevAction", -1);
            String itemIdsStr = entity.get(itemAttr + "s").asText();
            if (!"".equals(itemIdsStr)) {
                String user = entity.get(userAttr).asText();
                String item = entity.get(itemAttr).asText();
                int tstamp = entity.get(tstampAttr).asInt();
                for (String attr : historyAttrs) {
                    if (entity.has(attr)) {
                        String[] seq = entity.get(attr).asText().split(",", -1);
                        attr2seq.put(attr, seq);
                    }
                }
                int index = -1;
                String[] itemIds = attr2seq.get(itemAttr + "s");
                for (int i=0; i<itemIds.length; i++) {
                    String[] tstamps = attr2seq.get(tstampAttr + "s");
                    if (Integer.parseInt(tstamps[i]) < tstamp && itemIds[i].equals(item)) {
                        index = i;
                    }
                }
                if (index >= 0) {
                    ObjectNode features = InactionUtilities.getFeatures(
                            attr2seq, index, model, ratingModel, ratedModel, clickModel, wishlistModel,
                            item2info, itemAttr, userAttr, user);
                    features.put(userAttr, user);
                    features.put(itemAttr, item);
                    int prevTstamp = Integer.parseInt(attr2seq.get(tstampAttr + "s")[index]);
                    features.put(tstampAttr, prevTstamp);
                    LearningInstance instance = featurizer.featurize(features, false);
                    double[] preds = predictiveModel.predict(instance);
                    for (int i=0; i<preds.length; i++) {
                        predArr.set(i, Double.valueOf(preds[i]).toString());
                    }
                    entity.put("prevTstamp", prevTstamp);
                    entity.put("prevSessionId", attr2seq.get("sessionIds")[index]);
                    entity.put("prevAction", attr2seq.get("actions")[index]);
                }
            }
            entity.put(labelAttr, StringUtils.join(predArr, joiner));
        }
        return initialResult;
    }
}

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

package org.grouplens.samantha.server.predictor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class FieldBlendingPredictor extends AbstractPredictor {
    private final List<EntityExpander> entityExpanders;
    private final Object2DoubleMap<String> defaults;

    public FieldBlendingPredictor(Configuration config, Configuration daoConfigs, Injector injector,
                                  List<EntityExpander> entityExpanders,
                                  Object2DoubleMap<String> defaults, String daoConfigKey) {
        super(config, daoConfigs, daoConfigKey, injector);
        this.entityExpanders = entityExpanders;
        this.defaults = defaults;
    }

    public List<Prediction> predict(List<ObjectNode> entityList, RequestContext requestContext) {
        for (EntityExpander expander : entityExpanders) {
            entityList = expander.expand(entityList, requestContext);
        }
        List<Prediction> scoredList = new ArrayList<>(entityList.size());
        for (ObjectNode entity : entityList) {
            double score = 0.0;
            for (Object2DoubleMap.Entry<String> entry : defaults.object2DoubleEntrySet()) {
                String key = entry.getKey();
                if (entity.has(key)) {
                    score += (entry.getDoubleValue() * entity.get(key).asDouble());
                }
            }
            scoredList.add(new Prediction(entity, null, score));
        }
        return scoredList;
    }
}

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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.*;

public class InactionExpander implements EntityExpander {

    private final String labelAttr;
    private final Set<String> includedLabels;

    public InactionExpander(String labelAttr, List<String> includedLabels) {
        this.labelAttr = labelAttr;
        if (includedLabels != null) {
            this.includedLabels = new HashSet<>(includedLabels);
        } else {
            this.includedLabels = new HashSet<>();
        }
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        return new InactionExpander(
                expanderConfig.getString("labelAttr"),
                expanderConfig.getStringList("includedLabels"));
    }

    private void preprocess(ObjectNode sur) {
        String notice = sur.get("noticeSur").asText();
        if (notice.equals("DidNotNotice") || notice.equals("NotDisplayed")) {
            notice = "NotNoticed";
        }
        sur.put("noticedSur", notice);
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        List<ObjectNode> expanded = new ArrayList<>();
        for (ObjectNode sur : initialResult) {
            preprocess(sur);
            if (includedLabels.contains(sur.get(labelAttr).asText())) {
                expanded.add(sur);
            }
        }
        return expanded;
    }
}

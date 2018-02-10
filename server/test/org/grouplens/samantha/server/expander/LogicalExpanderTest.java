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

package org.grouplens.samantha.server.expander;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.grouplens.samantha.TestUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import org.junit.Test;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class LogicalExpanderTest {
    private final List<String> sourceAttrs = Lists.newArrayList("click", "rating", "wishlist");
    private final String valueAttr = "acted";

    @Test
    public void testAndExpand() {
        List<ObjectNode> entities = new ArrayList<>();
        TestUtilities.setUpUserMultiTypeSequence(entities);
        LogicalExpander expander = new LogicalExpander(sourceAttrs, valueAttr, "\\|",
                false, "|");
        List<ObjectNode> expanded = expander.expand(entities, new RequestContext(Json.newObject(), "test"));
        assertEquals(2, expanded.size());
        assertEquals("0|0|0|0|0|1", expanded.get(0).get(valueAttr).asText());
        assertEquals("0", expanded.get(1).get(valueAttr).asText());
    }

    @Test
    public void testOrExpand() {
        List<ObjectNode> entities = new ArrayList<>();
        TestUtilities.setUpUserMultiTypeSequence(entities);
        LogicalExpander expander = new LogicalExpander(sourceAttrs, valueAttr, "\\|",
                true, "|");
        List<ObjectNode> expanded = expander.expand(entities, new RequestContext(Json.newObject(), "test"));
        assertEquals(2, expanded.size());
        assertEquals("1|0|1|1|0|1", expanded.get(0).get(valueAttr).asText());
        assertEquals("1", expanded.get(1).get(valueAttr).asText());
    }
}

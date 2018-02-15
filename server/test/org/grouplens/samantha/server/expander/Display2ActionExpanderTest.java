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

public class Display2ActionExpanderTest {
    private final List<ObjectNode> entities = new ArrayList<>();
    private final List<String> nameAttrs = Lists.newArrayList("item", "action", "tstamp");
    private final List<String> valueAttrs = Lists.newArrayList("item", "action", "tstamp");
    private final String actionName = "action";

    public Display2ActionExpanderTest() {
        TestUtilities.setUpUserSequence(entities);
    }

    @Test
    public void testExpand() {
        Display2ActionExpander expander = new Display2ActionExpander(nameAttrs, valueAttrs, "\\|",
                actionName, "|", null, 0, 0, 1, null);
        List<ObjectNode> expanded = expander.expand(entities, new RequestContext(Json.newObject(), "test"));
        assertEquals(1, expanded.size());
        assertEquals("1|6", expanded.get(0).get("tstamp").asText());
        assertEquals("10|5", expanded.get(0).get("item").asText());
        assertEquals("1|1", expanded.get(0).get("action").asText());
    }

    @Test
    public void testExpandWithLimit() {
        Display2ActionExpander expander = new Display2ActionExpander(nameAttrs, valueAttrs, "\\|",
                actionName, "|", "tstamp", 3, 2, 2, "rank");
        List<ObjectNode> expanded = expander.expand(entities, new RequestContext(Json.newObject(), "test"));
        assertEquals(1, expanded.size());
        assertEquals("1", expanded.get(0).get("tstamp").asText());
        assertEquals("10", expanded.get(0).get("item").asText());
        assertEquals("1", expanded.get(0).get("action").asText());
    }
}

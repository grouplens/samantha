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

package org.grouplens.samantha;

import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;

import java.util.List;

public class TestUtilities {

    static public void setUpUserSequence(List<ObjectNode> entities) {
        ObjectNode entity1 = Json.newObject();
        entity1.put("user", "123");
        entity1.put("item", "10|2|10|7|4|5");
        entity1.put("action", "1|0|0|0|0|1");
        entity1.put("tstamp", "1|2|3|4|5|6");
        entity1.put("rank", "0|1|0|0|0|1");
        entities.add(entity1);
        ObjectNode entity2 = Json.newObject();
        entity2.put("user", "455");
        entity2.put("item", "5");
        entity2.put("action", "0");
        entity2.put("tstamp", "1");
        entity2.put("rank", "0");
        entities.add(entity2);
    }

    static public void setUpUserMultiTypeSequence(List<ObjectNode> entities) {
        ObjectNode entity1 = Json.newObject();
        entity1.put("user", "123");
        entity1.put("item", "10|2|10|7|4|5");
        entity1.put("click", "1|0|0|0|0|1");
        entity1.put("rating", "0|0|1|0|0|1");
        entity1.put("wishlist", "1|0|0|1|0|1");
        entity1.put("tstamp", "1|2|3|4|5|6");
        entities.add(entity1);
        ObjectNode entity2 = Json.newObject();
        entity2.put("user", "455");
        entity2.put("item", "5");
        entity2.put("click", "0");
        entity2.put("rating", "1");
        entity2.put("wishlist", "0");
        entity2.put("tstamp", "1");
        entities.add(entity2);
    }
}

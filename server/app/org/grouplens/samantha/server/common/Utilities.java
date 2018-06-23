/*
 * Copyright (c) [2016-2018] [University of Minnesota]
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

package org.grouplens.samantha.server.common;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Utilities {
    private static Logger logger = LoggerFactory.getLogger(Utilities.class);
    public static String composeKey(String prefix, String key) {
        return prefix + "\1" + key;
    }

    public static String composeKey(JsonNode entity, List<String> keyAttrs) {
        List<String> keys = new ArrayList<>();
        for (String attr : keyAttrs) {
            keys.add(composeKey(attr, entity.get(attr).asText()));
        }
        return StringUtils.join(keys, "\t");
    }

    public static boolean checkKeyAttributesComplete(JsonNode entity, List<String> keyAttrs) {
        boolean complete = true;
        for (String attr : keyAttrs) {
            if (!entity.has(attr)) {
                logger.warn("One key attribute {} in {} is missing from {}", attr, keyAttrs, entity);
                complete = false;
            }
        }
        return complete;
    }

    public static boolean isInHosts(List<String> hosts) {
        Set<String> shosts = new HashSet<>(hosts);
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            String port = System.getProperty("http.port");
            logger.info("The host of the current server instance is {}", hostName + ":" + port);
            if (!shosts.contains(hostName + ":" + port)) {
                return false;
            } else {
                return true;
            }
        } catch (UnknownHostException e) {
            logger.error(e.getMessage());
            return false;
        }
    }
}

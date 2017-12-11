package org.grouplens.samantha.server.common;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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
}

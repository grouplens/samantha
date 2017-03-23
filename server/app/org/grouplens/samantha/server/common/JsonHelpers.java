package org.grouplens.samantha.server.common;

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;

import java.util.*;

public class JsonHelpers {

    private JsonHelpers() {}

    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(JsonHelpers.class);

    /**
     * See:
     * http://labs.omniti.com/labs/jsend
     * http://stackoverflow.com/questions/12806386/standard-json-api-response-format
     */
    interface JsonKeys {
        /**
         * Should be one of JsonStatusTypes.
         *
         * Required.
         */
        String STATUS = "status";

        /**
         * This message should contain information useful to client
         * developers, but not data for display on the screen.
         *
         * Used with: fail/error
         */
        String MESSAGE = "message";

        /**
         * If success, this is the data object for rendering by the client.
         *
         * Used with: success
         */
        String DATA = "data";

        /**
         * If validation fails, this is the data object for storing error messages.
         *
         * Used with: fail
         */
        String VALIDATION_ERRORS = "validationErrors";
    }

    /**
     * Values used with JsonKeys.STATUS
     */
    interface JsonStatusTypes {
        /**
         * Yay!
         */
        String SUCCESS = "success";

        /**
         * We can't fulfill the request.  The code didn't break, but something is wrong.
         */
        String FAIL = "fail";

        /**
         * The code broke or the system is down.
         */
        String ERROR = "error";
    }

    private static ObjectNode baseJson(String statusType) {
        ObjectNode json = Json.newObject();
        json.put(JsonKeys.STATUS, statusType);
        return json;
    }

    /**
     * @return the start of a json object with status:success pre-populated
     */
    public static ObjectNode successJson() {
        return baseJson(JsonStatusTypes.SUCCESS);
    }

    /**
     * @return the start of a json object with status:fail pre-populated
     */
    public static ObjectNode failJson() {
        return baseJson(JsonStatusTypes.FAIL);
    }

    /**
     * @return the start of a json object with status:error pre-populated
     */
    public static ObjectNode errorJson() {
        return baseJson(JsonStatusTypes.ERROR);
    }

    /**
     * @return a json object from the input JsonNode with the given name, or throw an BadRequestException
     */
    public static JsonNode getRequiredJson(JsonNode json, String name) throws BadRequestException {
        JsonNode node = json.get(name);
        if (node == null || !node.isContainerNode()) {
            throw new BadRequestException("json is missing required object: " + name);
        }
        return node;
    }

    /**
     * @return an array object from the input JsonNode with the given name, or throw an BadRequestException
     */
    public static ArrayNode getRequiredArray(JsonNode json, String name) throws BadRequestException {
        JsonNode node = json.get(name);
        if (node == null || !node.isArray()) {
            throw new BadRequestException("json is missing required array: " + name);
        }
        return (ArrayNode)node;
    }

    public static List<String> getRequiredStringList(JsonNode json, String name) throws BadRequestException {
        JsonNode node = json.get(name);
        if (node == null || !node.isArray()) {
            throw new BadRequestException("json is missing required array: " + name);
        }
        List<String> values = new ArrayList<>(node.size());
        for (JsonNode one : node) {
            values.add(one.asText());
        }
        return values;
    }

    /**
     * @return a json object from the input JsonNode with the given name, or null
     */
    public static JsonNode getOptionalJson(JsonNode json, String name) throws BadRequestException {
        JsonNode node = json.get(name);
        if (node == null) {
            return null;
        }
        if (!node.isContainerNode()) {
            throw new BadRequestException("json is missing required object: " + name);
        }
        return node;
    }

    /**
     * @return a double from the input JsonNode with the given name, or throw an BadRequestException
     */
    public static double getRequiredDouble(JsonNode json, String name) throws BadRequestException {
        JsonNode node = json.get(name);
        if (node == null || !node.isNumber()) {
            throw new BadRequestException("json is missing required double: " + name);
        }
        return node.asDouble();
    }

    /**
     * @return an int from the input JsonNode with the given name, or throw an BadRequestException
     */
    public static int getRequiredInt(JsonNode json, String name) throws BadRequestException {
        JsonNode node = json.get(name);
        if (node == null || !node.canConvertToInt()) {
            throw new BadRequestException("json is missing required int: " + name);
        }
        return node.asInt();
    }

    /**
     * @return a Long from the input JsonNode with the given name, or the default value
     */
    public static Long getRequiredLong(JsonNode json, String name) throws BadRequestException {
        JsonNode node = json.get(name);
        if (node == null || !node.canConvertToLong()) {
            throw new BadRequestException("json is missing required long: " + name);
        }
        return node.asLong();
    }
    /**
     * @return an Integer from the input JsonNode with the given name, or the default value
     */
    public static Integer getOptionalInt(JsonNode json, String name, Integer defaultInt) throws BadRequestException {
        JsonNode node = json.get(name);
        if (node == null) {
            return defaultInt;
        }
        if (!node.canConvertToInt()) {
            throw new BadRequestException("json is not an int: " + name);
        }
        return node.asInt();
    }

    /**
     * @return a Long from the input JsonNode with the given name, or the default value
     */
    public static Long getOptionalLong(JsonNode json, String name, Long defaultLong) throws BadRequestException {
        JsonNode node = json.get(name);
        if (node == null) {
            return defaultLong;
        }
        if (!node.canConvertToLong()) {
            throw new BadRequestException("json is not a long: " + name);
        }
        return node.asLong();
    }

    /**
     * @return an Integer from the input JsonNode with the given name, or null
     */
    public static Integer getOptionalInt(JsonNode json, String name) throws BadRequestException {
        return getOptionalInt(json, name, null);
    }

    /**
     * @return a string from the input JsonNode with the given name, or throw an BadRequestException
     */
    public static String getRequiredString(JsonNode json, String name) throws BadRequestException {
        JsonNode node = json.get(name);
        if (node == null || !node.isTextual()) {
            throw new BadRequestException("json is missing required String: " + name);
        }
        return node.asText();
    }

    /**
     * @return a String from the input JsonNode with the given name, or null
     */
    public static String getOptionalString(JsonNode json, String name) throws BadRequestException {
        return getOptionalString(json, name, null);
    }

    /**
     * @return a String from the input JsonNode with the given name, or the defaultVal
     */
    public static String getOptionalString(JsonNode json, String name, String defaultVal) throws BadRequestException {
        JsonNode node = json.get(name);
        if (node == null) {
            return defaultVal;
        }
        if (!node.isTextual()) {
            throw new BadRequestException("json is not a string: " + name);
        }
        return node.asText();
    }

    /**
     * @return a List<String> from the input JsonNode with the given name, or the defaultVal
     */
    public static List<String> getOptionalStringList(JsonNode json, String name, List<String> defaultVal)
            throws BadRequestException {
        JsonNode node = json.get(name);
        if (node == null) {
            return defaultVal;
        }
        if (!node.isArray()) {
            throw new BadRequestException("json is not a string list: " + name);
        }
        List<String> strList = new ArrayList<>();
        for (JsonNode str : node) {
            strList.add(str.asText());
        }
        return strList;
    }

    /**
     * @return a Boolean from the input JsonNode with the given name, or null
     */
    public static Boolean getOptionalBoolean(JsonNode json, String name) throws BadRequestException {
        return getOptionalBoolean(json, name, null);
    }

    /**
     * @return a Boolean from the input JsonNode with the given name, or the defaultVal
     */
    public static Boolean getOptionalBoolean(JsonNode json, String name, Boolean defaultVal) throws BadRequestException {
        JsonNode node = json.get(name);
        if (node == null) {
            return defaultVal;
        }
        if (!node.isBoolean()) {
            throw new BadRequestException("json is not a boolean: " + name);
        }
        return node.asBoolean();
    }

    /**
     * @return a IntList from the input JsonNode with the given name, or throw an BadRequestException
     */
    public static IntList getRequiredListOfInteger(JsonNode json, String name) throws BadRequestException {
        final JsonNode node = json.get(name);

        if (node == null || !(node.isArray())) {
            throw new BadRequestException("json is missing required List: " + name);
        }

        final IntList list = new IntArrayList(node.size());
        for (JsonNode innerNode : node) {
            if (!innerNode.canConvertToInt()) {
                throw new BadRequestException("json is not an int: " + innerNode.toString());
            }
            list.add(innerNode.asInt());
        }
        return list;
    }

    /**
     * @return a Map<Integer, Integer> from the input JsonNode with the given name, or throw an BadRequestException.
     *         Must be a flat JsonNode or it will throw an BadRequestException.
     */
    public static Map<Integer, Integer> getRequiredIntegerToIntegerMap(JsonNode json, String name) throws  BadRequestException {
        final JsonNode node = json.get(name);

        if (node == null || node.isNull() || !(node.isObject())) {
            throw new BadRequestException("json is missing required object: " + name);
        }

        final Map<Integer, Integer> map = new HashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> iterator = node.fields(); iterator.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            final String key = entry.getKey();
            final Integer value = getRequiredInt(node, key);

            try {
                map.put(Integer.parseInt(key), value);
            } catch (NumberFormatException e) {
                throw new BadRequestException("key '" + key + "' is not an integer");
            }
        }

        return map;
    }

    /**
     * @return a Map<Integer, Double> from the input JsonNode with the given name, or throw an BadRequestException.
     *         Must be a flat JsonNode or it will throw an BadRequestException.
     */
    public static Map<Integer, Double> getRequiredIntegerToDoubleMap(JsonNode json, String name) throws  BadRequestException {
        final JsonNode node = json.get(name);

        if (node == null || node.isNull() || !(node.isObject())) {
            throw new BadRequestException("json is missing required object: " + name);
        }

        final Map<Integer, Double> map = new HashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> iterator = node.fields(); iterator.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            final String key = entry.getKey();
            final Double value = getRequiredDouble(node, key);

            try {
                map.put(Integer.parseInt(key), value);
            } catch (NumberFormatException e) {
                throw new BadRequestException("key '" + key + "' is not an integer");
            }
        }

        return map;
    }

    /**
     * @return a Map<String, Integer> from the input JsonNode with the given name, or throw an BadRequestException.
     *         Must be a flat JsonNode or it will throw an BadRequestException.
     */
    public static Map<String, Integer> getRequiredStringToIntegerMap(JsonNode json, String name) throws  BadRequestException {
        final JsonNode node = json.get(name);

        if (node == null || node.isNull() || !(node.isObject())) {
            throw new BadRequestException("json is missing required object: " + name);
        }

        final Map<String, Integer> map = new HashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> iterator = node.fields(); iterator.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            final String key = entry.getKey();
            final Integer value = getRequiredInt(node, key);
            map.put(key, value);
        }

        return map;
    }


    /**
     * @return a Map<String, String> from the input JsonNode with the given name, or throw an BadRequestException.
     *         Must be a flat JsonNode or it will throw an BadRequestException.
     */
    public static Map<String, String> getRequiredMapOfString(JsonNode json, String name) throws BadRequestException {
        final JsonNode node = json.get(name);

        if (node == null || !(node.isObject())) {
            throw new BadRequestException("json is missing required object: " + name);
        }

        final Map<String, String> map = new HashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> iterator = node.fields(); iterator.hasNext();) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            final String key = entry.getKey();
            final String value = getRequiredString(node, key);
            map.put(key, value);
        }
        return map;
    }

    /**
     * @return an ArrayNode built of the passed-in objects using Json.toJson(..).
     */
    public static ArrayNode arrayNodeOf(Object... objects) {
        ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
        for (Object object : objects) {
            arrayNode.add(Json.toJson(object));
        }
        return arrayNode;
    }

    /**
     * @return an ArrayNode built of the passed-in objects using Json.toJson(..).
     */
    @SuppressWarnings("unused")
    public static ArrayNode arrayNodeOf(Collection<Object> objects) {
        ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
        for (Object object : objects) {
            arrayNode.add(Json.toJson(object));
        }
        return arrayNode;
    }
}

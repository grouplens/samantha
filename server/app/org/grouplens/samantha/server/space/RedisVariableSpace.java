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

package org.grouplens.samantha.server.space;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.grouplens.samantha.modeler.space.SpaceMode;
import org.grouplens.samantha.modeler.space.VariableSpace;
import org.grouplens.samantha.server.common.RedisService;
import org.grouplens.samantha.server.io.IOUtilities;
import play.libs.Json;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class RedisVariableSpace extends RedisSpace implements VariableSpace {
    private static final long serialVersionUID = 1L;

    @Inject
    public RedisVariableSpace(RedisService redisService) {
        super(redisService);
    }

    synchronized public void setSpaceState(String spaceName, SpaceMode spaceMode) {
        if (spaceMode.equals(SpaceMode.DEFAULT)) {
            spaceVersion = redisService.get(spaceName + "_" + SpaceType.INDEX.get(), spaceMode.get());
        }
        if (spaceVersion == null) {
            spaceVersion = redisService.incre(spaceName, SpaceType.VARIABLE.get()).toString();
            redisService.set(spaceName + "_" + SpaceType.VARIABLE.get(), spaceMode.get(), spaceVersion);
        }
        this.spaceMode = spaceMode;
        this.spaceName = spaceName;
        this.spaceType = SpaceType.VARIABLE;
        this.spaceIdentifier = RedisService.composeKey(spaceName + "_" + spaceType.get(), spaceVersion);
    }

    public void requestScalarVar(String name, int size, double initial, boolean randomize) {
        ObjectNode val = Json.newObject();
        val.put("name", name);
        val.put("size", size);
        val.put("initial", initial);
        val.put("randomize", randomize);
        redisService.set(spaceIdentifier, "S_" + name, val.toString());
    }

    public boolean hasScalarVar(String name) {
        String val = redisService.get(spaceIdentifier, "S_" + name);
        if (val != null) {
            return true;
        } else {
            return false;
        }
    }

    public void ensureScalarVar(String name, int size, double initial, boolean randomize) {
        String varName = "S_" + name;
        ObjectNode obj = Json.newObject();
        obj.put("name", name);
        obj.put("initial", initial);
        obj.put("randomize", randomize);
        ensureVar(obj, varName, size);
    }

    public void requestVectorVar(String name, int size, int dim, double initial,
                                 boolean randomize, boolean normalize) {
        ObjectNode val = Json.newObject();
        val.put("name", name);
        val.put("size", size);
        val.put("dim", dim);
        val.put("initial", initial);
        val.put("randomize", randomize);
        val.put("normalize", normalize);
        redisService.set(spaceIdentifier, "V_" + name, val.toString());
    }

    public boolean hasVectorVar(String name) {
        String val = redisService.get(spaceIdentifier, "V_" + name);
        if (val != null) {
            return true;
        } else {
            return false;
        }
    }

    private void ensureVar(ObjectNode obj, String varName, int size) {
        List<Object> resps;
        do {
            JsonNode val = redisService.getValue(spaceIdentifier, varName);
            redisService.watch(spaceIdentifier, varName);
            redisService.multi(false);
            if (val != null) {
                IOUtilities.parseEntityFromJsonNode(val, obj);
                if (size > obj.get("size").asInt()) {
                    obj.put("size", size);
                    redisService.setWithoutLock(spaceIdentifier, varName, obj.toString());
                }
            } else {
                obj.put("size", size);
                redisService.setWithoutLock(spaceIdentifier, varName, obj.toString());
            }
            resps = redisService.exec();
        } while (resps.size() > 0 && resps.get(0) == null);
    }

    public void ensureVectorVar(String name, int size, int dim, double initial,
                                boolean randomize, boolean normalize) {
        String varName = "V_" + name;
        ObjectNode obj = Json.newObject();
        obj.put("name", name);
        obj.put("dim", size);
        obj.put("initial", initial);
        obj.put("randomize", randomize);
        obj.put("normalize", normalize);
        ensureVar(obj, varName, size);
    }

    public RealVector getScalarVarByName(String name) {
        String varName = "S_" + name;
        JsonNode obj = redisService.getValue(spaceIdentifier, varName);
        String varIdxName = "IDX_S_" + name + "_";
        List<String> keys = redisService.keysWithPrefixPattern(spaceIdentifier, varIdxName);
        int size = obj.get("size").asInt();
        RealVector vars = MatrixUtils.createRealVector(new double[size]);
        initializeVector(vars, obj.get("initial").asDouble(), obj.get("randomize").asBoolean(), false);
        List<JsonNode> values = redisService.bulkGet(keys);
        for (JsonNode one : values) {
            vars.setEntry(one.get(0).asInt(), one.get(1).asDouble());
        }
        return vars;
    }

    public int getScalarVarSizeByName(String name) {
        String varName = "S_" + name;
        JsonNode obj = redisService.getValue(spaceIdentifier, varName);
        return obj.get("size").asInt();
    }

    public void setScalarVarByName(String name, RealVector vars) {
        for (int i=0; i<vars.getDimension(); i++) {
            setScalarVarByNameIndex(name, i, vars.getEntry(i));
        }
    }

    public double getScalarVarByNameIndex(String name, int index) {
        String varIdxName = "IDX_S_" + name + "_" + Integer.valueOf(index).toString();
        JsonNode value = redisService.getValue(spaceIdentifier, varIdxName);
        if (value != null) {
            return value.get(1).asDouble();
        } else {
            JsonNode obj = redisService.getValue(spaceIdentifier, "S_" + name);
            double init = initialScalarVar(obj.get("initial").asDouble(), obj.get("randomize").asBoolean());
            return init;
        }
    }

    public void setScalarVarByNameIndex(String name, int index, double var) {
        String varIdxName = "IDX_S_" + name + "_" + Integer.valueOf(index).toString();
        ArrayNode values = Json.newArray();
        values.add(index);
        values.add(var);
        redisService.setValue(spaceIdentifier, varIdxName, values);
    }

    private void setValue(RealVector var, JsonNode one, int dim) {
        for (int i=0; i<dim; i++) {
            var.setEntry(i, one.get(i + 1).asDouble());
        }
    }

    private void setValue(List<RealVector> vars, JsonNode one, int dim) {
        RealVector chosen = vars.get(one.get(0).asInt());
        setValue(chosen, one, dim);
    }

    public List<RealVector> getVectorVarByName(String name) {
        String varName = "V_" + name;
        JsonNode obj = redisService.getValue(spaceIdentifier, varName);
        int size = obj.get("size").asInt();
        int dim = obj.get("dim").asInt();
        double initial = obj.get("initial").asDouble();
        boolean randomize = obj.get("randomize").asBoolean();
        boolean normalize = obj.get("normalize").asBoolean();
        List<RealVector> vars = new ArrayList<>(size);
        for (int i=0; i<size; i++) {
            RealVector var = MatrixUtils.createRealVector(new double[dim]);
            initializeVector(var, initial, randomize, normalize);
            vars.add(var);
        }
        String varIdxName = "IDX_V_" + name + "_";
        List<String> keys = redisService.keysWithPrefixPattern(spaceIdentifier, varIdxName);
        List<JsonNode> values = redisService.bulkGet(keys);
        for (JsonNode one : values) {
            setValue(vars, one, dim);
        }
        return vars;
    }

    public RealMatrix getMatrixVarByName(String name) {
        String varName = "V_" + name;
        JsonNode obj = redisService.getValue(spaceIdentifier, varName);
        int size = obj.get("size").asInt();
        int dim = obj.get("dim").asInt();
        RealMatrix matrix = MatrixUtils.createRealMatrix(size, dim);
        for (int i=0; i<size; i++) {
            matrix.setRowVector(i, getVectorVarByNameIndex(name, i));
        }
        return matrix;
    }

    public int getVectorVarSizeByName(String name) {
        String varName = "V_" + name;
        JsonNode obj = redisService.getValue(spaceIdentifier, varName);
        return obj.get("size").asInt();
    }

    public int getVectorVarDimensionByName(String name) {
        String varName = "V_" + name;
        JsonNode obj = redisService.getValue(spaceIdentifier, varName);
        return obj.get("dim").asInt();
    }

    public RealVector getVectorVarByNameIndex(String name, int index) {
        String varIdxName = "IDX_V_" + name + "_" + Integer.valueOf(index).toString();
        JsonNode value = redisService.getValue(spaceIdentifier, varIdxName);
        if (value != null) {
            int dim = value.size() - 1;
            RealVector var = MatrixUtils.createRealVector(new double[dim]);
            setValue(var, value, dim);
            return var;
        } else {
            JsonNode obj = redisService.getValue(spaceIdentifier, "V_" + name);
            RealVector var = MatrixUtils.createRealVector(new double[obj.get("dim").asInt()]);
            initializeVector(var, obj.get("initial").asDouble(), obj.get("randomize").asBoolean(),
                    obj.get("normalize").asBoolean());
            return var;
        }
    }

    public void setVectorVarByNameIndex(String name, int index, RealVector var) {
        String varIdxName = "IDX_V_" + name + "_" + Integer.valueOf(index).toString();
        ArrayNode values = Json.newArray();
        values.add(index);
        for (int i=0; i<var.getDimension(); i++) {
            values.add(var.getEntry(i));
        }
        redisService.setValue(spaceIdentifier, varIdxName, values);
    }

    private List<String> getAllVarNames(String label) {
        List<String> names = new ArrayList<>();
        List<String> keys = redisService.keysWithPrefixPattern(spaceIdentifier, label);
        List<JsonNode> values = redisService.bulkGet(keys);
        for (JsonNode val : values) {
            names.add(val.get("name").asText());
        }
        return names;
    }

    public List<String> getAllScalarVarNames() {
        return getAllVarNames("S_");
    }

    public List<String> getAllVectorVarNames() {
        return getAllVarNames("V_");
    }

    public void freeSpace() {
        List<String> keys = redisService.keysWithPrefixPattern(spaceIdentifier, null);
        for (String key : keys) {
            redisService.delWithKey(key);
        }
    }

    public void freeScalarVar(String name) {
        List<String> keys = redisService.keysWithPrefixPattern(spaceIdentifier, "S_" + name);
        for (String key : keys) {
            redisService.delWithKey(key);
        }
    }

    public void freeVectorVar(String name) {
        List<String> keys = redisService.keysWithPrefixPattern(spaceIdentifier, "V_" + name);
        for (String key : keys) {
            redisService.delWithKey(key);
        }
    }
}

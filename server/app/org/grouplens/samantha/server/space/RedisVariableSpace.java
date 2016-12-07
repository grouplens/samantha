package org.grouplens.samantha.server.space;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.grouplens.samantha.modeler.space.VariableSpace;
import org.grouplens.samantha.server.common.RedisService;

import javax.inject.Inject;
import java.util.List;

public class RedisVariableSpace extends RedisSpace implements VariableSpace {
    private static final long serialVersionUID = 1L;

    @Inject
    public RedisVariableSpace(RedisService redisService) {
        super(redisService);
    }

    public void requestScalarVar(String name, int size, double initial, boolean randomize) {

    }

    public boolean hasScalarVar(String name) {
        return false;
    }

    public void ensureScalarVar(String name, int size, double initial, boolean randomize) {

    }

    public void requestVectorVar(String name, int size, int dim, double initial,
                                 boolean randomize, boolean normalize) {

    }

    public boolean hasVectorVar(String name) {
        return false;
    }

    public void ensureVectorVar(String name, int size, int dim, double initial,
                                boolean randomize, boolean normalize) {

    }

    public RealVector getScalarVarByName(String name) {
        return null;
    }

    public int getScalarVarSizeByName(String name) {
        return 0;
    }

    public void setScalarVarByName(String name, RealVector vars) {

    }

    public double getScalarVarByNameIndex(String name, int index) {
        return 0.0;
    }

    public void setScalarVarByNameIndex(String name, int index, double var) {

    }

    public List<RealVector> getVectorVarByName(String name) {
        return null;
    }

    public RealMatrix getMatrixVarByName(String name) {
        return null;
    }

    public int getVectorVarSizeByName(String name) {
        return 0;
    }

    public int getVectorVarDimensionByName(String name) {
        return 0;
    }

    public RealVector getVectorVarByNameIndex(String name, int index) {
        return null;
    }

    public void setVectorVarByNameIndex(String name, int index, RealVector var) {

    }

    public List<String> getAllScalarVarNames() {
        return null;
    }

    public List<String> getAllVectorVarNames() {
        return null;
    }
}

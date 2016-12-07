package org.grouplens.samantha.modeler.svdfeature;

import org.apache.commons.lang3.StringUtils;

import org.grouplens.samantha.modeler.featurizer.AbstractLearningInstance;
import org.grouplens.samantha.modeler.featurizer.Feature;
import org.grouplens.samantha.modeler.common.LearningInstance;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class SVDFeatureInstance extends AbstractLearningInstance {
    private static final long serialVersionUID = 1L;

    double weight;
    double label;
    List<Feature> gfeas;
    List<Feature> ufeas;
    List<Feature> ifeas;

    static public double defaultWeight = 1.0;
    static public double defaultLabel = 0.0;

    SVDFeatureInstance(List<Feature> gfeas, List<Feature> ufeas, List<Feature> ifeas,
                       double label, double weight, String group) {
        super(group);
        this.gfeas = gfeas;
        this.ufeas = ufeas;
        this.ifeas = ifeas;
        this.label = label;
        this.weight = weight;
    }

    public LearningInstance newInstanceWithLabel(double label) {
        return new SVDFeatureInstance(this.gfeas, this.ufeas, this.ifeas,
                label, this.weight, this.group);
    }

    public double getLabel() {
        return this.label;
    }

    public double getWeight() {
        return this.weight;
    }

    public void setLabel(double label) {
        this.label = label;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public List<Feature> getBiasFeatures() {
        return this.gfeas;
    }

    public String toString() {
        ArrayList<String> fields = new ArrayList<>(5 + (gfeas.size() + ufeas.size() + ifeas.size()) * 2);
        fields.add(Double.toString(weight));
        fields.add(Double.toString(label));
        if (group != null) {
            fields.add(group);
        } else {
            fields.add("");
        }
        fields.add(Integer.toString(gfeas.size()));
        fields.add(Integer.toString(ufeas.size()));
        fields.add(Integer.toString(ifeas.size()));
        for (Feature fea : gfeas) {
            fields.add(Integer.toString(fea.getIndex()));
            fields.add(Double.toString(fea.getValue()));
        }
        for (Feature fea : ufeas) {
            fields.add(Integer.toString(fea.getIndex()));
            fields.add(Double.toString(fea.getValue()));
        }
        for (Feature fea : ifeas) {
            fields.add(Integer.toString(fea.getIndex()));
            fields.add(Double.toString(fea.getValue()));
        }
        return StringUtils.join(fields, "\t");
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.writeObject(toString());
    }

    private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
        String line = (String) stream.readObject();
        gfeas = new ArrayList<>();
        ufeas = new ArrayList<>();
        ifeas = new ArrayList<>();
        SVDFeatureUtilities.parseInstanceFromString(line, this);
    }
}

package org.grouplens.samantha.modeler.boosting;

import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureInstance;
import org.grouplens.samantha.modeler.featurizer.StandardLearningInstance;

public class GBCentLearningInstance implements LearningInstance {
    private static final long serialVersionUID = 1L;
    private final SVDFeatureInstance svdfeaIns;
    private final StandardLearningInstance treeIns;

    public GBCentLearningInstance(SVDFeatureInstance svdfeaIns, StandardLearningInstance treeIns) {
        this.svdfeaIns = svdfeaIns;
        this.treeIns = treeIns;
    }

    public double getLabel() {
        return this.svdfeaIns.getLabel();
    }

    public void setLabel(double label) {
        this.svdfeaIns.setLabel(label);
        this.treeIns.setLabel(label);
    }

    public void setWeight(double weight) {
        this.svdfeaIns.setWeight(weight);
        this.treeIns.setWeight(weight);
    }

    public LearningInstance newInstanceWithLabel(double label) {
        return new GBCentLearningInstance(
                (SVDFeatureInstance) this.svdfeaIns.newInstanceWithLabel(label),
                (StandardLearningInstance) this.treeIns.newInstanceWithLabel(label)
        );
    }

    public double getWeight() {
        return this.svdfeaIns.getWeight();
    }

    public SVDFeatureInstance getSvdfeaIns() {
        return svdfeaIns;
    }

    public StandardLearningInstance getTreeIns() {
        return treeIns;
    }

    public String getGroup() {
        return svdfeaIns.getGroup();
    }

    public void setGroup(String group) {
        this.svdfeaIns.setGroup(group);
        this.treeIns.setGroup(group);
    }
}

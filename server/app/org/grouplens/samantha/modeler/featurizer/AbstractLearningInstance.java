package org.grouplens.samantha.modeler.featurizer;

import org.grouplens.samantha.modeler.common.LearningInstance;

abstract public class AbstractLearningInstance implements LearningInstance {
    private String group;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
}

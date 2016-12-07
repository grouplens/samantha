package org.grouplens.samantha.modeler.featurizer;

import org.grouplens.samantha.modeler.common.LearningInstance;

abstract public class AbstractLearningInstance implements LearningInstance {
    protected String group;

    public AbstractLearningInstance() {}

    public AbstractLearningInstance(String group) {
        this.group = group;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
}

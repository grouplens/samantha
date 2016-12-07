package org.grouplens.samantha.modeler.common;

import java.io.Serializable;

public interface LearningInstance extends Serializable {
    double getLabel();
    double getWeight();
    void setLabel(double label);
    void setWeight(double weight);
    default String getGroup() {return null;}
    default void setGroup(String group) {}
    LearningInstance newInstanceWithLabel(double label);
}

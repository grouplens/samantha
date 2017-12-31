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

package org.grouplens.samantha.modeler.boosting;

import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureInstance;
import org.grouplens.samantha.modeler.instance.StandardLearningInstance;

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

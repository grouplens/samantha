/*
 * Copyright (c) [2016-2018] [University of Minnesota]
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

package org.grouplens.samantha.modeler.solver;

import java.util.List;

public class HingeLoss implements ObjectiveFunction {
    private static final long serialVersionUID = 1L;

    public HingeLoss() { }

    private double getLoss(double wrappedOutput, double label, double weight) {
        return (1 - wrappedOutput * label) * weight;
    }

    public List<StochasticOracle> wrapOracle(List<StochasticOracle> oracles) {
        for (StochasticOracle orc : oracles) {
            double label = orc.getLabel();
            if (label == 0) {
                label = -1;
            }
            double loss = getLoss(orc.getModelOutput(), label, orc.getWeight());
            orc.setObjVal((loss < 0) ? 0 : loss);
            orc.setGradient((loss == 0) ? 0 : -label);
        }
        return oracles;
    }
}

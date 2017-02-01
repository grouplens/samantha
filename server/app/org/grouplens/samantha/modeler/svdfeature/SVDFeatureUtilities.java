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

package org.grouplens.samantha.modeler.svdfeature;

import org.grouplens.samantha.modeler.featurizer.Feature;

public class SVDFeatureUtilities {
    private SVDFeatureUtilities() {}

    static public void parseInstanceFromString(String line, SVDFeatureInstance ins) {
        String[] fields = line.split("\t");
        ins.weight = Double.parseDouble(fields[0]);
        ins.label = Double.parseDouble(fields[1]);
        if (!"".equals(fields[2])) {
            ins.setGroup(fields[2]);
        }
        int gfeaNum = Integer.parseInt(fields[3]);
        int ufeaNum = Integer.parseInt(fields[4]);
        int ifeaNum = Integer.parseInt(fields[5]);
        int start = 6;
        for (int i = 0; i < gfeaNum; i++) {
            Feature fea = new Feature(Integer.parseInt(fields[start + 2 * i]),
                    Double.parseDouble(fields[start + 1 + 2 * i]));
            ins.gfeas.add(fea);
        }
        start = 5 + 2 * gfeaNum;
        for (int i = 0; i < ufeaNum; i++) {
            Feature fea = new Feature(Integer.parseInt(fields[start + 2 * i]),
                    Double.parseDouble(fields[start + 1 + 2 * i]));
            ins.ufeas.add(fea);
        }
        start = 5 + 2 * gfeaNum + 2 * ufeaNum;
        for (int i = 0; i < ifeaNum; i++) {
            Feature fea = new Feature(Integer.parseInt(fields[start + 2 * i]),
                    Double.parseDouble(fields[start + 1 + 2 * i]));
            ins.ifeas.add(fea);
        }
    }
}

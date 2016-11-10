package org.grouplens.samantha.modeler.svdfeature;

import org.grouplens.samantha.modeler.featurizer.Feature;

public class SVDFeatureUtilities {
    private SVDFeatureUtilities() {}

    static public void parseInstanceFromString(String line, SVDFeatureInstance ins) {
        String[] fields = line.split("\t");
        ins.weight = Double.parseDouble(fields[0]);
        ins.label = Double.parseDouble(fields[1]);
        int gfeaNum = Integer.parseInt(fields[2]);
        int ufeaNum = Integer.parseInt(fields[3]);
        int ifeaNum = Integer.parseInt(fields[4]);
        int start = 5;
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

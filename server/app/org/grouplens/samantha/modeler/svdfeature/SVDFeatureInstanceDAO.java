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

import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.featurizer.Feature;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SVDFeatureInstanceDAO implements LearningData {
    private static Logger logger = LoggerFactory.getLogger(SVDFeatureInstanceDAO.class);
    private final File sourceFile;
    private BufferedReader reader;

    public SVDFeatureInstanceDAO(File sourceFile)
            throws FileNotFoundException {
        this.sourceFile = sourceFile;
        reader = new BufferedReader(new FileReader(this.sourceFile));
    }

    public List<LearningInstance> getLearningInstance() {
        try {
            String line = reader.readLine();
            List<LearningInstance> instances = new ArrayList<>(1);
            if (line == null) {
                return instances;
            } else {
                List<Feature> gfeas = new ArrayList<>();
                List<Feature> ufeas = new ArrayList<>();
                List<Feature> ifeas = new ArrayList<>();
                SVDFeatureInstance ins = new SVDFeatureInstance(gfeas, ufeas, ifeas, 0.0, 0.0, null);
                SVDFeatureUtilities.parseInstanceFromString(line, ins);
                instances.add(ins);
                return instances;
            }
        } catch (IOException e) {
            logger.error("{}", e.getMessage());
            throw new BadRequestException(e);
        }
    }

    public void startNewIteration() {
        try {
            reader.close();
            reader = new BufferedReader(new FileReader(sourceFile));
        } catch (IOException e) {
            logger.error("{}", e.getMessage());
            throw new BadRequestException(e);
        }
    }
}

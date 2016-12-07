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
                SVDFeatureInstance ins = new SVDFeatureInstance(gfeas, ufeas, ifeas, 0.0, 0.0);
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

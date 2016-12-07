package org.grouplens.samantha.modeler.solver;

import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.server.exception.BadRequestException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

//TODO: support grouping learning instance according to group info.
public class ObjectStreamLearningData implements LearningData {
    final private String filePath;
    private ObjectInputStream inputStream;

    private void start() {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
            inputStream = new ObjectInputStream(new FileInputStream(filePath));
        } catch (IOException e) {
            throw new BadRequestException(e);
        }
    }

    public ObjectStreamLearningData(String filePath) {
        this.filePath = filePath;
        start();
    }

    public List<LearningInstance> getLearningInstance() {
        if (inputStream == null) {
            return null;
        }
        List<LearningInstance> instances = new ArrayList<>(1);
        try {
            LearningInstance ins = (LearningInstance) inputStream.readUnshared();
            instances.add(ins);
        } catch (IOException e) {
            inputStream = null;
        } catch (ClassNotFoundException e) {
            inputStream = null;
            throw new BadRequestException(e);
        }
        return instances;
    }

    public void startNewIteration() {
        start();
    }
}

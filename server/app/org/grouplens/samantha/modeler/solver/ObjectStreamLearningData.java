package org.grouplens.samantha.modeler.solver;

import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.server.exception.InvalidRequestException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

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
            throw new InvalidRequestException(e);
        }
    }

    public ObjectStreamLearningData(String filePath) {
        this.filePath = filePath;
        start();
    }

    public LearningInstance getLearningInstance() {
        LearningInstance ins;
        try {
            ins = (LearningInstance) inputStream.readUnshared();
        } catch (IOException e) {
            ins = null;
        } catch (ClassNotFoundException e) {
            throw new InvalidRequestException(e);
        }
        return ins;
    }

    public void startNewIteration() {
        start();
    }
}

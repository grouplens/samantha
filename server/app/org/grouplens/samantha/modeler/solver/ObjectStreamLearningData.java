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

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

import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

public class CacheInstanceRunnable implements ObjectiveRunnable {
    final private static Logger logger = LoggerFactory.getLogger(CacheInstanceRunnable.class);
    private final String cachePath;
    private final LearningData data;
    private long cnt = 0;

    public CacheInstanceRunnable(String cachePath, LearningData data) {
        this.cachePath = cachePath;
        this.data = data;
    }

    @Override
    public void run() {
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(cachePath));
            List<LearningInstance> instances;
            while ((instances = data.getLearningInstance()).size() > 0) {
                for (LearningInstance ins : instances) {
                    cnt++;
                    outputStream.writeUnshared(ins);
                    outputStream.reset();
                    if (cnt % 1000000 == 0) {
                        logger.info("Cached {} instances.", cnt);
                    }
                }
            }
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new BadRequestException(e);
        }
    }

    public double getObjVal() {
        return cnt;
    }
}

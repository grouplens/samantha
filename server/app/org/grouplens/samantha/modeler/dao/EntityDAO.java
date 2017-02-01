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

package org.grouplens.samantha.modeler.dao;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.common.LearningData;

/**
 * The interface for representing an outside data source.
 *
 * This interface is the one for Samantha to extend to recognize new formats or sources of data. E.g. you can
 * implement a DAO that takes in data from HDFS in a Hadoop cluster or PostgreSQL, etc.
 */
public interface EntityDAO {
    /**
     * Test whether there is more rows/data points in the DAO.
     *
     * This method should be called before calling {@link #getNextEntity()}.
     *
     * @return if having more data points, return true; otherwise, return false.
     */
    boolean hasNextEntity();

    /**
     * Get the next entity/data point from the data source (DAO).
     *
     * The entity/data point needs to be in a mutable JSON node format. It is usually flat, i.e. non-nested. It can be
     * nested as long as you handle the flattening in {@link org.grouplens.samantha.modeler.common.LearningData LearningData}.
     *
     * @return the mutable JSON representation of the data point.
     */
    ObjectNode getNextEntity();

    /**
     * Restart the DAO.
     *
     * So that when {@link #hasNextEntity()} is called, the DAO is testing from the beginning of the data source.
     * This is normally called by {@link LearningData#startNewIteration()}.
     */
    void restart();

    /**
     * Close the DAO.
     *
     * Resources should be closed/freed here, e.g. if the DAO is a file-based DAO, it should close the file handle etc.
     */
    void close();
}

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

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

package org.grouplens.samantha.modeler.dao;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JsonDirDAO implements EntityDAO {
    private final EntityDAO entityDAO;

    public JsonDirDAO(String dirPath) {
        Collection<File> fileList = FileUtils.listFiles(new File(dirPath), null, true);
        List<String> fileNames = new ArrayList<>(fileList.size());
        for (File file : fileList) {
            fileNames.add(file.getAbsolutePath());
        }
        this.entityDAO = new JsonFileListDAO(fileNames);
    }

    public boolean hasNextEntity() {
        return entityDAO.hasNextEntity();
    }

    public ObjectNode getNextEntity() {
        return entityDAO.getNextEntity();
    }

    public void restart() {
        entityDAO.restart();
    }

    public void close() {
        entityDAO.close();
    }
}

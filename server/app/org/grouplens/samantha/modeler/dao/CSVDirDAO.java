package org.grouplens.samantha.modeler.dao;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CSVDirDAO implements EntityDAO {
    private final EntityDAO entityDAO;

    public CSVDirDAO(String dirPath, String separator) {
        Collection<File> fileList = FileUtils.listFiles(new File(dirPath), null, true);
        List<String> fileNames = new ArrayList<>(fileList.size());
        for (File file : fileList) {
            fileNames.add(file.getAbsolutePath());
        }
        this.entityDAO = new CSVFileListDAO(fileNames, separator);
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

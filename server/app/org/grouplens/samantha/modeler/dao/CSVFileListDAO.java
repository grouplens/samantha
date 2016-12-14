package org.grouplens.samantha.modeler.dao;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

public class CSVFileListDAO implements EntityDAO {
    final private List<String> fileList;
    final private String separator;
    private int idx = 0;
    private CSVFileDAO csvFileDAO;

    public CSVFileListDAO(List<String> fileList, String separator) {
        this.fileList = fileList;
        this.separator = separator;
    }

    public boolean hasNextEntity() {
        do {
            if (csvFileDAO == null) {
                if (idx >= fileList.size()) {
                    return false;
                }
                csvFileDAO = new CSVFileDAO(separator, fileList.get(idx++));
            }
            if (csvFileDAO.hasNextEntity()) {
                return true;
            } else {
                csvFileDAO.close();
                csvFileDAO = null;
            }
        } while (idx < fileList.size());
        return false;
    }

    public ObjectNode getNextEntity() {
        do {
            if (csvFileDAO == null) {
                if (idx >= fileList.size()) {
                    return null;
                }
                csvFileDAO = new CSVFileDAO(separator, fileList.get(idx++));
            }
            if (csvFileDAO.hasNextEntity()) {
                return csvFileDAO.getNextEntity();
            } else {
                csvFileDAO.close();
                csvFileDAO = null;
            }
        } while (idx < fileList.size());
        return null;
    }

    public void restart() {
        close();
        idx = 0;
    }

    public void close() {
        if (csvFileDAO != null) {
            csvFileDAO.close();
            csvFileDAO = null;
        }
    }
}

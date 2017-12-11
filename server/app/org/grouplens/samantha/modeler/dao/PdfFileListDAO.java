package org.grouplens.samantha.modeler.dao;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

public class PdfFileListDAO implements EntityDAO {
    final private List<String> fileList;
    private int idx = 0;
    private PdfFileDAO pdfFileDAO;

    public PdfFileListDAO(List<String> fileList) {
        this.fileList = fileList;
    }

    public boolean hasNextEntity() {
        do {
            if (pdfFileDAO == null) {
                if (idx >= fileList.size()) {
                    return false;
                }
                pdfFileDAO = new PdfFileDAO(fileList.get(idx++));
            }
            if (pdfFileDAO.hasNextEntity()) {
                return true;
            } else {
                pdfFileDAO.close();
                pdfFileDAO = null;
            }
        } while (idx < fileList.size());
        return false;
    }

    public ObjectNode getNextEntity() {
        do {
            if (pdfFileDAO == null) {
                if (idx >= fileList.size()) {
                    return null;
                }
                pdfFileDAO = new PdfFileDAO(fileList.get(idx++));
            }
            if (pdfFileDAO.hasNextEntity()) {
                return pdfFileDAO.getNextEntity();
            } else {
                pdfFileDAO.close();
                pdfFileDAO = null;
            }
        } while (idx < fileList.size());
        return null;
    }

    public void restart() {
        close();
        idx = 0;
    }

    public void close() {
        if (pdfFileDAO != null) {
            pdfFileDAO.close();
            pdfFileDAO = null;
        }
    }
}

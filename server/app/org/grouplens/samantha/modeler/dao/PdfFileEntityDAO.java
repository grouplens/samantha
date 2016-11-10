package org.grouplens.samantha.modeler.dao;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.grouplens.samantha.server.exception.InvalidRequestException;
import play.libs.Json;

import java.io.File;
import java.io.IOException;

public class PdfFileEntityDAO implements EntityDAO {
    private final PDDocument pdfDoc;
    private final PDFTextStripper stripper;
    private final int paraLength = 250;
    private int curPage = 0;
    private final int numPages;
    private String[] lines = null;
    private int idx = 0;
    private int paraIdx = 0;

    private PdfFileEntityDAO(String filePath) {
        try {
            stripper = new PDFTextStripper();
            pdfDoc = PDDocument.load(new File(filePath));
        } catch (IOException e) {
            throw new InvalidRequestException(e);
        }
        numPages = pdfDoc.getNumberOfPages();
    }

    public boolean hasNextEntity() {
        if (lines != null && idx < lines.length) {
            return true;
        } else if (curPage < numPages) {
            return true;
        } else {
            try {
                pdfDoc.close();
            } catch (IOException e) {
                throw new InvalidRequestException(e);
            }
            return false;
        }
    }

    private String getParagraph() {
        String paragraph = "";
        for (; idx<lines.length; idx++) {
            paragraph += lines[idx];
            if (paragraph.length() >= paraLength) {
                break;
            }
        }
        return paragraph;
    }

    public ObjectNode getNextEntity() {
        String paragraph = "";
        if (lines != null && idx < lines.length) {
            paragraph += getParagraph();
        } else if (curPage < numPages) {
            stripper.setStartPage(curPage);
            stripper.setEndPage(curPage++);
            try {
                String pageText = stripper.getText(pdfDoc);
                lines = pageText.split("[\\n\\.!\\?]");
                idx = 0;
                paragraph += getParagraph();
            } catch (IOException e) {
                //TODO: logging error info
            }
        }
        ObjectNode entity = Json.newObject();
        entity.put("paraIdx", paraIdx++);
        entity.put("page", curPage + 1);
        entity.put("text", paragraph);
        return entity;
    }

    public void restart() {
        lines = null;
        idx = 0;
        curPage = 0;
        paraIdx = 0;
    }

    public void close() {
        try {
            pdfDoc.close();
        } catch (IOException e) {
            //TODO: logging error info
        }
    }
}

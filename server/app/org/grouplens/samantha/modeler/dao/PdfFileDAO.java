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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;

import java.io.File;
import java.io.IOException;

public class PdfFileDAO implements EntityDAO {
    private static Logger logger = LoggerFactory.getLogger(PdfFileDAO.class);
    private final PDDocument pdfDoc;
    private final PDFTextStripper stripper;
    private final int paraLength = 250;
    private int curPage = 0;
    private final int numPages;
    private String[] lines = null;
    private int idx = 0;

    public PdfFileDAO(String filePath) {
        try {
            stripper = new PDFTextStripper();
            pdfDoc = PDDocument.load(new File(filePath));
        } catch (IOException e) {
            throw new BadRequestException(e);
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
                throw new BadRequestException(e);
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
        int prevIdx = idx;
        if (lines != null && idx < lines.length) {
            prevIdx = idx;
            paragraph += getParagraph();
        } else if (curPage < numPages) {
            curPage += 1;
            stripper.setStartPage(curPage);
            stripper.setEndPage(curPage);
            try {
                String pageText = stripper.getText(pdfDoc);
                lines = pageText.split("[\\n\\.!\\?]");
                idx = 0;
                prevIdx = idx;
                paragraph += getParagraph();
            } catch (IOException e) {
                logger.error("{}", e.getMessage());
                throw new BadRequestException(e);
            }
        }
        ObjectNode entity = Json.newObject();
        entity.put("page", curPage);
        entity.put("start", Integer.valueOf(curPage).toString() + ":" +
                Integer.valueOf(prevIdx).toString());
        entity.put("end", Integer.valueOf(curPage).toString() + ":" +
                Integer.valueOf(idx).toString());
        entity.put("text", paragraph);
        return entity;
    }

    public void restart() {
        lines = null;
        idx = 0;
        curPage = 0;
    }

    public void close() {
        try {
            pdfDoc.close();
        } catch (IOException e) {
            logger.error("{}", e.getMessage());
            throw new BadRequestException(e);
        }
    }
}

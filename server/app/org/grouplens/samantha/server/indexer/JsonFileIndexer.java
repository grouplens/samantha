package org.grouplens.samantha.server.indexer;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.io.RequestContext;
import play.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class JsonFileIndexer implements Indexer {
    private final BufferedWriter writer;

    //TODO: this writer is not closed. not a big problem but needs attention.
    public JsonFileIndexer(String jsonFile) {
        try {
            writer = new BufferedWriter(new FileWriter(jsonFile));
        } catch (IOException e) {
            throw new BadRequestException(e);
        }
    }

    public void index(String type, JsonNode documents, RequestContext requestContext) {
        try {
            writer.write(documents.toString());
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new BadRequestException(e);
        }
    }

    public void index(RequestContext requestContext) {
        Logger.error("Indexing directly from RequestContext is not supported in this indexer.");
    }
}

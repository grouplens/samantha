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

package org.grouplens.samantha.server.indexer;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.grouplens.samantha.server.config.ConfigKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Configuration;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Singleton
public class FileWriterService {
    private static Logger logger = LoggerFactory.getLogger(FileWriterService.class);
    private final int maxWriter;
    private final String separator;
    private final List<String> dataDirs;
    private final String dirPattern;
    private final ReentrantReadWriteLock serviceLock = new ReentrantReadWriteLock();
    private Lock writeLock = serviceLock.writeLock();
    private final Map<String, TreeMap<String, BufferedWriter>> activeFiles = new HashMap<>();
    private final Map<String, Map<String, List<String>>> activeSchemas = new HashMap<>();
    private final Map<String, Map<String, Lock>> activeLocks = new HashMap<>();
    private int curDirIdx = 0;

    @Inject
    private FileWriterService(Configuration configuration) {
        String sep = configuration.getString(ConfigKey.CSV_FILE_SERVICE_SEPARATOR.get());
        if (sep != null) {
            separator = sep;
        } else {
            separator = "\t";
        }
        dataDirs = configuration.getStringList(ConfigKey.CSV_FILE_SERVICE_DATA_DIRS.get());
        String pattern = configuration.getString(ConfigKey.CSV_FILE_SERVICE_DIR_PATTERN.get());
        if (pattern == null) {
            dirPattern = "/yyyy/MM/dd/";
        } else {
            dirPattern = pattern;
        }
        maxWriter = configuration.getInt(ConfigKey.CSV_FILE_SERVICE_MAX_WRITER.get());
    }

    private String pickDirectory(int idx, String type, int tstamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(dirPattern);
        long ms = tstamp * 1000L;
        return dataDirs.get(idx) + "/" + type + dateFormat.format(new Date(ms));
    }

    private void freeResources(String type, int remain) {
        if (activeFiles.containsKey(type) && activeFiles.get(type).size() > remain) {
            Object[] keys = activeFiles.get(type).keySet().toArray();
            for (int i=0; i<keys.length - remain; i++) {
                if (activeLocks.get(type).get(keys[i]).tryLock()) {
                    try {
                        activeFiles.get(type).get(keys[i]).close();
                        activeFiles.get(type).remove(keys[i]);
                        activeSchemas.get(type).remove(keys[i]);
                    } catch (IOException e) {
                        logger.error(e.getMessage());
                    } finally {
                        activeLocks.get(type).get(keys[i]).unlock();
                        activeLocks.get(type).remove(keys[i]);
                    }
                }
            }
        }
    }

    private String lockFile(String type, String directory, List<String> dataFields, String appendix)
            throws IOException {
        if (!activeFiles.containsKey(type)) {
            activeFiles.put(type, new TreeMap<>());
            activeSchemas.put(type, new HashMap<>());
            activeLocks.put(type, new HashMap<>());
        }
        freeResources(type, maxWriter);
        Map<String, BufferedWriter> actFiles = activeFiles.get(type);
        Map<String, Lock> actLocks = activeLocks.get(type);
        Map<String, List<String>> actSchemas = activeSchemas.get(type);
        for (int i=0; i<Integer.MAX_VALUE; i++) {
            String file = directory + Integer.valueOf(i) + appendix;
            if (actSchemas.containsKey(file)) {
                if (dataFields == null || dataFields.equals(actSchemas.get(file))) {
                    actLocks.get(file).lock();
                    return file;
                }
            } else {
                BufferedWriter writer;
                try {
                    if (dataFields != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(
                                new FileInputStream(file), StandardCharsets.UTF_8));
                        String line = reader.readLine();
                        if (line != null) {
                            List<String> curFields = Lists.newArrayList(line.split(separator));
                            if (!dataFields.equals(curFields)) {
                                continue;
                            }
                        }
                        reader.close();
                    }
                    writer = new BufferedWriter(new OutputStreamWriter(
                            new FileOutputStream(file, true), StandardCharsets.UTF_8));
                } catch (FileNotFoundException e) {
                    new File(directory).mkdirs();
                    writer = new BufferedWriter(new OutputStreamWriter(
                            new FileOutputStream(file, true), StandardCharsets.UTF_8));
                    IndexerUtilities.writeCSVHeader(dataFields, writer, separator);
                }
                actFiles.put(file, writer);
                actSchemas.put(file, dataFields);
                ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
                Lock lock = rwl.writeLock();
                lock.lock();
                actLocks.put(file, lock);
                return file;
            }
        }
        throw new IOException("Can not find a good file to writeCSV in the directory.");
    }

    private void unlockFile(String type, String file) {
        if (activeLocks.containsKey(type) && activeLocks.get(type).containsKey(file)) {
            activeLocks.get(type).get(file).unlock();
        }
    }

    private List<String> getSchema(String type, String file) {
        return activeSchemas.get(type).get(file);
    }

    private BufferedWriter getWriter(String type, String file) {
        return activeFiles.get(type).get(file);
    }

    public void writeCSV(String type, JsonNode entity, List<String> dataFields, int tstamp) {
        write(type, entity, dataFields, tstamp, separator, ".csv");
    }

    public void writeJson(String type, JsonNode entity, int tstamp) {
        write(type, entity, null, tstamp, separator, ".json");
    }

    private void write(String type, JsonNode entity,
                       List<String> dataFields, int tstamp,
                       String separator, String appendix) {
        for (int idx=curDirIdx; idx<dataDirs.size(); idx++) {
            String directory = pickDirectory(idx, type, tstamp);
            String file = null;
            BufferedWriter writer;
            List<String> curFields;
            writeLock.lock();
            try {
                file = lockFile(type, directory, dataFields, appendix);
                writer = getWriter(type, file);
                curFields = getSchema(type, file);
            } catch (Exception e) {
                logger.error(e.getMessage());
                curDirIdx = (idx + 1) % dataDirs.size();
                unlockFile(type, file);
                continue;
            } finally {
                writeLock.unlock();
            }
            try {
                if (dataFields != null && dataFields.size() > 0 && separator != null) {
                    IndexerUtilities.writeCSVFields(entity, curFields, writer, separator);
                } else {
                    IndexerUtilities.writeJson(entity, writer);
                }
                break;
            } catch (Exception e) {
                logger.error(e.getMessage());
            } finally {
                unlockFile(type, file);
            }
            writeLock.lock();
            curDirIdx = (idx + 1) % dataDirs.size();
            writeLock.unlock();
        }
    }

    public List<String> getFiles(String type, int beginTime, int endTime) {
        Set<String> files = new HashSet<>();
        for (int idx=0; idx<dataDirs.size(); idx ++) {
            for (int i = beginTime; i <= endTime; i+=3600) {
                String dir = pickDirectory(idx, type, i);
                File folder = new File(dir);
                if (folder.isDirectory()) {
                    File[] list = folder.listFiles();
                    for (File file : list) {
                        String path = file.getAbsolutePath();
                        files.add(path);
                    }
                }
            }
        }
        return Lists.newArrayList(files);
    }

    public String getSeparator() {
        return separator;
    }
}

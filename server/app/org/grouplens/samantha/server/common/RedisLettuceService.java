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

package org.grouplens.samantha.server.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lambdaworks.redis.LettuceFutures;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Configuration;
import play.inject.ApplicationLifecycle;
import play.libs.F;
import play.libs.Json;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Singleton
public class RedisLettuceService implements RedisService {
    private static Logger logger = LoggerFactory.getLogger(RedisLettuceService.class);
    private final String cfgHost;
    private final Integer cfgPort;
    private final Integer cfgDb;

    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock readLock = rwl.readLock();
    private final Lock writeLock = rwl.writeLock();

    private RedisClient client = null;
    private StatefulRedisConnection<String, String> connection = null;
    private StatefulRedisConnection<String, String> asyncConnection = null;
    private RedisCommands syncCommands = null;
    private RedisAsyncCommands asyncCommands = null;

    @Inject
    private RedisLettuceService(Configuration configuration, ApplicationLifecycle lifecycle) {
        cfgHost = configuration.getString(ConfigKey.REDIS_HOST.get());
        cfgPort = configuration.getInt(ConfigKey.REDIS_PORT.get());
        cfgDb = configuration.getInt(ConfigKey.REDIS_DBID.get());
        startUp();
        lifecycle.addStopHook(() -> {
            shutDown();
            return F.Promise.pure(null);
        });
    }

    private void startUp() {
        logger.info("Starting RedisLettuceService");
        {
            logger.debug("Redis settings:");
            logger.debug("* host={}", cfgHost);
            logger.debug("* port={}", cfgPort);
            logger.debug("* db={}", cfgDb);
        }

        RedisURI redisURI = new RedisURI();
        redisURI.setHost(cfgHost);
        redisURI.setPort(cfgPort);
        redisURI.setDatabase(cfgDb);
        client = RedisClient.create(redisURI);
        connection = client.connect();
        asyncConnection = client.connect();
        syncCommands = connection.sync();
        asyncCommands = asyncConnection.async();
        asyncConnection.setAutoFlushCommands(false);

        logger.info("Connected to a redis client");
    }

    private void shutDown() {
        connection.close();
        client.shutdown();
    }

    private JsonNode getArrayNode(JsonNode data) {
        if (!data.isArray()) {
            ArrayNode arr = Json.newArray();
            arr.add(data);
            return arr;
        } else {
            return data;
        }
    }

    public void watch(String prefix, String key) {
        writeLock.lock();
        syncCommands.watch(Utilities.composeKey(prefix, key));
    }

    public void multi(boolean lock) {
        if (lock) {
            writeLock.lock();
        }
        syncCommands.multi();
    }

    public List<Object> exec() {
        try {
            return syncCommands.exec();
        } finally {
            writeLock.unlock();
        }
    }

    public Long incre(String prefix, String key) {
        readLock.lock();
        try {
            return syncCommands.incr(Utilities.composeKey(prefix, key));
        } finally {
            readLock.unlock();
        }
    }

    public Long increWithoutLock(String prefix, String key) {
        return syncCommands.incr(Utilities.composeKey(prefix, key));
    }

    public String get(String prefix, String key) {
        readLock.lock();
        try {
            return (String) syncCommands.get(Utilities.composeKey(prefix, key));
        } finally {
            readLock.unlock();
        }
    }

    public JsonNode getValue(String prefix, String key) {
        String val = null;
        readLock.lock();
        try {
            val = (String) syncCommands.get(Utilities.composeKey(prefix, key));
        } finally {
            readLock.unlock();
        }
        if (val != null) {
            return Json.parse(val);
        } else {
            return null;
        }
    }

    public String set(String prefix, String key, String value) {
        readLock.lock();
        try {
            return syncCommands.set(Utilities.composeKey(prefix, key), value);
        } finally {
            readLock.unlock();
        }
    }

    public String setWithoutLock(String prefix, String key, String value) {
        return syncCommands.set(Utilities.composeKey(prefix, key), value);
    }

    public void setValue(String prefix, String key, JsonNode value) {
        readLock.lock();
        try {
            syncCommands.set(Utilities.composeKey(prefix, key), value.toString());
        } finally {
            readLock.unlock();
        }
    }

    public void del(String prefix, String key) {
        readLock.lock();
        try {
            syncCommands.del(Utilities.composeKey(prefix, key));
        } finally {
            readLock.unlock();
        }
    }

    public void delWithKey(String key) {
        readLock.lock();
        try {
            syncCommands.del(key);
        } finally {
            readLock.unlock();
        }
    }

    public List<String> keysWithPrefixPattern(String prefix, String key) {
        String pattern = prefix;
        if (key != null) {
            pattern = Utilities.composeKey(prefix, key);
        }
        readLock.lock();
        try {
            return syncCommands.keys(pattern + "*");
        } finally {
            readLock.unlock();
        }
    }

    public List<JsonNode> bulkGet(List<String> keys) {
        readLock.lock();
        try {
            List<RedisFuture<String>> futures = new ArrayList<>();
            for (String key : keys) {
                RedisFuture<String> future = asyncCommands.get(key);
                futures.add(future);
            }
            asyncConnection.flushCommands();
            LettuceFutures.awaitAll(1, TimeUnit.MINUTES, futures.toArray(new RedisFuture[futures.size()]));
            List<JsonNode> results = new ArrayList<>(keys.size());
            for (RedisFuture<String> future : futures) {
                try {
                    String ret = future.get(5, TimeUnit.SECONDS);
                    results.add(Json.parse(ret));
                } catch (ExecutionException | TimeoutException | InterruptedException e) {
                    throw new BadRequestException(e);
                }
            }
            return results;
        } finally {
            readLock.unlock();
        }
    }

    private List<JsonNode> bulkGetFromHashSetWithKeys(Collection<String> keys) {
        readLock.lock();
        try {
            List<RedisFuture<List>> futures = new ArrayList<>();
            for (String key : keys) {
                RedisFuture<List> future = asyncCommands.hvals(key);
                futures.add(future);
            }
            asyncConnection.flushCommands();
            LettuceFutures.awaitAll(1, TimeUnit.MINUTES, futures.toArray(new RedisFuture[futures.size()]));
            List<JsonNode> results = new ArrayList<>(keys.size());
            for (RedisFuture<List> future : futures) {
                try {
                    List<String> ret = future.get(5, TimeUnit.SECONDS);
                    for (String val : ret) {
                        results.add(Json.parse(val));
                    }
                } catch (ExecutionException | TimeoutException | InterruptedException e) {
                    throw new BadRequestException(e);
                }
            }
            return results;
        } finally {
            readLock.unlock();
        }
    }

    public List<JsonNode> bulkUniqueGetFromHashSet(String prefix, List<String> keyAttrs, List<ObjectNode> data) {
        Set<String> uniqKeys = new HashSet<>();
        for (JsonNode entity : data) {
            boolean complete = Utilities.checkKeyAttributesComplete(entity, keyAttrs);
            if (complete) {
                uniqKeys.add(Utilities.composeKey(prefix, Utilities.composeKey(entity, keyAttrs)));
            }
        }
        return bulkGetFromHashSetWithKeys(uniqKeys);
    }

    public List<JsonNode> bulkGetFromHashSet(String prefix, List<String> keyAttrs, JsonNode data) {
        data = getArrayNode(data);
        List<String> keys = new ArrayList<>(data.size());
        for (JsonNode entity : data) {
            String key = Utilities.composeKey(prefix, Utilities.composeKey(entity, keyAttrs));
            keys.add(key);
        }
        return bulkGetFromHashSetWithKeys(keys);
    }

    public void indexIntoSortedSet(String prefix, String key, String scoreAttr, JsonNode data) {
        double score = data.get(scoreAttr).asDouble();
        readLock.lock();
        try {
            syncCommands.zadd(Utilities.composeKey(prefix, key), score, data.toString());
        } finally {
            readLock.unlock();
        }
    }

    public void indexIntoHashSet(String prefix, String key, String hash, JsonNode data) {
        readLock.lock();
        try {
            syncCommands.hset(Utilities.composeKey(prefix, key), hash, data.toString());
        } finally {
            readLock.unlock();
        }
    }

    public void bulkIndexIntoHashSet(String prefix, List<String> keyAttrs, List<String> hashAttrs, JsonNode data) {
        data = getArrayNode(data);
        for (JsonNode entity : data) {
            indexIntoHashSet(prefix, Utilities.composeKey(entity, keyAttrs),
                    Utilities.composeKey(entity, hashAttrs), entity);
        }
    }

    public void bulkIndexIntoSortedSet(String prefix, List<String> keyAttrs, String scoreAttr, JsonNode data) {
        data = getArrayNode(data);
        for (JsonNode entity : data) {
            indexIntoSortedSet(prefix, Utilities.composeKey(entity, keyAttrs), scoreAttr, entity);
        }
    }

    public void bulkDelWithKeys(Collection<String> keys) {
        readLock.lock();
        try {
            List<RedisFuture<String>> futures = new ArrayList<>();
            for (String key : keys) {
                RedisFuture<String> future = asyncCommands.del(key);
                futures.add(future);
            }
            asyncConnection.flushCommands();
            LettuceFutures.awaitAll(1, TimeUnit.MINUTES, futures.toArray(new RedisFuture[futures.size()]));
            for (RedisFuture<String> future : futures) {
                try {
                    future.get(5, TimeUnit.SECONDS);
                } catch (ExecutionException | TimeoutException | InterruptedException e) {
                    throw new BadRequestException(e);
                }
            }
        } finally {
            readLock.unlock();
        }
    }

    public void bulkDelWithData(String prefix, List<String> keyAttrs, JsonNode data) {
        Set<String> uniqKeys = new HashSet<>();
        for (JsonNode entity : data) {
            uniqKeys.add(Utilities.composeKey(prefix, Utilities.composeKey(entity, keyAttrs)));
        }
        bulkDelWithKeys(uniqKeys);
    }
}

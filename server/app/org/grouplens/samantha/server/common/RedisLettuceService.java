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
import play.Configuration;
import play.Logger;
import play.inject.ApplicationLifecycle;
import play.libs.F;
import play.libs.Json;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Singleton
public class RedisLettuceService implements RedisService {
    private final String cfgHost;
    private final Integer cfgPort;
    private final Integer cfgDb;

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
        Logger.info("Starting RedisLettuceService");
        {
            Logger.debug("Redis settings:");
            Logger.debug("* host={}", cfgHost);
            Logger.debug("* port={}", cfgPort);
            Logger.debug("* db={}", cfgDb);
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

        Logger.info("Connected to a redis client");
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

    public void watchKey(String key) {
        syncCommands.watch(key);
    }

    public void multi() {
        syncCommands.multi();
    }

    public List<Object> exec() {
        return syncCommands.exec();
    }

    public Long incre(String prefix, String key) {
        return syncCommands.incr(RedisService.composeKey(prefix, key));
    }

    public String get(String prefix, String key) {
        return (String) syncCommands.get(RedisService.composeKey(prefix, key));
    }

    public JsonNode getValue(String prefix, String key) {
        return Json.parse((String) syncCommands.get(RedisService.composeKey(prefix, key)));
    }

    public void set(String prefix, String key, String value) {
        syncCommands.set(RedisService.composeKey(prefix, key), value);
    }

    public void setValue(String prefix, String key, JsonNode value) {
        syncCommands.set(RedisService.composeKey(prefix, key), value.toString());
    }

    public void del(String prefix, String key) {
        syncCommands.del(RedisService.composeKey(prefix, key));
    }

    public void delWithKey(String key) {
        syncCommands.del(key);
    }

    public List<String> keysWithPrefixPattern(String prefix, String key) {
        String pattern = prefix;
        if (key != null) {
            pattern = RedisService.composeKey(prefix, key);
        }
        return syncCommands.keys(pattern + "*");
    }

    public List<JsonNode> getFromHashSet(String prefix, List<String> keyAttrs, JsonNode data) {
        List<JsonNode> results = new ArrayList<>();
        String key = RedisService.composeKey(prefix, RedisService.composeKey(data, keyAttrs));
        List<String> vals = syncCommands.hvals(key);
        for (String val : vals) {
            results.add(Json.parse(val));
        }
        return results;
    }

    public List<JsonNode> bulkGet(List<String> keys) {
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
    }

    private List<JsonNode> bulkGetFromHashSetWithKeys(Collection<String> keys) {
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
    }

    public List<JsonNode> bulkUniqueGetFromHashSet(String prefix, List<String> keyAttrs, List<ObjectNode> data) {
        Set<String> uniqKeys = new HashSet<>();
        for (JsonNode entity : data) {
            uniqKeys.add(RedisService.composeKey(prefix, RedisService.composeKey(entity, keyAttrs)));
        }
        return bulkGetFromHashSetWithKeys(uniqKeys);

    }

    public List<JsonNode> bulkGetFromHashSet(String prefix, List<String> keyAttrs, JsonNode data) {
        data = getArrayNode(data);
        List<String> keys = new ArrayList<>(data.size());
        for (JsonNode entity : data) {
            String key = RedisService.composeKey(prefix, RedisService.composeKey(entity, keyAttrs));
            keys.add(key);
        }
        return bulkGetFromHashSetWithKeys(keys);
    }

    public void indexIntoSortedSet(String prefix, String key, String scoreAttr, JsonNode data) {
        double score = data.get(scoreAttr).asDouble();
        syncCommands.zadd(RedisService.composeKey(prefix, key), score, data.toString());
    }

    public void indexIntoHashSet(String prefix, String key, String hash, JsonNode data) {
        syncCommands.hset(RedisService.composeKey(prefix, key), hash, data.toString());
    }

    public void bulkIndexIntoHashSet(String prefix, List<String> keyAttrs, List<String> hashAttrs, JsonNode data) {
        data = getArrayNode(data);
        for (JsonNode entity : data) {
            indexIntoHashSet(prefix, RedisService.composeKey(entity, keyAttrs),
                    RedisService.composeKey(entity, hashAttrs), entity);
        }
    }

    public void bulkIndexIntoSortedSet(String prefix, List<String> keyAttrs, String scoreAttr, JsonNode data) {
        data = getArrayNode(data);
        for (JsonNode entity : data) {
            indexIntoSortedSet(prefix, RedisService.composeKey(entity, keyAttrs), scoreAttr, entity);
        }
    }
}

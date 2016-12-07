package org.grouplens.samantha.server.expander;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.retriever.RetrievedResult;
import play.Configuration;
import play.Logger;
import play.inject.Injector;
import play.libs.Json;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class ExpanderUtilities {

    private ExpanderUtilities() {}

    static public List<EntityExpander> getEntityExpanders(RequestContext requestContext,
                                                          List<Configuration> expandersConfig,
                                                          Injector injector) {
        try {
            List<EntityExpander> resultExpanders = new ArrayList<>(expandersConfig.size());
            for (Configuration expanderConfig : expandersConfig) {
                Method method = Class.forName(expanderConfig.getString(ConfigKey.EXPANDER_CLASS.get()))
                        .getMethod("getExpander", Configuration.class, Injector.class, RequestContext.class);
                EntityExpander expander = (EntityExpander) method
                        .invoke(null, expanderConfig, injector, requestContext);
                resultExpanders.add(expander);
            }
            return resultExpanders;
        } catch (IllegalAccessException | InvocationTargetException
                | NoSuchMethodException | ClassNotFoundException e) {
            throw new BadRequestException(e);
        }
    }

    static public List<Configuration> getEntityExpandersConfig(Configuration parentConfig) {
        List<Configuration> expandersConfig = new ArrayList<>();
        if (parentConfig.asMap().containsKey(ConfigKey.EXPANDERS_CONFIG.get())) {
            expandersConfig = parentConfig.getConfigList(ConfigKey.EXPANDERS_CONFIG.get());
        }
        return expandersConfig;
    }

    static public List<ObjectNode> expand(List<ObjectNode> initial, List<EntityExpander> expanders,
                                          RequestContext requestContext) {
        for (EntityExpander expander : expanders) {
            long start = System.currentTimeMillis();
            initial = expander.expand(initial, requestContext);
            Logger.debug("{} time: {}", expander, System.currentTimeMillis() - start);
        }
        return initial;
    }

    static public List<ObjectNode> expandFromEntityDAO(EntityDAO entityDAO, List<ObjectNode> entityList,
                                           List<EntityExpander> entityExpanders, RequestContext requestContext) {
        entityList.clear();
        while (entityList.size() == 0 && entityDAO.hasNextEntity()) {
            ObjectNode entity = entityDAO.getNextEntity();
            entityList.clear();
            entityList.add(entity);
            entityList = ExpanderUtilities.expand(entityList, entityExpanders, requestContext);
        }
        return entityList;
    }

    static public void parseEntityFromSearchHit(List<String> entityFields,
                                                String elasticSearchScoreName,
                                                SearchHit hit, ObjectNode entity) {
        if (elasticSearchScoreName != null) {
            entity.put(elasticSearchScoreName, hit.getScore());
        }
        Map<String, SearchHitField> elasticSearchFields = hit.getFields();
        for (String fieldName : entityFields) {
            if (elasticSearchFields.containsKey(fieldName)) {
                entity.set(fieldName,
                        Json.toJson(elasticSearchFields
                                .get(fieldName).value()));
            }
        }
    }

    /**
     * Note that if exclude = false (i.e. candidate intersection), when filters is empty, all are preserved in
     *  the initialResult.
     * @return
     */
    static public List<ObjectNode> basicItemFilter(List<ObjectNode> initialResult, RetrievedResult filters,
                                                   List<String> itemAttrs, boolean exclude) {
        Set<String> filterSet = new HashSet<>();
        for (ObjectNode one : filters.getEntityList()) {
            String key = FeatureExtractorUtilities.composeConcatenatedKey(one, itemAttrs);
            filterSet.add(key);
        }
        List<ObjectNode> expandedResult = new ArrayList<>(initialResult.size());
        for (ObjectNode entity : initialResult) {
            String item = FeatureExtractorUtilities.composeConcatenatedKey(entity, itemAttrs);
            if (exclude && !filterSet.contains(item)) {
                expandedResult.add(entity);
            } else if (!exclude && (filterSet.size() == 0 || filterSet.contains(item))) {
                expandedResult.add(entity);
            }
        }
        return expandedResult;
    }
}

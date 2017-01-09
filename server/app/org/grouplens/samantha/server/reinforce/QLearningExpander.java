package org.grouplens.samantha.server.reinforce;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.ranker.RankedResult;
import org.grouplens.samantha.server.recommender.Recommender;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class QLearningExpander implements EntityExpander {
    final private String recommenderName;
    final private Transitioner transitioner;
    final private String rewardAttr;
    final private String delayedRewardAttr;
    final private double decay;
    final private Double sampleRate;
    final private Injector injector;

    public QLearningExpander(String recommenderName, Transitioner transitioner,
                             String rewardAttr, double decay, String delayedRewardAttr,
                             Double sampleRate, Injector injector) {
        this.transitioner = transitioner;
        this.rewardAttr = rewardAttr;
        this.decay = decay;
        this.delayedRewardAttr = delayedRewardAttr;
        this.recommenderName = recommenderName;
        this.injector = injector;
        this.sampleRate = sampleRate;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        try {
            Configuration transConfig = expanderConfig.getConfig("transitionerConfig");
            Method method = Class.forName(transConfig.getString(ConfigKey.TRANSITIONER_CLASS.get()))
                    .getMethod("getTransitioner", Configuration.class, Injector.class, RequestContext.class);
            Transitioner transitioner = (Transitioner) method
                    .invoke(null, transConfig, injector, requestContext);
            return new QLearningExpander(expanderConfig.getString("recommenderName"), transitioner,
                    expanderConfig.getString("rewardAttr"),
                    expanderConfig.getDouble("decay"), expanderConfig.getString("delayedRewardAttr"),
                    expanderConfig.getDouble("sampleRate"), injector);
        } catch (IllegalAccessException | InvocationTargetException
                | NoSuchMethodException | ClassNotFoundException e) {
            throw new BadRequestException(e);
        }
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        List<ObjectNode> expandedResult = new ArrayList<>();
        for (ObjectNode input : initialResult) {
            if (sampleRate == null || new Random().nextDouble() <= sampleRate) {
                List<ObjectNode> newStates = transitioner.transition(input, input);
                ObjectNode reqBody = Json.newObject();
                SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
                double qvalue = input.get(rewardAttr).asDouble();
                double delayedReward = 0.0;
                reqBody.setAll(input);
                for (ObjectNode newState : newStates) {
                    reqBody.setAll(newState);
                    RequestContext pseudoReq = new RequestContext(reqBody, requestContext.getEngineName());
                    Recommender recommender = configService.getRecommender(recommenderName, pseudoReq);
                    RankedResult rankedResult = recommender.recommend(pseudoReq);
                    if (rankedResult.getLimit() > 0) {
                        delayedReward += rankedResult.getRankingList().get(0).getScore();
                    }
                }
                input.put(delayedRewardAttr, qvalue + decay * delayedReward);
                expandedResult.add(input);
            }
        }
        return expandedResult;
    }
}

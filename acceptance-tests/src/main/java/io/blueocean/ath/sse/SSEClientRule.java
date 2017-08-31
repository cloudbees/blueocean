package io.blueocean.ath.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.blueocean.ath.BaseUrl;
import org.apache.log4j.Logger;
import org.glassfish.jersey.media.sse.EventListener;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.SseFeature;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.rules.ExternalResource;
import org.openqa.selenium.support.ui.FluentWait;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.security.SecureRandom;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;


public class SSEClientRule extends ExternalResource {
    private Logger logger = Logger.getLogger(SSEClientRule.class);

    @Override
    protected void before() throws Throwable {
        events = Lists.newCopyOnWriteArrayList();
        connect();
    }

    @Override
    protected void after() {
        clear();
    }

    @Inject @BaseUrl
    String baseUrl;

    public SSEClientRule() {
        mapper = new ObjectMapper();
    }

    ObjectMapper mapper;

    List<JSONObject> events;

    public List<JSONObject> getEvents() {
        return events;
    }

    public void clear() {
        events.clear();
    }

    private boolean logEvents;

    public boolean isLogEvents() {
        return logEvents;
    }

    public void setLogEvents(boolean logEvents) {
        this.logEvents = logEvents;
    }

    private EventListener listener = inboundEvent -> {
        JSONObject jenkinsEvent = new JSONObject(inboundEvent.readData());
        if(jenkinsEvent.has("jenkins_event") && jenkinsEvent.getString("jenkins_event").equals("job_run_queue_enter")) {
            if(jenkinsEvent.has("jenkins_object_type") &&
                jenkinsEvent.getString("jenkins_object_type").equals("org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject")) {
            } else
                if(jenkinsEvent.has("blueocean_job_pipeline_name")) {
                String pipelineName = jenkinsEvent.getString("blueocean_job_pipeline_name");
                logger.info("Build for " + pipelineName + " entered queue");
            }
        }
            events.add(jenkinsEvent);
        if(logEvents) {
            logger.info("SSE - " + jenkinsEvent.toString());
        }
    };

    EventSource source;
    public void connect() throws UnirestException, InterruptedException {
        SecureRandom rnd = new SecureRandom();
        String clientId = "ath-" + rnd.nextLong();
        HttpResponse<JsonNode> httpResponse = Unirest.get(baseUrl + "/sse-gateway/connect?clientId=" + clientId).asJson();
        JsonNode body = httpResponse.getBody();
        Client client = ClientBuilder.newBuilder().register(SseFeature.class).build();
        WebTarget target = client.target(baseUrl + "/sse-gateway/listen/" + clientId + ";jsessionid="+body.getObject().getJSONObject("data").getString("jsessionid"));
        source = EventSource.target(target).build();
        source.register(listener);
        source.open();

        JSONObject req = new JSONObject()
            .put("dispatcherId",clientId)
            .put("subscribe", new JSONArray(ImmutableList.of(
                new JSONObject().put("jenkins_org", "jenkins")
                    .put("jenkins_channel", "job"))))
            .put("unsubscribe", new JSONArray());

        Unirest.post(baseUrl + "/sse-gateway/configure?batchId=1")
            .body(req).asJson();

        logger.info("SSE Connected " + clientId);
    }

    public void untilEvent(Predicate<JSONObject> isEvent) {
        new FluentWait<List<JSONObject>>(getEvents())
            .pollingEvery(1000, TimeUnit.MILLISECONDS)
            .withTimeout(20, TimeUnit.SECONDS)
            .ignoring(NoSuchElementException.class)
            .until((Predicate<List<JSONObject>>) events -> Iterables.any(events, isEvent));
    }

    public void untilEvents(Predicate<List<JSONObject>> isEvents) {
        new FluentWait<>(getEvents())
            .pollingEvery(1000, TimeUnit.MILLISECONDS)
            .withTimeout(120, TimeUnit.SECONDS)
            .ignoring(NoSuchElementException.class)
            .until((Predicate<List<JSONObject>>) a -> {
               return isEvents.apply(a);
            });
    }
}

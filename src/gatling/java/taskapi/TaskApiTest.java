package taskapi;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;
import java.util.List;
import java.util.Random;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class TaskApiTest extends Simulation {

    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl("https://api.restful-api.dev")
            .header("Cache-Control", "no-cache")
            .contentTypeHeader("application/json")
            .acceptHeader("application/json");
    String concurrentUsers = System.getProperty("concurrentUsers", "10");

    private static final ChainBuilder initSession = exec(session -> session.set("authenticated", false));

    private static class Objects {
        private static final ChainBuilder list =
                exec(http("List objects")
                        .get("/objects")
                        .check(status().is(200))
                        .check(jmesPath("[*].id").ofList().saveAs("allObjectIds")));

        private static final ChainBuilder get =
                exec(session -> {
                    List<Integer> allObjectIds = session.getList("allObjectIds");
                    return session.set("objectId", allObjectIds.get(new Random().nextInt(allObjectIds.size())));
                })
                        .exec(http("Get object")
                                .get("/objects/#{objectId}")
                                .check(status().is(200))
                                .check(jmesPath("id").isEL("#{objectId}"))
                        );
        private static final ChainBuilder update =
                exec(http("Update object")
                        .put("/objects/#{objectId}")
                        .body(ElFileBody("data/create-object.json"))
                        .check(status().is(200)));

        private static final ChainBuilder create =
                exec(http("Create object")
                        .post("/objects")
                        .body(ElFileBody("data/create-object.json"))
                        .check(status().is(201))
                        .check(jmesPath("id").saveAs("objectId")));
    }

    private final ScenarioBuilder scn = scenario("TaskApiTest")
            .exec(initSession)
            .exec(Objects.list)
            .pause(2)
            .exec(Objects.get)
            .pause(2)
            .exec(Objects.update)
            .pause(2)
            .exec(Objects.create);

    {
        setUp(
                scn.injectClosed(
                        rampConcurrentUsers(1).to(5).during(Duration.ofSeconds(20)),
                        constantConcurrentUsers(5).during(Duration.ofSeconds(20))))
                .protocols(httpProtocol);
    }

}
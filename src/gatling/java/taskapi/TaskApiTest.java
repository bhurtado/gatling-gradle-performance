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
            .acceptHeader("application/json");

    private final FeederBuilder<Object> feeder = jsonFile("data/data.json").circular();

    private final ScenarioBuilder scn = scenario("API Performance Testing")
            .feed(feeder)
            .exec(http("POST Request")
                    .post("/objects")
                   // .body(ElFileBody("data/create-data.json"))
                    .body(StringBody("{\"id\": #{id},\"name\": \"#{name}\"}"))
                    .asJson()
                    .check(status().is(201))
                    .check(jsonPath("#.id").saveAs("generatedId")))
                    .pause(1)
                    .exec(session -> {
                        String generatedId = session.getString("generatedId");
                        System.out.println("Generated ID: " + generatedId);
                        return session;
                    })
            .pause(1)
            .exec(http("PUT Request")
                    .put("/objects/#{generatedId}")
                    .body(StringBody("{\"id\": #{generatedId}, \"name\": \"#{name} Updated\"}"))
                    .asJson()
                    .check(status().is(200)))
            .pause(1)
            .exec(http("GET Request")
                    .get("/objects/#{generatedId}")
                    .check(status().is(200)));

    {
        setUp(
                scn.injectOpen(constantUsersPerSec(5).during(Duration.ofSeconds(1)))
            ).protocols(httpProtocol);
    }
}
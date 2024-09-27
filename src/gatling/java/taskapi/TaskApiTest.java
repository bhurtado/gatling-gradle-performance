package taskapi;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class TaskApiTest extends Simulation {
        FeederBuilder.Batchable<String> feeder = csv("data/objects.csv").circular();

        HttpProtocolBuilder httpProtocol = http
                .baseUrl("https://api.restful-api.dev") // URL base
                .acceptHeader("application/json") // Header para aceptar JSON
                .contentTypeHeader("application/json"); // Header para enviar JSON

        ScenarioBuilder scn = scenario("DemoTaskApi")
                .feed(feeder)
                .exec(http("POST Object")
                        .post("/objects")
                        .body(StringBody("{ \"name\": \"#{name}\"," +
                                " \"description\": \"#{description}\", " +
                                "\"category\": \"#{category}\"," +
                                " \"price\": #{price} }"))
                        .check(status().in(201, 200))
                        .check(jsonPath("$.id").saveAs("objectId")))
                .pause(1)
                .exec(http("PUT Object")
                        .put("/objects/#{objectId}")
                        .body(StringBody("{ \"name\": \"#{name} Updated\", " +
                                "\"description\": \"#{description} Updated\", " +
                                "\"category\": \"#{category}\", " +
                                "\"price\": #{price} }"))
                        .check(status().in(200, 204)))
                .pause(1)
                .exec(http("GET Object")
                        .get("/objects/#{objectId}")
                        .check(status().is(200)));

        {
                setUp(scn.injectOpen(atOnceUsers(30)).protocols(httpProtocol));
        }
}

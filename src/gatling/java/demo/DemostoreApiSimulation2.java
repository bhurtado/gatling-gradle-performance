package demo;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.FeederBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class DemostoreApiSimulation2 extends Simulation {

    private HttpProtocolBuilder httpProtocol = http
            .baseUrl("https://demostore.gatling.io")
            .header("Cache-Control", "no-cache")
            .contentTypeHeader("application/json")
            .acceptHeader("application/json");


    private static Map<CharSequence, String> authorizationHeaders = Map.ofEntries(
            Map.entry("authorization", "Bearer #{jwt}")
    );

    private static ChainBuilder initSession = exec(session -> session.set("authenticated", false));

    private static class Authentication {
        private static ChainBuilder authenticate =
                doIf(session -> !session.getBoolean("authenticated")).then(
                        exec(http("Authenticate")
                                .post("/api/authenticate")
                                .body(StringBody("{\"username\": \"admin\",\"password\": \"admin\"}"))
                                .check(status().is(200))
                                .check(jmesPath("token").saveAs("jwt")))
                                .exec(session -> session.set("authenticated", true)));
    }

    private static class Categories {

        private static FeederBuilder.Batchable<String> categoriesFeeder =
                csv("data/categories.csv").random();

        private static ChainBuilder list =
                exec(http("List categories")
                        .get("/api/category")
                        .check(jmesPath("[? id == `6`].name").ofList().is(List.of("For Her"))));

        private static ChainBuilder update =
                feed(categoriesFeeder)
                        .exec(Authentication.authenticate)
                        .exec(http("Update category")
                                .put("/api/category/#{categoryId}")
                                .headers(authorizationHeaders)
                                .body(StringBody("{\"name\": \"#{categoryName}\"}"))
                                .check(jmesPath("name").isEL("#{categoryName}")));
    }

    private static class Products {

        private static FeederBuilder.Batchable<String> productsFeeder =
                csv("data/products.csv").circular();

        private static ChainBuilder list =
                exec(http("List products")
                        .get("/api/product?category=7")
                        .check(jmesPath("[? categoryId != '7']").ofList().is(Collections.emptyList()))
                        .check(jmesPath("[*].id").ofList().saveAs("allProductIds")));

        private static ChainBuilder get =
                exec(session -> {
                    List<Integer> allProductIds = session.getList("allProductIds");
                    return session.set("productId", allProductIds.get(new Random().nextInt(allProductIds.size())));
                })
                        .exec(http("Get product")
                                .get("/api/product/#{productId}")
                                .check(jmesPath("id").ofInt().isEL("#{productId}"))
                                .check(jmesPath("@").ofMap().saveAs("product")));


        private static ChainBuilder update =
                exec(Authentication.authenticate)
                        .exec( session -> {
                            Map<String, Object> product = session.getMap("product");
                            return session
                                    .set("productCategoryId", product.get("categoryId"))
                                    .set("productName", product.get("name"))
                                    .set("productDescription", product.get("description"))
                                    .set("productImage", product.get("image"))
                                    .set("productPrice", product.get("price"))
                                    .set("productId", product.get("id"));
                        })
                        .exec(http("Update product #{productName}")
                                .put("/api/product/#{productId}")
                                .headers(authorizationHeaders)
                                .body(ElFileBody("gatlingdemostoreapi/demostoreapisimulation/create-product.json"))
                                .check(jmesPath("price").isEL("#{productPrice}")));

        private static ChainBuilder create =
                exec(Authentication.authenticate)
                        .feed(productsFeeder)
                        .exec(http("Create product #{productName}")
                                .post("/api/product")
                                .headers(authorizationHeaders)
                                .body(ElFileBody("gatlingdemostoreapi/demostoreapisimulation/create-product.json")));
    }


    private ScenarioBuilder scn = scenario("DemostoreApiSimulation")
            .exec(initSession)
            .exec(Categories.list)
            .pause(2)
            .exec(Products.list)
            .pause(2)
            .exec(Products.get)
            .pause(2)
            .exec(Products.update)
            .pause(2)
            .repeat(3).on(exec(Products.create))
            .pause(2)
            .exec(Categories.update);

    // Open Model
//    {
//        setUp(
//                scn.injectOpen(
//                        atOnceUsers(3),
//                        nothingFor(Duration.ofSeconds(5)),
//                        rampUsers(10).during(Duration.ofSeconds(20)),
//                        nothingFor(Duration.ofSeconds(10)),
//                        constantUsersPerSec(1).during(Duration.ofSeconds(20))))
//                .protocols(httpProtocol);
//    }

    // Closed Model
    {
        setUp(
                scn.injectClosed(
                        rampConcurrentUsers(1).to(5).during(Duration.ofSeconds(20)),
                        constantConcurrentUsers(5).during(Duration.ofSeconds(20))))
                .protocols(httpProtocol);
    }
}

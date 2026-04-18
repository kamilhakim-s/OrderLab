package com.example.orderservice;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import java.time.Duration;

public class OrderSimulation extends Simulation {

    private static final String BASE_URL =
            System.getProperty("app.url", "http://localhost:8080");

    private static final String CREATE_BODY =
            "{\"customerId\":\"load-test-user\",\"status\":\"PENDING\",\"totalAmount\":\"99.99\"}";

    HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    ScenarioBuilder scn = scenario("Order CRUD Load Test")
            .exec(http("GET /orders")
                    .get("/orders")
                    .check(status().is(200)))
            .pause(Duration.ofMillis(100))
            .exec(http("POST /orders")
                    .post("/orders")
                    .body(StringBody(CREATE_BODY))
                    .check(status().is(201)));

    {
        setUp(
                scn.injectOpen(
                        rampUsersPerSec(1).to(10).during(Duration.ofSeconds(30)),
                        constantUsersPerSec(10).during(Duration.ofSeconds(30))
                )
        ).protocols(httpProtocol)
                .assertions(
                        global().responseTime().percentile(95).lt(500),
                        global().failedRequests().percent().lt(1.0)
                );
    }
}

package example;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ClosedInjectionStep;
import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.PopulationBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class GreetingSimulation extends Simulation {
    final HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://localhost:8080")
            .acceptHeader("application/json")
            .userAgentHeader("Gatling performance Test");

    ScenarioBuilder sampleScenario = scenario("Load Test greeting")
            .exec(http("get greeting")
                    .get(session -> "/greet/" + UUID.randomUUID())
                    .check(status().is(200))
            )
            .pause(5)
            .exec(http("Randomly slow")
                    .get("/slow")
                    .check(status().is(200))
            );

    public GreetingSimulation() {
        this.setUp(getPopulationBuilder())
                .protocols(httpProtocol);
    }

    /**
     * Open workload model - users arrive at a specified rate
     */
    public static OpenInjectionStep openLoadProfile() {
        String injectType = System.getProperty("injectType", "constantUsersPerSec");
        int users = Integer.getInteger("users", 10);
        int duration = Integer.getInteger("duration", 30);
        int rampDuration = Integer.getInteger("rampDuration", 60);
        int usersStart = Integer.getInteger("usersStart", users / 2);
        int usersEnd = Integer.getInteger("usersEnd", users);

        switch (injectType.toLowerCase()) {
            case "soaktest":
                return rampUsers(users).during(Duration.ofSeconds(rampDuration));
            case "capacitytest":
                return rampUsersPerSec(1).to(users).during(Duration.ofSeconds(duration));
            case "stresspeakusers":
                return stressPeakUsers(users).during(Duration.ofSeconds(duration));
            case "rampuserspersec":
                return rampUsersPerSec(usersStart).to(usersEnd).during(Duration.ofSeconds(duration));
            case "constantusers":
                return rampUsers(users).during(Duration.ofSeconds(rampDuration));
            default:
                return constantUsersPerSec(users).during(Duration.ofSeconds(duration));
        }
    }

    /**
     * Closed workload model - maintains constant concurrent users
     */
    public static ClosedInjectionStep closedLoadProfile() {
        int users = Integer.getInteger("users", 10);
        int duration = Integer.getInteger("duration", 30);
        String injectType = System.getProperty("injectType", "constantUsersPerSec");

        switch (injectType.toLowerCase()) {
            case "soaktest":
            case "capacitytest":
                return rampConcurrentUsers(1).to(users).during(Duration.ofSeconds(duration));
            default:
                return constantConcurrentUsers(users).during(Duration.ofSeconds(duration));
        }
    }

    /**
     * Determine which workload model to use based on test type
     */
    private PopulationBuilder getPopulationBuilder() {
        String workloadModel = System.getProperty("workloadModel", "open");
        String injectType = System.getProperty("injectType", "constantUsersPerSec");

        if ("closed".equalsIgnoreCase(workloadModel) ||
                "soaktest".equalsIgnoreCase(injectType) ||
                "capacitytest".equalsIgnoreCase(injectType)) {
            return sampleScenario.injectClosed(closedLoadProfile());
        } else {
            return sampleScenario.injectOpen(openLoadProfile());
        }
    }

    ChainBuilder greeting = exec(http("get greeting")
            .get(session -> "/greet/" + UUID.randomUUID())
            .check(status().is(200))
    )
            .pause(5);

    ChainBuilder slowcall = exec(http("Randomly slow")
            .get("/slow")
            .check(status().is(200))
    );

    ScenarioBuilder sampleScenario2 = scenario("Load test greeting").exec(greeting, slowcall);

    ChainBuilder sessionStep = exec(session -> {
        return session.set("someField", "value");
    });

    ChainBuilder inverseState = exec(session -> {
        boolean failed = session.isFailed();
        if (failed) {
            session.markAsSucceeded();
        } else {
            session.markAsFailed();
        }
        return session;
    });

    Iterator<Map<String, Object>> feeder = Stream.generate((Supplier<Map<String, Object>>) () -> Collections.singletonMap("dieRoll", ThreadLocalRandom.current().nextInt(1, 7))).iterator();

// The below code is to showcase a deeper usage of checks, to also validate data, and use it later on.
//    ScenarioBuilder sampleScenario = scenario("Load Test greeting")
//            .exec(http("get greeting")
//                    .get(session -> "/greet/" + UUID.randomUUID())
//                    .check(
//                            status().is(200),
//                            bodyString()
//                                    .transform(String::toUpperCase)
//                                    .validate("Contains HELLO validation", (value, session) -> {
//                                        if (value.startsWith("HELLO")) {
//                                            return value;
//                                        } else {
//                                            throw new IllegalStateException("Value " + value + " should start with HELLO");
//                                        }
//                                    })
//                                    .name("Greeting message check")
//                                    .saveAs("loudMessage")
//                    )
//            )
//            .pause(5)
//            .exec(http("Randomly slow")
//                    .get("/slow")
//                    .check(status().is(200))
//                    .checkIf(session -> session.getString("loudMessage") != null).then(status().not(404))
//            );
}

package pokemonapi;



import java.time.Duration;
import java.util.*;

import com.fasterxml.jackson.databind.jsonschema.JsonSchema;
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class PokemonApiTest extends Simulation {
    // Case 1 - Como puedo testearlo en diferentes ambientes
    String baseUrl = System.getProperty("baseUrl", "https://pokeapi.co/api/v2/pokemon");
    String concurrentUsers = System.getProperty("concurrentUsers", "5");
    // Define the data
    FeederBuilder.FileBased<Object> feeder = jsonFile("data/pokemon.json").circular();
    // Define preconditions
    // We want to test the Pokemon API with 5 pokemons
    //
    // Define the base URL and headers
    private HttpProtocolBuilder httpProtocol = http
            .baseUrl(baseUrl);


    // Define the scenario


    ScenarioBuilder scn = scenario("Pokemon API Test")
            .feed(feeder)
            .exec(http("Get Pikachu")
                    .get("/#{pokemonName}")
                    .check(jmesPath("base_experience").isEL("#{baseExperience}"))
                    .check(jmesPath("abilities[0].ability.name").find().saveAs("ability"))
                    .check(bodyString().saveAs("BODY"))
                    .check(status().is(200))
//                    .check(bodyString().transform(is -> {
//                        JsonSchema.getDefaultSchemaNode();
//                        System.out.println("Body: " + is);
//                        return true;
//                    } ))
            )
            .exec(
                    session -> {
                        System.out.println("Pokemon: " +session.getString( "ability"));
                        // return session.set("resultado", "false);
                        return session;
                    }
            );
    // Case 2 adicionar assert en el body
    // Set up the scenario

    {
        setUp(
                scn.injectClosed(
                        constantConcurrentUsers(Integer.parseInt(concurrentUsers)).during(Duration.ofSeconds(10)
                        )
                )
//                scn.injectOpen(   //Closed constantConcurrentUsers(10).during(Duration.ofSeconds(20))
//                        atOnceUsers(10),
//                        nothingFor(Duration.ofSeconds(5)),
//                        rampUsers(10).during(Duration.ofSeconds(10)),
//                        constantUsersPerSec(20).during(Duration.ofSeconds(10))
//                )
        ).protocols(httpProtocol);
    }





}

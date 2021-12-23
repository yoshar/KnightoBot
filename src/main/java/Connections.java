import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import reactor.core.publisher.Mono;

public class Connections {

    public MongoClient mongoClient;
    public DiscordClient discordClient;

    Connections() {
        String connectionString = "mongodb+srv://" + System.getenv("DBUSERNAME") + System.getenv("DBPASSWORD") + "@knightobot.noodt.mongodb.net/myFirstDatabase?retryWrites=true&w=majority";
        try{
            mongoClient = MongoClients.create(connectionString);
        }
        catch (Error e) {
            System.out.println(e);
        }

        try {
            discordClient = DiscordClient.create(System.getenv("TOKEN"));

            Mono<Void> login = discordClient.withGateway((GatewayDiscordClient gateway) -> Mono.empty());

            login.block();
        }
        catch (Error e) {
            System.out.println(e);
        }
    }
}
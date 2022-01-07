import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class KnightoBot {
    //creates a map
    private static final Map<String, Command> commands = new HashMap<>();
    private static final Properties props = new Properties();

    static {
        commands.put("ping", event -> event.getMessage().getChannel()
            .flatMap(channel -> channel.createMessage("Pong!")
            .then()));
    }

    public static void main(String[] args) {
        initProps();

        //logs into discord client
        final GatewayDiscordClient discordClient = DiscordClientBuilder.create(props.getProperty("app.token")).build()
            .login().block();

        assert discordClient != null;

        //I'll talk about in some docs lol its a messy function
        discordClient.getEventDispatcher().on(MessageCreateEvent.class)
                .flatMap(event -> Mono.just(event.getMessage().getContent())
                    .flatMap(content -> Flux.fromIterable(commands.entrySet())
                        .filter(entry -> content.startsWith('!' + entry.getKey()))
                        .flatMap(entry -> entry.getValue().execute(event))
                        .next()))
                .subscribe();

        discordClient.onDisconnect().block();

    }

    public static void initProps()   {
        String fileName = "src/main/resources/app.config";
        try(FileInputStream fis = new FileInputStream(fileName)) {
            props.load(fis);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public static MongoClient mongoConn()   {
        MongoClient mongoClient = null;

        String connectionString = "mongodb+srv://" + props.getProperty("app.dbuser") + props.getProperty("app.dbpass") + "@knightobot.noodt.mongodb.net/myFirstDatabase?retryWrites=true&w=majority";

        try{
            mongoClient = MongoClients.create(connectionString);
        }
        catch (Error e) {
            System.out.println(e);
        }

        return mongoClient;
    }
}

interface Command {
    Mono<Void> execute(MessageCreateEvent event);
}
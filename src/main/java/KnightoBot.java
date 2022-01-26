import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.MessageCreateSpec;
import org.bson.Document;
import org.bson.types.ObjectId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class KnightoBot {
    //creates a map of all the commands in the bot
    private static final Map<String, Command> commands = new HashMap<>();
    private static final Properties props = new Properties();
    private static GatewayDiscordClient discordClient;
    public static MongoClient mongo;


    static {
        commands.put("ping", event -> event.getMessage().getChannel()
            .flatMap(channel -> channel.createMessage("Pong!")
            .then()));

        commands.put("add", event -> Mono.justOrEmpty(event.getMessage().getContent())
            .map(content -> Arrays.asList(content.split(";")))
            .doOnNext(command -> {
                final String messageContent = "Does this entry look correct?\n```yaml" +
                        "\nEvent Name: " + command.get(1).trim() +
                        "\nGame Title: " + command.get(2).trim() +
                        "\nTeam Name: " + command.get(3).trim() +
                        "\nDate: " + command.get(4).trim() +
                        "\nTime: " + command.get(5).trim() +
                        "\nOpponent: " + command.get(6).trim() +
                        "\nStream: " + command.get(7).trim() + "```";

                //TODO remove after testing
                System.out.println(messageContent);

                final Snowflake channelID = event.getMessage().getChannelId();

                discordClient.getChannelById(channelID)
                        .ofType(GuildMessageChannel.class)
                        .flatMap(channel -> {
                            ButtonList buttonList = new ButtonList();

                            Mono<Message> createMessageMono = channel.createMessage(MessageCreateSpec.builder()
                                    .content(messageContent)
                                    .addComponent(ActionRow.of(buttonList.getButtons()))
                                    .build());

                            Mono<Void> tempListener = discordClient.on(ButtonInteractionEvent.class, listenerEvent -> {
                                if (listenerEvent.getCustomId().equals("accept")) {
                                    //TODO do we need to sanitize this or does mongo do that for us?
                                    //submit to mongo
                                    Document game = new Document("event", new ObjectId()); //named game to not confuse with the event keyword from the bot
                                    game.append("name", command.get(1).trim())
                                        .append("date", command.get(2).trim())
                                        .append("time", command.get(3).trim())
                                        .append("opponent", command.get(4).trim())
                                        .append("game", command.get(5).trim())
                                        .append("teamName", command.get(6).trim())
                                        .append("stream", command.get(7).trim());

                                    //TODO submit to mongo
                                    //TODO find a way to disable the buttons after they have been
                                    return listenerEvent.reply("Accepted!").withEphemeral(true);
                                }
                                else if(listenerEvent.getCustomId().equals("reject")) {
                                    //do nothing
                                    //TODO find a way to close the listener/disable the buttons
                                    return listenerEvent.reply("Rejected!").withEphemeral(false);
                                }
                                else {
                                    return Mono.empty();
                                }
                            }).timeout(Duration.ofMinutes(15))
                                    .onErrorResume(TimeoutException.class, ignore -> Mono.empty())
                                    .then();

                            return createMessageMono.then(tempListener);
                        }).subscribe();

            })
            .then()
        );
    }

    public static void main(String[] args) {
        initProps();


        //logs into discord client
        KnightoBot.discordClient = DiscordClientBuilder.create(props.getProperty("app.token")).build()
            .login().block();

        assert discordClient != null;

        //login to mongo
        mongo = mongoConn();

        //I'll talk about in some docs lol its a messy function
        discordClient.getEventDispatcher().on(MessageCreateEvent.class)
                .flatMap(event -> Mono.just(event.getMessage().getContent())
                    .flatMap(content -> Flux.fromIterable(commands.entrySet())
                        .filter(entry -> content.startsWith('!' + entry.getKey()))
                        .flatMap(entry -> entry.getValue().execute(event))
                        .next()))
                .subscribe();

        mongo.close();
        discordClient.onDisconnect().block();

    }

    public static void initProps()   {
        String fileName = "src/main/resources/app.config";
        try(FileInputStream fis = new FileInputStream(fileName)) {
            props.load(fis);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    //making mongo client
    public static MongoClient mongoConn()   {
        MongoClient mongoClient = null;

        String connectionString = "mongodb+srv://" + props.getProperty("app.dbuser") + props.getProperty("app.dbpass") + "@knightobot.noodt.mongodb.net/myFirstDatabase?retryWrites=true&w=majority";

        try{
            mongoClient = MongoClients.create(connectionString);
        }
        catch (Error e) {
            System.out.println(e.getMessage());
        }

        return mongoClient;
    }

}

interface Command {
    Mono<Void> execute(MessageCreateEvent event);
}
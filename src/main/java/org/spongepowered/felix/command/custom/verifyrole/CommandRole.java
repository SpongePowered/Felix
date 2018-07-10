package org.spongepowered.felix.command.custom.verifyrole;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ninja.leaping.configurate.ConfigurationNode;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.spongepowered.felix.command.custom.CustomCommand;
import org.spongepowered.felix.platform.DiscordPlatform;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.util.RequestBuffer;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

public class CommandRole implements CustomCommand {

    private ConfigurationNode config;

    private String discordGuild;

    private String baseDiscourseURL;
    private String baseOreURL;

    private long pluginDeveloperRole;

    private String discourseAPIKey;
    private String discourseAPIUsername;

    private int tokenLength;

    private static final String VERIFY_ROLE = "verify";
    private static final String TOKEN = "forum-token";
    private static final String[] SUBCOMMANDS = {VERIFY_ROLE, TOKEN};


    private String tokenCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private Map<String, TokenData> discordUsernameToToken = new HashMap<>();

    public CommandRole(ConfigurationNode config) {
        this.config = config;
        this.baseDiscourseURL = config.getNode("discourse", "base-url").getString();
        this.baseOreURL = config.getNode("ore", "base-url").getString();
        this.pluginDeveloperRole = config.getNode("discord", "plugin-developer-role").getLong();
        this.discourseAPIKey = config.getNode("discourse", "api", "key").getString();
        this.discourseAPIUsername = config.getNode("discourse", "api", "username").getString();
        this.tokenLength = config.getNode("discourse", "verification-token-length").getInt(20);
        this.discordGuild = config.getNode("discord", "guild").getString();
    }

    @Override
    public void process(String[] args, MessageReceivedEvent event) {

        if (!(event.getChannel() instanceof IPrivateChannel)) {
            return;
        }

        String subCommand = args.length < 2 ? "" : args[1];
        if (subCommand.equals(VERIFY_ROLE)) {
            if (args.length < 3) {
                RequestBuffer.request(() -> event.getChannel().sendMessage("Provide your Sponge forums username!"));
                return;
            }
            String forumUsername = args[2];
            if (forumUsername.contains(",")) {
                RequestBuffer.request(() -> event.getChannel().sendMessage("Forum username cannot contain a comma!"));
                return;
            }
            this.sendForumMessage(event, forumUsername);
        } else if (subCommand.equals(TOKEN)) {
            if (args.length < 3) {
                RequestBuffer.request(() -> event.getChannel().sendMessage("Missing token!"));
                return;
            }
            String token = args[2];
            this.onForumToken(event, token);
        } else {
            RequestBuffer
                    .request(() -> event.getChannel().sendMessage(String.format("Unknown subcommand '%s'. Available commands: %s", subCommand,
                            Joiner.on(",").join(SUBCOMMANDS))));
        }
    }

    private void sendForumMessage(MessageReceivedEvent event, String forumUsername) {
        String token = this.getRandomToken();
        System.err.println("Token: " + token);

        this.storeToken(event, new TokenData(token, forumUsername));

        String url = baseDiscourseURL + "/posts.json";

        HttpClient client = HttpClientBuilder.create().build();
        HttpPost postRequest = new HttpPost(url);

        ContentType contentType = ContentType.MULTIPART_FORM_DATA;

        HttpEntity entity = MultipartEntityBuilder.create()
                .addPart("api_key", new StringBody(this.discourseAPIKey, contentType))
                .addPart("api_username", new StringBody(this.discourseAPIUsername, contentType))
                .addPart("raw", new StringBody(String.format("Send the following private message to the bot: !role forum-token %s", token), contentType))
                .addPart("title", new StringBody("Your Discord verification token", contentType))
                .addPart("target_usernames", new StringBody(forumUsername, contentType))
                .addPart("archetype", new StringBody("private_message", contentType))
                .build();

        postRequest.setEntity(entity);

        try {
            org.apache.http.HttpResponse response = client.execute(postRequest);
            HttpEntity responseEntity = response.getEntity();

            if (!(response.getStatusLine().getStatusCode() == 200)) {

                String responseText = EntityUtils.toString(responseEntity);

                DiscordPlatform.LOGGER.error(String.format("Failed to send private message to %s: %s", forumUsername, responseText));
                RequestBuffer.request(() -> event.getChannel().sendMessage("Failed to send forum private message"));
                return;
            }

            RequestBuffer.request(() -> event.getChannel().sendMessage("Check your sponge forums account for a new private message!"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getRandomToken() {
        SecureRandom random = new SecureRandom();
        StringBuilder token = new StringBuilder();
        int tokenCharactersLen = this.tokenCharacters.length();
        for (int i = 0; i < tokenLength; i++) {
            token.append(this.tokenCharacters.charAt(random.nextInt(tokenCharactersLen)));
        }
        return token.toString();
    }

    private void storeToken(MessageReceivedEvent event, TokenData token) {
        this.discordUsernameToToken.put(event.getAuthor().getStringID(), token);
    }

    private TokenData getToken(MessageReceivedEvent event) {
        return this.discordUsernameToToken.get(event.getAuthor().getStringID());
    }

    private void clearToken(MessageReceivedEvent event) {
        this.discordUsernameToToken.remove(event.getAuthor().getStringID());
    }

    private void onForumToken(MessageReceivedEvent event, String forumToken) {
        TokenData tokenData = this.getToken(event);
        if (tokenData == null) {
            RequestBuffer.request(() -> event.getChannel().sendMessage(String.format("No saved token - run %s to generate one!", VERIFY_ROLE)));
            return;
        }
        if (forumToken.equals(tokenData.token)) {
            RequestBuffer.request(() -> event.getChannel().sendMessage("Granting roles!"));
            this.onForumVerify(event, tokenData.forumUsername);
        } else {
            RequestBuffer.request(() -> event.getChannel().sendMessage("Invalid token!"));
        }
    }

    private void onForumVerify(MessageReceivedEvent event, String forumUsername) {
        this.validatePluginDeveloperRole(event, forumUsername);
        this.clearToken(event);
    }

    private void validatePluginDeveloperRole(MessageReceivedEvent event, String forumUsername) {
        String url = this.baseOreURL + "/api/v1/users/" + forumUsername;
        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet getRequest = new HttpGet(url);

            HttpResponse resp = client.execute(getRequest);
            String responseText = EntityUtils.toString(resp.getEntity());

            if (!(resp.getStatusLine().getStatusCode() == 200)) {
                DiscordPlatform.LOGGER.error(String.format("Failed to get Ore projects for %s: %s", forumUsername, responseText));
                RequestBuffer.request(() -> event.getChannel().sendMessage("Failed to get Ore projects for " + forumUsername));
                return;
            }

            JsonObject user = new JsonParser().parse(responseText).getAsJsonObject();
            JsonElement projects = user.get("projects");
            if (!projects.isJsonNull() && projects.getAsJsonArray().size() > 0) {
                this.grantPluginDeveloperRole(event);
            } else {
                RequestBuffer.request(() -> event.getChannel().sendMessage("You have no Ore projects - not granting role!"));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void grantPluginDeveloperRole(MessageReceivedEvent event) {
        event.getAuthor().addRole(this.getPluginDeveloperRole(event));
        RequestBuffer.request(() -> event.getChannel().sendMessage("Successfully granted plugin developer role!"));
    }

    private IRole getPluginDeveloperRole(MessageReceivedEvent event) {
        return Preconditions.checkNotNull(this.getGuild(event.getClient()).getRoleByID(this.pluginDeveloperRole), "Failed to get plugin developer role with snowflake %s", this.pluginDeveloperRole);
    }

    private IGuild getGuild(IDiscordClient client) {
        return client.getGuilds()
                .stream().filter(g -> g.getName().equals(this.discordGuild))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(String.format("Not connected to guild %s", this.discordGuild)));
    }

    static class TokenData {
        String token;
        String forumUsername;

        TokenData(String token, String forumUsername) {
            this.token = token;
            this.forumUsername = forumUsername;
        }
    }
}

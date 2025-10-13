package app;

import config.Config;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class BotLauncher {
    public static void main(String[] args) {

        String token = Config.get("DISCORD_TOKEN");

        Bot bot = new Bot();

        System.setProperty("lavaplayer.youtube.country", "US");
        System.setProperty("http.agent", "Mozilla/5.0");

        JDABuilder.createDefault(token, GatewayIntent.GUILD_VOICE_STATES)
                .setStatus(OnlineStatus.ONLINE)
                .addEventListeners(bot)
                .build();
    }
}
package app;

import config.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;

public class BotLauncher {
    public static void main(String[] args) {

        String token = Config.get("DISCORD_TOKEN");

        Bot bot = new Bot();

        JDABuilder.createDefault(token)
                .setStatus(OnlineStatus.ONLINE)
                .addEventListeners(bot)
                .build();

    }
}

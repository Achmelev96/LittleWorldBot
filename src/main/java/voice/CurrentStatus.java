package voice;

import localization.BotLanguage;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.managers.AudioManager;

public class CurrentStatus {

    private final JDA jda;
    private final Guild guild;
    private final AudioManager audioManager;
    private final AudioChannelUnion userChannel;
    private final AudioChannelUnion botChannel;
    private final BotLanguage language;

    private CurrentStatus(
            JDA jda,
            Guild guild,
            AudioManager audioManager,
            AudioChannelUnion userChannel,
            AudioChannelUnion botChannel,
            BotLanguage language
    ) {
        this.jda = jda;
        this.guild = guild;
        this.audioManager = audioManager;
        this.userChannel = userChannel;
        this.botChannel = botChannel;
        this.language = language;
    }

    public static CurrentStatus from(SlashCommandInteractionEvent event, BotLanguage language) {
        return build(event.getJDA(), event.getGuild(), event.getMember(), language);
    }

    public static CurrentStatus from(CommandAutoCompleteInteractionEvent event, BotLanguage language) {
        return build(event.getJDA(), event.getGuild(), event.getMember(), language);
    }

    private static CurrentStatus build(JDA jda, Guild guild, Member member, BotLanguage language) {
        var voice = buildVoiceContext(guild, member);

        return new CurrentStatus(
                jda,
                guild,
                voice.audioManager,
                voice.userChannel,
                voice.botChannel,
                language == null ? BotLanguage.ENGLISH : language
        );
    }

    private static VoiceCtx buildVoiceContext(Guild guild, Member member) {

        if (guild == null) {
            return new VoiceCtx(null, null, null);
        }

        AudioManager manager = guild.getAudioManager();

        AudioChannelUnion userChannel = null;
        if (member != null && member.getVoiceState() != null) {
            userChannel = member.getVoiceState().getChannel();
        }

        AudioChannelUnion botChannel = manager.getConnectedChannel();

        return new VoiceCtx(manager, userChannel, botChannel);
    }

    private record VoiceCtx(
            AudioManager audioManager,
            AudioChannelUnion userChannel,
            AudioChannelUnion botChannel
    ) {}

    public JDA jda() { return jda; }
    public Guild guild() { return guild; }
    public AudioManager audioManager() { return audioManager; }
    public AudioChannelUnion userChannel() { return userChannel; }
    public AudioChannelUnion botChannel() { return botChannel; }
    public BotLanguage language() { return language; }
}

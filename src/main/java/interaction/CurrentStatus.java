package interaction;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;

import java.util.Locale;

public final class CurrentStatus {
    private final JDA jda;
    private final Guild guild;
    private final Member member;
    private final User user;
    private final Locale locale;
    private final VoiceStatus voice;

    private CurrentStatus(JDA jda, Guild guild, Member member, User user, Locale locale, VoiceStatus voice) {
        this.jda = jda;
        this.guild = guild;
        this.member = member;
        this.user = user;
        this.locale = locale;
        this.voice = voice;
    }

    public JDA jda() { return jda; }
    public Guild guild() { return guild; }
    public Member member() { return member; }
    public User user() { return user; }
    public Locale locale() { return locale; }
    public VoiceStatus voice() { return voice; }

    public static CurrentStatus from(SlashCommandInteractionEvent event) {
        return fromGeneric(event);
    }

    public static CurrentStatus from(CommandAutoCompleteInteractionEvent event) {
        return fromGeneric(event);
    }

    private static CurrentStatus fromGeneric(GenericInteractionCreateEvent event) {
        var jda = event.getJDA();
        var guild = event.getGuild();
        var member = event.getMember();
        var user = event.getUser();
        var locale = event.getUserLocale() != null
                ? Locale.forLanguageTag(event.getUserLocale().getLocale())
                : Locale.ROOT;

        var voice = buildVoiceStatus(guild, member);
        return new CurrentStatus(jda, guild, member, user, locale, voice);
    }

    private static VoiceStatus buildVoiceStatus(Guild guild, Member member) {
        if (guild == null) {
            return new VoiceStatus(null, null, null, null);
        }
        AudioManager audioManager = guild.getAudioManager();

        AudioChannelUnion userChannel = null;
        if (member != null && member.getVoiceState() != null) {
            userChannel = member.getVoiceState().getChannel();
        }

        AudioChannelUnion botChannel = audioManager.getConnectedChannel();

        return new VoiceStatus(guild, audioManager, userChannel, botChannel);
    }
}

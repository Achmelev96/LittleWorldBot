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

public final class InteractionContext {
    private final JDA jda;
    private final Guild guild;
    private final Member member;
    private final User user;
    private final Locale locale;
    private final VoiceContext voice;

    private InteractionContext(JDA jda, Guild guild, Member member, User user, Locale locale, VoiceContext voice) {
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
    public VoiceContext voice() { return voice; }

    public static InteractionContext from(SlashCommandInteractionEvent event) {
        return fromGeneric(event);
    }

    public static InteractionContext from(CommandAutoCompleteInteractionEvent event) {
        return fromGeneric(event);
    }

    private static InteractionContext fromGeneric(GenericInteractionCreateEvent event) {
        var jda = event.getJDA();
        var guild = event.getGuild();
        var member = event.getMember();
        var user = event.getUser();
        var locale = event.getUserLocale() != null
                ? Locale.forLanguageTag(event.getUserLocale().getLocale())
                : Locale.ROOT;

        var voice = buildVoiceContext(guild, member);
        return new InteractionContext(jda, guild, member, user, locale, voice);
    }

    private static VoiceContext buildVoiceContext(Guild guild, Member member) {
        if (guild == null) {
            return new VoiceContext(null, null, null, null);
        }
        AudioManager audioManager = guild.getAudioManager();

        AudioChannelUnion userChannel = null;
        if (member != null && member.getVoiceState() != null) {
            userChannel = member.getVoiceState().getChannel();
        }

        AudioChannelUnion botChannel = audioManager.getConnectedChannel();

        return new VoiceContext(guild, audioManager, userChannel, botChannel);
    }
}

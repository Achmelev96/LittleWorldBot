package musicpanel;

import audio.GuildHandler;
import audio.MusicCore;
import audio.PlayerControlService;
import localization.BotLanguage;
import localization.MessageCatalog;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import settings.GuildLanguageService;
import settings.SettingsAccessPolicy;

public final class MusicPanelInteractionHandler {
    private static final String LANGUAGE_SELECT_ID = "settings:language";

    private final MusicCore musicCore;
    private final MusicPanelService panelService;
    private final GuildLanguageService languageService;
    private final SettingsAccessPolicy accessPolicy;
    private final MessageCatalog messages;

    public MusicPanelInteractionHandler(
            MusicCore musicCore,
            MusicPanelService panelService,
            GuildLanguageService languageService,
            SettingsAccessPolicy accessPolicy,
            MessageCatalog messages
    ) {
        this.musicCore = musicCore;
        this.panelService = panelService;
        this.languageService = languageService;
        this.accessPolicy = accessPolicy;
        this.messages = messages;
    }

    public void handle(ButtonInteractionEvent event) {
        String[] id = event.getComponentId().split(":", 3);
        if (id.length != 3 || !"music".equals(id[0])) return;

        Guild guild = event.getGuild();
        if (guild == null || !panelService.isActive(guild.getIdLong(), id[1], event.getMessageIdLong())) {
            replyEphemeral(event, languageFor(guild), "panel.expired");
            return;
        }

        if ("menu".equals(id[2])) {
            openLanguageMenu(event, guild);
            return;
        }
        if (!"toggle".equals(id[2]) && !"skip".equals(id[2])) return;
        if (!isInBotVoiceChannel(event.getMember(), guild)) {
            replyEphemeral(event, languageFor(guild), "panel.same_channel_required");
            return;
        }

        GuildHandler handler = musicCore.getGuildHandler(guild);
        event.deferEdit().queue();
        if ("toggle".equals(id[2])) {
            if (handler.getPlayer().getPlayingTrack() != null) {
                PlayerControlService.togglePause(handler);
            }
        } else {
            PlayerControlService.skip(handler);
        }
        panelService.refresh(guild);
    }

    public void handle(StringSelectInteractionEvent event) {
        if (!LANGUAGE_SELECT_ID.equals(event.getComponentId())) return;
        Guild guild = event.getGuild();
        Member member = event.getMember();
        BotLanguage current = languageFor(guild);
        if (guild == null || !accessPolicy.canManage(member, event.getUser().getIdLong())) {
            event.reply(messages.get(current, "settings.no_permission")).setEphemeral(true).queue();
            return;
        }
        if (event.getValues().isEmpty()) {
            event.reply(messages.get(current, "settings.invalid_language")).setEphemeral(true).queue();
            return;
        }
        String code = event.getValues().getFirst();
        if (!"en".equals(code) && !"ru".equals(code)) {
            event.reply(messages.get(current, "settings.invalid_language")).setEphemeral(true).queue();
            return;
        }

        BotLanguage selected = BotLanguage.fromCode(code);
        languageService.setLanguage(guild.getIdLong(), selected);
        event.editMessage(messages.get(selected, "settings.language_updated"))
                .setComponents()
                .queue();
        panelService.refresh(guild);
    }

    private void openLanguageMenu(ButtonInteractionEvent event, Guild guild) {
        BotLanguage language = languageFor(guild);
        if (!accessPolicy.canManage(event.getMember(), event.getUser().getIdLong())) {
            replyEphemeral(event, language, "settings.no_permission");
            return;
        }

        StringSelectMenu menu = StringSelectMenu.create(LANGUAGE_SELECT_ID)
                .addOption("English", "en")
                .addOption("Русский", "ru")
                .setDefaultValues(language.code())
                .build();
        event.reply(messages.get(language, "settings.choose_language"))
                .setEphemeral(true)
                .addComponents(ActionRow.of(menu))
                .queue();
    }

    private boolean isInBotVoiceChannel(Member member, Guild guild) {
        if (member == null || member.getVoiceState() == null || guild.getSelfMember().getVoiceState() == null) {
            return false;
        }
        var userChannel = member.getVoiceState().getChannel();
        var botChannel = guild.getSelfMember().getVoiceState().getChannel();
        return userChannel != null && botChannel != null && userChannel.getIdLong() == botChannel.getIdLong();
    }

    private BotLanguage languageFor(Guild guild) {
        return guild == null ? BotLanguage.ENGLISH : languageService.getLanguage(guild.getIdLong());
    }

    private void replyEphemeral(ButtonInteractionEvent event, BotLanguage language, String key) {
        event.reply(messages.get(language, key)).setEphemeral(true).queue();
    }
}

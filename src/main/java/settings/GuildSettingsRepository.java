package settings;

import localization.BotLanguage;

import java.util.Optional;

public interface GuildSettingsRepository {
    Optional<BotLanguage> findLanguage(long guildId);
    void saveLanguage(long guildId, BotLanguage language);
}

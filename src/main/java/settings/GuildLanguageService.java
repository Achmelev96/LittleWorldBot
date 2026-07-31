package settings;

import localization.BotLanguage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GuildLanguageService {
    private final GuildSettingsRepository repository;
    private final Map<Long, BotLanguage> cache = new ConcurrentHashMap<>();

    public GuildLanguageService(GuildSettingsRepository repository) {
        this.repository = repository;
    }

    public BotLanguage getLanguage(long guildId) {
        return cache.computeIfAbsent(guildId, id -> repository.findLanguage(id)
                .orElseGet(() -> {
                    repository.saveLanguage(id, BotLanguage.ENGLISH);
                    return BotLanguage.ENGLISH;
                }));
    }

    public void setLanguage(long guildId, BotLanguage language) {
        repository.saveLanguage(guildId, language);
        cache.put(guildId, language);
    }
}

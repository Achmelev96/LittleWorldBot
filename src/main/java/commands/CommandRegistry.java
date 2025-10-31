package commands;

import commands.autocomplete.AutocompleteProvider;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CommandRegistry {

    private final Map<String, SlashCommand> slashMap = new ConcurrentHashMap<>();

    public void registerSlash(String commandName, SlashCommand handler) {
        Objects.requireNonNull(commandName, "commandName");
        Objects.requireNonNull(handler, "handler");
        String key = commandName.toLowerCase(Locale.ROOT);
        var prev = slashMap.putIfAbsent(key, handler);
        if (prev != null && prev != handler) {
            throw new IllegalStateException("Slash handler already registered for /" + key);
        }
    }

    public Optional<SlashCommand> getSlash(String commandName) {
        if (commandName == null) return Optional.empty();
        return Optional.ofNullable(slashMap.get(commandName.toLowerCase(Locale.ROOT)));
    }

    private final Map<String, AutocompleteProvider> autocompleteMap = new ConcurrentHashMap<>();

    private static String acKey(String commandName, String optionName) {
        return (commandName == null ? "" : commandName.toLowerCase(Locale.ROOT))
                + "|" +
                (optionName == null ? "" : optionName.toLowerCase(Locale.ROOT));
    }

    public void register(String commandName, String optionName, AutocompleteProvider provider) {
        Objects.requireNonNull(commandName, "commandName");
        Objects.requireNonNull(optionName, "optionName");
        Objects.requireNonNull(provider, "provider");

        String key = acKey(commandName, optionName);
        var prev = autocompleteMap.putIfAbsent(key, provider);
        if (prev != null && prev != provider) {
            throw new IllegalStateException("Autocomplete provider already registered for " + key);
        }
    }

    public Optional<AutocompleteProvider> findAutocomplete(String commandName, String optionName) {
        if (commandName == null || optionName == null) return Optional.empty();
        return Optional.ofNullable(autocompleteMap.get(acKey(commandName, optionName)));
    }
}

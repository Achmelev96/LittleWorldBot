package localization;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class MessageCatalog {
    private static final String BUNDLE_NAME = "i18n.messages";

    public String get(BotLanguage language, String key, Object... arguments) {
        BotLanguage resolvedLanguage = language == null ? BotLanguage.ENGLISH : language;
        ResourceBundle bundle = loadBundle(resolvedLanguage);

        String template;
        try {
            template = bundle.getString(key);
        } catch (MissingResourceException missingTranslation) {
            template = loadBundle(BotLanguage.ENGLISH).getString(key);
        }

        return new MessageFormat(template, localeFor(resolvedLanguage)).format(arguments);
    }

    private ResourceBundle loadBundle(BotLanguage language) {
        return ResourceBundle.getBundle(BUNDLE_NAME, localeFor(language));
    }

    private Locale localeFor(BotLanguage language) {
        return Locale.forLanguageTag(language.code());
    }
}

package localization;

import java.util.Arrays;

public enum BotLanguage {
    ENGLISH("en"),
    RUSSIAN("ru");

    private final String code;

    BotLanguage(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static BotLanguage fromCode(String code) {
        if (code == null) return ENGLISH;
        return Arrays.stream(values())
                .filter(language -> language.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(ENGLISH);
    }
}

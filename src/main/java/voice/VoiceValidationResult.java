package voice;

public enum VoiceValidationResult {
    OK,
    GUILD_UNAVAILABLE,
    USER_NOT_IN_VOICE,
    BOT_NOT_IN_VOICE,
    DIFFERENT_CHANNEL,
    MISSING_PERMISSIONS
}

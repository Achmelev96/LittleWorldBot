package settings;

import localization.BotLanguage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public final class SqliteGuildSettingsRepository implements GuildSettingsRepository {
    private final String jdbcUrl;

    public SqliteGuildSettingsRepository(Path databasePath) {
        prepareParentDirectory(databasePath);
        this.jdbcUrl = "jdbc:sqlite:" + databasePath.toAbsolutePath();
        initializeSchema();
    }

    @Override
    public Optional<BotLanguage> findLanguage(long guildId) {
        String sql = "SELECT language FROM guild_settings WHERE guild_id = ?";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, guildId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return Optional.empty();
                return Optional.of(BotLanguage.fromCode(result.getString("language")));
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Could not read guild language", error);
        }
    }

    @Override
    public void saveLanguage(long guildId, BotLanguage language) {
        String sql = """
                INSERT INTO guild_settings (guild_id, language, updated_at)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT(guild_id) DO UPDATE SET
                    language = excluded.language,
                    updated_at = CURRENT_TIMESTAMP
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, guildId);
            statement.setString(2, language.code());
            statement.executeUpdate();
        } catch (SQLException error) {
            throw new IllegalStateException("Could not save guild language", error);
        }
    }

    private void initializeSchema() {
        String sql = """
                CREATE TABLE IF NOT EXISTS guild_settings (
                    guild_id INTEGER PRIMARY KEY,
                    language TEXT NOT NULL DEFAULT 'en',
                    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """;
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException error) {
            throw new IllegalStateException("Could not initialize guild settings database", error);
        }
    }

    private Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }

    private static void prepareParentDirectory(Path databasePath) {
        Path absoluteParent = databasePath.toAbsolutePath().getParent();
        if (absoluteParent == null) return;
        try {
            Files.createDirectories(absoluteParent);
        } catch (IOException error) {
            throw new IllegalStateException("Could not create database directory", error);
        }
    }
}

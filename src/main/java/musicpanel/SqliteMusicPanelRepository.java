package musicpanel;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class SqliteMusicPanelRepository implements MusicPanelRepository {
    private final String jdbcUrl;

    public SqliteMusicPanelRepository(Path databasePath) {
        prepareParentDirectory(databasePath);
        this.jdbcUrl = "jdbc:sqlite:" + databasePath.toAbsolutePath();
        initializeSchema();
    }

    @Override
    public List<StoredMusicPanel> findAll() {
        String sql = "SELECT guild_id, channel_id, message_id FROM music_panels";
        List<StoredMusicPanel> panels = new ArrayList<>();
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                panels.add(new StoredMusicPanel(
                        result.getLong("guild_id"),
                        result.getLong("channel_id"),
                        result.getLong("message_id")
                ));
            }
            return panels;
        } catch (SQLException error) {
            throw new IllegalStateException("Could not read music panels", error);
        }
    }

    @Override
    public void save(StoredMusicPanel panel) {
        String sql = """
                INSERT INTO music_panels (guild_id, channel_id, message_id, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT(guild_id) DO UPDATE SET
                    channel_id = excluded.channel_id,
                    message_id = excluded.message_id,
                    updated_at = CURRENT_TIMESTAMP
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, panel.guildId());
            statement.setLong(2, panel.channelId());
            statement.setLong(3, panel.messageId());
            statement.executeUpdate();
        } catch (SQLException error) {
            throw new IllegalStateException("Could not save music panel", error);
        }
    }

    @Override
    public void delete(long guildId) {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM music_panels WHERE guild_id = ?"
             )) {
            statement.setLong(1, guildId);
            statement.executeUpdate();
        } catch (SQLException error) {
            throw new IllegalStateException("Could not delete music panel", error);
        }
    }

    private void initializeSchema() {
        String sql = """
                CREATE TABLE IF NOT EXISTS music_panels (
                    guild_id INTEGER PRIMARY KEY,
                    channel_id INTEGER NOT NULL,
                    message_id INTEGER NOT NULL,
                    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """;
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException error) {
            throw new IllegalStateException("Could not initialize music panel database", error);
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
        Path parent = databasePath.toAbsolutePath().getParent();
        if (parent == null) return;
        try {
            Files.createDirectories(parent);
        } catch (IOException error) {
            throw new IllegalStateException("Could not create database directory", error);
        }
    }
}

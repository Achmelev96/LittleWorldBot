package musicpanel;

import java.util.List;

public interface MusicPanelRepository {
    List<StoredMusicPanel> findAll();

    void save(StoredMusicPanel panel);

    void delete(long guildId);
}

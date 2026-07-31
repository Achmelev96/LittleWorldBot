package musicpanel;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.util.UUID;

final class MusicPanelState {
    MessageChannel desiredChannel;
    Message message;
    String token = newToken();
    boolean creating;
    boolean deleting;
    boolean editing;
    boolean refreshPending;

    void invalidate() {
        token = newToken();
    }

    private static String newToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}

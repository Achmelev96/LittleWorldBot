package settings;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

public final class SettingsAccessPolicy {
    private final long botOwnerId;

    public SettingsAccessPolicy(long botOwnerId) {
        this.botOwnerId = botOwnerId;
    }

    public boolean canManage(Member member, long userId) {
        if (botOwnerId > 0 && userId == botOwnerId) return true;
        if (member == null) return false;
        return member.isOwner()
                || member.hasPermission(Permission.ADMINISTRATOR)
                || member.hasPermission(Permission.MANAGE_SERVER)
                || member.hasPermission(Permission.MODERATE_MEMBERS);
    }
}

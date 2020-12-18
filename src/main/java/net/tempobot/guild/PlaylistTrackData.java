package net.tempobot.guild;

import org.jetbrains.annotations.NotNull;

public class PlaylistTrackData {

    private final long guildId;
    private final int trackId;
    private final String trackUrl;
    private final String member;
    private final long addedAt;

    public PlaylistTrackData(final long guildId,
                             final int trackId,
                             @NotNull("track cannot be null") final String trackUrl,
                             final String member,
                             final long addedAt) {
        this.guildId = guildId;
        this.trackId = trackId;
        this.trackUrl = trackUrl;
        this.member = member;
        this.addedAt = addedAt;
    }

    public long getGuildId() {
        return this.guildId;
    }

    public int getTrackId() {
        return this.trackId;
    }

    public String getTrackUrl() {
        return this.trackUrl;
    }

    public String getMember() {
        return this.member;
    }

    public long getAddedAt() {
        return this.addedAt;
    }

}

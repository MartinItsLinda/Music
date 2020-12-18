package net.tempobot.guild;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GuildPlaylist {

    private final long guildId;
    private final List<PlaylistTrackData> tracks;

    public GuildPlaylist(final long guildId,
                         @NotNull("tracks cannot be null") final List<PlaylistTrackData> tracks) {
        this.guildId = guildId;
        this.tracks = tracks;
    }

    public long getGuildId() {
        return this.guildId;
    }

    public List<PlaylistTrackData> getTracks() {
        return this.tracks;
    }

}

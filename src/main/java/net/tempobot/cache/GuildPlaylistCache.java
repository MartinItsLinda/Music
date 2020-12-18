package net.tempobot.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.sheepybot.api.entities.database.Database;
import com.sheepybot.api.entities.database.object.DBCursor;
import com.sheepybot.internal.caching.EntityLoadingCache;
import net.tempobot.Main;
import net.tempobot.guild.GuildPlaylist;
import net.tempobot.guild.PlaylistTrackData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GuildPlaylistCache extends EntityLoadingCache<Long, GuildPlaylist> {

    private static final GuildPlaylistCache INSTANCE = new GuildPlaylistCache();

    /**
     * @return An instance of this {@link GuildPlaylistCache}
     */
    public static GuildPlaylistCache get() {
        return GuildPlaylistCache.INSTANCE;
    }

    public GuildPlaylistCache() {
        super(CacheBuilder.newBuilder()
                .maximumSize(5_000L)
                .expireAfterAccess(1, TimeUnit.HOURS)
                .build(new GuildPlaylistCache.GuildPlaylistCacheLoader()));
    }

    private static final class GuildPlaylistCacheLoader extends CacheLoader<Long, GuildPlaylist> {

        @Override
        public GuildPlaylist load(@NotNull("key cannot be null") final Long guildId) {
            final Database db = Main.get().getDatabase();

            final DBCursor cursor = db.find("SELECT `playlist_id`, `playlist_name`, `track_name`, `track_url`, `member`, `added_at` FROM `guild_playlists` WHERE `guild_id` = ?", guildId);

            final List<PlaylistTrackData> tracks = new ArrayList<>(cursor.size());
            cursor.forEach(object -> tracks.add(new PlaylistTrackData(guildId, object.getInt("playlist_id"), object.getString("track_url"), object.getString("member"), object.getLong("added_at"))));

            return new GuildPlaylist(guildId, tracks);
        }

    }

}

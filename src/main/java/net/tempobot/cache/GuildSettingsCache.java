package net.tempobot.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.sheepybot.api.entities.database.Database;
import com.sheepybot.api.entities.database.object.DBObject;
import com.sheepybot.internal.caching.EntityLoadingCache;
import net.tempobot.Main;
import net.tempobot.guild.GuildSettings;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GuildSettingsCache extends EntityLoadingCache<Long, GuildSettings> {

    private static final GuildSettingsCache INSTANCE = new GuildSettingsCache();

    /**
     * @return An instance of this {@link GuildSettingsCache}
     */
    public static GuildSettingsCache get() {
        return GuildSettingsCache.INSTANCE;
    }

    public GuildSettingsCache() {
        super(CacheBuilder.newBuilder()
                .maximumSize(5_000L)
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .build(new GuildSettingsCacheLoader()));
    }

    private static final class GuildSettingsCacheLoader extends CacheLoader<Long, GuildSettings> {

        @Override
        public GuildSettings load(@NotNull(value = "key cannot be null") final Long guildId) {

            final Database db = Main.get().getDatabase();

            final DBObject guild = db.findOne("SELECT `guild_id`, `guild_prefix`, `guild_premium`, UNIX_TIMESTAMP(`added_at`) `added_at` FROM `guilds` WHERE `guild_id` = ?", guildId);
            final List<Long> djRoles = db.find("SELECT `role_id` FROM `guild_dj_roles` WHERE `guild_id` = ?", guildId).map(object -> object.getLong("role_id")).collect(Collectors.toList());

            return new GuildSettings(guildId, guild.getString("guild_prefix"), guild.getBoolean("guild_premium"), djRoles, guild.getLong("added_at"));
        }

    }

}

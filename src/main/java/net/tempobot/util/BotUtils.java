package net.tempobot.util;

import net.tempobot.Main;

public class BotUtils {

    /**
     * @return The amount of {@link net.dv8tion.jda.api.entities.Guild}s stored in the database.
     */
    public static int getGuildCount() {
        return Main.get().getDatabase().find("SELECT COUNT(`guild_id`) AS count FROM `guilds` WHERE `is_active` = TRUE;").next().getInt("count");
    }

    /**
     * @return The total amount of tracks we've played.
     */
    public static int getTotalTracksPlayed() {
        return Main.get().getDatabase().find("SELECT COUNT(*) AS count FROM `guild_track_history`;").next().getInt("count");
    }

    /**
     * @return The total amount of tracks played by a specific {@link net.dv8tion.jda.api.entities.Guild}
     */
    public static int getTotalTracksPlayed(final long guildId) {
        return Main.get().getDatabase().find("SELECT COUNT(*) AS count FROM `guild_track_history` WHERE `guild_id` = ?", guildId).next().getInt("count");
    }

    /**
     * Gets the total amount of tracks played in the last {@code days} days
     *
     * @return The amount of tracks played in the last {@code days} days.
     */
    public static int getTotalTracksPlayed(final int days) {
        return Main.get().getDatabase().find("SELECT COUNT(*) as COUNT FROM `guild_track_history` WHERE `added_at` >= NOW() - INTERVAL ? DAY;", days).next().getInt("count");
    }

}

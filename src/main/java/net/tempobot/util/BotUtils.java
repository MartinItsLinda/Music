package net.tempobot.util;

import net.tempobot.Main;

public class BotUtils {

    /**
     * @return The amount of {@link net.dv8tion.jda.api.entities.Guild}s stored in the database.
     */
    public static int getGuildCount() {
        return Main.get().getDatabase().find("SELECT COUNT(`guild_id`) AS count FROM `guilds` WHERE `is_active` = TRUE;").next().getInt("count");
    }

}

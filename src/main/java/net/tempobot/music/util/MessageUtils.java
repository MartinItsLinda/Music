package net.tempobot.music.util;

import com.sheepybot.api.entities.messaging.Messaging;
import net.dv8tion.jda.api.entities.Guild;
import net.tempobot.Main;
import net.tempobot.cache.GuildSettingsCache;
import net.tempobot.guild.GuildSettings;

import java.util.concurrent.TimeUnit;

public class MessageUtils {

    /**
     * This is a utility method which automagically sets a time to delete the message sent if the guild
     * is using the auto delete functionality.
     *
     * @param guild   The {@link Guild} to send in.
     * @param builder The {@link Messaging.MessageActionBuilder} to send.
     */
    public static void sendMessage(final Guild guild,
                                   final Messaging.MessageActionBuilder builder) {
        final GuildSettings settings = GuildSettingsCache.get().getEntity(guild.getIdLong());
        if (settings.isAutoDelete()) {
            builder.deleteAfter(Main.MESSAGE_DELETE_TIME, TimeUnit.SECONDS);
        }
        builder.send();
    }

}

package net.tempobot.music.commands;

import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import com.sheepybot.api.entities.command.parsers.ArgumentParsers;
import com.sheepybot.api.entities.database.Database;
import net.tempobot.Main;
import net.tempobot.cache.GuildPlaylistCache;
import net.tempobot.cache.GuildSettingsCache;
import net.tempobot.guild.GuildPlaylist;
import net.tempobot.guild.GuildSettings;
import net.tempobot.guild.PlaylistTrackData;
import net.tempobot.music.util.MessageUtils;

import java.util.concurrent.TimeUnit;

public class CommandPlaylist implements CommandExecutor {

    @Override
    public void execute(final CommandContext context,
                        final Arguments args) {

        final GuildSettings settings = GuildSettingsCache.get().getEntity(context.getGuild().getIdLong());
        if (!settings.isPremium()) {
            context.message("You've discovered a premium command :wink:, it appears your Guild doesn't have a premium membership. You can purchase one on our site [here](https://tempobot.net/premium).");
        } else {

            final Database database = Main.get().getDatabase();
            final String op = args.next(ArgumentParsers.STRING);

            final long guildId = context.getGuild().getIdLong();

            final GuildPlaylist playlist = GuildPlaylistCache.get().getEntity(guildId);

            switch (op.toLowerCase()) {
                case "add":

                    final String url = args.next(ArgumentParsers.STRING);

                    if (settings.isCheckingDuplicates()) {
                        for (final PlaylistTrackData data : playlist.getTracks()) {
                            if (data.getTrackUrl().equals(url)) {
                                context.message("That track is already in this guilds playlist. :confused:");
                                break;
                            }
                        }
                    }

                    //TODO: modify api to return a class containing returned data and update this to use right values

                    database.execute("INSERT INTO `guild_playlists`(`guild_id`, `track_name`, `track_author`, `track_url`, `member`) VALUES(?, ?, ?, ?, ?);",
                            guildId,
                            "",
                            "",
                            url,
                            context.getUser().getAsTag());

                    playlist.getTracks().add(new PlaylistTrackData(guildId, 0, url, context.getUser().getAsTag(), 0));

                    context.message("I've added %s by %s to this guilds playlist!").deleteAfter(15, TimeUnit.SECONDS).send();

                    break;
                case "remove":

                    final int trackId = args.next(ArgumentParsers.INTEGER);

                    break;
                case "load":
                    break;
                case "clear":
                    break;
                default:
                    MessageUtils.sendMessage(context.getGuild(), context.message(String.format("I'm not sure what you mean by %s, did you type it incorrectly?", op)));
                    break;
            }

        }

    }

}

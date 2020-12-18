package net.tempobot.music.commands;

import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import com.sheepybot.api.entities.command.parsers.ArgumentParsers;
import net.dv8tion.jda.api.Permission;
import net.tempobot.Main;
import net.tempobot.cache.GuildSettingsCache;
import net.tempobot.guild.GuildSettings;
import net.tempobot.music.audio.AudioController;
import net.tempobot.music.util.DateUtils;
import net.tempobot.music.util.MessageUtils;

import java.util.concurrent.TimeUnit;

public class CommandSeek implements CommandExecutor {

    @Override
    public void execute(final CommandContext context,
                        final Arguments args) {

        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (controller == null || controller.getPlayer().getPlayingTrack() == null) {
            MessageUtils.sendMessage(context.getGuild(), context.message("Sorry but there's no music currently playing :frowning:"));
        } else {

            if (!controller.getPlayer().getPlayingTrack().isSeekable()) {
                MessageUtils.sendMessage(context.getGuild(), context.message("I'm sorry but this kind of audio track doesn't support seek"));
            } else {

                final long timestamp;
                try {
                    timestamp = System.currentTimeMillis() - DateUtils.parseDateDiff(args.next(ArgumentParsers.REMAINING_STRING_NO_QUOTE), false);
                } catch (final Exception ignored) {
                    MessageUtils.sendMessage(context.getGuild(), context.message("Where do you want me to seek to? Type it like this 1h2m5s."));
                    return;
                }

                if (timestamp > controller.getPlayer().getPlayingTrack().getDuration()) {
                    MessageUtils.sendMessage(context.getGuild(), context.message("Sorry but the track isn't that long. :confused:"));
                } else {
                    controller.getPlayer().getPlayingTrack().setPosition(timestamp);
                    MessageUtils.sendMessage(context.getGuild(), context.message("I've zoomed ahead to requested time."));
                }

            }

        }

    }

}

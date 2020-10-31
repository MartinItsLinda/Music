package net.tempobot.music.commands;

import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import com.sheepybot.api.entities.command.parsers.ArgumentParsers;
import net.tempobot.Main;
import net.tempobot.music.audio.AudioController;
import net.tempobot.music.util.DateUtils;

import java.util.concurrent.TimeUnit;

public class CommandSeek implements CommandExecutor {

    @Override
    public void execute(final CommandContext context,
                        final Arguments args) {

        context.getMessage().delete().queue();

        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (controller == null || controller.getPlayer().getPlayingTrack() == null) {
            context.message("Sorry but there's no music currently playing :frowning:").deleteAfter(10, TimeUnit.SECONDS).send();
        } else {

            if (!controller.getPlayer().getPlayingTrack().isSeekable()) {
                context.message("I'm sorry but this kind of audio track doesn't support seek").deleteAfter(10, TimeUnit.SECONDS).send();
            } else {

                final long timestamp;
                try {
                    timestamp = System.currentTimeMillis() - DateUtils.parseDateDiff(args.next(ArgumentParsers.REMAINING_STRING_NO_QUOTE), false);
                } catch (final Exception ignored) {
                    context.message("Where do you want me to seek to? Type it like this 1h2m5s.").deleteAfter(10, TimeUnit.SECONDS);
                    return;
                }

                if (timestamp > controller.getPlayer().getPlayingTrack().getDuration()) {
                    context.message("Sorry but the track isn't that long. :confused:").deleteAfter(10, TimeUnit.SECONDS);
                } else {
                    controller.getPlayer().getPlayingTrack().setPosition(timestamp);
                    context.message("I've zoomed ahead to requested time.").deleteAfter(10, TimeUnit.SECONDS).send();
                }

            }

        }

    }

}

package me.seymourkrelborn.music.commands;

import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import com.sheepybot.api.entities.command.parsers.ArgumentParsers;
import me.seymourkrelborn.Main;
import me.seymourkrelborn.music.audio.AudioController;
import me.seymourkrelborn.music.util.DateUtils;

public class CommandSeek implements CommandExecutor {

    @Override
    public void execute(final CommandContext context,
                        final Arguments args) {

        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (controller == null || controller.getPlayer().getPlayingTrack() == null) {
            context.reply("There's no music currently playing.");
        } else {

            if (!controller.getPlayer().getPlayingTrack().isSeekable()) {
                context.reply("I'm sorry but this kind of audio track doesn't support seek");
            } else {

                final long timestamp;
                try {
                    timestamp = System.currentTimeMillis() - DateUtils.parseDateDiff(args.next(ArgumentParsers.REMAINING_STRING_NO_QUOTE), false);
                } catch (final Exception ignored) {
                    context.reply("Where do you want me to seek to? Type it like this 1h2m5s.");
                    return;
                }

                if (timestamp > controller.getPlayer().getPlayingTrack().getDuration()) {
                    context.reply("The track isn't that long.");
                } else {
                    controller.getPlayer().getPlayingTrack().setPosition(timestamp);
                    context.reply("I've seeked to the requested time.");
                }

            }

        }

    }

}

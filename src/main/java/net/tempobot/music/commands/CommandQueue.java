package net.tempobot.music.commands;

import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import com.sheepybot.api.entities.command.parsers.ArgumentParsers;
import net.dv8tion.jda.api.Permission;
import net.tempobot.Main;
import net.tempobot.music.audio.AudioController;
import net.tempobot.music.audio.TrackScheduler;

import java.util.concurrent.TimeUnit;

public class CommandQueue implements CommandExecutor {

    @Override
    public void execute(final CommandContext context,
                        final Arguments args) {

        if (context.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) context.getMessage().delete().queue();

        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (controller == null) {
            context.message("Sorry but there's no music currently playing :frowning:").deleteAfter(10, TimeUnit.SECONDS).send();
        } else {
            final TrackScheduler scheduler = controller.getTrackScheduler();

            int page = args.next(ArgumentParsers.alt(ArgumentParsers.INTEGER, 0));
            if (page < 1) page = 1;

            scheduler.sendCurrentQueue(context.getChannel(), page, true);

        }

    }
}

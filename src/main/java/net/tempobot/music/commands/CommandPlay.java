package net.tempobot.music.commands;

import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import com.sheepybot.api.entities.command.parsers.ArgumentParsers;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.tempobot.Main;
import net.tempobot.music.audio.AudioController;
import net.tempobot.music.commands.handler.DefaultLoadResultHandler;
import org.apache.commons.validator.routines.UrlValidator;

import java.util.concurrent.TimeUnit;

public class CommandPlay implements CommandExecutor {

    public static final UrlValidator VALIDATOR = new UrlValidator();

    @Override
    public void execute(final CommandContext context,
                        final Arguments args) {

        context.getMessage().delete().queue();

        final GuildVoiceState state = context.getMember().getVoiceState();
        final AudioController controller = Main.get().getAudioLoader().getOrCreate(context.getGuild(), context.getChannel(), state.getChannel());

        if (controller.getVoiceChannelId() != -1 && controller.getVoiceChannelId() != state.getChannel().getIdLong()) {
            context.message("Sorry but you've gotta be in the same voice channel as me to use this. :frowning:").deleteAfter(10, TimeUnit.SECONDS).send();
        } else {
            final DefaultLoadResultHandler handler = new DefaultLoadResultHandler(context, controller);

            String query = String.join(" ", args.next(ArgumentParsers.REMAINING_STRING_NO_QUOTE));
            if (!VALIDATOR.isValid(query)) {
                query = "ytsearch: " + query;
            }

            controller.setTextChannelId(context.getChannel());

            controller.getAudioPlayerManager().loadItem(query, handler);
        }

    }

}

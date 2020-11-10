package net.tempobot.music.commands;

import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import net.dv8tion.jda.api.Permission;
import net.tempobot.Main;
import net.tempobot.music.audio.AudioController;
import net.tempobot.music.audio.TrackScheduler;
import net.dv8tion.jda.api.entities.GuildVoiceState;

import java.util.concurrent.TimeUnit;

public class CommandSkip implements CommandExecutor {

    @Override
    public void execute(final CommandContext context, 
                        final Arguments args) {

        if (context.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) context.getMessage().delete().queue();

        final GuildVoiceState state = context.getMember().getVoiceState();
        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (state == null || state.getChannel() == null || controller == null || controller.getVoiceChannelId() != state.getChannel().getIdLong()) {
            context.message("Sorry but you've gotta be in the same voice channel as me to use this. :frowning:").deleteAfter(10, TimeUnit.SECONDS).send();
        } else {
            final TrackScheduler scheduler = controller.getTrackScheduler();
            if (scheduler.isDJ(context.getMember())) {
                context.message("The song was force skipped by DJ " + context.getUser().getAsTag() + ".").deleteAfter(10, TimeUnit.SECONDS).send();
                scheduler.next(true);
            } else {
                if (scheduler.voteSkip(context.getMember())) {
                    if ((state.getChannel().getMembers().stream().filter(member -> !member.getUser().isBot()).count() / 2) <= scheduler.getSkipVotes()) {
                        context.message("Skipping the current song as at least half the voice channel has voted.").deleteAfter(10, TimeUnit.SECONDS).send();
                        scheduler.next(true);
                    } else {
                        context.message(context.getMember().getAsMention() + " voted to skip this song.").deleteAfter(10, TimeUnit.SECONDS).send();
                    }
                } else {
                    context.message("You already voted skip :confused:").deleteAfter(10, TimeUnit.SECONDS).send();
                }
            }
        }
        
    }
    
}

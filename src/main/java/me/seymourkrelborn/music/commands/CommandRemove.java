package me.seymourkrelborn.music.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sheepybot.Bot;
import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import com.sheepybot.api.entities.command.parsers.ArgumentParsers;
import me.seymourkrelborn.Main;
import me.seymourkrelborn.music.audio.AudioController;
import me.seymourkrelborn.music.audio.TrackScheduler;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;

import java.util.LinkedList;
import java.util.List;

public class CommandRemove implements CommandExecutor {

    @Override
    public void execute(final CommandContext context, 
                        final Arguments args) {

        final GuildVoiceState state = context.getMember().getVoiceState();
        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (state == null || state.getChannel() == null || controller == null || controller.getVoiceChannelId() != state.getChannel().getIdLong()) {
            context.reply("You must be in a voice channel to do this.");
        } else {
            final TrackScheduler scheduler = controller.getTrackScheduler();
            if (scheduler.getPlayer().getPlayingTrack() == null) {
                context.reply("There's no music currently playing.");
            } if (!(scheduler.isDJ(context.getMember()))) {
                context.reply("You must be either alone, have the DJ role or an admin to do this.");
            } else {
                final List<AudioTrack> queue = (LinkedList<AudioTrack>) scheduler.getQueue();

                if (context.getMessage().getMentionedMembers().size() > 0) {

                    final Member member = context.getMessage().getMentionedMembers().get(0);
                    final String tag = member.getUser().getAsTag();

                    queue.removeIf(track -> track.getUserData() != null && track.getUserData().equals(tag));

                    context.reply("I've removed all tracks requested by " + member.getAsMention());

                } else {

                    int position = args.next(ArgumentParsers.INTEGER);

                    if (position <= 0 || position > queue.size()) {
                        context.reply("Please track between 1 and " + queue.size());
                    } else {
                        final AudioTrack track = queue.get(--position);
                        final AudioTrackInfo info = track.getInfo();

                        queue.remove(position);

                        context.reply("I've removed " + info.title + " by " + info.author + " from the queue");
                    }

                }
            }
        }
        
    }
}

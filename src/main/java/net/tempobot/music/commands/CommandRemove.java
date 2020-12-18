package net.tempobot.music.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import com.sheepybot.api.entities.command.parsers.ArgumentParsers;
import net.dv8tion.jda.api.Permission;
import net.tempobot.Main;
import net.tempobot.cache.GuildSettingsCache;
import net.tempobot.guild.GuildSettings;
import net.tempobot.music.audio.AudioController;
import net.tempobot.music.audio.TrackScheduler;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.tempobot.music.util.MessageUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CommandRemove implements CommandExecutor {

    @Override
    public void execute(final CommandContext context, 
                        final Arguments args) {

        final GuildVoiceState state = context.getMember().getVoiceState();
        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (state == null || state.getChannel() == null || controller == null || controller.getVoiceChannelId() != state.getChannel().getIdLong()) {
            MessageUtils.sendMessage(context.getGuild(), context.message("Sorry but you've gotta be in the same voice channel as me to use this. :frowning:"));
        } else {
            final TrackScheduler scheduler = controller.getTrackScheduler();
            if (scheduler.getPlayer().getPlayingTrack() == null) {
                MessageUtils.sendMessage(context.getGuild(), context.message("Sorry but there's no music currently playing :frowning:"));
            } if (!(scheduler.isDJ(context.getMember()))) {
                MessageUtils.sendMessage(context.getGuild(), context.message("You must be either alone, have a role called DJ or be an admin to do this."));
            } else {
                final List<AudioTrack> queue = (LinkedList<AudioTrack>) scheduler.getQueue();

                if (context.getMessage().getMentionedMembers().size() > 0) {

                    final Member member = context.getMessage().getMentionedMembers().get(0);
                    final String tag = member.getUser().getAsTag();

                    queue.removeIf(track -> track.getUserData() != null && track.getUserData().equals(tag));

                    MessageUtils.sendMessage(context.getGuild(), context.message("I've removed all tracks requested by " + member.getAsMention()));

                } else {

                    int position = args.next(ArgumentParsers.INTEGER);

                    if (position <= 0 || position > queue.size()) {
                        MessageUtils.sendMessage(context.getGuild(), context.message("Please give a track number between 1 and " + queue.size()));
                    } else {
                        final AudioTrack track = queue.get(--position);
                        final AudioTrackInfo info = track.getInfo();

                        queue.remove(position);

                        MessageUtils.sendMessage(context.getGuild(), context.message("I've removed " + info.title + " by " + info.author + " from the queue"));
                    }

                }
            }
        }
        
    }
}

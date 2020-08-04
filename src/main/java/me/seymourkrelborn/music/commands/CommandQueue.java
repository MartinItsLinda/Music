package me.seymourkrelborn.music.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sheepybot.Bot;
import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import com.sheepybot.api.entities.command.parsers.ArgumentParsers;
import com.sheepybot.api.entities.messaging.Messaging;
import me.seymourkrelborn.Main;
import me.seymourkrelborn.music.audio.AudioController;
import me.seymourkrelborn.music.audio.AudioLoader;
import me.seymourkrelborn.music.audio.TrackScheduler;
import me.seymourkrelborn.music.util.AudioUtils;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class CommandQueue implements CommandExecutor {

    @Override
    public void execute(final CommandContext context, 
                        final Arguments args) {

        final AudioController controller = Main.get().getAudioLoader().getController(context.getGuild());
        if (controller == null) {
            context.reply("There's no music currently playing.");
        } else {
            final TrackScheduler scheduler = controller.getTrackScheduler();
            if (scheduler.getQueue().isEmpty()) {
                context.reply("There are no songs in the queue.");
            } else {

                long skip = args.next(ArgumentParsers.alt(ArgumentParsers.LONG, 0L));
                if (skip < 1) skip = 1;

                final List<AudioTrack> queue = scheduler.getQueue().stream().skip(((skip - 1) * 10)).limit(10).collect(Collectors.toList());
                final int pageCount = (int) (Math.ceil(scheduler.getQueue().size() / 10) + 1);

                if (queue.size() == 0) {
                    context.reply("Please enter a page between 1 and " + pageCount);
                } else {

                    final long totalMusicLength = queue.stream().mapToLong(AudioTrack::getDuration).sum();

                    final EmbedBuilder builder = Messaging.getLocalEmbedBuilder();

                    String description;
                    if (scheduler.getQueue().size() > 1) {
                        description = "There are " + scheduler.getQueue().size() + " songs in the queue";
                    } else {
                        description = "There is one song in the queue";
                    }

                    description += "\n\nThere's " + AudioUtils.formatTrackLength(totalMusicLength, false) + " of music to play\n\n";

                    builder.setTitle(context.getGuild().getName() + "'s Music Queue");
                    builder.setColor(new Color(61, 90, 254));

                    final StringJoiner joiner = new StringJoiner("\n");

                    int position = 1;
                    for (final Iterator<AudioTrack> iterator = queue.iterator(); iterator.hasNext(); position++) {

                        final AudioTrack track = iterator.next();
                        final AudioTrackInfo info = track.getInfo();

                        joiner.add(String.format("%d) `%s` requested by `%s`", position, info.title, track.getUserData()));
                    }

                    builder.addField("Song Queue", joiner.toString(), true);
                    builder.setFooter(String.format("Page %d/%d", skip, pageCount));
                    builder.setDescription(description);

                    context.reply(builder.build());

                }

            }

        }
        
    }
}

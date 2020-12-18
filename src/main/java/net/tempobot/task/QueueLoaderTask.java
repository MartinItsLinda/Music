package net.tempobot.task;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sheepybot.api.entities.database.Database;
import com.sheepybot.api.entities.database.object.DBCursor;
import com.sheepybot.api.entities.database.object.DBObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.tempobot.Main;
import net.tempobot.cache.GuildSettingsCache;
import net.tempobot.guild.GuildSettings;
import net.tempobot.music.audio.AudioController;
import net.tempobot.music.audio.TrackScheduler;
import net.tempobot.music.commands.handler.AutoQueueLoadResultHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueLoaderTask implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueLoaderTask.class);

    private final long guildId;
    private final JDA jda;

    public QueueLoaderTask(final long guildId,
                           @NotNull("jda cannot be null") final JDA jda) {
        this.guildId = guildId;
        this.jda = jda;
    }

    @Override
    public void run() {

        final GuildSettings settings = GuildSettingsCache.get().getEntity(this.guildId);
        if (settings.isAutoJoin()) {

            final Database database = Main.get().getDatabase();

            final DBCursor queue = database.find("SELECT `id`, `guild_id`, `track_url`, `member` FROM `guild_queues` WHERE `guild_id` = ?;", this.guildId);
            final DBObject channelData = database.findOne("SELECT `guild_id`, `text_channel_id`, `voice_channel_id` FROM `guild_queue_channel_data` WHERE `guild_id` = ?", this.guildId);

            if (!queue.getCursor().isEmpty() && !channelData.isEmpty()) {

                final Guild guild = this.jda.getGuildById(this.guildId);
                if (guild == null) return;

                final TextChannel textChannel = guild.getTextChannelById(channelData.getLong("text_channel_id"));
                if (textChannel == null) return;

                final VoiceChannel voiceChannel = guild.getVoiceChannelById(channelData.getLong("voice_channel_id"));
                if (voiceChannel == null) return;

                if (!voiceChannel.getMembers().stream().allMatch(member -> member.getUser().isBot())) {

                    final AudioController controller = Main.get().getAudioLoader().getOrCreate(guild, textChannel, voiceChannel);
                    final TrackScheduler scheduler = controller.getTrackScheduler();

                    final AudioPlayerManager manager = controller.getAudioPlayerManager();

                    queue.forEach(track -> manager.loadItemOrdered(this.guildId, track.getString("track_url"), new AutoQueueLoadResultHandler(scheduler, track.getString("member"))));

                }

            }

            database.execute("DELETE FROM `guild_queues` WHERE `guild_id` = ?", this.guildId);
            database.execute("DELETE FROM `guild_queue_channel_data` WHERE `guild_id` = ?", this.guildId);

        }

    }

}

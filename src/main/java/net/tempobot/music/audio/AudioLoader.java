package net.tempobot.music.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;
import com.sheepybot.api.entities.messaging.Messaging;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.tempobot.Main;
import net.tempobot.cache.GuildSettingsCache;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AudioLoader {

    private final AudioPlayerManager audioPlayerManager;
    private final Map<Long, AudioController> controllers;

    public AudioLoader(final AudioPlayerManager audioPlayerManager) {
        this.audioPlayerManager = audioPlayerManager;
        this.controllers = new ConcurrentHashMap<>();

        this.audioPlayerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.HIGH);

        Main.get().getScheduler().runTaskRepeating(new AudioLoaderCleanupTask(this), TimeUnit.MINUTES.toMillis(15), TimeUnit.MINUTES.toMillis(5));
    }

    /**
     * @return A {@link Map} of {@link Guild} IDs and their associated {@link AudioController}
     */
    public Map<Long, AudioController> getControllers() {
        return this.controllers;
    }

    /**
     * Filters all {@link AudioController}s by those that are currently playing audio.
     *
     * @return A {@link Map} of {@link Guild} IDs and their associated {@link AudioController}s
     */
    public Map<Long, AudioController> getActiveControllers() {
        return this.controllers.entrySet().stream().filter(entry -> {
            final AudioController controller = entry.getValue();
            return !controller.getPlayer().isPaused() && controller.getPlayer().getPlayingTrack() != null && controller.getPlayer().getPlayingTrack().getState() != AudioTrackState.FINISHED;
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * @param guild        The owning {@link Guild} of the {@link AudioController}
     * @param textChannel  The {@link TextChannel} to submit messages
     * @param voiceChannel The {@link VoiceChannel} to run music in
     * @return An {@link AudioController} for the specified {@link Guild}
     */
    public AudioController getOrCreate(final Guild guild,
                                       final TextChannel textChannel,
                                       final VoiceChannel voiceChannel) {
        return this.controllers.computeIfAbsent(guild.getIdLong(), k -> new AudioController(this, this.audioPlayerManager, guild, GuildSettingsCache.get().getEntity(guild.getIdLong()), textChannel, voiceChannel));
    }

    /**
     * @param guild The owning {@link Guild} fo the {@link AudioController}
     * @return The {@link AudioController} or {@code null} if the specified guild has no associated {@link AudioController}
     */
    public AudioController getController(final Guild guild) {
        return this.controllers.get(guild.getIdLong());
    }

    /**
     * Remove the {@link AudioController} from the registered controllers
     * destroying the {@link AudioController} if it was found
     *
     * @param guild The guild to remove the associated {@link AudioController} from
     */
    public void remove(final Guild guild) {
        final AudioController controller = this.controllers.get(guild.getIdLong());
        if (controller == null) {
            throw new IllegalArgumentException("AudioController doesn't exist for guild");
        }
        this.remove(controller);
    }

    /**
     * Remove and destroy the registered {@link AudioController}
     *
     * @param controller The {@link AudioController} to remove
     */
    public void remove(final AudioController controller) {
        if (controller != null) {
            this.controllers.remove(controller.getGuildId());
        }
    }

    /**
     * Destroy this {@link AudioLoader}
     *
     * @param saveQueues Whether to save queues to the database
     */
    public void shutdown(final boolean saveQueues) {
        this.controllers.entrySet().removeIf(entry ->  {
            final AudioController controller = entry.getValue();
            controller.destroy(saveQueues);
            return true;
        });
    }

    private static class AudioLoaderCleanupTask implements Runnable {

        private static final Logger LOGGER = LoggerFactory.getLogger(AudioLoaderCleanupTask.class);

        private final AudioLoader audioLoader;

        AudioLoaderCleanupTask(@NotNull("audioLoader cannot be null") final AudioLoader audioLoader) {
            this.audioLoader = audioLoader;
        }

        @Override
        public void run() {

            LOGGER.info("Cleaning up expired AudioControllers (> 15 min waiting)");

            this.audioLoader.controllers.entrySet().removeIf(entry -> {
                final AudioController controller = entry.getValue();
                if (!controller.isValid()) {
                    return true;
                } else if (controller.getTimeLastPlayed() != -1 && (System.currentTimeMillis() - controller.getTimeLastPlayed()) >= TimeUnit.MINUTES.toMillis(15)) {

                    final TextChannel channel = controller.getJDA().getTextChannelById(controller.getTextChannelId());
                    if (channel != null) {
                        Messaging.message(channel, "To save on bandwidth I've automatically left the voice channel due to inactivity.").deleteAfter(10, TimeUnit.SECONDS).send();
                    }

                    controller.destroy(false);

                    return true;
                }
                return false;
            });

        }
    }

}

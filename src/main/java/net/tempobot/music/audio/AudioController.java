package net.tempobot.music.audio;

import com.sedmelluq.discord.lavaplayer.filter.equalizer.Equalizer;
import com.sedmelluq.discord.lavaplayer.filter.equalizer.EqualizerFactory;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.tempobot.guild.GuildSettings;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class AudioController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AudioController.class);

    private final AudioLoader loader;
    private final AudioPlayerManager audioPlayerManager;
    private final long guildId;
    private final AtomicLong textChannelId;
    private final AtomicLong voiceChannelId;
    private final AudioPlayer audioPlayer;
    private final TrackScheduler scheduler;
    private final JDA jda;
    private final EqualizerFactory equalizerFactory;

    private long pauseTime;

    private final AtomicBoolean isValid = new AtomicBoolean(false);

    public AudioController(final AudioLoader loader,
                           final AudioPlayerManager audioPlayerManager,
                           final Guild guild,
                           final GuildSettings settings,
                           final TextChannel textChannel,
                           final VoiceChannel voiceChannel) {
        this.loader = loader;
        this.audioPlayerManager = audioPlayerManager;
        this.guildId = guild.getIdLong();
        this.textChannelId = new AtomicLong(textChannel.getIdLong());
        this.voiceChannelId = new AtomicLong(voiceChannel.getIdLong());
        this.audioPlayer = audioPlayerManager.createPlayer();
        this.scheduler = new TrackScheduler(this, this.audioPlayer, settings, guild.getJDA());
        this.jda = guild.getJDA();
        this.equalizerFactory = new EqualizerFactory();
        this.audioPlayer.setFilterFactory(this.equalizerFactory);
        this.audioPlayer.setFrameBufferDuration(500);

        this.audioPlayer.addListener(this.scheduler);
        guild.getAudioManager().setSendingHandler(new AudioPlayerSendHandler(audioPlayer));

        this.isValid.set(true);

        this.connect(voiceChannel);
    }

    /**
     * @return The {@link AudioLoader}
     */
    public AudioLoader getAudioLoader() {
        return this.loader;
    }

    /**
     * @return The {@link AudioPlayerManager}
     */
    public AudioPlayerManager getAudioPlayerManager() {
        return this.audioPlayerManager;
    }

    /**
     * @return The {@link Guild} id this {@link AudioController} is for
     */
    public long getGuildId() {
        return this.guildId;
    }

    /**
     * @return The {@link TextChannel} id this {@link AudioController} was ran in.
     */
    public long getTextChannelId() {
        return this.textChannelId.get();
    }

    /**
     * @param channel The new channel to submit messages to
     */
    public void setTextChannelId(@NotNull("channel cannot be null") final TextChannel channel) {
        this.textChannelId.set(channel.getIdLong());
    }

    /**
     * @return The {@link VoiceChannel} id
     */
    public long getVoiceChannelId() {
        return this.voiceChannelId.get();
    }

    /**
     * @return The {@link AudioPlayer} for this {@link AudioController}
     */
    public AudioPlayer getPlayer() {
        return this.audioPlayer;
    }

    /**
     * @return The {@link TrackScheduler} for this {@link AudioController}
     */
    public TrackScheduler getTrackScheduler() {
        return this.scheduler;
    }

    /**
     * @return The {@link JDA} instance
     */
    public JDA getJDA() {
        return this.jda;
    }

    /**
     * Update the connected {@link VoiceChannel}
     *
     * @param channel The new {@link VoiceChannel}
     */
    public void setVoiceChannelId(@NotNull("channel cannot be null") final VoiceChannel channel) {
        this.voiceChannelId.set(channel.getIdLong());
    }

    /**
     * @return Whether the current player is paused or not
     */
    public boolean isPaused() {
        return this.getPlayer().isPaused();
    }

    /**
     * @param paused The new pause state
     */
    public void setPaused(final boolean paused) {
        this.getPlayer().setPaused(paused);
        if (paused) {
            this.pauseTime = System.currentTimeMillis();
        } else {
            this.pauseTime = -1;
        }
    }

    /**
     * Retrieve the time in which this {@link AudioController} was paused.
     *
     * @return The time in millis this {@link AudioController} last stopped playing at or {@code -1} if the {@link AudioController} is still playing.
     */
    public long getTimeLastPlayed() {
        return this.pauseTime;
    }

    /**
     * Set the time at which this {@link AudioController} stopped playing audio
     * so we can filter our inactive controllers.
     *
     * @param pauseTime The current time in millis
     */
    public void setPauseTime(final long pauseTime) {
        this.pauseTime = pauseTime;
    }

    /**
     * Connect to the given {@link VoiceChannel}, the connection attempt will be cancelled
     * if the bot doesn't have permission to view, connect or talk in the {@link VoiceChannel}
     *
     * @param channel The {@link VoiceChannel} to join
     *
     * @throws IllegalArgumentException If the {@link VoiceChannel} doesn't belong in this {@link AudioController}s owning {@link Guild}
     */
    public void connect(@NotNull("channel cannot be null") final VoiceChannel channel) throws IllegalArgumentException, PermissionException {
        if (!this.isValid()) {
            throw new IllegalArgumentException("Cannot connect to voice channel as this AudioController is no longer valid.");
        }

        LOGGER.info(String.format("Received connection request to voice channel #%s, checking permissions...", channel.getName()));

        if (!channel.getGuild().getSelfMember().hasPermission(channel, Permission.VOICE_CONNECT)) {
            LOGGER.info(String.format("Lacking required permission VOICE_CONNECT for channel #%s!", channel.getName()));
            return;
        }

        LOGGER.info(String.format("Self member has required permissions to connect to voice channel #%s, connecting...", channel.getName()));

        try {
            final Guild guild = this.jda.getGuildById(this.guildId);
            if (guild == null) {
                throw new IllegalStateException("Cannot connect to guild as guild isn't cached");
            } else {
                guild.getAudioManager().openAudioConnection(channel);
            }
        } catch (final Exception ex) {
            LOGGER.info("Encountered an error whilst attempting to connect to voice channel.", ex);
        }

        this.setVoiceChannelId(channel);

        LOGGER.info(String.format("Connected to #%s!", channel.getName()));

    }

    /**
     * @return {@code true} if this {@link AudioController} is valid, {@code false} otherwise
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isValid() {
        return this.isValid.get();
    }

    /**
     * Disconnect from the current {@link VoiceChannel} (this does not empty the queue)
     */
    public void disconnect() {
        if (this.getVoiceChannelId() != -1) {

            final Guild guild = this.jda.getGuildById(this.guildId);
            if (guild == null) {
                LOGGER.info(String.format("Couldn't retrieve guild %d from guild cache whilst attempting to disconnect Audio Manager", this.guildId));
            } else {

                LOGGER.info(String.format("Disconnecting Audio Controller from Guild %s", guild.getName()));

                this.audioPlayer.stopTrack();
                this.voiceChannelId.set(-1);

                guild.getAudioManager().closeAudioConnection();

            }

        }
    }

    /**
     * Destroy this {@link AudioController}
     *
     * @param saveQueues Whether to save the audio queue to the database
     */
    public boolean destroy(final boolean saveQueues) {
        if (this.isValid.get()) {

            if (saveQueues) {
                this.scheduler.saveQueue();
            }

            try {
                this.scheduler.saveTracks();
                this.scheduler.clear();
                this.scheduler.getUpdaterTask().cancel();
                this.audioPlayer.destroy();
                this.disconnect();
            } catch (final Throwable ex) {
                LOGGER.info("An error occurred during destruction of AudioController for guild %s", ex);
            }

            this.loader.remove(this);
            this.isValid.set(false);
            return true;
        }
        return false;
    }

}

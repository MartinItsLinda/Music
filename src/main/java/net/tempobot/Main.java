package net.tempobot;

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sheepybot.api.entities.command.Command;
import com.sheepybot.api.entities.database.Database;
import com.sheepybot.api.entities.messaging.Messaging;
import com.sheepybot.api.entities.module.CommandRegistry;
import com.sheepybot.api.entities.module.EventRegistry;
import com.sheepybot.api.entities.module.Module;
import com.sheepybot.api.entities.module.ModuleData;
import net.dv8tion.jda.api.Permission;
import net.tempobot.cache.GuildSettingsCache;
import net.tempobot.music.audio.AudioLoader;
import net.tempobot.music.commands.*;
import net.tempobot.music.event.GuildJoinLeaveListener;
import net.tempobot.music.event.GuildMessageReactionListener;
import net.tempobot.music.event.GuildVoiceListener;

@ModuleData(name = "Music", version = "1.6.9")
public class Main extends Module {

    private static Main instance;

    public static Main get() {
        if (instance == null) {
            throw new IllegalStateException("Cannot use #get() when the module isn't loaded yet");
        }
        return instance;
    }

    private AudioLoader loader;

    @Override
    public void onEnable() {

        Main.instance = this;

        Messaging.setDefaultFailureConsumer(__ -> {});

        getLogger().info("Starting up audio player manager...");

        final AudioPlayerManager audioPlayerManager = new DefaultAudioPlayerManager();

        final YoutubeAudioSourceManager youtubeAudioSourceManager = new YoutubeAudioSourceManager();
        youtubeAudioSourceManager.setPlaylistPageCount(5);

        getLogger().info("Registering audio sources...");

        audioPlayerManager.registerSourceManager(youtubeAudioSourceManager);
        audioPlayerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        audioPlayerManager.registerSourceManager(new BandcampAudioSourceManager());
        audioPlayerManager.registerSourceManager(new VimeoAudioSourceManager());
        audioPlayerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        audioPlayerManager.registerSourceManager(new BeamAudioSourceManager());
        audioPlayerManager.registerSourceManager(new LocalAudioSourceManager());

        getLogger().info("Configuring audio player manager...");

        audioPlayerManager.getConfiguration().setFilterHotSwapEnabled(true);
        audioPlayerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.HIGH);

        getLogger().info("Starting up audio loader...");

        this.loader = new AudioLoader(audioPlayerManager);

        getLogger().info("Creating required tables...");

        final Database database = this.getDatabase();

        database.execute("CREATE TABLE IF NOT EXISTS `guilds`(" +
                "`guild_id` BIGINT NOT NULL PRIMARY KEY, " +
                "`guild_prefix` VARCHAR(5) NOT NULL, " +
                "`guild_premium` BOOLEAN NOT NULL DEFAULT FALSE, " +
                "`is_active` BOOLEAN NOT NULL DEFAULT TRUE, " +
                "`added_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "`updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "INDEX `guild_id`(`guild_id`));");

        database.execute("CREATE TABLE IF NOT EXISTS `guild_dj_roles`(" +
                "`guild_id` BIGINT NOT NULL PRIMARY KEY, " +
                "`role_id` BIGINT NOT NULL, " +
                "`added_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "INDEX `guild_id`(`guild_id`));");

        database.execute("CREATE TABLE IF NOT EXISTS `guild_playlists`(" +
                "`guild_id` BIGINT NOT NULL PRIMARY KEY, " +
                "`playlist_id` VARCHAR(36) NOT NULL UNIQUE, " +
                "`playlist_name` VARCHAR(10) NOT NULL, " +
                "`track_name` VARCHAR(250) NOT NULL, " +
                "`track_url` VARCHAR(500) NOT NULL, " +
                "`member` VARCHAR(37) CHARACTER SET utf8mb4 NOT NULL, " +
                "`added_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "INDEX `guild_id`(`guild_id`));");

        database.execute("CREATE TABLE IF NOT EXISTS `guild_track_history`(" +
                "`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                "`guild_id` BIGINT NOT NULL, " +
                "`track_name` VARCHAR(250) NOT NULL, " +
                "`track_author` VARCHAR(500) NOT NULL, " +
                "`track_url` VARCHAR(500) NOT NULL, " +
                "`member` VARCHAR(37) CHARACTER SET utf8mb4 NOT NULL, " +
                "`added_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "INDEX `guild_id`(`guild_id`));");

        database.execute("CREATE TABLE IF NOT EXISTS `guild_queues`(" +
                "`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                "`guild_id` BIGINT NOT NULL, " +
                "`track_url` VARCHAR(500) NOT NULL, " +
                "`member` VARCHAR(37) CHARACTER SET utf8mb4 NOT NULL, " +
                "index `guild_id`(`guild_id`));");

        database.execute("CREATE TABLE IF NOT EXISTS `guild_queue_channel_data`(" +
                "`guild_id` BIGINT NOT NULL PRIMARY KEY," +
                "`text_channel_id` BIGINT NOT NULL," +
                "`voice_channel_id` BIGINT NOT NULL," +
                "INDEX `guild_id`(`guild_id`));");

        setPrefixGenerator((event) -> GuildSettingsCache.get().getEntity(event.getGuild().getIdLong()).getPrefix());

        getLogger().info("Registering commands...");

        final CommandRegistry commandRegistry = this.getCommandRegistry();

        commandRegistry.register(Command.builder()
                .names("clear")
                .description("Clear the music queue")
                .executor(new CommandClear())
                .build());

        commandRegistry.register(Command.builder()
                .names("loop")
                .description("Loop the music queue")
                .executor(new CommandLoop())
                .build());

        commandRegistry.register(Command.builder()
                .names("pause", "resume")
                .description("Pause/resume the playing song.")
                .executor(new CommandPause())
                .build());

        commandRegistry.register(Command.builder()
                .names("play")
                .description("Play a song either from a URL or select from a list of returned results.")
                .usage("<track URL / search query>")
                .botPermissions(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK)
                .executor(new CommandPlay())
                .build());

        commandRegistry.register(Command.builder()
                .names("playing", "np")
                .description("Show the currently playing song")
                .executor(new CommandPlaying())
                .build());

        commandRegistry.register(Command.builder()
                .names("queue")
                .description("List all songs in the queue")
                .usage("[page]")
                .executor(new CommandQueue())
                .build());

        commandRegistry.register(Command.builder()
                .names("remove")
                .description("Remove a song requested by a user / track ID from the queue")
                .usage("<trackId / @user>")
                .executor(new CommandRemove())
                .build());

        commandRegistry.register(Command.builder()
                .names("repeat")
                .description("Repeat the currently playing song")
                .executor(new CommandRepeat())
                .build());

        commandRegistry.register(Command.builder()
                .names("restart")
                .description("Restart the currently playing song")
                .executor(new CommandRestart())
                .build());

        commandRegistry.register(Command.builder()
                .names("seek", "goto")
                .description("Seek to a time in a song")
                .usage("<time (e.g. 5m3s = 5 minutes and 3 seconds)>")
                .executor(new CommandSeek())
                .build());

        commandRegistry.register(Command.builder()
                .names("shuffle")
                .description("Shuffle the music queue")
                .executor(new CommandShuffle())
                .build());

        commandRegistry.register(Command.builder()
                .names("skip")
                .description("Skip the currently playing song")
                .executor(new CommandSkip())
                .build());

        commandRegistry.register(Command.builder()
                .names("stop")
                .description("Stop playing and leave the voice channel emptying the song queue")
                .executor(new CommandStop())
                .build());

        commandRegistry.register(Command.builder()
                .names("volume")
                .description("Set the volume for music")
                .usage("<0-150> (higher than 100 is painful)")
                .executor(new CommandVolume())
                .build());

        commandRegistry.register(Command.builder()
                .names("history", "trackhistory")
                .description("View a list of recently played tracks")
                .usage("[page]")
                .executor(new CommandHistory())
                .build());

        commandRegistry.register(Command.builder()
                .names("prefix", "setprefix")
                .description("Set a new prefix to listen to")
                .usage("<newPrefix>")
                .userPermissions(Permission.ADMINISTRATOR)
                .executor(new CommandPrefix())
                .build());

        commandRegistry.register(Command.builder()
                .names("stats")
                .description("View bot stats")
                .executor(new CommandStats())
                .build());

        getLogger().info("Registering events...");

        final EventRegistry eventRegistry = this.getEventRegistry();
        eventRegistry.registerEvent(new GuildMessageReactionListener());
        eventRegistry.registerEvent(new GuildVoiceListener());
        eventRegistry.registerEvent(new GuildJoinLeaveListener());

    }

    @Override
    public void onDisable() {
        if (this.loader != null) {
            this.loader.shutdown(true);
        }
    }

    public AudioLoader getAudioLoader() {
        return this.loader;
    }

}

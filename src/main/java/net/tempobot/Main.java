package net.tempobot;

import com.moandjiezana.toml.Toml;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerRegistry;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.lava.extensions.youtuberotator.YoutubeIpRotatorSetup;
import com.sedmelluq.lava.extensions.youtuberotator.planner.AbstractRoutePlanner;
import com.sedmelluq.lava.extensions.youtuberotator.planner.RotatingNanoIpRoutePlanner;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv6Block;
import com.sheepybot.api.entities.command.Command;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.database.Database;
import com.sheepybot.api.entities.messaging.Messaging;
import com.sheepybot.api.entities.module.CommandRegistry;
import com.sheepybot.api.entities.module.EventRegistry;
import com.sheepybot.api.entities.module.Module;
import com.sheepybot.api.entities.module.ModuleData;
import net.dv8tion.jda.api.Permission;
import net.tempobot.cache.GuildSettingsCache;
import net.tempobot.guild.GuildSettings;
import net.tempobot.music.audio.AudioLoader;
import net.tempobot.music.commands.*;
import net.tempobot.music.event.GuildJoinLeaveListener;
import net.tempobot.music.event.GuildMessageReactionListener;
import net.tempobot.music.event.GuildVoiceListener;
import net.tempobot.music.handler.MusicCommandHandler;
import net.tempobot.music.source.spotify.SpotifyAudioSourceManager;

import java.util.Collections;
import java.util.function.Function;

@ModuleData(name = "Music", version = "1.11.16")
public class Main extends Module {

    public static final int MESSAGE_DELETE_TIME = 15;

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

        if (this.getConfig().getBoolean("use-ipv6-rotation", false)) {
            getLogger().info("Configuring ipv6 rotation using configured ipv6 block...");
            final AbstractRoutePlanner planner = new RotatingNanoIpRoutePlanner(Collections.singletonList(new Ipv6Block(this.getConfig().getString("ipv6-block"))));
            new YoutubeIpRotatorSetup(planner).forSource(youtubeAudioSourceManager).setup();
        }

        getLogger().info("Registering audio sources...");

        final Toml sourceManagers = this.getConfig().getTable("source-managers");
        if (sourceManagers != null) { //if someone is trying to make errors happen intentionally then well, guess no audio for you

            if (sourceManagers.getBoolean("youtube", false))
                audioPlayerManager.registerSourceManager(youtubeAudioSourceManager);
            if (sourceManagers.getBoolean("soundcloud", false))
                audioPlayerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
            if (sourceManagers.getBoolean("bandcamp", false))
                audioPlayerManager.registerSourceManager(new BandcampAudioSourceManager());
            if (sourceManagers.getBoolean("vimeo", false))
                audioPlayerManager.registerSourceManager(new VimeoAudioSourceManager());
            if (sourceManagers.getBoolean("twitch", false))
                audioPlayerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
            if (sourceManagers.getBoolean("beam", false))
                audioPlayerManager.registerSourceManager(new BeamAudioSourceManager());

            if (sourceManagers.getBoolean("spotify", false)) {
                audioPlayerManager.registerSourceManager(new SpotifyAudioSourceManager(youtubeAudioSourceManager,
                        this.getConfig().getString("spotify.client-id"), this.getConfig().getString("spotify.client-secret")));
            }

            if (sourceManagers.getBoolean("local", false))
                audioPlayerManager.registerSourceManager(new LocalAudioSourceManager());
            if (sourceManagers.getBoolean("http", false))
                audioPlayerManager.registerSourceManager(new HttpAudioSourceManager(MediaContainerRegistry.DEFAULT_REGISTRY));

        }

        getLogger().info("Configuring audio player manager...");

        audioPlayerManager.getConfiguration().setFilterHotSwapEnabled(true);
        audioPlayerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.HIGH);

        getLogger().info("Starting up audio loader...");

        this.loader = new AudioLoader(audioPlayerManager);

        getLogger().info("Creating required tables...");

        final Database database = this.getDatabase();

        if (database == null) {
            this.getLogger().error("Cannot operate without a database.");
            return;
        }

        database.execute("CREATE TABLE IF NOT EXISTS `guilds`(" +
                "`guild_id` BIGINT NOT NULL PRIMARY KEY, " +
                "`guild_prefix` VARCHAR(5) CHARACTER SET utf8mb4 NOT NULL, " +
                "`guild_premium` BOOLEAN NOT NULL DEFAULT FALSE, " +
                "`checking_duplicates` BOOLEAN NOT NULL DEFAULT FALSE, " +
                "`auto_announce` BOOLEAN NOT NULL DEFAULT TRUE, " +
                "`auto_delete` BOOLEAN NOT NULL DEFAULT FALSE, " +
                "`auto_search` BOOLEAN NOT NULL DEFAULT FALSE, " +
                "`auto_join` BOOLEAN NOT NULL DEFAULT FALSE, " +
                "`volume` SMALLINT(3) NOT NULL DEFAULT 80, " +
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
                "`guild_id` BIGINT NOT NULL, " +
                "`playlist_id` INT NULL AUTO_INCREMENT PRIMARY KEY, " +
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
                "`added_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "INDEX `guild_id`(`guild_id`));");

        database.execute("CREATE TABLE IF NOT EXISTS `guild_queue_channel_data`(" +
                "`guild_id` BIGINT NOT NULL PRIMARY KEY," +
                "`text_channel_id` BIGINT NOT NULL," +
                "`voice_channel_id` BIGINT NOT NULL," +
                "`added_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "INDEX `guild_id`(`guild_id`));");

        database.execute("CREATE TABLE IF NOT EXISTS `guild_blocked_text_channels`(" +
                "`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                "`guild_id` BIGINT NOT NULL, " +
                "`channel_id` BIGINT NOT NULL, " +
                "`added_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "INDEX `guild_id`(`guild_id`))");

        setPrefixGenerator((guild) -> GuildSettingsCache.get().getEntity(guild.getIdLong()).getPrefix());

        getLogger().info("Registering commands...");

        final CommandRegistry commandRegistry = this.getCommandRegistry();

        commandRegistry.setCommandHandler(new MusicCommandHandler());

        final Function<CommandContext, Boolean> preExecutor = context -> {
            final GuildSettings settings = GuildSettingsCache.get().getEntity(context.getGuild().getIdLong());
            if (settings.isAutoDelete() && context.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) {
                context.getMessage().delete().queue(null, __ -> {});
            }
            return true;
        };

        commandRegistry.register(Command.builder(this)
                .names("help", "support", "invite")
                .description("Retrieve the help page.")
                .executor(new CommandHelp())
                .preExecutor(preExecutor)
                .build());

        commandRegistry.register(Command.builder(this)
                .names("clear")
                .description("Clear the music queue.")
                .executor(new CommandClear())
                .preExecutor(preExecutor)
                .build());

        commandRegistry.register(Command.builder(this)
                .names("loop")
                .description("Loop the music queue.")
                .executor(new CommandLoop())
                .preExecutor(preExecutor)
                .build());

        commandRegistry.register(Command.builder(this)
                .names("pause", "resume")
                .description("Pause/resume the playing song.")
                .executor(new CommandPause())
                .preExecutor(preExecutor)
                .build());

        commandRegistry.register(Command.builder(this)
                .names("play", "p")
                .description("Play a song either from a URL or select from a list of returned results.")
                .usage("<track URL / search query>")
                .botPermissions(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK)
                .executor(new CommandPlay())
                .preExecutor(preExecutor)
                .build());

        commandRegistry.register(Command.builder(this)
                .names("playing", "np")
                .description("Show the currently playing song.")
                .executor(new CommandPlaying())
                .preExecutor(preExecutor)
                .build());

        commandRegistry.register(Command.builder(this)
                .names("queue", "q")
                .description("List all songs in the queue.")
                .usage("[page]")
                .executor(new CommandQueue())
                .preExecutor(preExecutor)
                .build());

        commandRegistry.register(Command.builder(this)
                .names("remove")
                .description("Remove a song requested by a user / track ID from the queue.")
                .usage("<trackId / @user>")
                .executor(new CommandRemove())
                .preExecutor(preExecutor)
                .build());

        commandRegistry.register(Command.builder(this)
                .names("repeat")
                .description("Repeat the currently playing song.")
                .executor(new CommandRepeat())
                .preExecutor(preExecutor)
                .build());

        commandRegistry.register(Command.builder(this)
                .names("restart")
                .description("Restart the currently playing song.")
                .executor(new CommandRestart())
                .preExecutor(preExecutor)
                .build());

        commandRegistry.register(Command.builder(this)
                .names("seek", "goto")
                .description("Seek to a time in a song.")
                .usage("<time (e.g. 5m3s = 5 minutes and 3 seconds)>")
                .executor(new CommandSeek())
                .preExecutor(preExecutor)
                .build());

        commandRegistry.register(Command.builder(this)
                .names("shuffle")
                .description("Shuffle the music queue.")
                .executor(new CommandShuffle())
                .preExecutor(preExecutor)
                .build());

        commandRegistry.register(Command.builder(this)
                .names("skip")
                .description("Skip the currently playing song.")
                .executor(new CommandSkip())
                .preExecutor(preExecutor)
                .build());

        commandRegistry.register(Command.builder(this)
                .names("stop")
                .description("Stop playing and leave the voice channel emptying the song queue.")
                .executor(new CommandStop())
                .preExecutor(preExecutor)
                .build());

        commandRegistry.register(Command.builder(this)
                .names("volume")
                .description("Set the volume for music.")
                .usage("<0-150> (higher than 100 is painful)")
                .executor(new CommandVolume())
                .preExecutor(preExecutor)
                .build());

        commandRegistry.register(Command.builder(this)
                .names("history", "trackhistory")
                .description("View a list of recently played tracks.")
                .usage("[page]")
                .executor(new CommandHistory())
                .preExecutor(preExecutor)
                .build());

        commandRegistry.register(Command.builder(this)
                .names("prefix", "setprefix")
                .description("Set a new prefix to listen to.")
                .usage("<newPrefix>")
                .userPermissions(Permission.MANAGE_SERVER)
                .executor(new CommandPrefix())
                .build());

        commandRegistry.register(Command.builder(this)
                .names("settings", "config")
                .description("Change the settings Tempo operates with.")
                .usage("<setting> <value>")
                .userPermissions(Permission.MANAGE_SERVER)
                .executor(new CommandSettings())
                .preExecutor(preExecutor)
                .build());

        commandRegistry.register(Command.builder(this)
                .names("stats")
                .description("View the bots stats.")
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

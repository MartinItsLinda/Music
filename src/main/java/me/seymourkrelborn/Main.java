package me.seymourkrelborn;

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
import com.sheepybot.api.entities.command.Command;
import com.sheepybot.api.entities.module.CommandRegistry;
import com.sheepybot.api.entities.module.EventRegistry;
import com.sheepybot.api.entities.module.Module;
import com.sheepybot.api.entities.module.ModuleData;
import me.seymourkrelborn.music.audio.AudioLoader;
import me.seymourkrelborn.music.commands.*;
import me.seymourkrelborn.music.event.GuildMessageReactionListener;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;

@ModuleData(name = "Music", version = "1.0")
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
    public void onEnable(final ShardManager shardManager) {

        Main.instance = this;

        final AudioPlayerManager audioPlayerManager = new DefaultAudioPlayerManager();

        final YoutubeAudioSourceManager youtubeAudioSourceManager = new YoutubeAudioSourceManager();

        audioPlayerManager.registerSourceManager(youtubeAudioSourceManager);
        audioPlayerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
//        audioPlayerManager.registerSourceManager(new SpotifyAudioSourceManager(new YoutubeAudioSourceManager(),
//                "", ""));
        audioPlayerManager.registerSourceManager(new BandcampAudioSourceManager());
        audioPlayerManager.registerSourceManager(new VimeoAudioSourceManager());
        audioPlayerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        audioPlayerManager.registerSourceManager(new BeamAudioSourceManager());
        audioPlayerManager.registerSourceManager(new HttpAudioSourceManager());
        audioPlayerManager.registerSourceManager(new LocalAudioSourceManager());

        audioPlayerManager.getConfiguration().setFilterHotSwapEnabled(true);
        audioPlayerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.HIGH);

        this.loader = new AudioLoader(audioPlayerManager);

        final CommandRegistry commandRegistry = this.getCommandRegistry();

        commandRegistry.register(Command.builder().names("clear").description("Clear the command queue").usage("clear").executor(new CommandClear()).build());
        commandRegistry.register(Command.builder().names("loop").description("Loop the music queue").usage("loop").executor(new CommandLoop()).build());
        commandRegistry.register(Command.builder().names("pause", "resume").description("Pause/resume the playing song.").usage("pause").executor(new CommandPause()).build());
        commandRegistry.register(Command.builder().names("play").description("Play a song either from a URL or select from a list of returned results.").usage("play <track URL / search query>").executor(new CommandPlay()).build());
        commandRegistry.register(Command.builder().names("playing", "np").description("Show the currently playing song").usage("playing").executor(new CommandPlaying()).build());
        commandRegistry.register(Command.builder().names("queue").description("List all songs in the queue").usage("queue").executor(new CommandQueue()).build());
        commandRegistry.register(Command.builder().names("remove").description("Remove a song requested by a user / track ID from the queue").usage("remove <trackId / @user>").executor(new CommandRemove()).build());
        commandRegistry.register(Command.builder().names("repeat").description("Repeat the currently playing song").usage("repeat").executor(new CommandRepeat()).build());
        commandRegistry.register(Command.builder().names("restart").description("Restart the currently playing song").usage("restart").executor(new CommandRestart()).build());
        commandRegistry.register(Command.builder().names("seek", "goto").description("Seek to a time in a song").usage("seek <time (e.g. 5m0s)>").executor(new CommandSeek()).build());
        commandRegistry.register(Command.builder().names("shuffle").description("Shuffle the music queue").usage("shuffle").executor(new CommandShuffle()).build());
        commandRegistry.register(Command.builder().names("skip").description("Skip the currently playing song").usage("skip").executor(new CommandSkip()).build());
        commandRegistry.register(Command.builder().names("stop").description("Stop playing and leave the voice channel emptying the song queue").usage("stop").executor(new CommandStop()).build());
        commandRegistry.register(Command.builder().names("volume").description("Set the volume for music").usage("volume <0-150 (higher than 100 is painful)").executor(new CommandVolume()).build());

        final EventRegistry eventRegistry = this.getEventRegistry();
        eventRegistry.registerEvent(new GuildMessageReactionListener());

    }

    public AudioLoader getAudioLoader() {
        return this.loader;
    }
}

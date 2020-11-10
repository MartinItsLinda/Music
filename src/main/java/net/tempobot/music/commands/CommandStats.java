package net.tempobot.music.commands;

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import com.sheepybot.Bot;
import com.sheepybot.BotInfo;
import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import com.sheepybot.api.entities.messaging.Messaging;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.tempobot.Main;
import net.tempobot.music.audio.AudioLoader;
import net.tempobot.music.util.AudioUtils;
import net.tempobot.util.BotUtils;

import java.awt.*;
import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

public class CommandStats implements CommandExecutor {

    @Override
    public void execute(final CommandContext context,
                        final Arguments args) {

        if (context.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) context.getMessage().delete().queue();

        final AudioLoader loader = Main.get().getAudioLoader();

        final EmbedBuilder builder = Messaging.getLocalEmbedBuilder();
        builder.setTitle("Tempo Stats");
        builder.setColor(Color.MAGENTA);

        builder.addField("Guilds", Integer.toString(BotUtils.getGuildCount()), true);
        builder.addField("Guilds Active", Integer.toString(loader.getControllers().size()), true);
        builder.addField("Guilds Listening Now", Integer.toString(loader.getActiveControllers().size()), true);

        builder.addField("Total Tracks Played", Integer.toString(BotUtils.getTotalTracksPlayed()), true);
        builder.addField("Total Tracks Played Here", Integer.toString(BotUtils.getTotalTracksPlayed(context.getGuild().getIdLong())), true);

        builder.addField("Current Shard ID", context.getJDA().getShardInfo().getShardId() + " / " + context.getJDA().getShardInfo().getShardTotal(), true);

        final long used = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) >> 20;
        final long max = Runtime.getRuntime().maxMemory() >> 20;

        builder.addField("Memory", used + "MB / " + max + "MB", true);
        builder.addField("Threads", Integer.toString(ManagementFactory.getThreadMXBean().getThreadCount()), true);

        builder.addField("Lava Player Version", PlayerLibrary.VERSION, true);
        builder.addField("Bot Version", BotInfo.VERSION, true);
        builder.addField("Music Version", Main.get().getData().version(), true);

        builder.addField("Bot Uptime", AudioUtils.formatTrackLength((System.currentTimeMillis() - Bot.get().getStartTime()), false), true);

        context.message(builder.build()).deleteAfter(15, TimeUnit.SECONDS).send();

    }

}

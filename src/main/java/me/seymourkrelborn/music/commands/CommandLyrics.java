package me.seymourkrelborn.music.commands;

import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;

public class CommandLyrics implements CommandExecutor {

    @Override
    public void execute(CommandContext context, Arguments args) {

    }

//    @Override
//    public void execute(Guild server, Message message, User user, String[] args) {
//
//        final AudioController controller = Bot.getAudioLoader().getController(server);
//        final AudioTrack track = controller == null ? null : controller.getTrackScheduler().getPlayer().getPlayingTrack();
//        if (track == null) {
//            message.getTextChannel().sendMessage("There's no music currently playing.").queue();
//        } else {
//
//            try {
//
//                final JSONObject obj = BotUtils.getFromKsoft(String.format("lyrics/search?limit=1&q=%s", URLEncoder.encode(track.getInfo().title, StandardCharsets.UTF_8.toString())), null);
//                if (obj == null) {
//                    message.getTextChannel().sendMessage("I'm sorry but I ran into an error trying to do that for you " + Emoji.SHEEPY_SAD).queue();
//                } else {
//
//                    final JSONObject data = obj.getJSONArray("data").getJSONObject(0);
//                    final String lyrics = data.getString("lyrics");
//                    if (lyrics.startsWith("Unfortunately, we are not licensed to display the full lyrics for this song")) {
//                        message.getTextChannel().sendMessage("I'm sorry but we can't display lyrics for that song " + Emoji.SHEEPY_SAD).queue();
//                    } else {
//
//                        final EmbedBuilder builder = Messaging.getLocalEmbedBuilder();
//
//                        builder.setColor(Colors.DARK_BLUE);
//
//                        for (final String lyricPage : BotUtils.splitMessage(lyrics)) {
//                            builder.setTitle(String.format("%s - %s", data.getString("artist"), data.getString("name")));
//                            builder.setDescription(lyricPage);
//                            message.getTextChannel().sendMessage(builder.build()).queue();
//                        }
//
//                    }
//
//                }
//
//            } catch (UnsupportedEncodingException ignored) {
//            }
//
//        }
//
//    }

}

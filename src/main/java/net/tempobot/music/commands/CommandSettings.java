package net.tempobot.music.commands;

import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import com.sheepybot.api.entities.command.argument.RawArguments;
import com.sheepybot.api.entities.command.parsers.ArgumentParsers;
import com.sheepybot.api.entities.database.Database;
import com.sheepybot.api.entities.database.object.DBObject;
import com.sheepybot.api.entities.messaging.Messaging;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.tempobot.Main;
import net.tempobot.cache.GuildSettingsCache;
import net.tempobot.guild.GuildSettings;
import net.tempobot.music.parsers.CustomParsers;
import net.tempobot.music.util.MessageUtils;

import java.awt.*;
import java.util.concurrent.TimeUnit;

public class CommandSettings implements CommandExecutor {

    @Override
    public void execute(final CommandContext context,
                        final Arguments args) {

        final GuildSettings settings = GuildSettingsCache.get().getEntity(context.getGuild().getIdLong());

        if (args.isDry()) {

            final EmbedBuilder builder = Messaging.getLocalEmbedBuilder();
            builder.setColor(Color.MAGENTA);
            builder.setTitle("Settings Menu");

            builder.setDescription("Here you can change any of the settings for Tempo to make it suit you.\n" +
                    "To modify a setting type {{prefix}}settings <setting> <value>!\n\n:star: Means that a setting is only available to premium guilds.\n_ _");
            builder.addField("prefix ({{prefix}})", "Change the prefix Tempo listens too.", true);
            builder.addField("blacklist", "Add or remove a channel from the blacklist.", true);
            builder.addField("volume ({{volume}})", "Set a preset volume for Tempo to start with.", true);
            builder.addField("dj", "Assign or remove a Role from being a DJ", true);
            builder.addField("autoannounce ({{autoannounce}})", "Enable / disable automatic music announcements.", true);
            builder.addField("autodelete ({{autodelete}})", "Enable / disable automatic message cleanup.", true);
            builder.addField("autosearch ({{autosearch}})", "Enable / disable auto searching.", true);
//            builder.addField(":star: maxsongs", "Set a max number of songs a member can queue.", true);
//            builder.addField(":star: preventduplicates", "Prevent duplicate songs.", true);

            context.replace("{{prefix}}", settings.getPrefix());
            context.replace("{{volume}}", settings.getVolume());
            context.replace("{{autoannounce}}", settings.isAutoAnnounce());
            context.replace("{{autodelete}}", settings.isAutoDelete());
            context.replace("{{autosearch}}", settings.isAutoSearch());

            MessageUtils.sendMessage(context.getGuild(), context.message(builder.build()));

        } else {

            final Database database = Main.get().getDatabase();
            final RawArguments raw = args.getRawArguments();
            final String op = args.next(ArgumentParsers.STRING);

            switch (op.toLowerCase()) {
                case "prefix":

                    if (raw.peek() == null) {
                        MessageUtils.sendMessage(context.getGuild(), context.message("You have to specify a prefix to change to. :confused:"));
                    } else {

                        final String newPrefix = args.next(ArgumentParsers.STRING);
                        if (settings.getPrefix().equals(newPrefix)) {
                            MessageUtils.sendMessage(context.getGuild(), context.message("You already have that as your prefix. :confused:"));
                        } else {

                            final DBObject object = database.execute("UPDATE `guilds` SET `guild_prefix` = ? WHERE `guild_id` = ?", newPrefix, context.getGuild().getIdLong());
                            if (object != null && object.getBoolean("success")) {
                                settings.setPrefix(newPrefix);
                                MessageUtils.sendMessage(context.getGuild(), context.message(String.format("Your prefix has been updated to `%s`.", newPrefix)));
                            } else {
                                MessageUtils.sendMessage(context.getGuild(), context.message("Something went wrong and your prefix might not have been updated, if this keeps happening then please let us know."));
                            }

                        }

                    }

                    break;
                case "blacklist":

                    if (raw.peek() == null) {
                        MessageUtils.sendMessage(context.getGuild(), context.message("You have to give me a channel to blacklist, you can either type its name, give its ID or ping it. :confused:"));
                    } else {

                        final TextChannel channel = args.next(CustomParsers.TEXT_CHANNEL);
                        if (settings.getBlockedTextChannels().contains(channel.getIdLong())) {
                            settings.getBlockedTextChannels().remove(channel.getIdLong());
                            final DBObject object = database.execute("DELETE FROM `guild_blocked_text_channels` WHERE `guild_id` = ? AND `channel_id` = ?;", context.getGuild().getIdLong(), channel.getIdLong());
                            if (object != null && object.getBoolean("success")) {
                                MessageUtils.sendMessage(context.getGuild(), context.message(String.format("I've removed %s from the blacklisted channels, you can now use commands there again.", channel.getAsMention())));
                            } else {
                                MessageUtils.sendMessage(context.getGuild(), context.message("Something went wrong and the channel might not have been removed from the blacklist, if this error keeps happening then please contact us."));
                            }
                        } else {
                            settings.getBlockedTextChannels().add(channel.getIdLong());
                            final DBObject object = database.execute("INSERT INTO `guild_blocked_text_channels`(`guild_id`, `channel_id`) VALUES(?, ?);", context.getGuild().getIdLong(), channel.getIdLong());
                            if (object != null && object.getBoolean("success")) {
                                MessageUtils.sendMessage(context.getGuild(), context.message(String.format("I've put %s into the blacklist, you will no longer be able to use commands there.\nIf you want to change this then you will have to run this command again in a different channel.", channel.getAsMention())));
                            } else {
                                MessageUtils.sendMessage(context.getGuild(), context.message("Something went wrong and the channel might not have been added to the blacklist, if this error keeps happening then please contact us."));
                            }
                        }

                    }

                    break;
                case "dj":

                    if (raw.peek() == null) {
                        context.message("You have to give me a role to assign as a DJ, you can either type its name, give its ID or ping it. :confused:");
                    } else {

                        final Role role = args.next(CustomParsers.ROLE);
                        if (settings.getDjRoles().contains(role.getIdLong())) {
                            settings.getDjRoles().remove(role.getIdLong());
                            final DBObject object = database.execute("DELETE FROM `guild_dj_roles` WHERE `guild_id` = ? AND `role_id` = ?;", context.getGuild().getIdLong(), role.getIdLong());
                            if (object != null && object.getBoolean("success")) {
                                MessageUtils.sendMessage(context.getGuild(), context.message(String.format("I've removed %s from the list of DJ's, they will have access to do anything that is DJ restricted and will **not** be able to forcibly skip songs.", role.getName())));
                            } else {
                                MessageUtils.sendMessage(context.getGuild(), context.message("Something went wrong and the role may not have been removed from the list of DJ's, if this error keeps happening then please contact us."));
                            }
                        } else {
                            settings.getDjRoles().add(role.getIdLong());
                            if (database.execute("INSERT INTO `guild_dj_roles`(`guild_id`, `role_id`) VALUES(?, ?);", context.getGuild().getIdLong(), role.getIdLong()) != null) {
                                MessageUtils.sendMessage(context.getGuild(), context.message(String.format("I've added %s to the list of DJ's, they will now have access to do anything that is DJ restricted and will be able to forcibly skip songs.", role.getName())));
                            } else {
                                MessageUtils.sendMessage(context.getGuild(), context.message("Something went wrong and the role may not have been added to the list of DJ's, if this error keeps happening then please contact us."));
                            }
                        }

                    }

                    break;
                case "autoplay":
                    context.message("Oooo, you've discovered something that's not been implemented yet. Check back later to find out more. :wink:");
                    break;
                case "autoannounce":

                    final boolean autoAnnounce = args.next(ArgumentParsers.alt(CustomParsers.BOOLEAN, !settings.isAutoAnnounce()));

                    if (autoAnnounce != settings.isAutoAnnounce()) {
                        database.execute("UPDATE `guilds` SET `auto_announce` = ? WHERE `guild_id` = ?;", autoAnnounce, context.getGuild().getIdLong());
                        settings.setAutoAnnounce(autoAnnounce);
                    }

                    if (autoAnnounce) {
                        MessageUtils.sendMessage(context.getGuild(), context.message("I will automatically announce music as it plays."));
                    } else {
                        context.replace("{{prefix}}", settings.getPrefix());
                        MessageUtils.sendMessage(context.getGuild(), context.message("I won't announce music as it plays anymore, you can use the `{{prefix}}np` command to view the current song."));
                    }

                    break;
                case "autodelete":

                    final boolean autoDelete = args.next(ArgumentParsers.alt(CustomParsers.BOOLEAN, !settings.isAutoDelete()));

                    if (autoDelete != settings.isAutoDelete()) {
                        database.execute("UPDATE `guilds` SET `auto_delete` = ? WHERE `guild_id` = ?;", autoDelete, context.getGuild().getIdLong());
                        settings.setAutoDelete(autoDelete);
                    }

                    if (autoDelete) {
                        if (context.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE))
                            context.getMessage().delete().queue(null, __ -> {
                            });
                        MessageUtils.sendMessage(context.getGuild(), context.message("I will automatically clean up my messages and commands after a period of time so I don't clutter up your chat.").deleteAfter(10, TimeUnit.SECONDS));
                    } else {
                        MessageUtils.sendMessage(context.getGuild(), context.message("I won't be cleaning up my messages anymore, this might mean that the channel will get cluttered and it's better to have a dedicated channel."));
                    }

                    break;
                case "autosearch":

                    final boolean autoSearch = args.next(ArgumentParsers.alt(CustomParsers.BOOLEAN, !settings.isAutoSearch()));

                    if (autoSearch != settings.isAutoSearch()) {
                        database.execute("UPDATE `guilds` SET `auto_search` = ? WHERE `guild_id` = ?;", autoSearch, context.getGuild().getIdLong());
                        settings.setAutoSearch(autoSearch);
                    }

                    if (autoSearch) {
                        MessageUtils.sendMessage(context.getGuild(), context.message("In the future I will select the first track that gets returned from a search."));
                    } else {
                        MessageUtils.sendMessage(context.getGuild(), context.message("Instead of auto selecting, I will give you an embed that has the first 10 tracks returned from a search."));
                    }

                    break;
                case "volume":

                    if (raw.peek() == null) {
                        MessageUtils.sendMessage(context.getGuild(), context.message("You have to specify a number between 1 and 150. :confused:"));
                    } else {

                        int volume = Math.max(1, Math.min(150, args.next(ArgumentParsers.INTEGER)));

                        settings.setVolume(volume);

                        final DBObject object = database.execute("UPDATE `guilds` SET `volume` = ? WHERE `guild_id` = ?;", volume, context.getGuild().getIdLong());
                        if (object != null && object.getBoolean("success")) {
                            MessageUtils.sendMessage(context.getGuild(), context.message(String.format("Your preset volume has been updated, whenever Tempo joins a voice channel he will start off playing at %d volume.", volume)));
                        } else {
                            MessageUtils.sendMessage(context.getGuild(), context.message("Something went wrong and your preset volume might not have been updated, if this error keeps happening then please contact us."));
                        }

                    }

                    break;
                case "djonly":
                    MessageUtils.sendMessage(context.getGuild(), context.message("Oooo, you've discovered something that's not been implemented yet. Check back later to find out more. :wink:"));
                    break;
                default:
                    context.message(String.format("There is no setting with the name %s. :confused:", op));
                    break;
            }

        }

    }

}

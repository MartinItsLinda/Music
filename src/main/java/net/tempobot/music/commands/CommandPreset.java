package net.tempobot.music.commands;

import com.google.common.collect.Lists;
import com.sheepybot.api.entities.command.Arguments;
import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.CommandExecutor;
import com.sheepybot.api.entities.command.argument.RawArguments;
import com.sheepybot.api.entities.command.parsers.ArgumentParsers;
import com.sheepybot.api.entities.database.Database;
import com.sheepybot.api.entities.database.object.DBObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.tempobot.Main;
import net.tempobot.cache.GuildSettingsCache;
import net.tempobot.guild.GuildSettings;
import net.tempobot.guild.Preset;
import net.tempobot.music.util.MessageUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class CommandPreset implements CommandExecutor {

    @Override
    public void execute(final CommandContext context,
                        final Arguments args) {

        final GuildSettings settings = GuildSettingsCache.get().getEntity(context.getGuild().getIdLong());
        final Database database = Main.get().getDatabase();

        context.replace("{{prefix}}", settings.getPrefix());

        final String op = args.next(ArgumentParsers.STRING);
        switch (op.toLowerCase()) {
            case "add": { //its annoying but i have to do this as variables in a switch/case aren't in a new scope, surrounding them in {} makes them a new scope.

                final String name = args.next(ArgumentParsers.STRING);
                final String command = args.next(ArgumentParsers.STRING);
                final String arguments = args.next(ArgumentParsers.REMAINING_STRING_NO_QUOTE);

                if (settings.getPresets().stream().anyMatch(comparePreset -> comparePreset.getName().equalsIgnoreCase(name))) {
                    MessageUtils.sendMessage(context.getGuild(), context.message(String.format("A preset already exists with the name %s. :confused:", name)));
                } else {
                    final DBObject object = database.execute("INSERT INTO `guild_presets`(`guild_id`, `name`, `command`, `arguments`) VALUES(?, ?, ?, ?);", context.getGuild().getIdLong(), name, command, arguments);
                    if (object != null && object.getBoolean("success")) {

                        final Preset preset = new Preset(object.getInt("GENERATED_KEY"), context.getGuild().getIdLong(), name, command, Lists.newArrayList(arguments.split("\\s+")));
                        settings.getPresets().add(preset);

                        MessageUtils.sendMessage(context.getGuild(), context.message(String.format("Your preset `{{prefix}}%s` was created successfully, you can begin using it straight away! :slight_smile:", name)));

                    } else {
                        MessageUtils.sendMessage(context.getGuild(), context.message("Something went wrong and your preset might not have been created, if this keeps happening then please let us know."));
                    }
                }

                break;
            }
            case "list": {

                final long totalPresets = settings.getPresets().size();
                if (totalPresets == 0) {
                    MessageUtils.sendMessage(context.getGuild(), context.message("You don't have any presets, you can add one at any time and start using it straight away. :slight_smile:"));
                } else {

                    int page = args.next(ArgumentParsers.alt(ArgumentParsers.INTEGER, 0));
                    if (page < 1) page = 1;

                    final List<Preset> presets = settings.getPresets().stream().skip(((page - 1) * 10)).limit(10).collect(Collectors.toList());

                    //noinspection IntegerDivisionInFloatingPointContext
                    final int pageCount = (int) (Math.ceil(totalPresets / 10) + 1);

                    final EmbedBuilder builder = new EmbedBuilder();

                    builder.setTitle(context.getGuild().getName() + "'s Presets");

                    String description;
                    if (totalPresets == 1) {
                        description = "You have 1 available preset!";
                    } else {
                        description = String.format("You have %d available presets!", totalPresets);
                    }

                    builder.setColor(Color.MAGENTA);

                    final StringJoiner joiner = new StringJoiner("\n");
                    joiner.add("_ _");

                    for (final Preset preset : presets) {
                        joiner.add(String.format("`{{prefix}}%s` - %s %s", preset.getName(), preset.getCommand(), this.getArgumentsFromPreset(preset)));
                    }

                    joiner.add("_ _");
                    joiner.add("_ _");

                    builder.addField("Presets", joiner.toString(), true);
                    builder.setDescription(description);
                    builder.setFooter(String.format("Page %d/%d", page, pageCount));

                    MessageUtils.sendMessage(context.getGuild(), context.message(builder.build()));

                }

                break;
            }
            case "remove": {

                final String name = args.next(ArgumentParsers.STRING);

                final Optional<Preset> presetOptional = settings.getPresets().stream().filter(comparePreset -> comparePreset.getName().equalsIgnoreCase(name)).findFirst();
                if (!presetOptional.isPresent()) {
                    MessageUtils.sendMessage(context.getGuild(), context.message(String.format("There isn't a preset with the name %s. :confused:", name)));
                } else {

                    final Preset preset = presetOptional.get();
                    final DBObject object = database.execute("DELETE FROM `guild_presets` WHERE `id` = ? AND `guild_id` = ?;", preset.getPresetId(), preset.getGuildId());
                    if (object != null && object.getBoolean("success")) {
                        settings.getPresets().remove(preset);
                        MessageUtils.sendMessage(context.getGuild(), context.message(String.format("Your preset `{{prefix}}%s` has been deleted successfully. :slight_smile:", name)));
                    } else {
                        MessageUtils.sendMessage(context.getGuild(), context.message("Something went wrong and your preset might not have been deleted, if this keeps happening then please let us know."));
                    }

                }

                break;
            }
            default:
                MessageUtils.sendMessage(context.getGuild(), context.message(String.format("I'm not sure what you mean by %s, did you type it incorrectly?", op)));
                break;
        }

    }

    private String getArgumentsFromPreset(@NotNull("preset cannot be null") final Preset preset) {
        final StringJoiner joiner = new StringJoiner(" ");
        for (final String argument : preset.getArguments()) {
            joiner.add(argument);
        }
        return joiner.toString();
    }

}

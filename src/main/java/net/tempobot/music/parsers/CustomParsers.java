package net.tempobot.music.parsers;

import com.sheepybot.api.entities.command.CommandContext;
import com.sheepybot.api.entities.command.argument.ArgumentParser;
import com.sheepybot.api.entities.command.argument.RawArguments;
import com.sheepybot.api.entities.utils.FinderUtil;
import com.sheepybot.api.exception.parser.ParserException;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.List;

public class CustomParsers {

    public static final ArgumentParser<TextChannel> TEXT_CHANNEL = new ArgumentParser<TextChannel>() {
        @Override
        public TextChannel parse(final CommandContext context,
                                 final RawArguments args) {
            if (args.peek() == null) {
                return null;
            }

            final List<TextChannel> channels = FinderUtil.findTextChannels(args.next(), context.getGuild());

            return channels.isEmpty() ? null : channels.get(0);
        }
    };

    public static final ArgumentParser<Role> ROLE = new ArgumentParser<Role>() {
        @Override
        public Role parse(final CommandContext context,
                          final RawArguments args) {
            final StringBuilder builder = new StringBuilder();

            Role role = null;
            while (args.peek() != null) {

                builder.append(args.next()).append(" ");

                final List<Role> roles = FinderUtil.findRoles(builder.toString().trim(), context.getGuild());
                if (roles.size() == 0) {
                    args.rollback();
                    break;
                }

                role = roles.get(0);

            }

            if (role == null) {
                throw new ParserException("I can't find any role with that name. :confused:");
            }

            return role;
        }
    };

    public static final ArgumentParser<Boolean> BOOLEAN = new ArgumentParser<Boolean>() {
        @Override
        public Boolean parse(final CommandContext context,
                             final RawArguments args) {
            if (args.peek() == null) {
                throw new ParserException("no input to parse");
            }

            final String parse = args.next().toLowerCase();

            return parse.equals("true") || parse.equals("t") || parse.equals("yes") || parse.equals("y") || parse.equals("on") || parse.equals("1");
        }
    };

}

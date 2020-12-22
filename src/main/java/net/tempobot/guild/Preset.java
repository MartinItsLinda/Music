package net.tempobot.guild;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class Preset {

    private final int presetId;
    private final long guildId;
    private final String name;
    private final String command;
    private final List<String> arguments;

    public Preset(final int presetId,
                  final long guildId,
                  @NotNull("name cannot be null") final String name,
                  @NotNull("command cannot be null") final String command,
                  @NotNull("arguments cannot be null") final List<String> arguments) {
        this.presetId = presetId;
        this.guildId = guildId;
        this.name = name;
        this.command = command;
        this.arguments = arguments;
    }

    public int getPresetId() {
        return this.presetId;
    }

    public long getGuildId() {
        return this.guildId;
    }

    public String getName() {
        return this.name;
    }

    public String getCommand() {
        return this.command;
    }

    public List<String> getArguments() {
        return this.arguments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Preset)) return false;
        final Preset preset = (Preset) o;
        return this.presetId == preset.presetId
                && this.guildId == preset.guildId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(presetId, guildId, name, command, arguments);
    }
}

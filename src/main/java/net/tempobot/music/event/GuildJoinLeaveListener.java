package net.tempobot.music.event;

import com.sheepybot.api.entities.database.Database;
import com.sheepybot.api.entities.database.object.DBObject;
import com.sheepybot.api.entities.event.EventHandler;
import com.sheepybot.api.entities.event.EventListener;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.tempobot.Main;
import net.tempobot.guild.GuildSettings;
import net.tempobot.task.QueueLoaderTask;
import org.jetbrains.annotations.NotNull;

public class GuildJoinLeaveListener implements EventListener {

    @EventHandler
    public void onGuildReady(final GuildReadyEvent event) {

        final Guild guild = event.getGuild();

        if (!this.addGuild(guild)) {
            Main.get().getScheduler().runTask(new QueueLoaderTask(guild.getIdLong(), event.getJDA()));
        }

    }

    @EventHandler
    public void onGuildJoin(final GuildJoinEvent event) {
        this.addGuild(event.getGuild());
    }

    @EventHandler
    public void onGuildLeave(final GuildLeaveEvent event) {

        final Guild guild = event.getGuild();
        final Database database = Main.get().getDatabase();

        database.execute("UPDATE `guilds` SET `is_active` = false WHERE `guild_id` = ?", guild.getIdLong());

    }

    //returns true if the guild is a new guild and an entry was created, otherwise it's false
    private boolean addGuild(@NotNull("guild cannot be null") final Guild guild) {

        final Database database = Main.get().getDatabase();

        final DBObject object = database.findOne("SELECT `guild_id` FROM `guilds` WHERE `guild_id` = ?;", guild.getIdLong());
        if (object.isEmpty()) { //if the object is empty then no rows were returned for that query.
            database.execute("INSERT INTO `guilds` (`guild_id`, `guild_prefix`, `guild_premium`) VALUES (?, ?, ?)",
                    guild.getIdLong(), ">", false);
            return true;
        } else {
            database.execute("UPDATE `guilds` SET `is_active` = true WHERE `guild_id` = ?", guild.getIdLong());
            return false;
        }

    }

}

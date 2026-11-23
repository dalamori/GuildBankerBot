package com.dalamori.emberline.guildbanker.discord;

import com.dalamori.emberline.guildbanker.config.BotConfig;
import com.dalamori.emberline.guildbanker.models.dao.BotUserDao;
import com.dalamori.emberline.guildbanker.models.dao.LootDao;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DiscordBotService extends ListenerAdapter {

    private BotConfig config;
    private BotUserDao buDao;
    private LootDao lootDao;
    private JDA jda;

    @Autowired
    public DiscordBotService(BotConfig config, BotUserDao buDao, LootDao lootDao) {
        this.config = config;
        this.buDao = buDao;
        this.lootDao = lootDao;

        this.jda = buildJDA();
    }

    JDA buildJDA() {
        JDA newJDA = JDABuilder
                .createLight(config.getDiscordToken())
                .addEventListeners(this)
                .build();

        CommandListUpdateAction botCommands = newJDA.updateCommands();
        botCommands.addCommands(
                // GOT
                Commands.slash(config.getDiscordSlashCommandPrefix() + "got", "Add new loot to the guild bank")
                        .addOptions(new OptionData(OptionType.NUMBER, "quantity", "How much/many did you find?", true))
                        .addOptions(new OptionData(OptionType.STRING, "loot_name", "What did you find?", true))
                        .addOptions(new OptionData(OptionType.STRING, "reason", "Add a note for the log", false)),

                // USED
                Commands.slash(config.getDiscordSlashCommandPrefix() + "used", "Remove loot from the guild bank")
                        .addOptions(new OptionData(OptionType.NUMBER, "quantity", "How much was used/lost?", true))
                        .addOptions(new OptionData(OptionType.STRING, "loot_name", "What did you use/lose?", true))
                        .addOptions(new OptionData(OptionType.STRING, "reason", "Add a note for the log", false)),

                // USED_ALL
                Commands.slash(config.getDiscordSlashCommandPrefix() + "used_all", "Remove loot from the guild bank")
                        .addOptions(new OptionData(OptionType.STRING, "loot_name", "What did you use/lose all of?", true))
                        .addOptions(new OptionData(OptionType.STRING, "reason", "Add a note for the log", false)),

                // REQUEST
                Commands.slash(config.getDiscordSlashCommandPrefix() + "request", "Wishlist a loot item")
                        .addOptions(new OptionData(OptionType.NUMBER, "quantity", "How much do you need?", true))
                        .addOptions(new OptionData(OptionType.STRING, "loot_name", "What do you need?", true))
                        .addOptions(new OptionData(OptionType.STRING, "reason", "Add a note for the log", false)),

                // TOOK
                Commands.slash(config.getDiscordSlashCommandPrefix() + "took", "transfer a loot item to yourself")
                        .addOptions(new OptionData(OptionType.NUMBER, "quantity", "How much did you take?", true))
                        .addOptions(new OptionData(OptionType.STRING, "loot_name", "What did you take?", true))
                        .addOptions(new OptionData(OptionType.USER, "from_whom", "Whom did you take it from?", true))
                        .addOptions(new OptionData(OptionType.STRING, "reason", "Add a note for the log", false)),

                // TOOK_ALL
                Commands.slash(config.getDiscordSlashCommandPrefix() + "took_all", "transfer a loot item to yourself")
                        .addOptions(new OptionData(OptionType.STRING, "loot_name", "What did you take all of?", true))
                        .addOptions(new OptionData(OptionType.USER, "from_whom", "Whom did you take it all from?", true))
                        .addOptions(new OptionData(OptionType.STRING, "reason", "Add a note for the log", false)),

                // GAVE
                Commands.slash(config.getDiscordSlashCommandPrefix() + "gave", "transfer a loot item to someone else")
                        .addOptions(new OptionData(OptionType.NUMBER, "quantity", "How much did you give?", true))
                        .addOptions(new OptionData(OptionType.STRING, "loot_name", "What did you give?", true))
                        .addOptions(new OptionData(OptionType.USER, "from_whom", "Whom did you give it to?", true))
                        .addOptions(new OptionData(OptionType.STRING, "reason", "Add a note for the log", false)),

                // GAVE_ALL
                Commands.slash(config.getDiscordSlashCommandPrefix() + "gave_all", "transfer a loot item to someone else")
                        .addOptions(new OptionData(OptionType.STRING, "loot_name", "What did you give all of?", true))
                        .addOptions(new OptionData(OptionType.USER, "from_whom", "Whom did you give it all to?", true))
                        .addOptions(new OptionData(OptionType.STRING, "reason", "Add a note for the log", false)),

                // HAVE
                Commands.slash(config.getDiscordSlashCommandPrefix() + "have", "See and edit your own loot totals")
                        .addOptions(new OptionData(OptionType.NUMBER, "quantity", "How much do you have right now?", false))
                        .addOptions(new OptionData(OptionType.STRING, "loot_name", "Which loot?", false))
                        .addOptions(new OptionData(OptionType.STRING, "reason", "Add a note for the log", false)),

                // WANT
                Commands.slash(config.getDiscordSlashCommandPrefix() + "want", "Search the guild bank")
                        .addOptions(new OptionData(OptionType.STRING, "loot_name_starts_with", "What are you looking for?", true)),

                // CANHELP
                Commands.slash(config.getDiscordSlashCommandPrefix() + "can_help", "see what is requested")
                        .addOptions(new OptionData(OptionType.STRING, "loot_name_starts_with", "Is there anything specific you can help with?", false)),

                // REPORT
                Commands.slash(config.getDiscordSlashCommandPrefix() + "report", "Send me a spreadsheet with everything."),

                // ADMIN DROPUSER
                Commands.slash(config.getDiscordSlashCommandPrefix() + "admin_dropuserloot", "Administratively drop a User")
                        .addOptions(new OptionData(OptionType.USER, "drop_whom", "Forget a user exists, forget all their loot", true)),

                // ADMIN SETUSER
                Commands.slash(config.getDiscordSlashCommandPrefix() + "admin_setuserloot", "Administratively edit someone's loot records")
                        .addOptions(new OptionData(OptionType.USER, "for_whom", "Edit whose loot records?", true))
                        .addOptions(new OptionData(OptionType.NUMBER, "quantity", "How much do you have right now?", true))
                        .addOptions(new OptionData(OptionType.STRING, "loot_name", "Which loot?", true))
                        .addOptions(new OptionData(OptionType.STRING, "reason", "Add a note for the log", false)),

                // ADMIN SETHELPER
                Commands.slash(config.getDiscordSlashCommandPrefix() + "admin_sethelpermsg",
                                "Set the message people see when they " + config.getDiscordSlashCommandPrefix() + "canhelp")
                        .addOptions(new OptionData(OptionType.STRING, "new_helper_message", "The new Helper message, keep it short.", true)),

                // OWNER DROPALL
                Commands.slash(config.getDiscordSlashCommandPrefix() + "owner_dropall", "Administratively reset everyone")
                        .addOptions(new OptionData(OptionType.STRING, "confirm", "Write the word 'CONFIRM' in all caps, or it wont work", true)),

                // OWNER SETUSERAUTHLEVEL
                Commands.slash(config.getDiscordSlashCommandPrefix() + "owner_setuserauthlevel", "make someone an owner, an admin, or not")
                        .addOptions(new OptionData(OptionType.USER, "for_whom", "Set whose auth level?", true))
                        .addOptions(new OptionData(OptionType.STRING, "auth_level", "Either 'Owner', 'Admin', or 'User'", true)
                                .addChoice("User", "User")
                                .addChoice("Admin", "Admin")
                                .addChoice("Owner", "Owner")
                        )

        ).queue();
        return newJDA;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getUser().isBot()) {
            event.reply("Sorry, no bots... :(").setEphemeral(true).queue();
            return;
        }

        // strip prefix if exists
        String cmd = (config.getDiscordSlashCommandPrefix().isEmpty()
                ? event.getName()
                : event.getName().replace(config.getDiscordSlashCommandPrefix(), ""));

        switch (cmd) {
            case "got":
            case "used":
            case "used_all":
            case "request":
            case "took":
            case "took_all":
            case "gave":
            case "gave_all":
            case "have":
            case "want":
            case "can_help":
            case "report":
            case "admin_dropuserloot":
            case "admin_setuserloot":
            case "admin_sethelpermsg":
            case "owner_dropall":
            case "owner_setuserauthlevel":
                event.reply("Not yet implemented.").setEphemeral(true).queue();
                return;

            default:
                event.reply("guildbanker bot is leaking logic!  Tell an officer.").queue();
        }
    }
}

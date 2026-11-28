package com.dalamori.emberline.guildbanker.discord;

import com.dalamori.emberline.guildbanker.config.BotConfig;
import com.dalamori.emberline.guildbanker.models.*;
import com.dalamori.emberline.guildbanker.models.dao.BotUserDao;
import com.dalamori.emberline.guildbanker.models.dao.LootAutocompleteDao;
import com.dalamori.emberline.guildbanker.models.dao.LootDao;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DiscordBotService extends ListenerAdapter {

    private BotConfig config;
    private BotUserDao buDao;
    private LootDao lootDao;
    private LootAutocompleteDao lacDao;
    private JDA jda;

    private final Set<BotUserAuthLevel> ADMINS = Set.of(BotUserAuthLevel.OWNER, BotUserAuthLevel.ADMIN);
    private final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd YYYY");

    @Autowired
    public DiscordBotService(BotConfig config, BotUserDao buDao, LootDao lootDao, LootAutocompleteDao lacDao) {
        this.config = config;
        this.buDao = buDao;
        this.lootDao = lootDao;
        this.lacDao = lacDao;

        this.jda = buildJDA();
    }

    private JDA buildJDA() {
        JDA newJDA = JDABuilder
                .createLight(config.getDiscordToken())
                .addEventListeners(this)
                .setMemberCachePolicy(MemberCachePolicy.DEFAULT)
                .build();

        CommandListUpdateAction botCommands = newJDA.updateCommands();
        botCommands.addCommands(
                // GOT
                Commands.slash(config.getDiscordSlashCommandPrefix() + "got", "Add new loot to the guild bank")
                        .addOptions(new OptionData(OptionType.NUMBER, "quantity", "How much/many did you find?", true))
                        .addOptions(new OptionData(OptionType.STRING, "loot_name", "What did you find?", true, true))
                        .addOptions(new OptionData(OptionType.STRING, "reason", "Add a note for the log", false)),

                // USED
                Commands.slash(config.getDiscordSlashCommandPrefix() + "used", "Remove loot from the guild bank")
                        .addOptions(new OptionData(OptionType.NUMBER, "quantity", "How much was used/lost?", true))
                        .addOptions(new OptionData(OptionType.STRING, "loot_name", "What did you use/lose?", true, true))
                        .addOptions(new OptionData(OptionType.STRING, "reason", "Add a note for the log", false)),

                // USED_ALL
                Commands.slash(config.getDiscordSlashCommandPrefix() + "used_all", "Remove loot from the guild bank")
                        .addOptions(new OptionData(OptionType.STRING, "loot_name", "What did you use/lose all of?", true).setAutoComplete(true))
                        .addOptions(new OptionData(OptionType.STRING, "reason", "Add a note for the log", false)),

                // REQUEST
                Commands.slash(config.getDiscordSlashCommandPrefix() + "request", "Wishlist a loot item")
                        .addOptions(new OptionData(OptionType.NUMBER, "quantity", "How much do you need?", true))
                        .addOptions(new OptionData(OptionType.STRING, "loot_name", "What do you need?", true).setAutoComplete(true))
                        .addOptions(new OptionData(OptionType.STRING, "reason", "Add a note for the log", false)),

                // TOOK
                Commands.slash(config.getDiscordSlashCommandPrefix() + "took", "transfer a loot item to yourself")
                        .addOptions(new OptionData(OptionType.NUMBER, "quantity", "How much did you take?", true))
                        .addOptions(new OptionData(OptionType.STRING, "loot_name", "What did you take?", true).setAutoComplete(true))
                        .addOptions(new OptionData(OptionType.USER, "from_whom", "Whom did you take it from?", true))
                        .addOptions(new OptionData(OptionType.STRING, "reason", "Add a note for the log", false)),

                // TOOK_ALL
                Commands.slash(config.getDiscordSlashCommandPrefix() + "took_all", "transfer a loot item to yourself")
                        .addOptions(new OptionData(OptionType.STRING, "loot_name", "What did you take all of?", true).setAutoComplete(true))
                        .addOptions(new OptionData(OptionType.USER, "from_whom", "Whom did you take it all from?", true))
                        .addOptions(new OptionData(OptionType.STRING, "reason", "Add a note for the log", false)),

                // GAVE
                Commands.slash(config.getDiscordSlashCommandPrefix() + "gave", "transfer a loot item to someone else")
                        .addOptions(new OptionData(OptionType.NUMBER, "quantity", "How much did you give?", true))
                        .addOptions(new OptionData(OptionType.STRING, "loot_name", "What did you give?", true).setAutoComplete(true))
                        .addOptions(new OptionData(OptionType.USER, "to_whom", "Whom did you give it to?", true))
                        .addOptions(new OptionData(OptionType.STRING, "reason", "Add a note for the log", false)),

                // GAVE_ALL
                Commands.slash(config.getDiscordSlashCommandPrefix() + "gave_all", "transfer a loot item to someone else")
                        .addOptions(new OptionData(OptionType.STRING, "loot_name", "What did you give all of?", true).setAutoComplete(true))
                        .addOptions(new OptionData(OptionType.USER, "to_whom", "Whom did you give it all to?", true))
                        .addOptions(new OptionData(OptionType.STRING, "reason", "Add a note for the log", false)),

                // HAVE
                Commands.slash(config.getDiscordSlashCommandPrefix() + "have", "See and edit your own loot totals")
                        .addOptions(new OptionData(OptionType.NUMBER, "quantity", "How much do you have right now?", false))
                        .addOptions(new OptionData(OptionType.STRING, "loot_name", "Which loot?", false).setAutoComplete(true))
                        .addOptions(new OptionData(OptionType.STRING, "reason", "Add a note for the log", false)),

                // WANT
                Commands.slash(config.getDiscordSlashCommandPrefix() + "want", "Search the guild bank")
                        .addOptions(new OptionData(OptionType.STRING, "loot_name_starts_with", "What are you looking for?", true).setAutoComplete(true)),

                // CANHELP
                Commands.slash(config.getDiscordSlashCommandPrefix() + "can_help", "see what is requested")
                        .addOptions(new OptionData(OptionType.STRING, "loot_name_starts_with", "Is there anything specific you can help with?", false).setAutoComplete(true)),

                // REPORT
                Commands.slash(config.getDiscordSlashCommandPrefix() + "report", "Send me a spreadsheet with everything."),

                // ADMIN DROPUSER
                Commands.slash(config.getDiscordSlashCommandPrefix() + "admin_dropuserloot", "Administratively drop a User")
                        .addOptions(new OptionData(OptionType.USER, "drop_whom", "Forget a user exists, forget all their loot", true)),

                // ADMIN SETUSER
                Commands.slash(config.getDiscordSlashCommandPrefix() + "admin_setuserloot", "Administratively edit someone's loot records")
                        .addOptions(new OptionData(OptionType.USER, "for_whom", "Edit whose loot records?", true))
                        .addOptions(new OptionData(OptionType.NUMBER, "quantity", "How much do you have right now?", true))
                        .addOptions(new OptionData(OptionType.STRING, "loot_name", "Which loot?", true).setAutoComplete(true))
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

        event.deferReply().queue();

        switch (cmd) {
            case "got":
                got_command(event);
                break;

            case "used":
                used_command(event);
                break;

            case "used_all":
                usedAll_command(event);
                break;

            case "request":
                request_command(event);
                break;

            case "took":
                took_command(event);
                break;

            case "took_all":
                tookAll_command(event);
                break;

            case "gave":
                gave_command(event);
                break;

            case "gave_all":
                gaveAll_command(event);
                break;

            case "have":
                have_command(event);
                break;

            case "want":
                want_command(event);
                break;

            case "can_help":
                canHelp_command(event);
                break;

            case "report":
                report_command(event);
                break;

            case "admin_dropuserloot":
                admin_dropUserLoot_command(event);
                break;

            case "admin_setuserloot":
                admin_setUserLoot_command(event);
                break;

            case "admin_sethelpermsg":
                admin_setHelperMessage_command(event);
                break;

            case "owner_dropall":
                event.getHook().sendMessage("Not yet implemented; just drop the Database.").setEphemeral(true).queue();
                break;

            case "owner_setuserauthlevel":
                owner_setUserAuthLevel_command(event);
                break;

            default:
                event.getHook().sendMessage("guildbanker bot is leaking logic!  Tell an officer.").queue();
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        switch (event.getFocusedOption().getName()) {
            case "loot_name":
            case "loot_name_starts_with":
                List<LootAutocomplete> results = lacDao.findFirst25ByNameStartingWith(event.getFocusedOption().getValue());
                event.replyChoiceStrings(results.stream().map(LootAutocomplete::getName).collect(Collectors.toSet())).queue();

                break;
            default:
                log.warn("unexpected autocomplete event {}", event.getFocusedOption().getName());
                event.replyChoices(new ArrayList<>()).queue();
        }
    }

    public void got_command(SlashCommandInteractionEvent event) {
        Float quantity = null;
        String lootName = null;
        String reason = "";
        try {
            quantity = Float.parseFloat(event.getOption("quantity").getAsString());
            lootName = event.getOption("loot_name").getAsString();
            OptionMapping reasonOption = event.getOption("reason");
            if (Objects.nonNull(reasonOption)) {
                reason = reasonOption.getAsString();
            }
        } catch (NullPointerException e) {
            event.getHook().sendMessage("GOT_COMMAND - Parse error! " + e.getMessage()).setEphemeral(true).queue();
            return;
        }

        BotUser actor = findOrCreateUserFromDiscord(event.getUser(), event.getGuild());

        Optional<Loot> hasLoot = lootDao.findByNameAndHeldBy(lootName, actor.getSnowflake());
        Loot lootItem;
        if (hasLoot.isPresent()) {
            // delta add
            lootItem = hasLoot.get();


            if (lootItem.getQuantity() < 0.0) {
                event.getHook()
                        .sendMessage("OK. This was something you had requested, grats!")
                        .setEphemeral(true).queue();

                lootItem.setQuantity(lootItem.getQuantity() + quantity);
                logLoot(lootItem, LootLogType.CREATED, actor.getSnowflake(), reason);

                if (lootItem.getQuantity() == 0.0) {
                    lootDao.delete(lootItem);
                } else {
                    lootDao.save(lootItem);
                }
            } else {
                lootItem.setQuantity(lootItem.getQuantity() + quantity);

                logLoot(lootItem, LootLogType.CREATED, actor.getSnowflake(), reason);

                lootDao.save(lootItem);

                event.getHook()
                        .sendMessage("OK. You now have " + lootItem.getQuantity() + " " + lootName + " total.")
                        .setEphemeral(true).queue();
            }
        } else {
            lootItem = new Loot();
            lootItem.setName(lootName);
            lootItem.setHeldBy(actor.getSnowflake());
            lootItem.setQuantity(quantity);

            logLoot(lootItem, LootLogType.CREATED, actor.getSnowflake(), reason);

            lootDao.save(lootItem);

            event.getHook()
                    .sendMessage("OK.  You now have " + quantity + " " + lootName)
                    .setEphemeral(true).queue();
        }
    }

    public void used_command(SlashCommandInteractionEvent event) {
        Float quantity = null;
        String lootName = null;
        String reason = "";
        try {
            quantity = Float.parseFloat(event.getOption("quantity").getAsString());
            lootName = event.getOption("loot_name").getAsString();
            OptionMapping reasonOption = event.getOption("reason");
            if (Objects.nonNull(reasonOption)) {
                reason = reasonOption.getAsString();
            }
        } catch (NullPointerException e) {
            event.getHook().sendMessage("USED_COMMAND - Parse error! " + e.getMessage()).setEphemeral(true).queue();
            return;
        }

        BotUser actor = findOrCreateUserFromDiscord(event.getUser(), event.getGuild());

        Optional<Loot> hasLoot = lootDao.findByNameAndHeldBy(lootName, actor.getSnowflake());
        Loot lootItem;
        if (hasLoot.isPresent()) {
            // delta sub
            lootItem = hasLoot.get();

            if (lootItem.getQuantity() < quantity) {
                event.getHook()
                        .sendMessage("You don't have that many!  Are you trying to use " + config.getDiscordSlashCommandPrefix() + "used_all?")
                        .setEphemeral(true).queue();
            } else if (lootItem.getQuantity() + quantity == 0.0) {
                event.getHook()
                        .sendMessage("OK. You use the last of your " + lootName + ".")
                        .setEphemeral(true).queue();

                lootDao.delete(lootItem);
            } else {
                lootItem.setQuantity(lootItem.getQuantity() - quantity);

                logLoot(lootItem, LootLogType.USED, actor.getSnowflake(), reason);

                lootDao.save(lootItem);

                event.getHook()
                        .sendMessage("OK. You now have " + lootItem.getQuantity() + " " + lootName + " left.")
                        .setEphemeral(true).queue();
            }
        } else {
            event.getHook()
                    .sendMessage("You don't have any " + lootName + " to begin with!")
                    .setEphemeral(true).queue();
        }
    }

    public void usedAll_command(SlashCommandInteractionEvent event) {
        String lootName = null;
        String reason = "";
        try {
            lootName = event.getOption("loot_name").getAsString();
            OptionMapping reasonOption = event.getOption("reason");
            if (Objects.nonNull(reasonOption)) {
                reason = reasonOption.getAsString();
            }
        } catch (NullPointerException e) {
            event.getHook().sendMessage("USED_ALL_COMMAND - Parse error! " + e.getMessage()).setEphemeral(true).queue();
            return;
        }

        BotUser actor = findOrCreateUserFromDiscord(event.getUser(), event.getGuild());

        Optional<Loot> hasLoot = lootDao.findByNameAndHeldBy(lootName, actor.getSnowflake());
        Loot lootItem;
        if (hasLoot.isPresent()) {
            // delta sub
            lootItem = hasLoot.get();

            if (lootItem.getQuantity() < 0.0) {
                event.getHook()
                        .sendMessage("OK. You cancel your request for " + lootName + ".")
                        .setEphemeral(true).queue();

                lootDao.delete(lootItem);
            } else {
                event.getHook()
                        .sendMessage("OK. You now have no " + lootName + " left.")
                        .setEphemeral(true).queue();

                lootDao.delete(lootItem);
            }
        } else {
            event.getHook()
                    .sendMessage("You don't have any " + lootName + " to begin with!")
                    .setEphemeral(true).queue();
        }
    }

    public void request_command(SlashCommandInteractionEvent event) {
        Float quantity = null;
        String lootName = null;
        String reason = "";
        try {
            quantity = Float.parseFloat(event.getOption("quantity").getAsString());
            lootName = event.getOption("loot_name").getAsString();
            OptionMapping reasonOption = event.getOption("reason");
            if (Objects.nonNull(reasonOption)) {
                reason = reasonOption.getAsString();
            }
        } catch (NullPointerException e) {
            event.getHook().sendMessage("REQUEST_COMMAND - Parse error! " + e.getMessage()).setEphemeral(true).queue();
            return;
        }

        BotUser actor = findOrCreateUserFromDiscord(event.getUser(), event.getGuild());

        Optional<Loot> hasLoot = lootDao.findByNameAndHeldBy(lootName, actor.getSnowflake());
        Loot lootItem;
        if (hasLoot.isPresent()) {
            // delta sub
            lootItem = hasLoot.get();

            if (lootItem.getQuantity() < quantity) {
                // Subtract and post negative remainder
                lootItem.setQuantity(lootItem.getQuantity() - quantity);
                lootDao.save(lootItem);

                event.getHook()
                        .sendMessage("After subtracting what you already had, you request " + lootItem.getQuantity() + " " + lootName + ".")
                        .setEphemeral(true).queue();
            } else {
                // You have enough
                event.getHook()
                        .sendMessage("You should have this much already on-hand, use " + config.getDiscordSlashCommandPrefix() + "used")
                        .setEphemeral(true).queue();
            }
        } else {
            // new request
            lootItem = new Loot();
            lootItem.setHeldBy(actor.getSnowflake());
            lootItem.setName(lootName);
            lootItem.setQuantity(quantity * -1f);
            lootDao.save(lootItem);

            event.getHook()
                    .sendMessage("You request " + quantity + " " + lootName + ".")
                    .setEphemeral(true).queue();
        }
    }

    public void took_command(SlashCommandInteractionEvent event) {
        Float quantity = null;
        String lootName = null;
        BotUser fromWhom = null;
        String reason = "";
        try {
            quantity = Float.parseFloat(event.getOption("quantity").getAsString());
            lootName = event.getOption("loot_name").getAsString();
            fromWhom = findOrCreateUserFromDiscord(event.getOption("from_whom").getAsUser(), event.getGuild());
            OptionMapping reasonOption = event.getOption("reason");
            if (Objects.nonNull(reasonOption)) {
                reason = reasonOption.getAsString();
            }
        } catch (NullPointerException e) {
            event.getHook().sendMessage("TOOK_COMMAND - Parse error! " + e.getMessage()).setEphemeral(true).queue();
            return;
        }

        Optional<Loot> hasFromLoot = lootDao.findByNameAndHeldBy(lootName, fromWhom.getSnowflake());
        if (hasFromLoot.isEmpty()) {
            event.getHook().sendMessage("They do not have any " + lootName + " to take!").setEphemeral(true).queue();
            return;
        }
        Loot fromLoot = hasFromLoot.get();
        if (fromLoot.getQuantity() < quantity) {
            event.getHook().sendMessage("They do not have that much " + lootName + "to take!").setEphemeral(true).queue();
            return;
        }

        BotUser actor = findOrCreateUserFromDiscord(event.getUser(), event.getGuild());
        Optional<Loot> hasToLoot = lootDao.findByNameAndHeldBy(lootName, actor.getSnowflake());
        Loot toLoot;
        if (hasToLoot.isEmpty()) {
            toLoot = new Loot();
            toLoot.setQuantity(0);
            toLoot.setName(lootName);
            toLoot.setHeldBy(actor.getSnowflake());
        } else {
            toLoot = hasToLoot.get();
        }

        toLoot.setQuantity(toLoot.getQuantity() + quantity);
        fromLoot.setQuantity(fromLoot.getQuantity() - quantity);

        lootDao.save(toLoot);
        lootDao.save(fromLoot);

        event.getHook().sendMessage("You take " + quantity + " " + lootName + ".")
                .setEphemeral(true).queue();
    }

    public void tookAll_command(SlashCommandInteractionEvent event) {
        String lootName = null;
        BotUser fromWhom = null;
        String reason = "";
        try {
            lootName = event.getOption("loot_name").getAsString();
            fromWhom = findOrCreateUserFromDiscord(event.getOption("from_whom").getAsUser(), event.getGuild());
            OptionMapping reasonOption = event.getOption("reason");
            if (Objects.nonNull(reasonOption)) {
                reason = reasonOption.getAsString();
            }
        } catch (NullPointerException e) {
            event.getHook().sendMessage("TOOK_ALL_COMMAND - Parse error! " + e.getMessage()).setEphemeral(true).queue();
            return;
        }

        Optional<Loot> hasFromLoot = lootDao.findByNameAndHeldBy(lootName, fromWhom.getSnowflake());
        if (hasFromLoot.isEmpty()) {
            event.getHook().sendMessage("They do not have any " + lootName + " to take!").setEphemeral(true).queue();
            return;
        }
        Loot fromLoot = hasFromLoot.get();
        Float quantity = fromLoot.getQuantity();

        BotUser actor = findOrCreateUserFromDiscord(event.getUser(), event.getGuild());
        Optional<Loot> hasToLoot = lootDao.findByNameAndHeldBy(lootName, actor.getSnowflake());
        Loot toLoot;
        if (hasToLoot.isEmpty()) {
            toLoot = new Loot();
            toLoot.setQuantity(0);
            toLoot.setName(lootName);
            toLoot.setHeldBy(actor.getSnowflake());
        } else {
            toLoot = hasToLoot.get();
        }

        toLoot.setQuantity(toLoot.getQuantity() + quantity);
        fromLoot.setQuantity(fromLoot.getQuantity() - quantity);

        lootDao.save(toLoot);
        lootDao.save(fromLoot);

        event.getHook().sendMessage("You take " + quantity + " " + lootName + ".")
                .setEphemeral(true).queue();
    }

    public void gave_command(SlashCommandInteractionEvent event) {
        Float quantity = null;
        String lootName = null;
        BotUser toWhom = null;
        String reason = "";
        try {
            quantity = Float.parseFloat(event.getOption("quantity").getAsString());
            lootName = event.getOption("loot_name").getAsString();
            toWhom = findOrCreateUserFromDiscord(event.getOption("to_whom").getAsUser(), event.getGuild());
            OptionMapping reasonOption = event.getOption("reason");
            if (Objects.nonNull(reasonOption)) {
                reason = reasonOption.getAsString();
            }
        } catch (NullPointerException e) {
            event.getHook().sendMessage("GAVE_COMMAND - Parse error! " + e.getMessage()).setEphemeral(true).queue();
            return;
        }

        BotUser actor = findOrCreateUserFromDiscord(event.getUser(), event.getGuild());
        Optional<Loot> hasFromLoot = lootDao.findByNameAndHeldBy(lootName, actor.getSnowflake());
        if (hasFromLoot.isEmpty()) {
            event.getHook().sendMessage("You do not have any " + lootName + " to give!").setEphemeral(true).queue();
            return;
        }
        Loot fromLoot = hasFromLoot.get();
        if (fromLoot.getQuantity() < quantity) {
            event.getHook().sendMessage("You do not have that much " + lootName + "to give!").setEphemeral(true).queue();
            return;
        }

        Optional<Loot> hasToLoot = lootDao.findByNameAndHeldBy(lootName, toWhom.getSnowflake());
        Loot toLoot;
        if (hasToLoot.isEmpty()) {
            toLoot = new Loot();
            toLoot.setQuantity(0);
            toLoot.setName(lootName);
            toLoot.setHeldBy(toWhom.getSnowflake());
        } else {
            toLoot = hasToLoot.get();
        }

        toLoot.setQuantity(toLoot.getQuantity() + quantity);
        fromLoot.setQuantity(fromLoot.getQuantity() - quantity);

        lootDao.save(toLoot);
        lootDao.save(fromLoot);

        event.getHook().sendMessage("You give them " + quantity + " " + lootName + ".")
                .setEphemeral(true).queue();
    }

    public void gaveAll_command(SlashCommandInteractionEvent event) {
        String lootName = null;
        BotUser toWhom = null;
        String reason = "";
        try {
            lootName = event.getOption("loot_name").getAsString();
            toWhom = findOrCreateUserFromDiscord(event.getOption("from_whom").getAsUser(), event.getGuild());
            OptionMapping reasonOption = event.getOption("reason");
            if (Objects.nonNull(reasonOption)) {
                reason = reasonOption.getAsString();
            }
        } catch (NullPointerException e) {
            event.getHook().sendMessage("TOOK_ALL_COMMAND - Parse error! " + e.getMessage()).setEphemeral(true).queue();
            return;
        }

        BotUser actor = findOrCreateUserFromDiscord(event.getUser(), event.getGuild());
        Optional<Loot> hasFromLoot = lootDao.findByNameAndHeldBy(lootName, actor.getSnowflake());
        if (hasFromLoot.isEmpty()) {
            event.getHook().sendMessage("You do not have any " + lootName + " to give!").setEphemeral(true).queue();
            return;
        }
        Loot fromLoot = hasFromLoot.get();
        Float quantity = fromLoot.getQuantity();

        Optional<Loot> hasToLoot = lootDao.findByNameAndHeldBy(lootName, toWhom.getSnowflake());
        Loot toLoot;
        if (hasToLoot.isEmpty()) {
            toLoot = new Loot();
            toLoot.setQuantity(0);
            toLoot.setName(lootName);
            toLoot.setHeldBy(actor.getSnowflake());
        } else {
            toLoot = hasToLoot.get();
        }

        toLoot.setQuantity(toLoot.getQuantity() + quantity);
        fromLoot.setQuantity(fromLoot.getQuantity() - quantity);

        lootDao.save(toLoot);
        lootDao.save(fromLoot);

        event.getHook().sendMessage("You give them " + quantity + " " + lootName + ".")
                .setEphemeral(true).queue();
    }

    private void have_command(SlashCommandInteractionEvent event) {
        BotUser actor = findOrCreateUserFromDiscord(event.getUser(), event.getGuild());

        if (event.getOptions().isEmpty()) {
            // I_HAVE (NO ARGS) Subcommand
            List<Loot> inv = lootDao.findByHeldBy(actor.getSnowflake());

            if (inv.size() >= 5) {
                byte[] csvBytes = getRecordsAsCSVBytes(inv);
                FileUpload csv = FileUpload.fromData(csvBytes, "inventory.csv");

                event.getHook().sendMessage("You have the following items: ")
                        .addFiles(csv)
                        .setEphemeral(true).queue();
            } else if(!inv.isEmpty()) {
                event.getHook().sendMessage("You have the following items:\n" + getRecordsAsShortDescs(inv))
                        .setEphemeral(true).queue();

            } else {
                event.getHook().sendMessage("You're empty.").setEphemeral(true).queue();
            }

        } else {
            // I HAVE (ARGS) Subcommand
            Float quantity = null;
            String lootName = null;
            String reason = "";
            try {
                quantity = Float.parseFloat(event.getOption("quantity").getAsString());
                lootName = event.getOption("loot_name").getAsString();
                OptionMapping reasonOption = event.getOption("reason");
                if (Objects.nonNull(reasonOption)) {
                    reason = reasonOption.getAsString();
                }

            } catch (NullPointerException e) {
                event.getHook().sendMessage("Quantity and Loot Name are both required when updating.")
                        .setEphemeral(true).queue();
                return;
            }

            Optional<Loot> hasLoot = lootDao.findByNameAndHeldBy(lootName, actor.getSnowflake());
            if (hasLoot.isPresent()) {
                Loot lootItem = hasLoot.get();
                Float oldQuantity = lootItem.getQuantity();

                lootItem.setQuantity(quantity);

                logLoot(lootItem, LootLogType.UPDATED, actor.getSnowflake(), reason);

                event.getHook().sendMessage("You update your stock of " + lootName + " from " +
                        oldQuantity.toString() + " to " + quantity + ".")
                        .setEphemeral(true).queue();
            } else {
                // input already validated, ok to chain.
                got_command(event);
            }
        }
    }



    private void want_command(SlashCommandInteractionEvent event) {
        String beginsWith = null;
        try {
            beginsWith = event.getOption("loot_name_begins_with").getAsString();
        } catch (NullPointerException e) {
            event.getHook().sendMessage("WANT_COMMAND - Parse error! " + e.getMessage()).setEphemeral(true).queue();
            return;
        }

        List<Loot> results = lootDao.findByNameStartingWithAndQuantityGreaterThan(beginsWith, 0.0f);
        if (results.isEmpty()) {
            event.getHook().sendMessage("Nobody has anything like that.")
                    .setEphemeral(true).queue();

        } else if (results.size() >= 5) {
            FileUpload fileUpload = FileUpload.fromData(getRecordsAsCSVBytes(results), "search_results.csv");
            event.getHook().sendMessage("The following people have loot like that:\n")
                    .addFiles(fileUpload)
                    .setEphemeral(true).queue();
        } else {
            event.getHook().sendMessage("The following people have loot like that:\n" + getRecordsAsShortDescs(results))
                    .setEphemeral(true).queue();
        }
    }

    private void canHelp_command(SlashCommandInteractionEvent event) {
        String beginsWith = null;
        List<Loot> results = null;

        if (event.getOptions().isEmpty()) {
            // No Args subcommand
            results = lootDao.findByQuantityLessThan(0f);

        } else {
            // Args Subcommand
            try {
                beginsWith = event.getOption("loot_name_starts_with").getAsString();
            } catch (NullPointerException e) {
                event.getHook().sendMessage("CAN_HELP_COMMAND - Parse error! " + e.getMessage()).setEphemeral(true).queue();
                return;
            }

            results = lootDao.findByNameStartingWithAndQuantityLessThan(beginsWith, 0f);
        }

        if (results.isEmpty()) {
            event.getHook().sendMessage(config.getHelperMessage() + "\nNo matching requests found.")
                    .setEphemeral(true).queue();
        } else if (results.size() >= 5) {
            FileUpload fileUpload = FileUpload.fromData(getRecordsAsCSVBytes(results), "request_results.csv");
            event.getHook().sendMessage(config.getHelperMessage() + "\nThe following requests match your search:")
                    .addFiles(fileUpload)
                    .setEphemeral(true).queue();
        } else {
            event.getHook().sendMessage(config.getHelperMessage() + "\nThe following requests match your search:\n"
                            + getRecordsAsShortDescs(results))
                    .setEphemeral(true).queue();
        }
    }

    public void report_command(SlashCommandInteractionEvent event) {
        List<Loot> results = lootDao.findByQuantityGreaterThan(0f);
        if (results.isEmpty()) {
            event.getHook().sendMessage(config.getHelperMessage() + "\nNobody has anything.");
        } else if (results.size() >= 5) {
            FileUpload fileUpload = FileUpload.fromData(getRecordsAsCSVBytes(results), "request_results.csv");
            event.getHook().sendMessage("Here's your report:")
                    .addFiles(fileUpload)
                    .setEphemeral(true).queue();
        } else {
            event.getHook().sendMessage("Here's your report:\n"
                            + getRecordsAsShortDescs(results))
                    .setEphemeral(true).queue();
        }
    }

    public void admin_dropUserLoot_command(SlashCommandInteractionEvent event) {
        BotUser actor = findOrCreateUserFromDiscord(event.getUser(), event.getGuild());
        if (!ADMINS.contains(actor.getAuthLevel())) {
            event.getHook().sendMessage("No.").queue();
            return;
        }

        Optional<BotUser> victim = null;
        try {
            Long victimSnowflake = event.getOption("drop_whom").getAsUser().getIdLong();
            victim = buDao.findBySnowflake(victimSnowflake);
        } catch (NullPointerException e) {
            event.getHook().sendMessage("ADMIN_DROP_USER_LOOT - parse error! - " + e.getMessage())
                    .setEphemeral(true).queue();
            return;
        }

        if (victim.isPresent()) {
            lootDao.deleteByHeldBy(victim.get().getSnowflake());
            event.getHook().sendMessage("OK.   You nuke them from orbit ... it's the only way to be sure")
                    .queue();
        } else {
            event.getHook().sendMessage("Get your glasses checked!  Nobody tracked by that name.")
                    .setEphemeral(true).queue();
        }

    }

    public void admin_setUserLoot_command(SlashCommandInteractionEvent event) {
        BotUser actor = findOrCreateUserFromDiscord(event.getUser(), event.getGuild());
        if (!ADMINS.contains(actor.getAuthLevel())) {
            event.getHook().sendMessage("No.").queue();
            return;
        }

        BotUser forWhom = null;
        Float quantity = null;
        String lootName = null;
        String reason = "";

        try {
            forWhom = findOrCreateUserFromDiscord(event.getOption("for_whom").getAsUser(), event.getGuild());
            quantity = Float.parseFloat(event.getOption("quantity").getAsString());
            lootName = event.getOption("loot_name").getAsString();
            OptionMapping reasonOption = event.getOption("reason");
            if (Objects.nonNull(reasonOption)) {
                reason = reasonOption.getAsString();
            }
        } catch (NullPointerException e) {
            event.getHook().sendMessage("ADMIN_SET_USER_LOOT_COMMAND - parser error! " + e.getMessage())
                    .setEphemeral(true).queue();
            return;
        }

        Optional<Loot> hasLoot = lootDao.findByNameAndHeldBy(lootName, forWhom.getSnowflake());
        if (hasLoot.isPresent()) {
            Loot lootItem = hasLoot.get();
            Float oldQuantity = lootItem.getQuantity();
            lootItem.setQuantity(quantity);

            lootDao.save(lootItem);
            event.getHook().sendMessage("You update their inventory of " + lootName + " from " + oldQuantity.toString() + " to " + quantity.toString() + ".")
                    .setEphemeral(true).queue();
        } else {
            Loot lootItem = new Loot();
            lootItem.setQuantity(quantity);
            lootItem.setHeldBy(forWhom.getSnowflake());
            lootItem.setName(lootName);

            lootDao.save(lootItem);
            event.getHook().sendMessage("You update their inventory of " + lootName + " to " + quantity.toString() + ".")
                    .setEphemeral(true).queue();
        }
    }

    private void admin_setHelperMessage_command(SlashCommandInteractionEvent event) {
        BotUser actor = findOrCreateUserFromDiscord(event.getUser(), event.getGuild());
        if (!ADMINS.contains(actor.getAuthLevel())) {
            event.getHook().sendMessage("No.").queue();
            return;
        }

        String newHelperMsg = null;
        try {
            newHelperMsg = event.getOption("new_helper_message").getAsString();
        } catch (NullPointerException e) {
            event.getHook().sendMessage("ADMIN_SET_HELPER_MESSAGE_COMMAND - parse error! " + e.getMessage())
                    .setEphemeral(true).queue();
            return;
        }

        config.setHelperMessage(newHelperMsg);

        event.getHook().sendMessage("OK! New message set.")
                .setEphemeral(true).queue();

    }

    public void owner_setUserAuthLevel_command(SlashCommandInteractionEvent event) {
        BotUser actor = findOrCreateUserFromDiscord(event.getUser(), event.getGuild());

        if (!config.getDisableAuthProtection() && !Objects.equals(actor.getAuthLevel(), BotUserAuthLevel.OWNER)) {
            event.getHook().sendMessage("No.").queue();
            return;
        }

        BotUser forWhom = null;
        String level = null;

        try {
            forWhom = findOrCreateUserFromDiscord(event.getOption("for_whom").getAsUser(), event.getGuild());
            level = event.getOption("auth_level").getAsString();
        } catch (NullPointerException e) {
            event.getHook().sendMessage("OWNER_SET_USER_AUTH_LEVEL - parse error! " + e.getMessage()).setEphemeral(true).queue();
            return;
        }

        switch (level) {
            case "Owner":
                event.getHook().sendMessage(forWhom.getLastKnownNickname() + " is now an OWNER.").setEphemeral(true).queue();
                forWhom.setAuthLevel(BotUserAuthLevel.OWNER);
                break;

            case "Admin":
                event.getHook().sendMessage(forWhom.getLastKnownNickname() + " is now an ADMIN.").setEphemeral(true).queue();
                forWhom.setAuthLevel(BotUserAuthLevel.ADMIN);
                break;

            default:
                event.getHook().sendMessage(forWhom.getLastKnownNickname() + " is now a USER.").setEphemeral(true).queue();
                forWhom.setAuthLevel(BotUserAuthLevel.USER);
        }
        buDao.save(forWhom);
    }

    private String getRecordsAsShortDescs(List<Loot> lootList) {
        StringBuilder builder = new StringBuilder();
        lootList.forEach(lootItem -> {
            LocalDate date = LocalDate.ofInstant(lootItem.getLastSeen(), ZoneId.of("UTC"));
            builder.append(Float.toString(lootItem.getQuantity()) + " of \"" + lootItem.getName() +
                    "\", last seen on " + date.format(DATE_FORMAT) +
                    " held by " + resolveDiscordUsername(lootItem.getHeldBy()) + ".\n");
        });
        return builder.toString();
    }

    private byte[] getRecordsAsCSVBytes(List<Loot> lootList) {
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("\"Loot Description\", \"Quantity\", \"Held By\", \"Last Seen\"\n");
        lootList.forEach(lootItem -> {
            LocalDate date = LocalDate.ofInstant(lootItem.getLastSeen(), ZoneId.of("UTC"));
            csvBuilder.append(String.join(", ",
                    "\"" + lootItem.getName() + "\"",
                    Float.toString(lootItem.getQuantity()),
                    "\"" + resolveDiscordUsername(lootItem.getHeldBy()) + "\"",
                    date.format(DATE_FORMAT)));

            csvBuilder.append("\n");
        });

        return csvBuilder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String resolveDiscordUsername(Long whomSnowflake) {
        Optional<BotUser> hasUser = buDao.findBySnowflake(whomSnowflake);
        if (hasUser.isPresent()) {
            return hasUser.get().getLastKnownNickname();
        }
        return "Unknown[" + whomSnowflake.toString() + "]";
    }

    private BotUser findOrCreateUserFromDiscord(User discordUser, Guild guildContext) {
        String lastNickname = discordUser.getEffectiveName();

        // Try to find the guild-specific nick
        if (Objects.nonNull(guildContext)) {
            Member guildMember = guildContext.getMemberById(discordUser.getIdLong());
            if (Objects.nonNull(guildMember)) {
                lastNickname = guildMember.getEffectiveName();
            }
        }

        Optional<BotUser> dbUserObj = buDao.findBySnowflake(discordUser.getIdLong());
        if (dbUserObj.isPresent()) {
            BotUser user = dbUserObj.get();

            if (!Objects.equals(user.getLastKnownNickname(), lastNickname)){
                user.setLastKnownNickname(lastNickname);

                buDao.save(user);
            }

            return user;
        } else {

            BotUser newBotUser = new BotUser();
            newBotUser.setLastKnownNickname(lastNickname);
            newBotUser.setSnowflake(discordUser.getIdLong());
            newBotUser.setAuthLevel(BotUserAuthLevel.USER);

            buDao.save(newBotUser);

            return newBotUser;
        }
    }

    private LootLog logLoot(Loot loot, LootLogType type, Long whoSaid, String reason) {
        return null;
        /*
        LootLog logItem = new LootLog();
        logItem.setLoot(loot);
        logItem.setType(type);
        logItem.setWhoSaid(whoSaid);
        logItem.setReason(reason);

        loot.getLootLogs().add(logItem);

        return logItem;
        */
    }
}

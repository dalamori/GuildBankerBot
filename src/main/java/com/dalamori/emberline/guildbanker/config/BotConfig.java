package com.dalamori.emberline.guildbanker.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@Data
@Configuration
@PropertySources({
        @PropertySource(value = "classpath:application.properties"),
        // secret.properties is designed to hold discord API token
        @PropertySource(value = "classpath:secret.properties", ignoreResourceNotFound = true)
})
public class BotConfig {
    @Value("${guildbanker.initialHelperMessage:Helper message not initialized}")
    private String helperMessage;

    @Value("${discord.token:DISCORD_API_TOKEN_NOT_SET_IN_SECRET.PROPERTIES}")
    private String discordToken;

    @Value("${discord.recordReplyThreshold:5}")
    private int discordRecordReplyThreshold;

    @Value("${discord.slashCommandPrefix:i_}")
    private String discordSlashCommandPrefix;
}

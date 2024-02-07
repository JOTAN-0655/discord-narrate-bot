package jotan.discord.bot.narrate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jotan.discord.bot.narrate.discord.ControlLavaplayer;
import jotan.discord.bot.narrate.discord.EventHandler;
import jotan.discord.bot.narrate.managerConfig.ManagerConfig;
import jotan.discord.bot.narrate.managerConfig.ManagerConfig.BotConfigKey;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Main {

	private static JDA jda;
	public static JDA getJDA() {
		return jda;
	}
	public static void setJDA(JDA j) {
		jda = j;
	}

	public static void main(String[] args){

		if(!ManagerConfig.loadConfiguration())
			System.exit(0);

		System.out.println("[CONFIG-CONFIRM]BOT TOKEN : " + ManagerConfig.getConfigValue(BotConfigKey.DISCORD_TOKEN));
		System.out.println("[CONFIG-CONFIRM]AUDIO EXPORT : " + ManagerConfig.getConfigValue(BotConfigKey.NARRATE_EXPORT_PATH));
		System.out.println("[CONFIG-CONFIRM]AUDIO GEN COMMAND : " + ManagerConfig.getConfigValue(BotConfigKey.NARRATE_GENERATE_COMMAND));
		System.out.println("[CONFIG-CONFIRM]DICTIONARY PATH : " + ManagerConfig.getConfigValue(BotConfigKey.DICTIONARY_SAVE_PATH));


		List<GatewayIntent> intents = new ArrayList<GatewayIntent>(Arrays.asList(
				GatewayIntent.GUILD_MEMBERS,
				GatewayIntent.GUILD_MESSAGES,
				GatewayIntent.GUILD_MESSAGE_REACTIONS,
				GatewayIntent.GUILD_VOICE_STATES,
				GatewayIntent.MESSAGE_CONTENT
				));
		JDA discord = null;
		try {
			discord = JDABuilder.createDefault(ManagerConfig.getConfigValue(BotConfigKey.DISCORD_TOKEN),intents).build().awaitReady();
			discord.awaitReady();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("LOGIN FAILED...");
			return;
		}
		setJDA(discord);
		discord.addEventListener(new EventHandler());
		EventHandler.registerCommand(discord);
		ControlLavaplayer.initialize();


	}

}

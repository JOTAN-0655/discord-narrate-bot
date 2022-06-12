package narrate_bot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import narrate_bot.config_manager.Config_Manager;
import narrate_bot.config_manager.Config_Manager.Bot_Config_Keys;
import narrate_bot.discord.Discord_Events;
import narrate_bot.discord.Lavaplayer_Control;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Main {

	private static JDA jda;
	public static JDA get_JDA() {
		return jda;
	}
	public static void set_JDA(JDA j) {
		jda = j;
	}

	public static void main(String[] args){

		if(!Config_Manager.load_Configuration())
			System.exit(0);

		System.out.println("[CONFIG-CONFIRM]BOT TOKEN : " + Config_Manager.get_Bot_Property(Bot_Config_Keys.DISCORD_TOKEN));
		System.out.println("[CONFIG-CONFIRM]AUDIO EXPORT : " + Config_Manager.get_Bot_Property(Bot_Config_Keys.AUDIO_EXPORT_PATH));
		System.out.println("[CONFIG-CONFIRM]AUDIO GEN COMMAND : " + Config_Manager.get_Bot_Property(Bot_Config_Keys.AUDIO_GEN_CMD));
		System.out.println("[CONFIG-CONFIRM]DICTIONARY PATH : " + Config_Manager.get_Bot_Property(Bot_Config_Keys.DICTIONARY_PATH));


		List<GatewayIntent> intents = new ArrayList<GatewayIntent>(Arrays.asList(
				GatewayIntent.GUILD_MEMBERS,
				GatewayIntent.GUILD_MESSAGES,
				GatewayIntent.GUILD_MESSAGE_REACTIONS,
				GatewayIntent.GUILD_VOICE_STATES
				));
		JDA discord = null;
		try {
			discord = JDABuilder.createDefault(Config_Manager.get_Bot_Property(Bot_Config_Keys.DISCORD_TOKEN),intents).build().awaitReady();
			discord.awaitReady();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("LOGIN FAILED...");
			return;
		}
		set_JDA(discord);
		discord.addEventListener(new Discord_Events());

		Lavaplayer_Control.initialize();


	}

}

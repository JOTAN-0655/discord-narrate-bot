package jotan.discord.bot.narrate.discord;

import java.awt.Color;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class MessageUtil {

	public static Color BarColor = new Color(0x3F84E5);
	public static Color ErrorColor = Color.RED;
	public static Color SuccessColor = Color.GREEN;

	public static MessageEmbed sendSuccessMessage(String message) {

		EmbedBuilder eb = new EmbedBuilder();

		eb.setColor(SuccessColor);
		eb.setTitle(":white_check_mark:OK");
		eb.appendDescription(message);

		return eb.build();

	}

	public static MessageEmbed sendErrorMessage(String message) {

		EmbedBuilder eb = new EmbedBuilder();

		eb.setColor(ErrorColor);
		eb.setTitle(":x:ERROR");
		eb.appendDescription(message);

		return eb.build();

	}

	public static MessageEmbed sendInfoMessage(String message) {

		EmbedBuilder eb = new EmbedBuilder();

		eb.setColor(BarColor);
		eb.setTitle(":information_source:INFO");
		eb.appendDescription(message);

		return eb.build();
	}


}

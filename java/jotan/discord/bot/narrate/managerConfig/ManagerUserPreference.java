package jotan.discord.bot.narrate.managerConfig;

import java.io.File;
import java.util.HashMap;
import java.util.Properties;

import jotan.discord.bot.narrate.managerConfig.ManagerConfig.BotConfigKey;
import jotan.discord.bot.narrate.managerConfig.ManagerConfig.Config;

public class ManagerUserPreference {

	private static HashMap<Long,Config> dictionary = new HashMap<Long,Config>();

	public static Config getGuild(Long guild_id) {
		String savePath = ManagerConfig.getConfigValue(BotConfigKey.USER_PREFERENCE_SAVE_PATH);
		if(!new File(savePath).exists()) new File(savePath).mkdir();
		if(dictionary.get(guild_id) == null) {
			String path = savePath + File.separator + String.valueOf(guild_id) + ".preferences";
			Config dic = new Config(path , false);
			if(dic.exists()) {
				dic.load();
			}else {
				dic.save("A preferences for " + String.valueOf(guild_id));
			}
			dictionary.put(guild_id, dic);
			return dic;
		}else {
			return dictionary.get(guild_id);
		}
	}

	public static String getUser(Long guild, String user) {
		return ManagerUserPreference.getGuild(guild).getProperties().getProperty(user);
	}

	public static void setUser(Long guild, String user, String content) {
		Config config = ManagerUserPreference.getGuild(guild);
		Properties pro = config.getProperties();
		pro.setProperty(user, content);
		config.setProperties(pro);
		config.save("Update user preference.");
	}

}

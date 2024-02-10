package jotan.discord.bot.narrate.managerConfig;

import java.io.File;
import java.util.HashMap;

import jotan.discord.bot.narrate.managerConfig.ManagerConfig.BotConfigKey;
import jotan.discord.bot.narrate.managerConfig.ManagerConfig.Config;

public class ManagerDictionary {

	private static HashMap<Long,Config> dictionary = new HashMap<Long,Config>();

	public static Config getGuild(Long guild_id) {
		String dictionaryPath = ManagerConfig.getConfigValue(BotConfigKey.DICTIONARY_SAVE_PATH);
		if(!new File(dictionaryPath).exists()) new File(dictionaryPath).mkdir();
		if(dictionary.get(guild_id) == null) {
			String path = dictionaryPath + File.separator + String.valueOf(guild_id) + ".dictionary";
			Config dic = new Config(path , false);
			if(dic.exists()) {
				dic.load();
			}else {
				dic.save("A dictionary for " + String.valueOf(guild_id));
			}
			dictionary.put(guild_id, dic);
			return dic;
		}else {
			return dictionary.get(guild_id);
		}
	}

}

package jotan.discord.bot.narrate.discord;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jotan.discord.bot.narrate.managerConfig.ManagerConfig;
import jotan.discord.bot.narrate.managerConfig.ManagerConfig.BotConfigKey;

public class ManagerNarrate {

	public static String generateNarrate(String message,String name) {
		String audio_gen_command = ManagerConfig.getConfigValue(BotConfigKey.NARRATE_GENERATE_COMMAND);

		List<String> commands = new ArrayList<String>();
		commands.addAll(Arrays.asList(audio_gen_command.split(" ")));
		commands.add("\""+message+"\"");
		commands.add(name);


		ProcessBuilder generate_sound_process_builder = new ProcessBuilder(commands);
		try {
			Process generate_sound_process = generate_sound_process_builder.start();

			if(Boolean.parseBoolean(ManagerConfig.getConfigValue(BotConfigKey.NARRATE_GENERATE_LOG))) {
		        new StreamThread(generate_sound_process.getInputStream(), "OUTPUT").start();
		        new StreamThread(generate_sound_process.getErrorStream(), "ERROR").start();
			}

			int r = generate_sound_process.waitFor();


			if(r==0) return ManagerConfig.getConfigValue(BotConfigKey.NARRATE_EXPORT_PATH) + "/" + name + ".wav";
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	//code borrowed from
	//https://qiita.com/mfqmagic/items/5469fd4057144b76ad87
	public static class StreamThread extends Thread {
	    private InputStream in;
	    private String type;

	    public StreamThread(InputStream in, String type) {
	        this.in = in;
	        this.type = type;
	    }

	    @Override
	    public void run() {
	        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, "MS932"))) {
	            String line = null;
	            while ((line = br.readLine()) != null) {
	                System.out.println(type + ">" + line);
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	}

}

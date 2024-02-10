package jotan.discord.bot.narrate.managerConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class ManagerNarrateVoiceList {

	public static class NarrateVoiceId{
		private String name;
		private String id;
		public NarrateVoiceId(String name, String id) {
			super();
			this.name = name;
			this.id = id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
	}

	public static List<NarrateVoiceId> narrateVoices = new ArrayList<NarrateVoiceId>();

	public static NarrateVoiceId findVoice(String name) {
		return narrateVoices.stream().filter(x -> x.getName().equals(name)).findFirst().orElse(null);
	}

	public static void readListFile(String path) throws Exception {
		File file = new File(path);

		if(!file.exists()) return;

		FileReader fr = new FileReader(file);
		BufferedReader br = new BufferedReader(fr);
		String content;

		while ((content = br.readLine()) != null) {
			String[] data = content.split(",");
			if(data.length != 2) continue;
			NarrateVoiceId id = new NarrateVoiceId(data[0],data[1]);
			narrateVoices.add(id);
		}
		br.close();
	}

	public static String[] getNames() {
		return narrateVoices.stream().map(x -> x.getName()).toArray(String[]::new);
	}


}

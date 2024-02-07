package jotan.discord.bot.narrate.discord;

import java.awt.Color;
import java.io.File;
import java.net.http.WebSocket.Listener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import jotan.discord.bot.narrate.discord.lavaplayer.GuildMusicManager;
import jotan.discord.bot.narrate.managerConfig.ManagerConfig;
import jotan.discord.bot.narrate.managerConfig.ManagerDictionary;
import jotan.discord.bot.narrate.managerConfig.ManagerNameReadMode;
import jotan.discord.bot.narrate.managerConfig.ManagerConfig.BotConfigKey;
import jotan.discord.bot.narrate.managerConfig.ManagerConfig.Config;
import jotan.discord.bot.narrate.managerConfig.ManagerNameReadMode.Name_Read_Mode;

import java.util.Properties;
import java.util.Random;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.managers.AudioManager;

public class EventHandler extends ListenerAdapter implements Listener {

	HashMap<Long, MessageChannelUnion> narrateTextChannel = new HashMap<Long, MessageChannelUnion>();
	private HashMap<Long, Long> lastUsedTextChannel = new HashMap<Long, Long>();

	class AudioFileNarrater extends Thread {
		public String path = "";
		public String message = "";
		public MessageChannelUnion channel;
		public Guild guild;
		public long start_time = Calendar.getInstance().getTimeInMillis();

		@Override
		public void run() {
			start_time = Calendar.getInstance().getTimeInMillis();
			File file = new File(path);
			long timeout = 2000;
			try {
				timeout = Long.parseLong(ManagerConfig.getConfigValue(BotConfigKey.NARRATE_GENERATE_TIMEOUT));
				if (timeout == -1)
					timeout = Long.MAX_VALUE;
				else
					timeout *= 1000;
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("[WARN] : generate_sound_timeout is not set");
			}
			while (!file.exists()) {
				long current_time = Calendar.getInstance().getTimeInMillis();
				if (current_time - start_time > timeout) {
					channel.sendMessage("音声生成タイムアウト「" + message + "」").complete();
					return;
				}
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			int delay = -1;
			try {
				delay = Integer.parseInt(ManagerConfig.getConfigValue(BotConfigKey.NARRATE_GENERATE_DELAY));
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("[WARN] : file_export_delay is not proper value.");
			}

			if (delay > 0) {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			System.out.println(message + " => " + path);
			ControlLavaplayer.playLocalFile(guild, channel, path);
		}
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent e) {
		if (!e.getChannelType().equals(ChannelType.TEXT))
			return;

		Guild guild = e.getGuild();
		Member member = e.getMember();
		MessageChannelUnion textchannel = e.getChannel();
		Message message = e.getMessage();
		String message_content = message.getContentDisplay();
		AudioManager audio_manager = guild.getAudioManager();

		if (member.getUser().isBot())
			return;

		lastUsedTextChannel.put(guild.getIdLong(), textchannel.getIdLong());

		if (narrateTextChannel.get(guild.getIdLong()) == null)
			return;
		if (textchannel.getIdLong() != narrateTextChannel.get(guild.getIdLong()).getIdLong())
			return;
		if (!audio_manager.isConnected()) {
			textchannel.sendMessage("VCに参加していないため、読み上げを停止します。").complete();
			narrateTextChannel.remove(guild.getIdLong());
			return;
		}

		String message_to_read = message_content;

		if (message_to_read.startsWith(ManagerConfig.getConfigValue(BotConfigKey.NARRATE_IGNORE_PREFIX))) {
			return;
		}

		// URL READ MODE
		if (message_to_read.toLowerCase().startsWith("http")) {
			String url_read_mode = ManagerConfig.getConfigValue(BotConfigKey.NARRATE_URL_MODE);
			if (url_read_mode.equalsIgnoreCase("message")) {
				message_to_read = "URLが送信されました";
			} else if (url_read_mode.equalsIgnoreCase("no")) {
				return;
			}
		}

		// NAME READ MODE
		List<Name_Read_Mode> nrms = ManagerNameReadMode
				.parse_Name_Read_Mode(ManagerConfig.getConfigValue(BotConfigKey.NARRATE_NAME_MODE));
		Collections.reverse(nrms);
		if (!nrms.contains(Name_Read_Mode.OFF)) {
			for (Name_Read_Mode nrm : nrms) {
				message_to_read = ManagerNameReadMode.get_Name_Read_Mode_Value(nrm, member) + " " + message_to_read;
			}
		}

		// REPLACE DICTIONARY WORDS
		Config dic = ManagerDictionary.get_Dictionary(guild.getIdLong());
		for (Entry<Object, Object> s : dic.getProperties().entrySet()) {
			String key = (String) s.getKey();
			String yomi = (String) s.getValue();
			message_to_read = message_to_read.replace(key, yomi);
		}

		// GENERATE NARRATE AUDIO
		String path = ManagerNarrate.generateNarrate(message_to_read, e.getMessageId());
		if (path == null) {
			textchannel.sendMessage("音声の生成に失敗しました。").complete();
			return;
		}

		AudioFileNarrater afn = new AudioFileNarrater();
		afn.path = path;
		afn.message = message_to_read;
		afn.channel = textchannel;
		afn.guild = guild;
		afn.start();
		return;

	}

	public static void registerCommand(JDA jda) {
		List<CommandData> commands = new ArrayList<CommandData>();
		commands.add(Commands.slash("join", "VCに参加し、読み上げを開始。").addOption(OptionType.STRING, "channel", "VC"));
		commands.add(Commands.slash("leave", "VCを離脱し、読み上げを停止。"));

		SlashCommandData commandDic = Commands.slash("dictionary", "辞書に関するコマンド");
		SubcommandData commandDicAdd = new SubcommandData("add", "辞書を追加します。");
		commandDicAdd.addOption(OptionType.STRING, "word", "読みを訂正したい単語を入力します。");
		commandDicAdd.addOption(OptionType.STRING, "read", "単語の読みを入力します。");
		SubcommandData commandDicRemove = new SubcommandData("remove", "辞書を削除します。");
		commandDicRemove.addOption(OptionType.STRING, "word", "削除したい単語を入力します。");
		commandDic.addSubcommands(commandDicAdd);
		commandDic.addSubcommands(commandDicRemove);
		commands.add(commandDic);

		jda.updateCommands().addCommands(commands).queue();
	}

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent  e) {
		Guild guild = e.getGuild();
		Member member = e.getMember();
		AudioManager audioManager = guild.getAudioManager();
		MessageChannelUnion channel = e.getChannel();

		if(e.getName().equals("join")) {
			OptionMapping id = e.getOption("id");
			AudioChannel vc;
			if(id == null) {
				GuildVoiceState gvs = member.getVoiceState();
				if (!gvs.inAudioChannel()) {
					e.reply("おいおい、" + member.getEffectiveName() + "。VCに参加してからこのコマンドを実行してくれ。").queue();
					return;
				}
				vc = gvs.getChannel();
			}
			else {
				vc = guild.getVoiceChannelById(id.getAsString());
				if(vc == null) {
					e.reply(id + "は無効です。").queue();
				}
			}
			audioManager.openAudioConnection(vc);
			narrateTextChannel.put(e.getGuild().getIdLong(), channel);
			EmbedBuilder eb = new EmbedBuilder();
			eb.setColor(Color.CYAN);
			eb.setTitle("読み上げ開始");
			eb.addField("読み上げるチャンネル名", channel.getName(), false);
			eb.addField("VC", vc.getName(), false);
			e.replyEmbeds(eb.build()).complete();
		}
		else if(e.getName().equals("leave")) {
			if (audioManager.isConnected()) {
				audioManager.closeAudioConnection();
				long guild_id = e.getGuild().getIdLong();
				narrateTextChannel.remove(guild_id);
				EmbedBuilder eb = new EmbedBuilder();
				eb.setColor(Color.ORANGE);
				eb.setTitle("読み上げ停止");
				e.replyEmbeds(eb.build()).complete();
				GuildMusicManager musicManager = ControlLavaplayer.getGuildAudioPlayer(e.getGuild());
				musicManager.player.stopTrack();
			} else {
				e.reply("読み上げを行っていません。。").queue();
			}

		}
		else if(e.getName().equals("dictionary")) {
			Config dic = ManagerDictionary.get_Dictionary(guild.getIdLong());
			Properties pro = dic.getProperties();

			if(e.getSubcommandName().equals("add")) {
				OptionMapping word = e.getOption("word");
				OptionMapping read = e.getOption("read");
				if(word == null || read == null) {
					e.reply("単語及び読みを正しく入力してください。").queue();
					return;
				}
				pro.setProperty(word.getAsString(), read.getAsString());


				EmbedBuilder eb = new EmbedBuilder();
				eb.setColor(Color.GREEN);
				eb.setTitle("辞書を追加");
				eb.addField("単語", word.getAsString(), true);
				eb.addField("読み", read.getAsString(), true);
				e.replyEmbeds(eb.build()).complete();

				dic.save("A dictionary for " + String.valueOf(guild.getIdLong()));
			}else if(e.getSubcommandName().equals("remove")) {
				OptionMapping word = e.getOption("word");
				if(word == null) {
					e.reply("単語を正しく入力してください。").queue();
					return;
				}

				Object result = pro.remove(word.getAsString());
				if (result == null) {
					e.reply(word.getAsString() + "を辞書から除去できませんでした。").queue();
				} else {
					EmbedBuilder eb = new EmbedBuilder();
					eb.setColor(Color.ORANGE);
					eb.setTitle("辞書を削除");
					eb.addField("単語", word.getAsString(), true);
					e.replyEmbeds(eb.build()).complete();
					dic.save("A dictionary for " + String.valueOf(guild.getIdLong()));
				}
			}
		}
//		else if (command_args[1].equalsIgnoreCase("reload")) {
//			ManagerConfig.getConfigInstance().load();
//			channel.sendMessage("Reload Complete!").complete();
//		} else if (command_args[1].equalsIgnoreCase("manage")) {
//			if (command_args[2].equalsIgnoreCase("shutdown")) {
//				channel.sendMessage("Good bye.").complete();
//				Main.get_JDA().shutdownNow();
//				System.out.println("SHUTING DOWN...");
//				try {
//					Thread.sleep(200);
//				} catch (InterruptedException e1) {
//					e1.printStackTrace();
//				}
//				System.exit(0);
//			}
//		}

	}

	@Override
	public void onGuildVoiceUpdate(GuildVoiceUpdateEvent e) {
		if (e.getChannelJoined() != null)
			onGuildVoiceJoin(e);
		else
			onGuildVoiceLeave(e);
	}

	private void onGuildVoiceJoin(GuildVoiceUpdateEvent e) {
		autoJoin(e);
		narrateUpdate(e, true);
	}

	private void autoJoin(GuildVoiceUpdateEvent e) {
		if (!Boolean.parseBoolean(ManagerConfig.getConfigValue(BotConfigKey.AUTO_JOIN)))
			return;
		Guild guild = e.getGuild();
		AudioManager audio_manager = guild.getAudioManager();
		AudioChannel vc = e.getChannelJoined();

		if (narrateTextChannel.containsKey(guild.getIdLong()) && audio_manager.isConnected())
			return;

		// is bot
		if (e.getMember().getUser().isBot())
			return;

		if (narrateTextChannel.containsKey(guild.getIdLong()) == false) {
			MessageChannelUnion tc = null;
			if (lastUsedTextChannel.get(guild.getIdLong()) == null) {
				if (tc == null)
					tc = (MessageChannelUnion) guild.getTextChannels().get(0);
				if (tc == null)
					return;
			} else {
				tc = guild.getChannelById(MessageChannelUnion.class ,lastUsedTextChannel.get(guild.getIdLong()));
			}

			narrateTextChannel.put(guild.getIdLong(), tc);
		}

		MessageChannelUnion channel_to_read = narrateTextChannel.get(guild.getIdLong());

		audio_manager.openAudioConnection(vc);

		EmbedBuilder eb = new EmbedBuilder();
		eb.setColor(Color.CYAN);
		eb.setTitle("読み上げ開始");
		eb.addField("読み上げるチャンネル名", channel_to_read.getName(), false);
		eb.addField("VC", vc.getName(), false);
		channel_to_read.sendMessageEmbeds(eb.build()).complete();
	}

	private void onGuildVoiceLeave(GuildVoiceUpdateEvent e) {
		autoLeave(e);
		narrateUpdate(e, false);
	}

	private void autoLeave(GuildVoiceUpdateEvent e) {
		if (!Boolean.parseBoolean(ManagerConfig.getConfigValue(BotConfigKey.AUTO_LEAVE)))
			return;
		Guild guild = e.getGuild();
		AudioManager audio_manager = guild.getAudioManager();
		AudioChannel vc = e.getChannelLeft();

		if (!narrateTextChannel.containsKey(guild.getIdLong()))
			return;

		// when bot is not connected to any vc.
		if (!audio_manager.isConnected())
			return;

		// when event vc and connected vc are different.
		if (audio_manager.getConnectedChannel().getIdLong() != vc.getIdLong())
			return;

		// when everyone isn't got disconnected
		int human_count = 0;
		for (Member member : vc.getMembers()) {
			if (!member.getUser().isBot())
				human_count++;
		}
		if (human_count > 0)
			return;

		MessageChannelUnion channel_to_read = narrateTextChannel.get(guild.getIdLong());

		EmbedBuilder eb = new EmbedBuilder();
		eb.setColor(Color.ORANGE);
		eb.setTitle("読み上げ終了");
		eb.addField("VC", vc.getName(), false);
		channel_to_read.sendMessageEmbeds(eb.build()).complete();

		narrateTextChannel.remove(guild.getIdLong());

		GuildMusicManager musicManager = ControlLavaplayer.getGuildAudioPlayer(e.getGuild());
		musicManager.player.stopTrack();
		musicManager.player.destroy();

		audio_manager.closeAudioConnection();
	}

	private void narrateUpdate(GuildVoiceUpdateEvent e, boolean join) {
		Guild guild = e.getGuild();
		AudioManager audio_manager = guild.getAudioManager();
		if (!audio_manager.isConnected())
			return;
		if (!narrateTextChannel.containsKey(guild.getIdLong()))
			return;
		if (e.getMember().getUser().isBot())
			return;

		MessageChannelUnion channel_to_read = narrateTextChannel.get(guild.getIdLong());
		String narrate = ManagerConfig
				.getConfigValue(join ? BotConfigKey.NARRATE_USER_JOIN : BotConfigKey.NARRATE_USER_LEAVE);
		if (narrate.equalsIgnoreCase("off"))
			return;
		narrate = narrate.replace("{name}", e.getMember().getEffectiveName());

		// REPLACE DICTIONARY WORDS
		Config dic = ManagerDictionary.get_Dictionary(guild.getIdLong());
		for (Entry<Object, Object> s : dic.getProperties().entrySet()) {
			String key = (String) s.getKey();
			String yomi = (String) s.getValue();
			narrate = narrate.replace(key, yomi);
		}

		// GENERATE NARRATE AUDIO
		String path = ManagerNarrate.generateNarrate(narrate, String.valueOf(new Random().nextInt()));
		if (path == null) {
			channel_to_read.sendMessage("音声の生成に失敗しました。").complete();
			return;
		}
		AudioFileNarrater afn = new AudioFileNarrater();
		afn.path = path;
		afn.message = narrate;
		afn.channel = channel_to_read;
		afn.start();
		return;
	}

}

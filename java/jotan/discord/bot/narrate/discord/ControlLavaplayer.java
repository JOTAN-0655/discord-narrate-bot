package jotan.discord.bot.narrate.discord;

import java.util.HashMap;
import java.util.Map;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import jotan.discord.bot.narrate.discord.lavaplayer.GuildMusicManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;

public class ControlLavaplayer {

	private static AudioPlayerManager playerManager;
	private static Map<Long, GuildMusicManager> musicManagers;

	public static void initialize() {
		musicManagers = new HashMap<>();

	    playerManager = new DefaultAudioPlayerManager();
	    AudioSourceManagers.registerRemoteSources(playerManager);
	    AudioSourceManagers.registerLocalSource(playerManager);
	}

	public static synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
		long guildId = Long.parseLong(guild.getId());
	    GuildMusicManager musicManager = musicManagers.get(guildId);
	    if (musicManager == null) {
	      musicManager = new GuildMusicManager(playerManager);
	      musicManagers.put(guildId, musicManager);
	    }
	    guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());
	    return musicManager;
	}

	public static void playLocalFile(TextChannel tc,String path) {
		GuildMusicManager musicManager = getGuildAudioPlayer(tc.getGuild());
		playerManager.loadItem(path, new AudioLoadResultHandler() {

			@Override
			public void trackLoaded(AudioTrack track) {
				play(tc.getGuild(),musicManager,track);
			}

			@Override
			public void playlistLoaded(AudioPlaylist playlist) {
			}

			@Override
			public void noMatches() {
				tc.sendMessage("読み上げるための音声ファイルが見つかりませんでした。").complete();
			}

			@Override
			public void loadFailed(FriendlyException exception) {
				tc.sendMessage("読み上げるための音声ファイルの読み込みに失敗しました。").complete();
				exception.printStackTrace();
			}

		});
	}

	public static void play(Guild guild, GuildMusicManager musicManager, AudioTrack track) {
	    connectToFirstVoiceChannel(guild.getAudioManager());
	    musicManager.scheduler.queue(track);
	}

	public static void skipTrack(TextChannel channel) {
	  GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
	  musicManager.scheduler.nextTrack();
	}
	public static void connectToFirstVoiceChannel(AudioManager audioManager) {
	  if (!audioManager.isConnected()) {
	    for (VoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
	      audioManager.openAudioConnection(voiceChannel);
	      break;
	    }
	  }
	}

}

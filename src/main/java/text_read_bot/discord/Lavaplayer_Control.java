package text_read_bot.discord;

import java.util.HashMap;
import java.util.Map;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;
import text_read_bot.discord.lavaplayer.GuildMusicManager;

public class Lavaplayer_Control {

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

	public static void loadAndPlay(final TextChannel channel, final String trackUrl) {
	    GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
	    playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
	      @Override
	      public void trackLoaded(AudioTrack track) {
	        channel.sendMessage("�Đ��L���[�ɒǉ����܂��� :  " + track.getInfo().title).queue();

	        play(channel.getGuild(), musicManager, track);
	      }

	      @Override
	      public void playlistLoaded(AudioPlaylist playlist) {
	        AudioTrack firstTrack = playlist.getSelectedTrack();

	        if (firstTrack == null) {
	          firstTrack = playlist.getTracks().get(0);
	        }

	        channel.sendMessage("�Đ��L���[�ɒǉ����܂��� :  " + firstTrack.getInfo().title + " (�v���C���X�g�̍ŏ��̋�: " + playlist.getName() + ")").queue();

	        play(channel.getGuild(), musicManager, firstTrack);
	      }

	      @Override
	      public void noMatches() {
	        channel.sendMessage("" + trackUrl + "�ŁA����������܂���ł����B^q^").queue();
	      }

	      @Override
	      public void loadFailed(FriendlyException exception) {
	        channel.sendMessage("�Đ��ł��܂���^q^: " + exception.getMessage()).queue();
	      }
	    });
	}

	public static void narrate_message(TextChannel tc,String path) {
		GuildMusicManager musicManager = getGuildAudioPlayer(tc.getGuild());
		playerManager.loadItem(path, new AudioLoadResultHandler() {

			@Override
			public void trackLoaded(AudioTrack track) {
				// TODO �����������ꂽ���\�b�h�E�X�^�u

				play(tc.getGuild(),musicManager,track);
			}

			@Override
			public void playlistLoaded(AudioPlaylist playlist) {
				// TODO �����������ꂽ���\�b�h�E�X�^�u

			}

			@Override
			public void noMatches() {
				// TODO �����������ꂽ���\�b�h�E�X�^�u
				tc.sendMessage("�ǂݏグ��t�@�C�������݂��܂���ł����B").complete();
			}

			@Override
			public void loadFailed(FriendlyException exception) {
				// TODO �����������ꂽ���\�b�h�E�X�^�u
				tc.sendMessage("�ǂݏグ�t�@�C���̓ǂݍ��݂Ɏ��s���܂����B").complete();
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
	  channel.sendMessage("���̃g���b�N�ɃX�L�b�v���܂��B").queue();
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

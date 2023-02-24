package jotan.discord.bot.narrate.discord.lavaplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import jotan.discord.bot.narrate.managerConfig.ManagerConfig;
import jotan.discord.bot.narrate.managerConfig.ManagerConfig.BotConfigKey;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class schedules tracks for the audio player. It contains the queue of tracks.
 */
public class TrackScheduler extends AudioEventAdapter {
	private final AudioPlayer player;
	private final BlockingQueue<AudioTrack> queue;

	/**
	 * @param player The audio player this scheduler uses
	 */
	public TrackScheduler(AudioPlayer player) {
		this.player = player;
		this.queue = new LinkedBlockingQueue<>();
	}

	/**
	 * Add the next track to queue or play right away if nothing is in the queue.
	 *
	 * @param track The track to play or add to queue.
	 */
	public void queue(AudioTrack track) {
		// Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
		// something is playing, it returns false and does nothing. In that case the player was already playing so this
		// track goes to the queue instead.
		if (!player.startTrack(track, true)) {
			queue.offer(track);
		}
	}

	/**
	 * Start the next track, stopping the current one if it is playing.
	 */
	public void nextTrack() {
		player.startTrack(queue.poll(), false);
	}
	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		String uri = track.getInfo().uri;
		File file = new File(uri);
		if(file.exists()) {
			File audio_path = new File(ManagerConfig.getConfigValue(BotConfigKey.NARRATE_EXPORT_PATH));
			if(file.getParentFile().equals(audio_path)) {
				file.delete();
			}

		}

		if (endReason.mayStartNext) {
	      nextTrack();
	    }
	}
}
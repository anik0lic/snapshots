package app.snapshot_bitcake;

import app.snapshot_bitcake.acharya_badrinath.AbSnapshotResult;
import app.snapshot_bitcake.coordinated_checkpointing.KcSnapshotResult;

/**
 * This class is used if the user hasn't specified a snapshot type in config.
 * 
 * @author bmilojkovic
 *
 */
public class NullSnapshotCollector implements SnapshotCollector {

	@Override
	public void run() {}

	@Override
	public void stop() {}

	@Override
	public BitcakeManager getBitcakeManager() {
		return null;
	}

	@Override
	public void addKcSnapshotInfo(int id, KcSnapshotResult kcSnapshotResult) {}

	@Override
	public void addAbSnapshotInfo(int id, AbSnapshotResult abSnapshotResult) {}

	@Override
	public void startCollecting() {}

	@Override
	public boolean isCollecting() {
		return false;
	}

}

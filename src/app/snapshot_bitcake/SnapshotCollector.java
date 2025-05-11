package app.snapshot_bitcake;

import app.Cancellable;
import app.snapshot_bitcake.acharya_badrinath.AbSnapshotResult;
import app.snapshot_bitcake.alagar_venkatesan.AvSnapshotResult;
import app.snapshot_bitcake.coordinated_checkpointing.KcSnapshotResult;

/**
 * Describes a snapshot collector. Made not-so-flexibly for readability.
 * 
 * @author bmilojkovic
 *
 */
public interface SnapshotCollector extends Runnable, Cancellable {

	BitcakeManager getBitcakeManager();

	void addKcSnapshotInfo(int id, KcSnapshotResult kcSnapshotResult);
	void addAbSnapshotInfo(int id, AbSnapshotResult abSnapshotResult);
	void addAvSnapshotInfo(int id, AvSnapshotResult avSnapshotResult);

	void startCollecting();

	boolean isCollecting();

}
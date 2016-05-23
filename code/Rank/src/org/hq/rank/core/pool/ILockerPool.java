package org.hq.rank.core.pool;

import org.hq.rank.core.node.Node;
import org.hq.rank.core.node.RankElement;

public interface ILockerPool {
	//node
	public boolean tryLockNodeWLocker(Node node , int conditionLevel);
	public void unlockNodeWLocker(Node node , int conditionLevel);
	public boolean tryLockNodeRLocker(Node node , int conditionLevel);
	public void unlockNodeRLocker(Node node , int conditionLevel);
	// rankElement �������rankElement�д���nodeStepʱ���õģ����ұ�����ס
	public void lockRankElementWLocker(RankElement rankElement,int conditionLevel);
	public void unLockRankElementWLocker(RankElement rankElement,int conditionLevel);
	public void lockRankElementRLocker(RankElement rankElement,int conditionLevel);
	public void unLockRankElementRLocker(RankElement rankElement,int conditionLevel);
}

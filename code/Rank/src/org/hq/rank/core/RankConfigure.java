package org.hq.rank.core;

import org.hq.rank.core.Rank.ReOperType;

public class RankConfigure {
	/**
	 * rank������
	 * ReOperType
	 * scheduleThreadCount
	 * maxScheduleTime
	 * multiThreadCount
	 * warnReOperTimes
	 * errorReoperTimes
	 * 
	 * NodeStep.fullCount
	 * NodeStep.deleteCount
	 * NodeStep.maxHitTimes
	 * ElementStep.fullCount
	 * ElementStep.deleteCount
	 */
	// Ĭ��ֵ
	private static final ReOperType REOPERTYPE_DEFAULT = ReOperType.MultiSche;
	private static final int SCHEDULETHREADCOUNT_DEFAULT = 10;
	private static final int MAXSCHEDULETHREADCOUNT_DEFAULT = 100;
	private static final int MAXSCHEDULETIME_DEFAULT = 100000;// ����
	private static final int MULTITHREADCOUNT_DEFAULT = 10;
	private static final int WARNREOPERTIMES_DEFAULT = 2000;
	private static final int ERRORREOPERTIMES_DEFAULT = 10000;
	private static final int CUTCOUNTNODESTEP_DEFAULT = 600;
	private static final int COMBINECOUNTNODESTEP_DEFAULT = 60;
	private static final int MAXHITTIMESNODESTEP_DEFAULT = 3;
	
	private static final int CUTCOUNTELEMENTSTEP_DEFAULT = 800;
	private static final int COMBINECOUNTELEMENTSTEP_DEFAULT = 100;
	
	private static final int MAXGETRANKDATATIMES_DEFAULT = 5;
	private static final int RANKELEMENTNODEMAPCOUNT_DEFAULT = 1000;
	//
	private String rankName;
	// ����
	private ReOperType reOperType = REOPERTYPE_DEFAULT;
	private int scheduleThreadCount = SCHEDULETHREADCOUNT_DEFAULT;
	private int maxScheduleThreadCount = MAXSCHEDULETHREADCOUNT_DEFAULT;
	private int maxScheduleTime = MAXSCHEDULETIME_DEFAULT;
	private int multiThreadCount = MULTITHREADCOUNT_DEFAULT; //MultiThread ʹ��
	private int warnReOperTimes = WARNREOPERTIMES_DEFAULT; //
	private int errorReoperTimes = ERRORREOPERTIMES_DEFAULT;
	
	private int cutCountNodeStep = CUTCOUNTNODESTEP_DEFAULT;
	private int combineCountNodeStep = COMBINECOUNTNODESTEP_DEFAULT;
	private int maxHitTimesNodeStep = MAXHITTIMESNODESTEP_DEFAULT;
	// �������������ﵽ�������ʱ��������һ��step
	private int cutCountElementStep = CUTCOUNTELEMENTSTEP_DEFAULT;
	// ȥ������count��С�ڸ�����ʱ������ϲ�����һ��step��Ҳ����˵��һ��step����п��ܴﵽ(fullCount+deleteCount)��
	private int combineCountElementStep = COMBINECOUNTELEMENTSTEP_DEFAULT;
	
	private int maxGetRankDataTimes = MAXGETRANKDATATIMES_DEFAULT;
	//rankElement�Ƚ϶࣬ÿ�����浥��һ��ConcurrentHashMapռ��̫���ڴ�, ͳһ��һ��ConcurrentHashMap������������
	private int rankElementNodeMapCount = RANKELEMENTNODEMAPCOUNT_DEFAULT;
	
	// ���������У���������
	private int rankConditionCount = 1; // >0
	
	public RankConfigure() {
		rankName = "rank";
	}
	/**
	 * ���ص�ǰ�����Ƿ����
	 * @return
	 */
	public boolean check(){
		return true;
	}

	public ReOperType getReOperType() {
		return reOperType;
	}

	public void setReOperType(ReOperType reOperType) {
		this.reOperType = reOperType;
	}

	public int getScheduleThreadCount() {
		return scheduleThreadCount;
	}

	public void setScheduleThreadCount(int scheduleThreadCount) {
		this.scheduleThreadCount = scheduleThreadCount;
	}

	public int getMaxScheduleThreadCount() {
		return maxScheduleThreadCount;
	}
	public void setMaxScheduleThreadCount(int maxScheduleThreadCount) {
		this.maxScheduleThreadCount = maxScheduleThreadCount;
	}
	public int getMaxScheduleTime() {
		return maxScheduleTime;
	}

	public void setMaxScheduleTime(int maxScheduleTime) {
		this.maxScheduleTime = maxScheduleTime;
	}

	public int getMultiThreadCount() {
		return multiThreadCount;
	}

	public void setMultiThreadCount(int multiThreadCount) {
		this.multiThreadCount = multiThreadCount;
	}

	public int getWarnReOperTimes() {
		return warnReOperTimes;
	}

	public void setWarnReOperTimes(int warnReOperTimes) {
		this.warnReOperTimes = warnReOperTimes;
	}

	public int getErrorReoperTimes() {
		return errorReoperTimes;
	}

	public void setErrorReoperTimes(int errorReoperTimes) {
		this.errorReoperTimes = errorReoperTimes;
	}

	public int getCutCountNodeStep() {
		return cutCountNodeStep;
	}

	public void setCutCountNodeStep(int cutCountNodeStep) {
		this.cutCountNodeStep = cutCountNodeStep;
	}

	public int getCombineCountNodeStep() {
		return combineCountNodeStep;
	}

	public void setCombineCountNodeStep(int combineCountNodeStep) {
		this.combineCountNodeStep = combineCountNodeStep;
	}

	public int getMaxHitTimesNodeStep() {
		return maxHitTimesNodeStep;
	}

	public void setMaxHitTimesNodeStep(int maxHitTimesNodeStep) {
		this.maxHitTimesNodeStep = maxHitTimesNodeStep;
	}

	public int getRankConditionCount() {
		return rankConditionCount;
	}
	public void setRankConditionCount(int rankConditionCount) {
		this.rankConditionCount = rankConditionCount;
	}
	public String getRankName() {
		return rankName;
	}
	public void setRankName(String rankName) {
		this.rankName = rankName;
	}
	public int getCutCountElementStep() {
		return cutCountElementStep;
	}
	public void setCutCountElementStep(int cutCountElementStep) {
		this.cutCountElementStep = cutCountElementStep;
	}
	public int getCombineCountElementStep() {
		return combineCountElementStep;
	}
	public void setCombineCountElementStep(int combineCountElementStep) {
		this.combineCountElementStep = combineCountElementStep;
	}
	public int getMaxGetRankDataTimes() {
		return maxGetRankDataTimes;
	}
	public void setMaxGetRankDataTimes(int maxGetRankDataTimes) {
		this.maxGetRankDataTimes = maxGetRankDataTimes;
	}
	public int getRankElementNodeMapCount() {
		return rankElementNodeMapCount;
	}
	public void setRankElementNodeMapCount(int rankElementNodeMapCount) {
		this.rankElementNodeMapCount = rankElementNodeMapCount;
	}
	
	
}

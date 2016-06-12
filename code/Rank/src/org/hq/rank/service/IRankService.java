package org.hq.rank.service;

import java.util.List;

import org.hq.rank.core.RankData;

/**
 * IRankService
 * ����һ������
 * ɾ��һ������
 * ɾ�����е�����
 * �ж�һ�������Ƿ����
 * ��������������ݣ���ӣ���û������ӣ�
 * ��������ɾ������
 * �����Ƿ����ĳ��id
 * ��ȡĳ��id������
 * ��ȡĳ�����е�id
 * ��ȡĳҳ���е�id
 * ��ȡĳ��������¶��ٸ���ҵ���������
 * 
 * ���е����ݴ�С����0
 * 
 * @author zhen
 */
public interface IRankService {
	/**
	 * ����һ�����У�{@code createRank(rankName , 1)}
	 * @param rankName ���е����֣������ظ�
	 * @return �������Ѿ����� ����false
	 */
	public boolean createRank(String rankName);
	/**
	 * ����һ������
	 * @param rankName ���е����֣������ظ�
	 * @param fieldCount ���е��ֶ���
	 * @return �������Ѿ����� ����false
	 */
	public boolean createRank(String rankName,int fieldCount);
	/**
	 * ɾ��һ������
	 * @param rankName ���е�����
	 */
	public void deleteRank(String rankName);
	/**
	 * ɾ�����е�����
	 * ��ϵͳҪֹͣ��ʱ����ã��ȴ�ʣ�µ�������ɵ�ǰ��������������
	 * @param rankName
	 */
	public void deleteAllRank();
	/**
	 * �ж�һ�������Ƿ����
	 * @param rankName ���е�����
	 * @return
	 */
	public boolean hasRank(String rankName);
	/**
	 * ����һ�����ݣ������id���ڣ��滻Ϊ��ǰ����
	 * @param rankName ���е�����
	 * @param id �����ṩ�ߵ�id
	 * @param value ����
	 * @return �������֮ǰ��ֵ������֮ǰ��ֵ�����򣬷���-1
	 */
	public long put(String rankName,int id , long value);
	/**
	 * ����һ�����ݣ������id���ڣ��滻Ϊ��ǰ����
	 * @param rankName ���е�����
	 * @param id �����ṩ�ߵ�id
	 * @param value ���ݣ�����Ͷ�Ӧ��Ӧ�������ֶ�����ͬ
	 * @return �������֮ǰ��ֵ������֮ǰ��ֵ�����򣬷���null
	 */
	public long[] put(String rankName,int id , long... value);
	/**
	 * ����һ�����ݣ������id���ڣ�������
	 * @param rankName ���е�����
	 * @param id �����ṩ�ߵ�id
	 * @param value ����
	 * @return �������֮ǰ��ֵ������֮ǰ��ֵ�����򣬷���-1
	 */
	public long putIfAbsent(String rankName,int id , long value);
	/**
	 * ����һ�����ݣ������id���ڣ�������
	 * @param rankName ���е�����
	 * @param id �����ṩ�ߵ�id
	 * @param value ���ݣ�����Ͷ�Ӧ��Ӧ�������ֶ�����ͬ
	 * @return �������֮ǰ��ֵ������֮ǰ��ֵ�����򣬷���null
	 */
	public long[] putIfAbsent(String rankName,int id , long... value);
	/**
	 * ���������ֶθ������ݣ���Ҫע�⣬������������Ѿ����ڵ����ݣ����򣬱����ݲ������쳣
	 * @param rankName ���е�����
	 * @param id �����ṩ�ߵ�id
	 * @param field ���޸�ֵ���ֶ�
	 * @param value ���޸ĵ�ֵ
	 * @return ԭ�����ֶ��ϵ�ֵ
	 */
	public long putByField(String rankName,int id ,int field,long value);
	/**
	 * ɾ��һ������
	 * @param rankName ���е�����
	 * @param id �����ṩ�ߵ�id
	 * @return ����ֵ�������ڷ���null
	 */
	public long[] delete(String rankName,int id);
	/**
	 * �Ƿ����ĳ��ҵ���������
	 * @param rankName ���е�����
	 * @param id �����ṩ�ߵ�id
	 * @return 
	 */
	public boolean has(String rankName,int id);
	/**
	 * ��ѯһ����ҵ�����
	 * @param rankName ���е�����
	 * @param id �����ṩ�ߵ�id
	 * @return ��Ӧid��ҵ��������Σ�û�л��ѯʧ�ܷ���-1
	 */
	public int getRankNum(String rankName,int id);
	/**
	 * ����id��ѯһ����ҵ���������
	 * @param rankName ���е�����
	 * @param id �����ṩ�ߵ�id
	 * @return {@code RankData}
	 */
	public RankData getRankDataById(String rankName,int id);
	/**
	 * ��ѯĳ���е����id
	 * @param rankName ���е�����
	 * @param rankNum ���е����Σ���0��ʼ
	 * @return ��Ӧ������ҵ�id��û�л�ʧ���򷵻�-1
	 */
	public int getRankId(String rankName,int rankNum);
	/**
	 * �����������λ�ȡ�����������
	 * @param rankName ���е�����
	 * @param rankNum ���е�����
	 * @return {@code RankData}
	 */
	public RankData getRankDataByRankNum(String rankName,int rankNum);
	/**
	 * ��ҳ��ѯ��������
	 * @param rankName ���е�����
	 * @param page ҳ������0��ʼ
	 * @param pageSize ÿһҳ�Ĵ�С
	 * @return {@code RankData}
	 */
	public List<RankData> getRankDatasByPage(String rankName,int page,int pageSize);
	/**
	 * ��ȡ�û�����ǰ�󼸸��û�����������
	 * @param rankName ���е�����
	 * @param id �����ṩ�ߵ�id
	 * @param beforeNum ��ȡ��ǰ���û�����
	 * @param afterNum	��ȡ�ĺ����û�����
	 * @return {@code RankData}
	 */
	public List<RankData> getRankDatasAroundId(String rankName,int id,int beforeNum,int afterNum);
}

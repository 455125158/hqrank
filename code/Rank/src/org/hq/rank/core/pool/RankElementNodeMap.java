package org.hq.rank.core.pool;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.hq.rank.core.Rank;
import org.hq.rank.core.node.Node;
import org.hq.rank.core.node.RankElement;

/**
 * ����Ƿֶ����У�rankElement�Ƚ϶࣬ÿ�����浥��һ��ConcurrentHashMapռ��̫���ڴ�
 * ͳһ��һ��ConcurrentHashMap������������
 * 
 * hashcode��������һ����ɢ�����������������hashcode
 * get put remove clear
 * @author zhen
 *
 */
public class RankElementNodeMap {
	private final Rank rank;
	private final int mapCount;
	//����rankElement.hascode%mapCount�� key = rankElement.hascode+"_"+node.value
	private final ConcurrentHashMap<String, Node>[] maps; // id
	private final AtomicLong elementIdCreator = new AtomicLong(0);
	
	public RankElementNodeMap(Rank rank){
		this.rank = rank;
		this.mapCount = 1000;
		if(rank.getRankConfigure().getRankConditionCount() > 1){
			maps = new ConcurrentHashMap[this.mapCount];
			for(int i = 0;i< mapCount;i++){
				maps[i] = new ConcurrentHashMap<String, Node>();
			}
		}else{
			maps = null;
		}
	}
	// ElementId����������������element��Ψһid
	public String getNewId(){
		return ""+elementIdCreator.getAndIncrement();
	}
	public long getId(){
		return elementIdCreator.get();
	}
	// ����rankElement.hashCode()�����ڴ��ַ�����ԣ���ͬ��RankElementһ����ͬ
	public Node get(RankElement rankElement,long value){
		int hashCode = rankElement.hashCode();
		ConcurrentHashMap<String, Node> map = maps[hashCode%mapCount];
		return map.get(rankElement.id+"_"+value);
	}
	public void put(RankElement rankElement,long value,Node node){
		int hashCode = rankElement.hashCode();
		ConcurrentHashMap<String, Node> map = maps[hashCode%mapCount];
		map.put(rankElement.id+"_"+value, node);
	}
	public void remove(RankElement rankElement,long value){
		int hashCode = rankElement.hashCode();
		ConcurrentHashMap<String, Node> map = maps[hashCode%mapCount];
		map.remove(rankElement.id+"_"+value);
	}
	
}

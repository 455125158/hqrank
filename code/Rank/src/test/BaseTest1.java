package test;

import org.hq.rank.service.IRankService;
import org.hq.rank.service.RankService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ����һЩ�����Ĳ��������ֶε�
 * ������service�еĽӿڵļ򵥷��ʣ���Ҫ˵�����ߵ�ʹ�÷����Ͷ�����ȷ�ԵĲ���
 * @author a
 *
 */
public class BaseTest1 {
	private static Logger log = LoggerFactory.getLogger(BaseTest1.class);
	
	public static void main(String[] args){
		IRankService rankService = new RankService();
		
		BaseTest1 test = new BaseTest1();
		test.test1(rankService);
		
		rankService.deleteAllRank();
	}
	/**
	 * ��rank�Ĵ�����ɾ��
	 */
	private void test1(IRankService rankService){
		boolean hasRankA,hasRankB;
		
		rankService.createRank("rank_a");
		hasRankA = rankService.hasRank("rank_a");
		hasRankB = rankService.hasRank("rank_b");
		log.info("hasRankA:"+hasRankA+",hasRankB:"+hasRankB);
		
		rankService.createRank("rank_b");
		hasRankA = rankService.hasRank("rank_a");
		hasRankB = rankService.hasRank("rank_b");
		log.info("hasRankA:"+hasRankA+",hasRankB:"+hasRankB);
		
		rankService.deleteRank("rank_a");
		hasRankA = rankService.hasRank("rank_a");
		hasRankB = rankService.hasRank("rank_b");
		log.info("hasRankA:"+hasRankA+",hasRankB:"+hasRankB);
		
		rankService.deleteAllRank();
		hasRankA = rankService.hasRank("rank_a");
		hasRankB = rankService.hasRank("rank_b");
		log.info("hasRankA:"+hasRankA+",hasRankB:"+hasRankB);
	}
}

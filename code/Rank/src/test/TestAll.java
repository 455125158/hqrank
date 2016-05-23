package test;

import java.util.concurrent.ThreadPoolExecutor;

import org.hq.rank.service.IRankService;
import org.hq.rank.service.RankService;


/***
 * �������һ�ֲ��ԣ������������һ�¾��ܽ��ܶ��ֿ��ܲ���һ��
 * ��Ҫ���Ե�������£�
 * id���̶�����������٣����ظ���
 * 
 * ���ֶΣ�
 * �̶�value��
 * ���value��
 * ����value��
 * �����ֶΣ�
 * �������
 * ��һ���࣬�ڶ�����
 * ��һ���٣��ڶ�����
 * ����
 * �����ֶΣ�
 * 
 * 
 * ɾ�����
 * @author zhen
 *
 */
public class TestAll {
	
	
	static IRankService rankService = new RankService();
	
	public static void main(String[] args){
		
		rankService.createRank("first");
		rankService.createRank("second");
		for(int i=0;i<100;i++){
			rankService.put("first", 123*i, 321*i);
//			rankService.put("first", 123*i, 654);
			rankService.put("second", 123*i, 321*i);
			rankService.put("first", 123*i, 123*i);
			rankService.put("second", 123*i, 123*i);
		}
		rankService.delete("first", 123*6);
		System.out.println(rankService.getRankDataById("first", 123*7));
		rankService.destroy("first");
		rankService.destroy("second");
		
	}
}

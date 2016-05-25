package test;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.hq.rank.core.RankData;
import org.hq.rank.service.IRankService;
import org.hq.rank.service.RankService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ѹ������
 * ��Ϊ��
 * 1��ѹ�������µ���ȷ�ԣ���redis����
 * 2��������ִ��Ч�ʣ���ִ��ʱ��
 * ���Է�����Ϊ���֣�
 * 1��һ���������ݺ��̣߳�Ҫ���ִ�����
 * 2��ÿ����ִ��һ�������̺߳����ݣ���������
 * @author a
 *
 */
public class BaseTest2 {
	private static Logger log = LoggerFactory.getLogger(BaseTest2.class);
	
	public static void main(String[] args) throws InterruptedException{
		IRankService rankService = new RankService();
		
		BaseTest2 test = new BaseTest2();
		test.test1(rankService);
		
		rankService.deleteAllRank();
	}
	/**
	 * ���߳�
	 * @param rankService
	 * @throws InterruptedException
	 */
	private void test1(final IRankService rankService) throws InterruptedException{
		final int threadCount = 100;
		final int dataCountPerThread = 1000;
		final int maxId = 100000;
		final int maxValue = 1000000;
		Thread[] threads = new Thread[threadCount];
		final int[][] ids = new int[threadCount][];
		final long[][] values = new long[threadCount][];
		
		final CountDownLatch latch = new CountDownLatch(threadCount);
		
		rankService.createRank("rank_a");
		// ����id������
		for(int i = 0;i<threadCount;i++){
			ids[i] = new int[dataCountPerThread];
			values[i] = new long[dataCountPerThread];
			for(int j = 0;j<dataCountPerThread;j++){
				ids[i][j] = randomId(maxId);
				values[i][j] = randomValue(maxValue);
			}
		}
		// �����߳�
		for(int threadI=0;threadI<threadCount ;threadI++){
			final int threadIndex = threadI;
			Thread thread = new Thread("threadIndex"+threadIndex){
				@Override
				public void run(){
					for(int i=0;i<dataCountPerThread;i++){
						rankService.put("rank_a", ids[threadIndex][i], values[threadIndex][i]);
					}
					latch.countDown();
				}
			};
			threads[threadI] = thread;
		}
		// ִ��
		long t1 = System.nanoTime();
		for(int threadI=0;threadI<threadCount ;threadI++){
			threads[threadI].start();
		}
		latch.await();
		long t2 = System.nanoTime();
		log.info("useTime:"+(t2-t1)/1000000);
		// get
		int testId=30;
		for(int i=0;i<10;i++){
			RankData rankData = rankService.getRankDataById("rank_a", testId+i);
			log.info("rankData1:"+rankData);
		}
		rankService.put("rank_a", testId, 1);
		RankData rankData2 = rankService.getRankDataById("rank_a", testId);
		
		log.info("rankData2:"+rankData2);
	}
	
	
	private Random random = new Random();
	private int randomId(int maxId){
		maxId = Math.abs(maxId);
		return random.nextInt(maxId);
	}
	private long randomValue(long maxValue){
		maxValue = Math.abs(maxValue);
		return Math.abs(random.nextLong())%maxValue;
	}
}

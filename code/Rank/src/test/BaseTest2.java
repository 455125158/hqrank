package test;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.hq.rank.core.Rank;
import org.hq.rank.core.RankConfigure;
import org.hq.rank.core.RankData;
import org.hq.rank.service.IRankService;
import org.hq.rank.service.RankService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

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
//		test.test1(rankService);
//		test.test2(rankService);
		test.test3(rankService);
		
		rankService.deleteAllRank();
	}
	/**
	 * ���߳�
	 * @param rankService
	 * @throws InterruptedException
	 */
	private void test1(final IRankService rankService) throws InterruptedException{
		final String rankName = "rank_a";
		final int threadCount = 100;
		final int dataCountPerThread = 1000;
		final int maxId = 100000;
		final int maxValue = 1000000;
		Thread[] threads = new Thread[threadCount];
		final int[][] ids = new int[threadCount][];
		final long[][] values = new long[threadCount][];
		
		final CountDownLatch latch = new CountDownLatch(threadCount);
		
		rankService.createRank(rankName);
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
						rankService.put(rankName, ids[threadIndex][i], values[threadIndex][i]);
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
			RankData rankData = rankService.getRankDataById(rankName, testId+i);
			log.info("rankData1:"+rankData);
		}
		rankService.put(rankName, testId, 1);
		RankData rankData2 = rankService.getRankDataById(rankName, testId);
		
		log.info("rankData2:"+rankData2);
	}
	/**
	 * ���߳̿��ٷ��ʣ����redis�Ĳ�������ȷ��
	 * ��ɾ�Ľ��
	 * @param rankService
	 * @throws InterruptedException
	 */
	private void test2(final IRankService rankService) throws InterruptedException{
		final String rankName = "rank_a";
		final int threadCount = 10;
		final int dataCountPerThread = 1000;
		final int maxId = 100000;
		final int maxValue = 1000000;
		Thread[] threads = new Thread[threadCount];
		final int[][] ids = new int[threadCount][];
		final long[][] values = new long[threadCount][];
		
		final JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxTotal(threadCount);
		config.setMinIdle(threadCount);
		final JedisPool pool = new JedisPool(config, "192.168.1.240");
		
		final boolean isRedis = true;
		final boolean isDel = true;
		
		final CountDownLatch latch = new CountDownLatch(threadCount);
		
		rankService.createRank(rankName);
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
					final Jedis jedis = pool.getResource();
					for(int i=0;i<dataCountPerThread;i++){
						setValue(rankService, jedis, rankName, ids[threadIndex][i], values[threadIndex][i], isRedis);
						if(isDel){
							if(i%2 == 0 && threadIndex>1){
								int id = ids[randomId(threadIndex-1)][randomId(dataCountPerThread)];
								rankService.delete(rankName, id);
								if(isRedis){
									jedis.zrem(rankName,""+id);
								}
							}
						}
						
					}
					jedis.close();
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
		final Jedis jedis = pool.getResource();
		int testId=30;
		// �鿴��ֵ��������ڲ�ֵ�Ǻ��������������Ϊ���߳����и��£�hqrank��redis������ܲ�ͬ
		int num = 0;
		for (int[] is : ids) {
			for (int i : is) {
				if(getAndShowIfDiff(rankService,jedis,rankName,i,2)){
					num++;
				}
			}
		}
		log.info("num:"+num);
		// ͨ������һ��С��ֵ�鿴�����������������Ƿ���ͬ��
		// �����ͬ���������ɾ������ô���ɿ��ܵģ���������̫���¹�������ɾ��˵��������
		setValue(rankService, jedis, rankName, testId, 0, isRedis);
		RankData rankData = rankService.getRankDataById(rankName, testId);
		log.info("rankData:"+rankData);
		if(isRedis){
			Long jedisValue = jedis.zrevrank(rankName, ""+testId);
			log.info("redis:"+jedisValue);
			if(jedisValue!=rankData.getRankNum()){
				if(isDel){
					log.warn("jedisValue!=rankData.getRankNum()");
				}else{
					log.error("������ڴ���jedisValue!=rankData.getRankNum()");
				}
			}
		}
		
		jedis.del(rankName);
		pool.close();
	}
	
	public void test3(final IRankService rankService) throws InterruptedException{
		final int threadCount = 200;
		
		final String rankName1 = "rank_a";
		final String rankName2 = "rank_b";
		rankService.createRank(rankName1);
		rankService.createRank(rankName2, 3);
		final JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxTotal(threadCount);
		config.setMinIdle(threadCount);
		final JedisPool pool = new JedisPool(config, "192.168.1.240");
		// ɾ��֮ǰ�����ݣ�����ܴ��ڵ�����
		Jedis jedis = pool.getResource();
		jedis.del(rankName1);
		jedis.del(rankName2);
		
		final int intervalPerSet = 50; //  ÿintervalPerSet/2������ӻ��޸�һ�Σ�random.nextInt(intervalPerSet)
		final int maxId = 2000000;// id��Χ
		final Random random = new Random();
		final boolean isUseRank = true;
		final boolean isUseRedis = true;
		final boolean isUseRank2 = true;
		
		for(int i=0;i<threadCount;i++){
			Thread thread = new Thread(){
				@Override
				public void run(){
					Jedis jedis ;
					if(isUseRedis){
						jedis = pool.getResource();
					}
					while(true){
						long value = random.nextInt(100000000);
						long value1 = value/1000000l,
								value2 = value%1000000l/100,
								value3 = value%100l;
						int id = random.nextInt(maxId)+1;
						
						int interval = random.nextInt(intervalPerSet);
						if(isUseRedis){
							jedis.zadd(rankName1, value, ""+id);
						}
						
						if(isUseRank){
							rankService.put(rankName1, id, value);
						}
						if(isUseRank2){
							rankService.put(rankName2, id, value1,value2,value3);
						}
						try {
							Thread.sleep(interval);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			};
			thread.start();
		}
		// ÿ10���ӡһ�� ���
		new Thread(){
			@Override
			public void run(){
				Jedis jedis ;
				if(isUseRedis){
					jedis = pool.getResource();
				}
				int printTime = 0;
				while (true) {
					System.err.println("-----------------------"+(printTime++)+"------------------------");
					int baseValue = maxId/2;
					for(int i=0;i<10;i++){
						int id = baseValue+i;
						StringBuilder sb = new StringBuilder("---------------------------------------"+(id)+":");
						if(isUseRank){
							sb.append(rankService.getRankDataById(rankName1, id));
						}
						if(isUseRedis){
							sb.append(","+jedis.zrevrank(rankName1,""+(id)));
						}
						if(isUseRank2){
							sb.append(","+rankService.getRankDataById(rankName2, id));
						}
						System.out.println(sb.toString());
					}
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
		synchronized (this) {
			wait();
		}
	}
	
	private void setValue(IRankService rankService,Jedis jedis,
			String rankName,int id,long value,boolean isRedis){
		rankService.put(rankName, id, value);
		if(isRedis){
			jedis.zadd(rankName, value, ""+id);
		}
	}
	private void getAndShow(IRankService rankService,Jedis jedis,
			String rankName,int id,boolean isRedis){
		RankData rankData = rankService.getRankDataById(rankName, id);
		log.info("rankData:"+rankData);
		if(isRedis){
			log.info("redis:"+jedis.zrevrank(rankName, ""+id));
		}
	}
	/**
	 * ��ȡ����ʾ��ͬ
	 * @param rankService
	 * @param jedis
	 * @param rankName
	 * @param id
	 * @param D_value ��ֵ�������ڸò�ֵ��ʱ�����ʾ
	 * @return �Ƿ�ͬ
	 */
	private boolean getAndShowIfDiff(IRankService rankService,Jedis jedis,
			String rankName,int id,int D_value){
		RankData rankData = rankService.getRankDataById(rankName, id);
		Long jedisValue = jedis.zrevrank(rankName, ""+id);
		if((rankData!=null && jedisValue == null) || (rankData==null && jedisValue != null)){
			// �������˵���д���
			log.error(rankData+",-----------------------------"+jedisValue);
			return true;
		}
		if(rankData != null && jedisValue != null){
			if(rankData.getRankNum() != jedisValue){
				if(Math.abs(rankData.getRankNum()-jedisValue)>D_value){
					log.info(rankData.getRankNum()+","+jedisValue);
				}
				return true;
			}
		}
		return false;
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

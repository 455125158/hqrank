package org.hq.rank.core.node;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.hq.rank.core.Rank;
import org.hq.rank.core.element.Element;
import org.hq.rank.core.pool.RankPoolElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/***
 * Node��Ϊ���֣�һ���Ǵ洢���ݵģ����洢һ��element������һ���Ǵ洢һ��rank������ͬһ����������£�����
 * ��һ�ַ�����������
 * @author zhen
 *
 */
public abstract class Node extends AbNode implements INode,RankPoolElement{
	private static Logger log = LoggerFactory
			.getLogger(Node.class);
	protected final Rank rank; // ��node������rank
	// ��Node��Ӧ��ֵ
	protected long value;
	// Node�е�Element����
	protected volatile AtomicInteger elementCount = new AtomicInteger(0);
	/**
	 * �����ʱ����û�п��ܱ����ʣ�add����
	 * */
	public Node(Rank rank){
		this.rank = rank;
	}
	
	public void init(Element element,long value,final int conditionLevel){
		this.value = value;
		// ��һ��Node��ֵ��MAX_VALUE���Ǹ�����
		if(value != Long.MAX_VALUE){
			elementCount.getAndIncrement();
		}else{
			// ��һ���ֶ�Ĭ�Ͼʹ�����������
			if(conditionLevel == rank.getRankConfigure().getRankConditionCount() - 1){
				// ����������null��������Ͳ����и�����ˣ����򣬻��и���㣬�������д�����
				NodeStepBase nodeStepStep = rank.getRankPool().getNodeStepBase(null); 
				this.parentNS = rank.getRankPool().getNodeStepBase(nodeStepStep);
				this.parentNS.putAbNodeWithElement(this);
				nodeStepStep.putAbNode(this.parentNS); // ��һ���ͻ����������step��Ҫ�ӵ�elementCount�ļ�����
			}
		}
	}
	@Override
	public int getCount() {
		if(/*value == -1 || */value == Long.MAX_VALUE){
			return 0;
		}
		return elementCount.get();
	}
	public long getValue() {
		return value;
	}

	@Override
	public abstract Element add(Element element) ;
	
	@Override
	public abstract int getRankValue(Element element);
	/**
	 * �������ͨ��������һ��List<Element>��������new���Ĵ���
	 * @param begin
	 * @param length
	 * @return
	 */
	public abstract void getElementsByIndex(List<Element> elementList , int begin ,int length) ;
	@Override
	public abstract boolean delete(Element element) ;
	/**
	 * ��ס���Element����ʧ�ܣ�Ҫ����
	 * @param elements
	 * @return �Ƿ�ɹ���ס
	 */
	protected boolean lockMultipleElement(Element... elements){
		Element[] lockElements = new Element[elements.length];
		int i=0;
		for (Element element : elements) {
			if(element == null){
				i++;
				continue;
			}
			boolean isLock = element.lock();
			if(!isLock){
				for (int j = elements.length-1 ;j>=0 ;j-- ) {
					if(lockElements[j]!=null){
						lockElements[j].unLock();
					}
					// ����
				}
				// ���Է�����һ��ȥ
				return false;
			}
			lockElements[i++] = element;
		}
		return true;
	}
	/**
	 * �������Element
	 * @param elements
	 */
	protected void unLockMultipleElement(Element... elements){
		for (Element element : elements) {
			if(element == null){
				continue;
			}
			element.unLock();
		}
	}
	
	@Override
	public void reset() {
		if(elementCount.get() > 0){
			log.error("its not possible , on reset, elementCount.get() > 0:"
					+elementCount.get()+",conditionLevel:"+getConditionLevel());
		}
		
		value = -1;
		parentNS = null;
		// ���ﲻҪ����������Ϊ�����ʱ���������ٸ��������ǲ�ȷ����
//		locker.set(0);
		// Node�е�Element����
		elementCount.set(0);
		previous = null;
		next = null;
	}

	public abstract int getConditionLevel();
}

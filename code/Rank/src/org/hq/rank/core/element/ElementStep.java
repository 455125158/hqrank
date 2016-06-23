package org.hq.rank.core.element;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.hq.rank.core.Rank;
import org.hq.rank.core.node.ElementNode;
import org.hq.rank.core.node.Node;
import org.hq.rank.core.pool.RankPoolElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElementStep implements RankPoolElement{
	private static Logger log = LoggerFactory.getLogger(ElementStep.class);
	// 
	private final Rank rank;
	
	/**
	 * ������ֵ�����ù������£�
	 * 1��fullCount���Ϊnode���������Ŀ���*2������ò�ҪС��100��û��Ҫ
	 * 2��deleteCount���ΪfullCount/10������ò�ҪС��20��û��Ҫ
	 * **/
	private ElementNode node;
	
	private Lock locker = new ReentrantLock();
	
	private AtomicInteger count = new AtomicInteger(0);
	
	private Element head = null;
	
	private ElementStep previous;
	private ElementStep next;
	
	public ElementStep(final Rank rank){
		this.rank = rank;
	}
	public void init(Node node){
		this.node = (ElementNode)node;
	}
	
	public void putElement(Element element){
		if(this.head == null){
			this.head = element;
		}
		element.setStep(this);
		count.getAndIncrement();
	}
	// ������previous������ȫ�ֱ���������֮��Ҫ����У��
	private ElementStep lockPrevious(){
		ElementStep _previous = previous;
		if(_previous == null){
			return null;
		}
		_previous.lock();
		while(_previous != previous){
			_previous.unLock();
			_previous = previous;
			if(_previous == null){
				return null;
			}
			_previous.lock();
		}
		return _previous;
	}
	
	public void removeElement(Element element){
		//  ��ʱelement����ǰ������element�Ǳ���ס��
		count.getAndDecrement();
		if(this.head == element){
			this.head = element.getNext();
		}
		
		if(count.get() < rank.getRankConfigure().getCombineCountElementStep()){//deleteCount){
			if(this != node.getTailStep() && this !=node.getHeadStep()){
				// �ϲ�step
				if(lockPrevious() == null){
					return ;
				}
				lock();
				if(next != null){
					next.lock();
				}
				// 
				if(node == null || this == node.getTailStep() || this ==node.getHeadStep() 
						|| count.get() >= rank.getRankConfigure().getCombineCountElementStep()){//deleteCount){
					if(next != null){
						next.unLock();
					}
					unLock();
					previous.unLock();
					return;
				}
				rank.getRankStatistics().addElementStepCombineTime();
				
				
				previous.count.getAndAdd(count.get());
				previous.next = next;
				if(next != null){
					next.previous = previous;
				}
				// ��������Ч����deleteCountӰ��
				if(count.get()>0){
					Element cuElement = head;
					while(cuElement.getStep() == this){
						cuElement.setStep(previous);
						cuElement = cuElement.getNext();
					}
				}else{
					log.warn("why count.get() = "+count.get());
				}
				// �ϲ����
				ElementStep _previous = previous;
				ElementStep _next = next;
				rank.getRankPool().putElementStep(this);
				if(_next != null){
					_next.unLock();
				}
				unLock();
				_previous.unLock();
			}
		}
	}
	
	public int getRankValue(Element element){
		int rankNum = 0;
		Element currentElement = head;
		while(currentElement != null && currentElement != element){
			rankNum++;
			currentElement = currentElement.getNext();
		}
		if(currentElement == null){
			rankNum = -1;
		}
		return rankNum;
	}
	
	public void lock(){
		locker.lock();
	}
	public void unLock(){
		locker.unlock();
	}
	
	public int getCount(){
		return count.get();
	}

	public ElementStep getPrevious() {
		return previous;
	}
	public void setPrevious(ElementStep previous) {
		this.previous = previous;
	}
	public ElementStep getNext() {
		return next;
	}
	public void setNext(ElementStep next) {
		this.next = next;
	}
	public Element getHead() {
		return head;
	}
	@Override
	public void reset() {
		node = null;
		count.set(0);
		
		head = null;
		
		previous = null;
		next = null;
	}
}

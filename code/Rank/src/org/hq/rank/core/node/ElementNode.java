package org.hq.rank.core.node;

import java.util.List;

import org.hq.rank.core.Rank;
import org.hq.rank.core.RankException;
import org.hq.rank.core.element.Element;
import org.hq.rank.core.element.ElementStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElementNode extends Node{

	private static Logger log = LoggerFactory
			.getLogger(ElementNode.class);
	/**
	 * Element���͵�Node����
	 */
	private Element head;
	//Node��ElementStep������
	private ElementStep headStep , tailStep;
	private volatile Element tail;
	/**
	 * �����ʱ����û�п��ܱ����ʣ�add����
	 * */
	
	public ElementNode(Rank rank){
		super(rank);
	}
	@Override
	public void init(Element element,long value,final int conditionLevel){
		super.init(element, value, 0);
		
		if(value != Long.MAX_VALUE){ // ͷ����ʲôҲ��Ҫ��������ܳ����ڴ����
			element.setNode(this);
			this.head = element;
			this.tail = element;
		}
	}

	public ElementStep getHeadStep() {
		return headStep;
	}

	public ElementStep getTailStep() {
		return tailStep;
	}

	public Element getHead() {
		return head;
	}

	@Override
	public Element add(Element element) {
		boolean isLock = rank.getLockerPool().tryLockNodeRLocker(this, 0);
		if(!isLock){
			if(element == rank.getFailElement()){ // ---------------�����ô��룬����׷�ٴ���ջ-------------
				throw new RankException("reoper fail");
			}
			return null;
		}
		boolean isNeedUnlock = true;
		try{
			if(elementCount.get() <= 0){
				return null;
			}
			// ��ֹ�������̹߳�ͬ��ӻ����
			// ��tail lock�ĺ����и�����ִ��getAndIncrement֮ǰ��tailָ���޸ģ���ʱ��lock��element�Ͳ����µ�element
			Element _tail=tail;
			if(_tail == null){
				if(element == rank.getFailElement()){ // ---------------�����ô��룬����׷�ٴ���ջ-------------
					throw new RankException("reoper fail");
				}
				return null;
			}
			if(!_tail.lock()){
				if(element == rank.getFailElement()){ // ---------------�����ô��룬����׷�ٴ���ջ-------------
					throw new RankException("reoper fail");
				}
				return null;
			}
			if(_tail != tail){
				_tail.unLock();
				if(element == rank.getFailElement()){ // ---------------�����ô��룬����׷�ٴ���ջ-------------
					throw new RankException("reoper fail");
				}
				return null;
			}
			// ���ܱ��޸Ļ���ɾ�����������ڳ�������ȡ��֮��
			if(!tail.equalsValue(element)){
				_tail.unLock();
				if(element == rank.getFailElement()){ // ---------------�����ô��룬����׷�ٴ���ջ-------------
					throw new RankException("reoper fail");
				}
				return null;
			}
			
			
			if(tailStep == null && elementCount.get() > rank.getRankConfigure().getCutCountElementStep()){//ElementStep.fullCount){
				// ����֮��������ʱ���д����������ֹ�����̼߳����ݽ�ȥ
				rank.getLockerPool().unlockNodeRLocker(this, 0);
				isLock = rank.getLockerPool().tryLockNodeWLocker(this, 0);
				if(!isLock){
					isNeedUnlock = false;
					_tail.unLock();
					if(element == rank.getFailElement()){ // ---------------�����ô��룬����׷�ٴ���ջ-------------
						throw new RankException("reoper fail");
					}
					return null;
				}
				if(this.tailStep == null && elementCount.get() > rank.getRankConfigure().getCutCountElementStep()){//ElementStep.fullCount){ // �ٴ�У��
					this.headStep = rank.getRankPool().getElementStep(this);
					Element currentElement = head;
					while(currentElement != null){
						this.headStep.putElement(currentElement); 
						currentElement = currentElement.getNext();
					}
					this.tailStep = this.headStep;
				}
				rank.getLockerPool().unlockNodeWLocker(this, 0);
				isLock = rank.getLockerPool().tryLockNodeRLocker(this, 0);
				if(!isLock){
					isNeedUnlock = false;
					_tail.unLock();
					if(element == rank.getFailElement()){ // ---------------�����ô��룬����׷�ٴ���ջ-------------
						throw new RankException("reoper fail");
					}
					return null;
				}
			}
			if(tailStep != null){
				// ���
				if(tailStep.getCount() >= rank.getRankConfigure().getCutCountElementStep()){//ElementStep.fullCount){
					
					rank.getLockerPool().unlockNodeRLocker(this, 0);
					isLock = rank.getLockerPool().tryLockNodeWLocker(this, 0);
					if(!isLock){
						isNeedUnlock = false;
						_tail.unLock();
						if(element == rank.getFailElement()){ // ---------------�����ô��룬����׷�ٴ���ջ-------------
							throw new RankException("reoper fail");
						}
						return null;
					}
					if(tailStep.getCount() >= rank.getRankConfigure().getCutCountElementStep()){//ElementStep.fullCount){ // �ٴ�У��
						
						ElementStep newStep = rank.getRankPool().getElementStep(this);
						tailStep.putElement(element);
						newStep.setPrevious(tailStep);
						tailStep.setNext(newStep);
						tailStep = newStep;
					}
					rank.getLockerPool().unlockNodeWLocker(this, 0);
					isLock = rank.getLockerPool().tryLockNodeRLocker(this, 0);
					if(!isLock){
						isNeedUnlock = false;
						_tail.unLock();
						if(element == rank.getFailElement()){ // ---------------�����ô��룬����׷�ٴ���ջ-------------
							throw new RankException("reoper fail");
						}
						return null;
					}
				}else{
					tailStep.putElement(element);
				}
			}
			
			element.setPrevious(_tail); //Ҫ������������ټ������������ѯ��ز������ܻ����
			element.setNode(this);
			_tail.setNext(element);
			tail = element;
			
			elementCount.getAndIncrement();
			
//			if(nodeStep != null){
//				nodeStep.putElement(); // ��û�п��ܿ�ָ�룿������
//			}
			if(parentNS != null){
				parentNS.putElement(); // ��û�п��ܿ�ָ�룿������
			}
			_tail.unLock(); // ע�⣬ǰ���tail����ֵ��
			
			return element;
		}finally{
			if(isNeedUnlock){
				rank.getLockerPool().unlockNodeRLocker(this, 0);
			}
		}
	}
	
	@Override
	public int getRankValue(Element element) {
		int rankNum = 0;
		Element currentElement = head;
		ElementStep step = element.getStep();
		if(step != null){
			ElementStep currentStep = headStep;
			while(currentStep != null && currentStep != step){
				rankNum += currentStep.getCount();
				currentStep = currentStep.getNext();
			}
			
			if(currentStep == null){
				log.warn("currentStep is null");
				currentElement = head;
				rankNum = 0;
			}else{
				currentElement = step.getHead();
			}
		}
		
//		int localRankNum = step.getRankValue(element);
		
		while(currentElement != null && currentElement != element){
			rankNum++;
			currentElement = currentElement.getNext();
		}
		if(currentElement == null){
			log.warn("currentElement is null");
			return -1;
		}
		return rankNum;
	}
	/**
	 * �������ͨ��������һ��List<Element>��������new���Ĵ���
	 * @param begin
	 * @param length
	 * @return
	 */
	@Override
	public void getElementsByIndex(List<Element> elementList , int begin ,int length) {
		if(elementCount.get() <= begin || elementCount.get()<1){
			return;
		}
		Element currentElement = head;
		int currentIndex = 0;
		while(currentElement != null && currentIndex < begin+length){
			if(currentIndex >= begin){
				elementList.add(currentElement);
			}
			currentElement = currentElement.getNext();
			currentIndex++;
		}
		return;
	}

	@Override
	public boolean delete(Element element) {
		boolean _isLock = rank.getLockerPool().tryLockNodeRLocker(this, 0);
		if(!_isLock){
			return false;
		}
		try {
			// ���ʱ���ǲ�����������ɾ��node�ģ����Բ������node�Ķ���
			Element pre = element.getPrevious();
			Element next = element.getNext();
			boolean isLock = lockMultipleElement(pre,next);
			if(!isLock){
				return false;
			}
			// ��У��
			if(element.getPrevious() != pre || element.getNext()!=next){
				unLockMultipleElement(pre,next);
				return false;
			}
			if((pre !=null && pre.getNext() !=element) || (next !=null && next.getPrevious() !=element)){
				unLockMultipleElement(pre,next);
				return false;
			}
//			if(nodeStep != null){
//				nodeStep.removeElement();
//			}
			if(parentNS != null){
				parentNS.removeElement();
			}
			int c = elementCount.decrementAndGet();
			if(c <= 0){
				// ��ǰnode�����þͲ������������������߳̾Ͳ���������ֱ������ɾ��
				tail = null;
				unLockMultipleElement(pre,next);
				return true;
			}
			
			if(pre!=null){
				pre.setNext(next);
			}
			if(next != null){
				next.setPrevious(pre);
			}
			if(head == element){
				head = next;
			}
			if(tail == element){
				tail = pre;
			}
			
			ElementStep step = element.getStep();
			if(step != null){
				step.removeElement(element);
			}
			unLockMultipleElement(pre,next);
			return true;
		} finally {
			rank.getLockerPool().unlockNodeRLocker(this, 0);
		}
	}
	@Override
	public String toString(){
		if(value == Long.MAX_VALUE){
			return "head";
		}
		// step��������������ÿ��step�е�����
		int stepNum = 0;
		int elementNum = 0;
		ElementStep currentStep = headStep ;
		StringBuilder sb = new StringBuilder("(");
		while(currentStep != null){
			stepNum++;
			elementNum += currentStep.getCount();
			sb.append(currentStep.getCount()+",");
			currentStep = currentStep.getNext();
		}
		StringBuilder rSb = new StringBuilder();
		rSb.append("node(value:"+value+"):")
		.append("stepNum:"+stepNum)
		.append(",elementNum:("+elementCount+"=="+elementNum+")")
		.append(",steps:"+sb.toString()+")");
		return rSb.toString();
	}
	
	@Override
	public void reset() {
		super.reset();
		rank.getRankPool().putElementStep(headStep);
		headStep =null ;
		tailStep = null;
		tail = null;
		head = null; // �������ϵ�Ӧ�ò�����������~
	}
	@Override
	public int getConditionLevel() {
		return 0;
	}

}

package org.hq.rank.core.node;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.hq.rank.core.Rank;
import org.hq.rank.core.element.Element;
import org.hq.rank.core.pool.RankPoolElement;
import org.hq.rank.core.reoper.ReOper.OperType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RankElement implements RankPoolElement{
	private static Logger log = LoggerFactory
			.getLogger(RankElement.class);
	private final Rank rank;
	// ��֧�ָ���
	private Node head;
	//
	private int conditionLevel; // >0
	// ��ǰrankElement�е�node������������head
	private final AtomicInteger nodeCount = new AtomicInteger(0);
	
	public final String id ;
	public RankElement(final Rank rank){
		this.rank = rank;
		id = rank.getRankElementNodeMap().getNewId();
		// Node�Ĺ��췽���������жϣ���ʼ��node���ᴴ��rankElelment
		// ����ʹ���head��������������ʱ���ٴ���
		head = rank.getRankPool().getNode(Long.MAX_VALUE, 0,conditionLevel);
		// head����������
//		putTo(head.getValue(), head);
	}
	private void putTo(long value,Node node){
		rank.getRankElementNodeMap().put(this, value, node);
		nodeCount.getAndIncrement();
	}
	private void removeFrom(long value){
		rank.getRankElementNodeMap().remove(this, value);
		nodeCount.getAndDecrement();
	}
	private Node getFrom(long value){
		return rank.getRankElementNodeMap().get(this, value);
	}
	public boolean init(final Element element,final int conditionLevel){
		this.conditionLevel = conditionLevel;
		if(element.getId() >= 0){
			if(!add(element)){
				return false;
			}
		}else{
			System.err.println("buhuichuxian,id="+element.getId());
		}
		return true;
	}
	
	/**
	 * ��ɾ���ڽ��е�ʱ���������������ܽ���
	 * 1�� ��ɾ��һ��node��ʱ�򣬲�����������ӵ�
	 * */
	public boolean delete(final Element element) {
		if(element == null){
			return true;
		}
		return doDelete(element);
	}
	/**
	 * 
	 * @param element
	 * @return ���ص��Ǹ�Element�ڸ�rank�е����� -1����û���ҵ�
	 */
	public int get(Element element){
		long value = element.getValue()[rank.getRankConfigure().getRankConditionCount() - conditionLevel];
		Node node = getFrom(value);//nodeMap.get(value);
		if(node == null){
			return -1;
		}
		SearchNodeStepResult result = getStartNodeByNodeStep(value);
		Node currentNode = result.node;
		NodeStepBase nodeStep = currentNode.getParentNS();
		int rankNum = result.rankNum;
		if (currentNode.getValue()<value) {
			log.warn("currentNode.getValue()<value����˵����������֮���޸Ĺ�");
			currentNode = head;
		}
		
		while(currentNode != null && currentNode.getParentNS() == nodeStep){
			if(currentNode == node){
				int localRankNum = currentNode.getRankValue(element);
				if(localRankNum == -1){
					log.warn("currentNode.getRankValue = -1");
					return -1;
				}
				rankNum += localRankNum;
				break;
			}
			rankNum += currentNode.getCount();
			currentNode = (Node)currentNode.getNext();
		}
		if(currentNode == null || currentNode.getParentNS()!= nodeStep){
			log.warn("id "+element.getId()+" not exist:currentNode:"+currentNode+","+(currentNode.getParentNS() != nodeStep));
			return -1;
		}
		return rankNum;
	}
	public void getElementsByRankNum(List<Element> elementList,int begin, int length) {
		// ���ҵ�begin�����element�������element���ߺ���node��element
		int rankNum = 0;
		Node currentNode = head;
		NodeStepBase currentNodeStep = head.getParentNS();
		if(currentNodeStep != null){
			NodeStepBase currentnodeStepStep = currentNodeStep.getParentNS();
			while(currentnodeStepStep != null){
				int countNSS = currentnodeStepStep.getElementCount();
				if(rankNum+countNSS >= begin){
					currentNodeStep = (NodeStepBase)currentnodeStepStep.getHead();
					break;
				}
				rankNum += countNSS;
				currentnodeStepStep = (NodeStepBase)currentnodeStepStep.getNext();
			}
			if(currentnodeStepStep == null){
				log.warn("has no enough player on currentnodeStepStep");
				return;
			}
			
			while(currentNodeStep != null){
				int countNs = currentNodeStep.getElementCount();
				if(rankNum + countNs >= begin){
					currentNode = (Node)currentNodeStep.getHead();
					break;
				}
				rankNum += countNs;
				currentNodeStep = (NodeStepBase)currentNodeStep.getNext();
			}
			if(currentNodeStep == null){
				log.warn("has no enough player on currentNodeStep");
				return;
			}
		}
		
		while(currentNode != null){
			int countN = currentNode.getCount();
			if(rankNum + countN >= begin){
				int beginE = begin - rankNum;
				int lengthE =length;
				int baseSize = elementList.size();
				int currentGetNum = 0;
				Node currentNode2 = currentNode;
				while(currentGetNum < length && currentNode2 != null){
					currentNode2.getElementsByIndex(elementList,beginE,lengthE);
					beginE = 0;
					currentGetNum = elementList.size() - baseSize;
					lengthE = length - currentGetNum;
					currentNode2 = (Node)currentNode2.getNext();
				}
//				System.err.println(currentNode2+"rankElement");
				break;
			}
			rankNum += countN;
			currentNode = (Node)currentNode.getNext();
		}
		if(currentNode == null){
			log.warn("has no enough player on currentNode");
			return;
		}
		return ;
	}
	/**
	 * ���ﷵ�ص�Nodeһ���ǿ�����Ϊvalue��Ӧnode��ǰnode���п�����head
	 * */
	private SearchNodeStepResult getStartNodeByNodeStep(long value){
		int rankNum = 0;
		int currentHitTimes = 0;
		Node currentNode=head;
		// ͨ��nodestepstepѰ��Ū��step
		NodeStepBase currentNodeStep = head.getParentNS();
		if(currentNodeStep != null){
			NodeStepBase currentNodeStepStep = head.getParentNS().getParentNS();
			NodeStepBase previousNodeStepStep = null;
			while(currentNodeStepStep != null){
				if(currentNodeStepStep.getValue() < value){
					if(previousNodeStepStep != null && previousNodeStepStep.getValue() >= value){
						currentNodeStep = (NodeStepBase)previousNodeStepStep.getHead();
						if(currentNodeStep.getHead().getValue() < value){
							log.warn("ֻҪ����ط���������˵��������Ҳ���ֵ�����������ʹ�õ���Ӧ��node��Ҳ�ǲ��Ե�node1");
							rankNum = 0;
							currentNodeStep = head.getParentNS();
						}
						break;
					}else{
						rankNum = 0;
						currentNodeStep = head.getParentNS();
						currentNodeStepStep = head.getParentNS().getParentNS();
						previousNodeStepStep = null;
						if(currentHitTimes++>rank.getRankConfigure().getMaxHitTimesNodeStep()){
							log.warn("nodestepstep:currentHitTimes++>NodeStep.maxHitTimes:"+
									currentHitTimes+","+previousNodeStepStep);
							break;
						}
						Thread.yield();
						continue;
					}
				}else if(currentNodeStepStep.getNext() == null){ // ��������һ��
					if(previousNodeStepStep != null){
						rankNum += previousNodeStepStep.getElementCount();
					}
					currentNodeStep = (NodeStepBase)currentNodeStepStep.getHead();
					if(currentNodeStep.getHead().getValue() < value){
						log.warn("ֻҪ����ط���������˵��������Ҳ���ֵ�����������ʹ�õ���Ӧ��node��Ҳ�ǲ��Ե�node1");
						rankNum = 0;
						currentNodeStep = head.getParentNS();
					}
					break;
				}
				if(previousNodeStepStep != null){
					rankNum += previousNodeStepStep.getElementCount();
				}
				previousNodeStepStep = currentNodeStepStep;
				currentNodeStepStep = (NodeStepBase)currentNodeStepStep.getNext();
			}
			// ͨ��nodestepѰ��node
			currentHitTimes = 0;
			
			NodeStepBase previouNodeStep = null;
			while(currentNodeStep != null){
				if(currentNodeStep.getHead().getValue() < value){
					// �п��ܲ����У�����Ӧ��nodestep���ڲ�ֻ��ߺϲ���ʱ�򣬿��ܲ�����
					if(previouNodeStep!=null && previouNodeStep.getHead().getValue() >= value){
						currentNode=(Node)previouNodeStep.getHead(); // ����ط���������null
						if(currentNode.getValue()<value){
							log.warn("ֻҪ����ط���������˵��������Ҳ���ֵ�����������ʹ�õ���Ӧ��node��Ҳ�ǲ��Ե�node2");
							currentNode = head;
							rankNum = 0;
						}
						break;
					}else{ // û�����еĻ�����ȡ��ͳ�ķ�������ͷ��β
						rankNum = 0;
						currentNode=head;
						currentNodeStep = head.getParentNS();
						previouNodeStep = null;
						if(currentHitTimes++>rank.getRankConfigure().getMaxHitTimesNodeStep()){
							log.warn("nodestep:currentHitTimes++>NodeStep.maxHitTimes:"+currentHitTimes);
							break;
						}
						Thread.yield();
						continue;
					}
				}else if(currentNodeStep.getNext() == null){ // ��������һ��
					if(previouNodeStep != null){
						rankNum += previouNodeStep.getElementCount();
					}
					currentNode=(Node)currentNodeStep.getHead(); 
					if(currentNode.getValue()<value){
						log.warn("ֻҪ����ط���������˵��������Ҳ���ֵ�����������ʹ�õ���Ӧ��node��Ҳ�ǲ��Ե�node2");
						currentNode = head;
						rankNum = 0;
					}
					break;
				}
				if(previouNodeStep != null){
					rankNum += previouNodeStep.getElementCount();
				}
				previouNodeStep = currentNodeStep;
				currentNodeStep = (NodeStepBase)currentNodeStep.getNext();
			}
			//
			if(currentNodeStep == null){
				currentNode=(Node)previouNodeStep.getHead();
				if(previouNodeStep.getHead() == null || previouNodeStep.getHead().getValue() < value){
					log.warn("�����û�����У�����ʧ�ܣ���˵����������֮���޸Ĺ�");
					rankNum = 0;
					currentNode=head;
				}
			}
		}
		
		SearchNodeStepResult result = new SearchNodeStepResult();
		result.node = currentNode;
		result.rankNum = rankNum;
		return result;
	}
	/**
	 * ��Ϊһ������ֵ��
	 * @author a
	 *
	 */
	private static class SearchNodeStepResult{
		Node node;
		int rankNum;
	}
	/**
	 * �ҵ�λ��
	 * 1��û�ж�Ӧ��node
	 * ��������node
	 * ����У��
	 * ���������
	 * ����
	 * 2�����ڶ�Ӧ��node
	 * add�Ϳ���
	 * 
	 * ��η�ֹ�ظ����
	 * @param value :���value�Ǹò㼶������������value
	 * */
	public boolean add(Element element) {
		// ����conditionLevel��ȡ��Ӧ������ֵ
		long value = element.getValue()[rank.getRankConfigure().getRankConditionCount() - conditionLevel];
		Node valueNode = getFrom(value);//nodeMap.get(value);
		if(valueNode != null){
			element = valueNode.add(element);
			return element != null;
		}
		SearchNodeStepResult result = getStartNodeByNodeStep(value);
		
		Node currentNode = result.node;
		Node previousNode = (Node)currentNode.getPrevious();
		Node prePreNode ;
		int level = conditionLevel-1; // �����õ���node����������һ���
		if (currentNode.getValue()<value) {
			log.warn("currentNode.getValue()<value����˵����������֮���޸Ĺ�");
			currentNode = head;
		} 
		while(currentNode != null){
			if(currentNode.getValue()>value){
				prePreNode = previousNode;
				previousNode = currentNode;
				currentNode = (Node)currentNode.getNext();
				if(currentNode == null){
					// ɾ����ʱ����ҪУ���lock
					Node node = rank.getRankPool().getNode(element,value,level);
					// ��������һ���߳�������ط�ִ�е�add����������ס֮�󣬻�Ҫ����һ��У��
					boolean isLock = lockMultipleNode(level,previousNode,node);
					if(!isLock){	// ����Ҫ��Ҫ��node�Żس���?
						return false;
					}
					// �ٴ�У��:��������������������Ϳ��Ե�
					if(previousNode.getNext()!=null || (previousNode.getPrevious() != prePreNode) || 
							(previousNode != head && prePreNode.getNext() != previousNode)){
						unLockMultipleNode(level,previousNode,node);
						return false;
					}
					
					addToNodeLinkedList(previousNode, node, currentNode);
					unLockMultipleNode(level,previousNode,node);
					return true;
				}
			}else if(currentNode.getValue() == value){ // ����ߵ����˵����node�������У�����֮ǰû����map��
				element = currentNode.add(element);
				return element != null;
			}else{
				Node node = rank.getRankPool().getNode(element,value,level);
				boolean isLock = lockMultipleNode(level,previousNode,node,currentNode);
				if(!isLock){
					return false;
				}
				// �ٴ�У��ǰ�к��ϵ
				if(previousNode.getNext()!=currentNode 
						|| currentNode.getPrevious()!=previousNode){
					unLockMultipleNode(level,previousNode,node,currentNode);
					return false;
				}
				addToNodeLinkedList(previousNode, node, currentNode);
				unLockMultipleNode(level,previousNode,node,currentNode);
				return true;
			}
		}
		return false;
	}
	
	private void addToNodeLinkedList(Node previous,Node node,Node next){
		node.setNext(next); // ע�⣬�����˳�������⣬���߶��̲߳�ѯ�����
		NodeStepBase nodeStep = previous.getParentNS(); // ��Ҫʱͬһ��nodestep�����ң�Ҫ�ڼ�������֮ǰ���ã���������֮����ӵ�nodeStep
		// �жϲ�����nodestep
		if(nodeStep == null && nodeCount.get()>rank.getRankConfigure().getCutCountNodeStep()){//nodeMap.size() > rank.getRankConfigure().getCutCountNodeStep()){
			rank.getLockerPool().lockRankElementWLocker(this, conditionLevel);
			// ����֮��У��
			nodeStep = previous.getParentNS();
			if(nodeStep == null){
				// ���ﴫ��null��ȷ��������߲�
				NodeStepBase nodeStepStep = rank.getRankPool().getNodeStepBase(null);
				
				NodeStepBase newNodeStep = rank.getRankPool().getNodeStepBase(nodeStepStep);
//				nodeStepStep.putNodeStep(newNodeStep);
				nodeStepStep.putAbNode(newNodeStep);
				
				newNodeStep.setHead(head);
				Node currentNode = head;
				
				while(currentNode != null){
//					newNodeStep.putAbNode(currentNode);
					newNodeStep.putAbNodeWithElement(currentNode);
					currentNode.setParentNS(newNodeStep);
					currentNode = (Node)currentNode.getNext();
				}
			}
			rank.getLockerPool().unLockRankElementWLocker(this, conditionLevel);
		}
		rank.getLockerPool().lockRankElementRLocker(this, conditionLevel);
		nodeStep = previous.getParentNS();
		if(nodeStep != null){
			if(nodeStep.cutBeforePut()){
				nodeStep = previous.getParentNS();
			}
			nodeStep.getReadLock().lock();
			while(nodeStep != previous.getParentNS()){ // ��У��
				nodeStep.getReadLock().unlock();
				nodeStep = previous.getParentNS();
				nodeStep.getReadLock().lock();
			}
			node.setParentNS(nodeStep);
		}
		
		if(previous != null){ // ��ʵ�����һ���У���Ϊͷ����һ��Long.MAX_VALUE
			node.setPrevious(previous);
			previous.setNext(node);
		}
		if(next != null){
			next.setPrevious(node);
		}
		putTo(node.getValue(), node);
		if(nodeStep != null){
//			nodeStep.putAbNode(node);
			nodeStep.putAbNodeWithElement(node);
			nodeStep.getReadLock().unlock();
		}
		rank.getLockerPool().unLockRankElementRLocker(this, conditionLevel);
	}
	
	/**
	 * ���ɾ���ɹ�������node����element���򷵻سɹ������ߣ��Ż�ʧ��
	 * */
	public boolean deleteNode(Node node){
		boolean success = doDeleteNode(node);
		if(!success){
			return rank.getReOperService().addQueue(OperType.RankElementDeleteNode, 0, node,this);
		}
		return true;
	}
	/**
	 * ���ɾ���ɹ�������node����element���򷵻سɹ������ߣ��Ż�ʧ��
	 * */
	public boolean doDeleteNode(Node node){
		if(node.getCount() <= 0){
			int level = conditionLevel -1;
			
			Node pre = (Node)node.getPrevious();
			Node next = (Node)node.getNext();
			// �����ᵼ�ºܶ�reoper������ڼ�����ɵ�һ��֮�󣬾��ж��Ƿ���Ҫ������
			boolean isLock = lockMultipleNode(level,pre,node,next); 
			if(!isLock){
				return false;
			}
			if(node.getCount() > 0){ // ˵����ɾ�������У������µ���ӽ�ȥ�������Ͳ�ɾ������
				unLockMultipleNode(level,pre,node,next);
				return true;
			}
			
			// ��У��
			if(pre != node.getPrevious() || next != node.getNext()){ // ������У��Ƿ��������µ�node��ɾ��
				unLockMultipleNode(level,pre,node,next);
				return false;
			}
			
			if(pre.getNext() != node){ // ˵������һ���߳�ɾ������
				unLockMultipleNode(level,pre,node,next);
				return true;
			}
			if(next != null && next.getPrevious() != node){
				log.error("��Ȼnode��ǰ������ȷ��pre�����Ǻ���ȷʵ����ȷ��next�������������һ���߳���ɾ��������������������ܣ�����������δ֪����");
				unLockMultipleNode(level,pre,node,next);
				return true;
			}
			// ��Ϊ�Ƕ�̬�����ģ����ԡ���������Ҫ�Ӷ���
			rank.getLockerPool().lockRankElementRLocker(this, level+1);
			NodeStepBase nodeStep = node.getParentNS();
			if(nodeStep != null){
				if(nodeStep.combineBeforeRemove()){ // Ҫ�ȴ�������ٴ��������Ƴ�����Ϊ�������и��ݼ��������е���ش������Ƴ��������ݼ���
					nodeStep = node.getParentNS();
				}
				nodeStep.getReadLock().lock();
				while(nodeStep != node.getParentNS()){
					nodeStep.getReadLock().unlock();
					nodeStep = node.getParentNS();
					nodeStep.getReadLock().lock();
				}
			}
			
			// �ȴ������Ƴ�����ֹ���Ƴ������У����������Ԫ��
			if(pre != null){
				pre.setNext(next);
			}
			if(next != null){
				next.setPrevious(pre);
			}
			// �������۳ɹ����Ѿ�ɾ������
			removeFrom(node.getValue());
			
			
			if(nodeStep != null){
				nodeStep.removeAbNode(node);
				nodeStep.getReadLock().unlock();
			}
			rank.getRankPool().putNode(node); 
			rank.getLockerPool().unLockRankElementRLocker(this, level+1);
			unLockMultipleNode(level,pre,node,next);
			
			return true;
		}
		return true;
	}
	
	/**
	 * ��ɾ���ڽ��е�ʱ���������������ܽ���
	 * 1�� ��ɾ��һ��node��ʱ�򣬲�����������ӵ�
	 * */
	private boolean doDelete(Element element) {
		Node node = getFrom(element.getValue()[rank.getRankConfigure().getRankConditionCount() - conditionLevel]);//nodeMap.get(element.getValue()[rank.getRankConfigure().getRankConditionCount() - conditionLevel]);
		if(node == null){
			return true;
		}
		boolean success = node.delete(element);
		if(!success){
			return false;
		}else{
			// �п���ɾ��node
			if(node.getCount() <= 0){
				deleteNode(node);
			}
			return true;
		}
	}
	
	private boolean lockMultipleNode(int level,Node... nodes){
		Node[] lockNodes = new Node[nodes.length];
		int i=0;
		for (Node node : nodes) {
			if(node == null){
				continue;
			}
			boolean isLock = rank.getLockerPool().tryLockNodeWLocker(node, level);
			if(!isLock){
				for (int j=nodes.length-1;j>=0;j--) {
					if(lockNodes[j]!=null){
						rank.getLockerPool().unlockNodeWLocker(lockNodes[j], level);
					}
					// ����
				}
				// ���Է�����һ��ȥ
				return false;
			}
			lockNodes[i++] = node;
		}
		
		return true;
	}
	
	private void unLockMultipleNode(int level, Node... nodes){
		for (Node node : nodes) {
			if(node == null){
				continue;
			}
			rank.getLockerPool().unlockNodeWLocker(node, level);
		}
	}

	public Node getHead() {
		return head;
	}
	public int getConditionLevel() {
		return conditionLevel;
	}
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder("nodeCount:"+nodeCount.get()+"\n");
		Node currentNode = (Node)head.getNext();
		while(currentNode != null){
			sb.append(currentNode.toString()+"\n");
			currentNode = (Node)currentNode.getNext();
		}
		return sb.toString();
	}
	public int getNodeCount(){
		return nodeCount.get();
	}
	@Override
	public void reset() {
//		head = null;
		head.setNext(null); // headҪ����
		head.setParentNS(null);
		//
//		����ط�������˵��deletenodeʧ�ܣ����ǣ����ϵ�nodeɾ���ɹ���resetʱ���õ�����
		if(nodeCount.get() > 0){
			System.err.println("its not possible :"+nodeCount.get()+","+conditionLevel);
		}
		conditionLevel = -1; // >0
		nodeCount.set(0);
	}
}

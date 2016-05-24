package org.hq.rank.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.hq.rank.core.element.Element;
import org.hq.rank.core.node.AbNode;
import org.hq.rank.core.node.Node;
import org.hq.rank.core.node.NodeStepBase;
import org.hq.rank.core.node.SearchAbNodeResult;
import org.hq.rank.core.pool.ILockerPool;
import org.hq.rank.core.pool.LockerPool;
import org.hq.rank.core.pool.RankElementNodeMap;
import org.hq.rank.core.pool.RankPool;
import org.hq.rank.core.reoper.ReOper;
import org.hq.rank.core.reoper.ReOper.OperType;
import org.hq.rank.core.reoper.ReOperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Rank implements IRank {
	private static Logger log = LoggerFactory
			.getLogger(Rank.class);
	//
	private final RankElementNodeMap rankElementNodeMap;
	// lockerPool
	private final ILockerPool lockerPool;
	// rankpool
	private final RankPool rankPool;
	// ��rank������
	private final RankConfigure rankConfigure;
	// ��Rank��ͳ����Ϣ
	private RankStatistics rankStatistics = new RankStatistics(this);
	// ��֧�ָ���
	private final Node head;
	// �洢���е�Element ���.sizeЧ�ʵͣ��͵����洢������
	private ConcurrentHashMap<Integer, Element> elementMap=new ConcurrentHashMap<Integer, Element>();
	// �����������Ϊ�������ʱ��element����������node�����Ƚ϶��ʱ����Դ�����Ч��
	private ConcurrentHashMap<Long, Node> nodeMap = new ConcurrentHashMap<Long, Node>();
	
	// reoper���
	private final ReOperService reOperService;
//	// ����
	private final int maxHitTimesNodeStep;
	private final int maxHitTimesNodeStepStep;
	
	public Rank(){
		this(new RankConfigure());
	}
	public Rank(final RankConfigure rankConfigure){
		try {
			if(rankConfigure == null){
				this.rankConfigure = new RankConfigure();
			}else{
				this.rankConfigure = rankConfigure;
			}
			rankElementNodeMap = new RankElementNodeMap(this);
			lockerPool = new LockerPool(this);
			rankPool = new RankPool(this);
			if(!rankConfigure.check()){
				throw new RankException("rankConfigure is unavailable!"+rankConfigure);
			}
			reOperService = new ReOperService(this); // ��������г�ʼ�������в�����ʼ������������
			// ��ʼ��
			maxHitTimesNodeStep = rankConfigure.getMaxHitTimesNodeStep();
			maxHitTimesNodeStepStep = rankConfigure.getMaxHitTimesNodeStepStep();
			// Node�Ĺ��췽���������жϣ���ʼ��node���ᴴ��rankElelment
			head = rankPool.getNode(Long.MAX_VALUE, 0,rankConfigure.getRankConditionCount()-1);
			nodeMap.put(head.getValue(), head);
		} catch (Exception e) {
			try {
				destory();
			} catch (InterruptedException e2) {
				throw new RankException("rank �������� , ���ٴ���");
			}
			throw new RankException("rank ��������");
		}
	}
	
	/**
	 * ��ɾ���ڽ��е�ʱ���������������ܽ���
	 * 1�� ��ɾ��һ��node��ʱ�򣬲�����������ӵ�
	 * */
	@Override
	public long[] delete(int id) {
		rankStatistics.addDeleteCount();
//		Element element = elementMap.get(id);
		Element element = elementMap.remove(id);
		if(element == null){
			return null;
		}
		// ����ط���һ���ɹ����������޸�
		boolean success = doDelete(element);
		long[] result = element.getValue();
		if(!success){
			if(reOperService.addQueue(element, OperType.Delete, 0, null)){
				return result;
			}
			throw new RankException("rank exception , addQueue fail");
		}
		return result;
	}
	@Override
	public boolean has(int id) {
		return elementMap.containsKey(id);
	}
	@Override
	public long set(int id, long value) {
		long[] result = set(id, new long[]{value});
		return result == null ? -1 : result[0];
	}
	/**
	 * ����element��û������ӣ��������
	 * **/
	@Override
	public long[] set(int id, long... value) {
		if(id < 0){
			log.error("id requird >= 0");
			throw new RankException("id requird >= 0");
		}
		rankStatistics.addSetCount();
		if(value == null || value.length != rankConfigure.getRankConditionCount()){
			throw new RankException("value is error!");
		}
		Element element = rankPool.getElement(id, value);
		while(!element.lock()){
			element = rankPool.getElement(id, value);
//			System.err.println("error+++++++++++++++++++++++++++++++++++++");
		}
		//Element oldElement = elementMap.putIfAbsent(id, element);
		// �ڽ�ȥ֮ǰ�Ϳ��Խ�����ס����ֹ��������,putIfAbsent������
		Element oldElement = elementMap.put(id,element);
		if(oldElement != null){
			if(!doUpdate(oldElement, element)){
				if(reOperService.addQueue(element, OperType.Update, 0, null, oldElement,null)){
					return oldElement.getValue();
				}
				elementMap.put(id, oldElement);
				throw new RankException("rank exception,addQueue fail");
			}
			return oldElement.getValue();
		}else{
			if(doAdd(element/*, value*/)){
				element.unLock();
				return null;
			}else{
				if(reOperService.addQueue(element, OperType.Add, 0, null)){
					return null;
				}
				throw new RankException("rank exception,addQueue fail");
			}
		}
	}

	@Override
	public RankData get(int id) {
		rankStatistics.addGetCount();
		Element element = elementMap.get(id);
		if(element == null || element.getNode() == null){
			return null;
		}
		int maxGetTimes = 5;
		int times = 0;
		while(times++<maxGetTimes){
			RankData rankData = doGet(element);
			if(rankData != null){
				return rankData;
			}
			// �����һ�λ�ȡʧ�ܣ�������ʱ��Ƭ����Ϊ���п������ںͱ���߳�������Դ
			Thread.yield();
		}
//		System.err.println("get null");
		return null;
	}
	/**
	 * �������е����ʻ�ȡ��������
	 */
	@Override
	public List<RankData> getRankDatasByRankNum(int begin, int length) {
		if(begin >= elementMap.size()){
			log.warn("has no enough player: begin:"+begin+",elementMapSize:"+elementMap.size());
			return null;
		}
		// ���ҵ�begin�����element�������element���ߺ���node��element
		int rankNum = 0;
		NodeStepBase currentnodeStepStep = head.getParentNS().getParentNS();
		NodeStepBase currentNodeStep = head.getParentNS();
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
			return null;
		}
		Node currentNode = head;
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
			return null;
		}
		List<Element> elementList = new ArrayList<Element>(length);
		System.err.println(currentNode.getValue());
		while(currentNode != null){
			int countN = currentNode.getCount();
			if(rankNum + countN >= begin){
				int beginE = begin - rankNum;
				int lengthE =length;
				Node currentNode2 = currentNode;
				while(elementList.size() < length && currentNode2 != null){
					currentNode2.getElementsByIndex(elementList,beginE,lengthE);
					beginE = 0;
					lengthE = length - elementList.size();
					currentNode2 = (Node)currentNode2.getNext();
				}
				break;
			}
			rankNum += countN;
			currentNode = (Node)currentNode.getNext();
		}
		if(currentNode == null){
			log.warn("has no enough player on currentNode");
			return null;
		}
		List<RankData> result = new ArrayList<RankData>(elementList.size());
		int count = 0;
		for (Element element : elementList) {
			RankData rankData = new RankData();
			rankData.setId(element.getId());
			rankData.setRankNum(begin+count++);
			rankData.setValue(element.getValue());
			result.add(rankData);
		}
		return result;
	}
	
	private RankData doGet(Element element){
		// ��Ҫ��ס�Լ������������ʱ���ı䣨�޸Ļ�ɾ�����Ļ����ͻ�����Ҳ��������
		if(!element.lock()){
//			log.warn("!element.lock(),id:"+element.getId());
			return null;
		}
		long value = element.getValue()[0];
		Node node = nodeMap.get(value);
		if(node == null){
			log.warn("node == null");
			element.unLock();
			return null;
		}
		SearchAbNodeResult result = getStartNodeByNodeStep(value);
		Node currentNode = (Node)result.getNode();
		NodeStepBase nodeStep = currentNode.getParentNS();
		int rankNum = result.getRankNum();
		if (currentNode.getValue()<value) {
			log.warn("currentNode.getValue()<value����˵����������֮���޸Ĺ�");
			currentNode = head;
		}
		
		while(currentNode != null && currentNode.getParentNS() == nodeStep){
			if(currentNode == node){
				int localRankNum = currentNode.getRankValue(element);
				if(localRankNum == -1){
					element.unLock();
					log.warn("currentNode.getRankValue = -1:"+element);
					return null;
				}
				rankNum += localRankNum;
				break;
			}
			rankNum += currentNode.getCount();
			currentNode = (Node)currentNode.getNext();
		}
		if(currentNode == null || currentNode.getParentNS() != nodeStep){
			log.warn("id "+element.getId()+" not exist:currentNode:"+currentNode);
			element.unLock();
			return null;
		}
		
		RankData rankData = new RankData();
		rankData.setId(element.getId());
		rankData.setRankNum(rankNum);
		rankData.setValue(element.getValue());
		element.unLock();
		return rankData;
	}
	/**
	 * ��ȡ������nodeStep
	 * @return
	 */
	private NodeStepBase getHeadNodesStep(){
		NodeStepBase currentNodeStep = head.getParentNS();
		NodeStepBase result = currentNodeStep; 
		while(currentNodeStep != null){
			result = currentNodeStep;
			currentNodeStep = head.getParentNS();
		}
		return result;
	}
	private SearchAbNodeResult getStartNodeByValue(long value,NodeStepBase nodeStep){
		int rankNum = 0;
		int currentHitTimes = 0;
		// ͨ��nodestepstepѰ��Ū��step
		AbNode currentNode = nodeStep.getHead();
		NodeStepBase currentNodeStep = nodeStep;
		NodeStepBase previousNodeStep = null;
		while(currentNodeStep != null){
			if(currentNodeStep.getValue() < value){
//				System.err.println("currentNodeStepStep.getValue():"+currentNodeStepStep.getValue()+",value:"+value);
				if(previousNodeStep != null && previousNodeStep.getValue() >= value){
					currentNode = (NodeStepBase)previousNodeStep.getHead();
					if(currentNode.getValue() < value){
						log.warn("ֻҪ����ط���������˵��������Ҳ���ֵ�����������ʹ�õ���Ӧ��node��Ҳ�ǲ��Ե�node1");
						rankNum = -1;
						currentNode = null;
					}
					break;
				}else{
					rankNum = 0;
					currentNode = nodeStep.getHead();
					currentNodeStep = nodeStep;
					previousNodeStep = null;
					if(currentHitTimes++>maxHitTimesNodeStepStep){
						log.warn("nodestepstep:currentHitTimes++>NodeStep.maxHitTimes:"+
								currentHitTimes+","+previousNodeStep);
						rankStatistics.addFialHitByNodeStepStep();
						break;
					}
					Thread.yield();
					continue;
				}
			}else if(currentNodeStep.getNext() == null){ // ��������һ��
				if(previousNodeStep != null){
					rankNum += previousNodeStep.getElementCount();
				}
				currentNode = currentNodeStep.getHead();
				if(currentNode.getValue() < value){
					log.warn("ֻҪ����ط���������˵��������Ҳ���ֵ�����������ʹ�õ���Ӧ��node��Ҳ�ǲ��Ե�node1");
					rankNum = -1;
					currentNode = null;
				}
				break;
			}
			if(previousNodeStep != null){
				rankNum += previousNodeStep.getElementCount();
			}
			previousNodeStep = currentNodeStep;
			currentNodeStep = (NodeStepBase)currentNodeStep.getNext();
		}
		return new SearchAbNodeResult(currentNode, rankNum);
	}
//	private SearchAbNodeResult getStartNodeByNodeStep2(long value){
//		
//	}
	/**
	 * ���ﷵ�ص�Nodeһ���ǿ�����Ϊvalue��Ӧnode��ǰnode���п�����head
	 * {@code SearchNodeStepResult}
	 * */
	private SearchAbNodeResult getStartNodeByNodeStep(long value){
		int rankNum = 0;
		int currentHitTimes = 0;
		// ͨ��nodestepstepѰ��Ū��step
		NodeStepBase currentNodeStep = head.getParentNS();
		NodeStepBase currentNodeStepStep = head.getParentNS().getParentNS();
		NodeStepBase previousNodeStepStep = null;
		while(currentNodeStepStep != null){
			if(currentNodeStepStep.getValue() < value){
//				System.err.println("currentNodeStepStep.getValue():"+currentNodeStepStep.getValue()+",value:"+value);
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
					if(currentHitTimes++>maxHitTimesNodeStepStep){
						log.warn("nodestepstep:currentHitTimes++>NodeStep.maxHitTimes:"+
								currentHitTimes+","+previousNodeStepStep);
						rankStatistics.addFialHitByNodeStepStep();
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
		Node currentNode=head;
		NodeStepBase previouNodeStep = (NodeStepBase)currentNodeStep.getPrevious(); //null;
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
					if(currentHitTimes++>maxHitTimesNodeStep){
						log.warn("nodestep:currentHitTimes++>NodeStep.maxHitTimes:"+currentHitTimes);
						rankStatistics.addFialHitByNodeStep();
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
			if(previouNodeStep.getHead().getValue() < value){
				log.warn("�����û�����У�����ʧ�ܣ���˵����������֮���޸Ĺ�");
				rankNum = 0;
				currentNode=head;
			}
		}
		SearchAbNodeResult result = new SearchAbNodeResult();
		result.setNode(currentNode);
		result.setRankNum(rankNum);
		return result;
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
	 * 
	 * */
	private boolean doAdd(Element element) {
		long[] _value = element.getValue();
		long value = _value[0];
		Node valueNode = nodeMap.get(value);
		if(valueNode != null){
			element = valueNode.add(element);
			return element != null;
		}
		SearchAbNodeResult result = getStartNodeByNodeStep(value);
		Node currentNode = (Node)result.getNode();
		Node previousNode = (Node)currentNode.getPrevious();
		Node prePreNode ;
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
					Node node = rankPool.getNode(element,value,rankConfigure.getRankConditionCount()-1);
					// ��������һ���߳�������ط�ִ�е�add����������ס֮�󣬻�Ҫ����һ��У��
					boolean isLock = lockMultipleNode(previousNode,node);
					if(!isLock){
						return false;
					}
					// �ٴ�У�飬����ط���Ҫ��һ���У�飬�����п���previous�Ǵ���ģ�
					if(previousNode.getNext()!=null || (previousNode.getPrevious() != prePreNode) || 
							(previousNode != head && prePreNode.getNext() != previousNode)){
						unLockMultipleNode(previousNode,node);
						return false;
					}
					addToNodeLinkedList(previousNode, node, currentNode);
					unLockMultipleNode(previousNode,node);
					return true;
				}
			}else if(currentNode.getValue() == value){
				element = currentNode.add(element);
				return element != null;
			}else{
				Node node = rankPool.getNode(element,value,rankConfigure.getRankConditionCount()-1);
				boolean isLock = lockMultipleNode(previousNode,node,currentNode);
				if(!isLock){
					return false;
				}
				// �ٴ�У��ǰ�к��ϵ
				if(previousNode.getNext()!=currentNode 
						|| currentNode.getPrevious()!=previousNode){
					unLockMultipleNode(previousNode,node,currentNode);
					return false;
				}
				addToNodeLinkedList(previousNode, node, currentNode);
				unLockMultipleNode(previousNode,node,currentNode);
				return true;
			}
		}
		
		return false;
	}
	
	private void addToNodeLinkedList(Node previous,Node node,Node next){
		node.setNext(next); // ע�⣬�����˳�������⣬���߶��̲߳�ѯ�����
		NodeStepBase nodeStep = previous.getParentNS(); // ��Ҫʱͬһ��nodestep�����ң�Ҫ�ڼ�������֮ǰ���ã���������֮����ӵ�nodeStep
		if(nodeStep == null){
			System.err.println(previous.getValue());
		}
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
		if(previous != null){ // ��ʵ�����һ���У���Ϊͷ����һ��Long.MAX_VALUE
			node.setPrevious(previous);
			previous.setNext(node);
		}
		if(next != null){
			next.setPrevious(node);
		}
		nodeMap.put(node.getValue(), node);
//		nodeStep.putAbNode(node);
		nodeStep.putAbNodeWithElement(node);
		nodeStep.getReadLock().unlock();
	}
	
	/**
	 * ���ɾ���ɹ�������node����element���򷵻سɹ������ߣ��Ż�ʧ��
	 * */
	public boolean deleteNode(Node node){
		boolean success = doDeleteNode(node);
		if(!success){
			return reOperService.addQueue(null,OperType.DeleteNode, 0, node);
		}
		return true;
	}
	/**
	 * ���ɾ���ɹ�������node����element���򷵻سɹ������ߣ��Ż�ʧ��
	 * */
	private boolean doDeleteNode(Node node){
		if(node.getCount() <= 0){
			// ��ɾ��֮ǰ��isexist�Ѿ���false�ˣ���������delete�����õģ���������add���ܲ��еģ������ò��Ŷ�д����
			Node pre = (Node)node.getPrevious();
			Node next = (Node)node.getNext();
			// �����ᵼ�ºܶ�reoper������ڼ�����ɵ�һ��֮�󣬾��ж��Ƿ���Ҫ������
			boolean isLock = lockMultipleNode(pre,node,next); 
			if(!isLock){
				return false;
			}
			// ��У��
			if((pre != node.getPrevious() || ( pre != null && pre.getNext() != node)) || 
				(next != node.getNext() || (next != null && next.getPrevious()!=node))){
				unLockMultipleNode(pre,node,next);
				return false;
			}
			
			
			NodeStepBase nodeStep = node.getParentNS();
			if(nodeStep.combineBeforeRemove()){ // Ҫ�ȴ�������ٴ��������Ƴ�����Ϊ�������и��ݼ��������е���ش������Ƴ��������ݼ���
				nodeStep = node.getParentNS();
			}
			nodeStep.getReadLock().lock();
			while(nodeStep != node.getParentNS()){
				nodeStep.getReadLock().unlock();
				nodeStep = node.getParentNS();
				nodeStep.getReadLock().lock();
			}
			// �ȴ������Ƴ�����ֹ���Ƴ������У����������Ԫ��
			if(pre != null){
				pre.setNext(next);
			}
			if(next != null){
				next.setPrevious(pre);
			}
			// �������۳ɹ����Ѿ�ɾ������
			nodeMap.remove(node.getValue(),node);
			
			node.getParentNS().removeAbNode(node);
			rankPool.putNode(node); 
			nodeStep.getReadLock().unlock();
			
			unLockMultipleNode(pre,node,next);
			
			return true;
		}
		return true;
	}
	
	/**
	 * ��ɾ���ڽ��е�ʱ���������������ܽ���
	 * 1�� ��ɾ��һ��node��ʱ�򣬲�����������ӵ�
	 * */
	private boolean doDelete(Element element) {
		if(!element.lock()){
			return false;
		}
		Node node = nodeMap.get(element.getValue()
				[0]);
		if(node == null){
			elementMap.remove(element.getId(), element); // ����ط��ܹؼ�������������ɾ��
			rankPool.putElement(element); // ����֮ǰ����pool
			element.unLock();
			
			return true;
		}
		if(node.getValue() == Long.MAX_VALUE){
			log.error("delete head , value is too big");
		}
		boolean success = node.delete(element);
		if(!success){
			element.unLock();
			return false;
		}else{
			// �п���ɾ��node
			if(node.getCount() <= 0){
				deleteNode(node);
			}
			// ������ԭ����element����ɾ�������ұ�����ԭ�ӵ�
			elementMap.remove(element.getId(), element);
			rankPool.putElement(element); // ����֮ǰ����pool
			element.unLock();
			return true;
		}
	}
	/**
	 * update ʵ�е�����ɾ������ӣ���ԭ�ӵģ�������ȷ������������ȷ
	 * ��������Ż�
	 * */
	private boolean doUpdate(Element oldElement,Element element){
		if(doDelete(oldElement)){
			if(doAdd(element/*, value*/)){
				element.unLock();
				return true;
			}else{
				if(reOperService.addQueue(element,OperType.Add, 0, null)){
					return true;
				}
			}
		}else{
			return false;
		}
		// ����ط���return false������������update
		log.warn("����ط���return false������������update����һ�㲻�ᷢ��");
		return false;
	}
	public boolean doReOper_(ReOper reOper,boolean isLastTime){
		boolean success = false;
		switch (reOper.getOperType()) {
		case Delete:
			success = doDelete(reOper.getElement());
			if(!success && isLastTime){
				// �򵥷Ž�ȥ�����������У����﷢����ʱ����Ǵ�������
				elementMap.put(reOper.getElement().getId(), reOper.getElement());
			}
			break;
		case RandomDeleteNode:
			success = reOper.getRankElement().doDeleteNode(reOper.getNode());
			break;
		case DeleteNode:
			success = doDeleteNode(reOper.getNode());
			break;
		case Add:
			success = doAdd(reOper.getElement());
			if(success){
				reOper.getElement().unLock();
			}else if(isLastTime){
				reOper.getElement().unLock();
			}
			break;
		case Update:
			success = doUpdate(reOper.getOldElement(),reOper.getElement());
			if(!success && isLastTime){
				reOper.getElement().unLock();
				// �򵥷Ž�ȥ�����������У����﷢����ʱ����Ǵ�������
				elementMap.put(reOper.getOldElement().getId(), reOper.getOldElement());
			}
			break;
		default:
			break;
		}
		return success;
	}
	
	private boolean lockMultipleNode(Node... nodes){
		Node[] lockNodes = new Node[nodes.length];
		int i=0;
		for (Node node : nodes) {
			if(node == null){
				continue;
			}
			boolean isLock = lockerPool.tryLockNodeWLocker(node, rankConfigure.getRankConditionCount()-1);
			if(!isLock){
				for (int j=nodes.length-1;j>=0;j--) {
					if(lockNodes[j]!=null){
						lockerPool.unlockNodeWLocker(lockNodes[j], rankConfigure.getRankConditionCount()-1);
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
	/**
	 * �ڴ�֮ǰ�Ķ����������
	 * ��������������ʱ��ܳ���˵�������������ȣ��ܿ����ǶԽ��ٵ�id���˽ϴ�ķ���
	 * */
	@Override
	public void destory() throws InterruptedException{
		reOperService.destory();
	}
	
	private void unLockMultipleNode(Node... nodes){
		for (Node node : nodes) {
			if(node == null){
				continue;
			}
			lockerPool.unlockNodeWLocker(node, rankConfigure.getRankConditionCount()-1);
		}
	}
	
	public RankStatistics getRankStatistics() {
		return rankStatistics;
	}

	public Node getHead() {
		return head;
	}
	public int getNodeCount(){
		return nodeMap.size();
	}
	public int getElementCount(){
		return elementMap.size();
	}
	public int getReOperQueueSize(){
		return reOperService.getReOperQueueSize();
	}
	public RankConfigure getRankConfigure() {
		return rankConfigure;
	}
	public RankPool getRankPool() {
		return rankPool;
	}
	public ILockerPool getLockerPool() {
		return lockerPool;
	}
	public RankElementNodeMap getRankElementNodeMap() {
		return rankElementNodeMap;
	}
	public Element getFailElement() {
		return reOperService.getFailElement();
	}
	public ReOperService getReOperService() {
		return reOperService;
	}
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		Node currentNode = head;
		while(currentNode != null){
			sb.append(currentNode.toString()+"\n");
			currentNode = (Node)currentNode.getNext();
		}
		return sb.toString();
	}
	
	public String rankStatisticsInfo(){
		return rankStatistics.toString();
	}

	public static enum ReOperType{
		SingleThread, // Ч�ʽϸߣ��׷�����ͻ
		MultiThread,// ������Ч����ߣ������׷�����ͻ�������ݲ���������
		MultiSche // С������Ч����Խϵͣ����������ֵ�һ�룬���ݴ�Ч�ʺ������࣬���׷�����ͻ������ʹ������
		
	}
}

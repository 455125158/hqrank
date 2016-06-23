package org.hq.rank.core.node;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.hq.rank.core.Rank;
import org.hq.rank.core.RankException;
import org.hq.rank.core.pool.RankPoolElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeStepBase extends AbNode implements RankPoolElement{
	private static Logger log = LoggerFactory.getLogger(NodeStepBase.class);
	
	protected final Rank rank;
	protected AbNode head =null;
	
	protected AtomicInteger nodeCount = new AtomicInteger(0);
	
	protected ReadWriteLock locker = new ReentrantReadWriteLock();
	protected Lock readLock = locker.readLock();
	protected Lock writeLock = locker.writeLock();
	
	public NodeStepBase(Rank rank){
		this.rank = rank;
	}
	public void init(NodeStepBase nodeStep){
		this.parentNS = nodeStep;
	}
	// ������
	public boolean cutBeforePut(){
		if(nodeCount.get() > rank.getRankConfigure().getCutCountNodeStep()){
			// ���nodeStep
			NodeStepBase _previous = lockPrevious();
			this.writeLock.lock();
			if(_previous == previous && nodeCount.get() > rank.getRankConfigure().getCutCountNodeStep()){
				int newNodeCount = rank.getRankConfigure().getCutCountNodeStep() / 2;
				NodeStepBase nodeStepBase = rank.getRankPool().getNodeStepBase(parentNS);
				nodeStepBase.writeLock.lock();
				int currentCount = 0;
				AbNode currentNode = this.head;
				
				nodeStepBase.head = currentNode;
				
				nodeStepBase.next =this; // �����ã��������õĺô�����߲�ѯ��������
				nodeStepBase.previous = this.previous;
				
				int changeElementCount = 0;
				int changeNodeCount = 0;
				List<AbNode> changeNodeList = new ArrayList<AbNode>(newNodeCount);
				while(currentCount++ < newNodeCount && currentNode != null && currentNode.getParentNS() == this){
					changeNodeList.add(currentNode);
					// ����������в������µļ��룬��Ϊ�������д����������µ�nodeҪ��Ӷ���
//					nodeStep.putNode(currentNode);
					nodeStepBase.nodeCount.getAndIncrement();
					changeNodeCount++; 
					
					currentNode.setParentNS(nodeStepBase);
					int eC=currentNode.getCount();
					changeElementCount += eC;
					nodeStepBase.elementCount.getAndAdd(eC);
					currentNode = (AbNode)currentNode.getNext();
				}
				
				
				if(currentCount != newNodeCount+1){
					// ���ò�Ʋ�����֣�Ӧ�øĳ�log.error���������Ҫ��ϸ���
					log.warn("��nodestep��ֹ����У�����ֵ�nodestep���ٵ�fullCount / 2��ֹͣ��ֲ��ع�������õ�nodestep�����������������֣��Ǵ�����");
					log.warn("currentCount:"+currentCount+",nodeCount.get():"+nodeCount.get()+
							",currentNode != null:"+(currentNode != null)+",currentNode.getNodeStep() == this:"+(currentNode.getParentNS() == this)+
							",currentNode.getParentNS():"+currentNode.getParentNS());
					// �ع����ع�֮�󣬲����ֱ����ӽ�ȥ
					for (AbNode changeNode : changeNodeList) {
						changeNode.setParentNS(this);
					}
				}else{
					/// ��ӽ�NodeStepStep
//					nodeStepStep.cutBeforePut();
					if(parentNS != null){
						parentNS.cutBeforePut(); //parentNS����������ط��ı��ˣ��ı���������´�������һ��
						parentNS.getReadLock().lock();
						nodeStepBase.setParentNS(parentNS);
						parentNS.putAbNode(nodeStepBase);
					}
					
					//������һ���ֺ�������һ���ֵ��Ⱥ�˳��Ҫ�޸ģ�����ή������ʱ��������
					// ÿ�ε������ﶼ��1001����û�м��٣����������Ͻ����п��ܼ��ٵ�fullCount / 2����
					this.head = currentNode;
					this.elementCount.addAndGet(changeElementCount*-1); 
					this.nodeCount.addAndGet(changeNodeCount*-1);// ������������в�ѯ����������ʾ����Ľ������ȻӰ�첻�󣬵�����Ż���
					this.head = currentNode;
//					System.err.println("nodeCount after cut:"+this.nodeCount.get());
					//
					if(this.previous != null){
						this.previous.setNext(nodeStepBase);
					}
					this.previous = nodeStepBase;
					if(parentNS != null){
						parentNS.getReadLock().unlock();
					}
					
					nodeStepBase.writeLock.unlock();
					if(_previous != null){
						_previous.writeLock.unlock(); // ���ﲻ���delete��ͻ����Ϊ����Ҫ��ס����
					}
					this.writeLock.unlock();
					rank.getRankStatistics().addCutNodeStepCount();
					return true;
				}
			}else{
//				log.info("������У��ʧ�ܣ������");
			}
			this.writeLock.unlock();
			if(_previous != null){
				_previous.writeLock.unlock(); // ���ﲻ���delete��ͻ����Ϊ����Ҫ��ס����
			}
		}
		return false;
	}
	
	public void putAbNode(AbNode node){
		if(head == null || head == node.getNext()){
			head = node;
		}
		nodeCount.getAndIncrement();
	}
	public void putAbNodeWithElement(AbNode node){
		if(head == null || head == node.getNext()){
			head = node;
		}
		nodeCount.getAndIncrement();
		addElement(node.getCount());
	}
	// ������previous������ȫ�ֱ���������֮��Ҫ����У��
	private NodeStepBase lockPrevious(){
		NodeStepBase _previous = (NodeStepBase)previous;
		if(_previous == null){
			return null;
		}
		_previous.writeLock.lock();
		while(_previous != previous){
			_previous.writeLock.unlock();
			_previous = (NodeStepBase)previous;
			if(_previous == null){
				return null;
			}
			_previous.writeLock.lock();
		}
		return _previous;
	}
	// ������previous������ȫ�ֱ���������֮��Ҫ����У��
	private NodeStepBase lockNext(){
		NodeStepBase _next = (NodeStepBase)next;
		if(_next == null){
			return null;
		}
		_next.writeLock.lock();
		while(_next != next){
			_next.writeLock.unlock();
			_next = (NodeStepBase)next;
			if(_next == null){
				return null;
			}
			_next.writeLock.lock();
		}
		return _next;
	}
	
	public boolean combineBeforeRemove(){
		// ���������У����ܺϲ�
		if(nodeCount.get() < rank.getRankConfigure().getCombineCountNodeStep()){
			// ��һ������ɾ�������һ������
			if(previous != null){
				NodeStepBase previous = (NodeStepBase)this.previous;
				NodeStepBase _previous = lockPrevious();
				if(_previous==null){
					return false;
				}
				this.writeLock.lock();
				NodeStepBase _next = lockNext();
				
				// ���жϣ�����Ѿ���֮ǰ���߳̽����ϲ���
				// ��ֹ�����߳�ͬʱɾ��һ��nodestep
				if(_previous.next != this || nodeCount.get() >= rank.getRankConfigure().getCombineCountNodeStep()){
					if(_next != null){
						_next.writeLock.unlock();
					}
					this.writeLock.unlock();
					_previous.writeLock.unlock();
					return false;
				}
				// �ϲ�nodestepstep
				if(parentNS != null){
					if(parentNS.combineBeforeRemove()){
						parentNS = this.getParentNS();
					}
					parentNS.getReadLock().lock();
//					parentNS.removeAbNode(this);
					parentNS.removeNodeStepBase(this);
				}
				
//				// �ȴ������Ƴ�����ֹ���Ƴ������У����������Ԫ��
				int _nodeCount = nodeCount.get();
				previous.nodeCount.getAndAdd(_nodeCount);
				previous.next = next;
				if(next != null){
					next.setPrevious(previous);
				}
				//
				if(_nodeCount > 0){
					AbNode currentNode = head;
					int count = 0;
					// ����������У�û�п����������ļ��룬�������Ӷ���
					while(currentNode != null && currentNode.getParentNS() == this){
						currentNode.setParentNS(previous);
						previous.elementCount.getAndAdd(currentNode.getCount());
						currentNode = (AbNode)currentNode.getNext();
						count++;
					}
					if(count != _nodeCount){
						StringBuilder sb = new StringBuilder("----count:"+count+",_nodeCount:"+_nodeCount);
						if(currentNode != null){
							sb.append(",currentNode.getNodeStep():"+currentNode.getParentNS());
						}else{
							sb.append("currentNode == null");
						}
						sb.append(",this:"+this);
						sb.append(",this.getNext():"+this.getNext());
						sb.append(",currentNode == this.getNext().getHead():"+(currentNode == ((NodeStepBase)this.getNext()).head));
						log.error(sb.toString());
					}else{
//						System.err.println("-------------------------------zhelishizhengquede:"+head.getConditionLevel());
					}
				}else{
					if(parentNS != null){
						parentNS.getReadLock().unlock();
					}
					
					log.warn("why nodeCount.get() = "+nodeCount.get());
					if(next != null){
						((NodeStepBase)next).writeLock.unlock();
					}
					this.writeLock.unlock();
					previous.writeLock.unlock();
					return false;
				}
				nodeCount.set(0);
				if(parentNS != null){
					parentNS.getReadLock().unlock();
				}
				rank.getRankPool().putNodeStepBase(this); // ������previous �� next �����޸ĳ���null
				if(_next != null){
					_next.writeLock.unlock();
				}
				this.writeLock.unlock();
				_previous.writeLock.unlock();
				rank.getRankStatistics().addCombineNodeStepCount();
				return true;
			}
		}
		return false;
	}
	
	public void removeAbNode(AbNode node){
		int count = node.getCount();
		elementCount.getAndAdd(count*-1);
		if(parentNS != null){
			parentNS.addElement(count*-1);
		}
		
		nodeCount.getAndDecrement();
		if(this.head == node){
			this.head = (AbNode)node.getNext();// ��ʱnode.getNext()�Ǳ���ס�ģ�����ʱdeleteCountҪ����0
			if(head == null){
				log.error("����֮ǰ���й��ϲ������������֣�˵������������");
			}
		}
	}
	public void removeNodeStepBase(NodeStepBase nodeStep){
		NodeStepBase previousNodeStepStep = ((NodeStepBase)nodeStep.getPrevious()).getParentNS();
		if(previousNodeStepStep != this){ // ׿�����nodeStep��this�ĵ�һ��nodeStep
			if(previousNodeStepStep == null || previousNodeStepStep != previous){
				log.error("���ڵ�nodestep��nodestepstep�������ڹ�ϵ");
				throw new RankException();
			}else{
				// ��ס�����
				// ȡ��nodeStep��ʱ����ͨ�����Ϻϲ����еģ����ԣ�ԭ����elementCountӦ�ø������nodeStep��������ҲҪ��previous
				lockPrevious();
				elementCount.getAndAdd(nodeStep.getElementCount() * -1);
				((NodeStepBase)previous).elementCount.getAndAdd(nodeStep.getElementCount());
				((NodeStepBase)previous).writeLock.unlock();
			}
		}
		nodeCount.getAndDecrement();
		if(this.head == nodeStep){
			this.head = (NodeStepBase)nodeStep.getNext();
			if(this.head == null){
				log.error("����֮ǰ���й��ϲ������������֣�˵������������");
			}
		}
	}

	
	public void addElement(int count){
		elementCount.getAndAdd(count);
		if(parentNS != null)
			parentNS.addElement(count);
	}
	
	public void putElement(){
		elementCount.getAndIncrement();
		if(parentNS != null)
			parentNS.putElement();
	}
	
	public void removeElement(){
		elementCount.getAndDecrement();
		if(parentNS != null)
			parentNS.removeElement();
	}
	
	public int getElementCount() {
		return elementCount.get();
	}

	public int getNodeCount() {
		return nodeCount.get();
	}

	public Lock getReadLock() {
		return readLock;
	}

	public Lock getWriteLock() {
		return writeLock;
	}

	public void setHead(AbNode head) {
		this.head = head;
	}
	public AbNode getHead() {
		return head;
	}
	public int getAbNodeCount(){
		return nodeCount.get();
	}
	@Override
	public String toString(){
		return ""+getValue();
	}
	
	@Override
	public long getValue() {
		return head.getValue();
	}
	@Override
	public void reset() {
		head = null;
		elementCount.set(0);
		nodeCount.set(0);
		
		previous = null;
		next = null;
		
		parentNS = null;
	}
}

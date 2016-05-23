package org.hq.rank.core.pool;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.hq.rank.core.Rank;
import org.hq.rank.core.RankException;
import org.hq.rank.core.element.Element;
import org.hq.rank.core.element.ElementStep;
import org.hq.rank.core.node.ElementNode;
import org.hq.rank.core.node.Node;
import org.hq.rank.core.node.NodeStepBase;
import org.hq.rank.core.node.RankElement;
import org.hq.rank.core.node.RankElementNode;

/**
 * ������п��Դ洢һЩNode��RankElement��Element��NodeStep,NodeStepStep,ElementStepͬ�������ṩreset����
 * 
 * ����ͨ��put��ʱ�򣬲鿴�������������Ƿ��ڣ�����ֹ�ڴ�й¶
 * @author zhen
 *
 */
public class RankPool {
	// �ص����ֵ
	private final int eNodeMaxSize = 100;
	private final int rNodeMaxSize = 100;
	private final int rankElementMaxSize = 100;
	private final int elementMaxSize = 100;
	private final int nodeStepBaseMaxSize = 100;
	private final int nodeStepMaxSize = 100;
	private final int nodeStepStepMaxSize = 100;
	private final int elementStepMaxSize = 100;
	
	private final Rank rank; // һ��rankһ����
	public RankPool(Rank rank){
		this.rank = rank;
	}
	/**
	 * element
	 */
	private final Queue<Element> elements = new ConcurrentLinkedQueue<Element>();
	public void putElement(Element element){
		if(element == null){
			return;
		}
		if(elements.size() > elementMaxSize){
			return;
		}
		element.reset();
		elements.offer(element);
	}
	public Element getElement(int id,long... value){
		Element element = elements.poll();// new Element(rank);
		if(element == null){
			element = new Element(rank);
		}
		element.setId(id);
		element.setValue(value);
		return element;
	}
	public Element getNewElement(int id,long... value){
		Element element = new Element(rank);
		element.setId(id);
		element.setValue(value);
		return element;
	}
	/**
	 * node
	 */
	private final Queue<Node> eNodes = new ConcurrentLinkedQueue<Node>();
	private final Queue<Node> rNodes = new ConcurrentLinkedQueue<Node>();
	/**
	 * �Ž�ȥ��ʱ�򣬱���������ǲ�ȷ����
	 * @param node
	 */
	public void putNode(Node node){
		if(node == null){
			return;
		}
		node.reset();
		if(node instanceof ElementNode){
			if(eNodes.size() > eNodeMaxSize){
				return;
			}
			eNodes.offer(node);
		}else{
			if(rNodes.size() > rNodeMaxSize){
				return;
			}
			rNodes.offer(node);
		}
	}
	/**
	 * �õ�һ��node����node����������ǲ�ȷ����
	 * @param element
	 * @param value
	 * @param conditionLevel
	 * @return
	 */
	public Node getNode(Element element,long value,final int conditionLevel){
		if(conditionLevel < 0){ // ����ط�������ϸ��֤
			throw new RankException("error");
		}
		Node node = null;
		if(conditionLevel > 0){
			node = rNodes.poll();
			if(node == null){
				node = new RankElementNode(rank);
			}
		}else{
			node = eNodes.poll();
			if(node == null){
				node = new ElementNode(rank);
			}
		}
		node.init(element, value, conditionLevel);
		return node;
	}
	public Node getNode(long value,int id,final int conditionLevel){
		return getNode(getNewElement(id, value), value, conditionLevel);
	}
	private final Queue<NodeStepBase> nodeStepBases = new ConcurrentLinkedQueue<NodeStepBase>();
	public void putNodeStepBase(NodeStepBase nodeStepBase){
		if(nodeStepBase == null){
			return;
		}
		if(nodeStepBases.size() > nodeStepBaseMaxSize){
			return;
		}
		nodeStepBase.reset();
		nodeStepBases.offer(nodeStepBase);
	}
	public NodeStepBase getNodeStepBase(NodeStepBase parentNS){ // ���parentNS��null��˵�������һ��
		NodeStepBase nodeStepBase = nodeStepBases.poll();
		if(nodeStepBase == null){
			nodeStepBase = new NodeStepBase(rank);
		}
		nodeStepBase.init(parentNS);
		return nodeStepBase;
	}
	/**
	 * NodeStep
	 */
//	private final Queue<NodeStep> nodeSteps = new ConcurrentLinkedQueue<NodeStep>();
//	public void putNodeStep(NodeStep nodeStep){
//		if(nodeStep == null){
//			return;
//		}
//		if(nodeSteps.size() > nodeStepMaxSize){
//			return;
//		}
//		nodeStep.reset();
//		nodeSteps.offer(nodeStep);
//	}
//	public NodeStep getNodeStep(NodeStepStep nodeStepStep){
//		NodeStep nodeStep = nodeSteps.poll();
//		if(nodeStep == null){
//			nodeStep = new NodeStep(rank);
//		}
//		nodeStep.init(nodeStepStep);
//		return nodeStep;
//	}
//	/**
//	 * NodeStepStep
//	 */
//	private final Queue<NodeStepStep> nodeStepSteps = new ConcurrentLinkedQueue<NodeStepStep>();
//	public void putNodeStepStep(NodeStepStep nodeStepStep){
//		if(nodeStepStep == null){
//			return;
//		}
//		if(nodeStepSteps.size() > nodeStepStepMaxSize){
//			return;
//		}
//		nodeStepStep.reset();
//		nodeStepSteps.offer(nodeStepStep);
//	}
//	public NodeStepStep getNodeStepStep(){
//		NodeStepStep nodeStepStep = nodeStepSteps.poll();
//		if(nodeStepStep == null){
//			nodeStepStep = new NodeStepStep(rank);
//		}
//		return nodeStepStep;
//	}
	/**
	 * RankElement
	 */
	private final Queue<RankElement> rankElements = new ConcurrentLinkedQueue<RankElement>();
	public void putRankElement(RankElement rankElement){
		if(rankElement == null){
			return;
		}
		if(rankElements.size() > rankElementMaxSize){
			return;
		}
		rankElement.reset();
		rankElements.offer(rankElement);
	}
	public RankElement getRankElement(final Element element,final int conditionLevel){
		RankElement rankElement = rankElements.poll();
		if(rankElement == null){
			rankElement = new RankElement(rank);
		}
		while(!rankElement.init(element, conditionLevel)){
			rankElement = rankElements.poll();
			if(rankElement == null){
				rankElement = new RankElement(rank);
			}
		}
		return rankElement;
	}
	/**
	 * ElementStep
	 */
	private final Queue<ElementStep> elementSteps = new ConcurrentLinkedQueue<ElementStep>();
	public void putElementStep(ElementStep elementStep){
		if(elementStep == null){
			return;
		}
		if(elementSteps.size() > elementStepMaxSize){
			return;
		}
		elementStep.reset();
		elementSteps.offer(elementStep);
	}
	public ElementStep getElementStep(Node node){
		ElementStep elementStep = elementSteps.poll();
		if(elementStep == null){
			elementStep = new ElementStep(rank);
		}
		elementStep.init(node);
		return elementStep;
	}
	@Override
	public String toString(){
		// Node��RankElement��Element��NodeStep,NodeStepStep,ElementStep
		StringBuilder sb = new StringBuilder();
		sb.append("RankPool:")
//		.append("elementNode:"+eNodes.size()+",rankENode:"+rNodes.size())
		.append(",rankElement:"+rankElements.size())
		.append(",Element:"+elements.size())
		.append(",NodeStepBase:"+nodeStepBases.size())
//		.append(",NodeStep:"+nodeSteps.size())
//		.append(",NodeStepStep:"+nodeStepSteps.size())
		.append(",ElementStep:"+elementSteps.size());
		return sb.toString();
	}
}

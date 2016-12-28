import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.text.Position;
import javax.swing.text.AbstractDocument.Content;
import javax.xml.ws.handler.MessageContext.Scope;

import org.lsmp.djep.vectorJep.function.MSubtract;

import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;

public class ElectionProtocolImpl implements ElectionProtocol{
	private static final int MAXVAL = 2000;
	private static final int NONE = -1;

	private static final String PROBE = "PROBE";
	private static final String ELECTION = "ELECTION";
	private static final String ACK = "ACK";

	private static final String PAR_POSITIONPID = "positionprotocol";
	private static final String PAR_EMITTER = "emitter";

	private static final int DELTA = 2;
	
	private final int protocol_id;
	private final int position_pid;
	private final int emitter_pid;

	private boolean inElection;
	private long idLeader;
	private IDElection currElec;
	private long myNumElec;
	private int myValue;
	private Node parent;
	private boolean pendingAck;
	private AckContent ackContent;

	private List<Long> neighbors;
	private List<Long> waitedNeighbors;
	private HashMap<Long, Integer> timerNeighbor;

	public ElectionProtocolImpl(String prefix) {
		// TODO Auto-generated constructor stub
		String tmp[]= prefix.split("\\.");
		protocol_id = Configuration.lookupPid(tmp[tmp.length-1]);
		position_pid = Configuration.getPid(prefix+"."+PAR_POSITIONPID);
		emitter_pid = Configuration.getPid(prefix+"."+PAR_EMITTER);

		inElection = false;
		idLeader = NONE;
		myNumElec = 0;
		pendingAck = false;
		myValue = (int) (Math.random() * MAXVAL);

		currElec = null;
		parent = null;
		neighbors = new ArrayList<Long>();
		waitedNeighbors = new ArrayList<Long>();
		timerNeighbor = new HashMap<Long, Integer>();
		ackContent = new AckContent(currElec, NONE, NONE);
	}

	public Object clone(){
		ElectionProtocolImpl ep = null;
		try{
			ep = (ElectionProtocolImpl) super.clone();
			ep.neighbors = new ArrayList<Long>();
			ep.waitedNeighbors = new ArrayList<Long>();
			ep.timerNeighbor = new HashMap<Long, Integer>();
			ep.myValue = (int) (Math.random() * MAXVAL);
			ackContent = new AckContent(currElec, NONE, NONE);
		}
		catch( CloneNotSupportedException e ) {} // never happens
		return ep;
	}

	@Override
	public boolean isInElection() {
		return inElection;
	}

	@Override
	public long getIDLeader() {
		return idLeader;
	}

	@Override
	public int getMyValue() {
		return myValue;
	}

	@Override
	public List<Long> getNeighbors() {
		
		return neighbors;
	}
	
	public Long getMyNumElec(){
		return myNumElec;
	}
	
	public void setMyNumElec(Long value){
		myNumElec = value;
	}

	@Override
	public void processEvent(Node node, int pid, Object event) {
		if(protocol_id != pid){
			throw new RuntimeException("Receive Message for wrong protocol");
		}
		ElectionProtocolImpl ep = (ElectionProtocolImpl) node.getProtocol(protocol_id);
		EmitterImpl emitter = (EmitterImpl) node.getProtocol(emitter_pid);

		//Happens before every paint of the nodes
		if (event == null){
			//We delete neighbors which doesn't respond
			for(Map.Entry<Long, Integer> entry : ep.timerNeighbor.entrySet()) {		    
			    if (entry.getValue() > 0){
			    	Integer tmp = entry.getValue() - 1;
			    	entry.setValue(tmp);

				    if(entry.getValue() <= 0 ){
						ep.getNeighbors().remove(entry.getKey());

					/*	//If we waited this neighbor for an election we remove it
						if(ep.inElection) {
							ep.waitedNeighbors.remove(entry.getKey());

							//If it was our parent we become the new src of leader election
							if(ep.parent != null && ep.parent.getID() == entry.getKey()){
								System.out.println("Node "+node.getID()+" is the new src");
								ep.triggerElection(node);
							}
						}

						//If it was the leader we triger a new election
						if(entry.getKey() == idLeader){
							ep.triggerElection(node);
						}
					*/
				    }
			    }
			}

			//Sends a Probe msg to all nodes in scope
			MessageProbe msg = new MessageProbe(node.getID(),Emitter.ALL, protocol_id);
			emitter.emit(node, msg);

			//If we are inElection we look if we have all ack
			/*if(ep.inElection && ep.waitedNeighbors.size() == 0){

				//If we didnt send yet our ack to our father and we are not the src
				//We do it
				if(!ep.pendingAck && ep.currElec.getId() != node.getID()){
					System.out.println("Node "+node.getID()+" sends its ack to "+ep.parent.getID());
					sendAck(node, ep.parent);
					ep.pendingAck = true;
				}
				//If we are the src of election we broadcast it
				else if (ep.currElec.getId() == node.getID()) {
					System.out.println("Node "+node.getID()+" new leader is node "+ep.ackContent.idLid);

					ep.inElection = false;
					ep.idLeader = ep.ackContent.idLid;
					ep.currElec = null;
					ep.pendingAck = false;

					MessageLeader msglid = new MessageLeader(node.getID(), Emitter.ALL, ep.ackContent, protocol_id);
					emitter.emit(node, msglid);
				}
			}*/
		}
		else if(event instanceof Message){
			Message mess =(Message) event;

			//Check if we are in range to rcv it

			//If we detect a new neighbor
			if(mess instanceof MessageProbe){
				ArrayList<Long> listNeighbors = (ArrayList<Long>) ep.getNeighbors();
				if(!listNeighbors.contains(mess.getIdSrc())){
					listNeighbors.add(mess.getIdSrc());
				}
				ep.timerNeighbor.put(mess.getIdSrc(), DELTA);
			}

			//If we are invited to an election
			/*else if(mess instanceof MessageElection){

				IDElection idelec = (IDElection)mess.getContent();
				System.out.println("Node "+node.getID()+" :: MSG ELECTION rcv :"+idelec+" from Node "+mess.getIdSrc());

				
				ep.ackContent.idElec = ep.currElec;
				ep.ackContent.idLid = node.getID();
				ep.ackContent.valueLid = ep.myValue;

				//If this invitation is not more priority than the current we respond him
				//notice him about the current election
				if((ep.inElection && idelec.isLowerThan(ep.currElec))
					|| (ep.inElection && idelec.isEqualTo(ep.currElec)) //it's not our parent
					)
				{
					ep.sendAck(node, ep.getNodeWithId(mess.getIdSrc()));
				}

				//If we are not in election process or it is a more important election
				//We participate to it
				else if(!ep.inElection || idelec.isHigherThan(ep.currElec)){
					ep.currElec = idelec;
					ep.parent = getNodeWithId(mess.getIdSrc());
					ep.inElection = true;

					//Propagate the election msg
					MessageElection prop;
					for(int i = 0; i < ep.neighbors.size(); i++){
						if(ep.neighbors.get(i) != ep.parent.getID()){
							prop = new MessageElection(node.getID(), ep.neighbors.get(i), idelec, protocol_id);
							emitter.emit(node, prop);								
						}
					}

					//We wait the child acks
					ep.waitedNeighbors = new ArrayList<Long>();
					ep.waitedNeighbors.addAll(ep.neighbors);
					ep.waitedNeighbors.remove(mess.getIdSrc());

					ep.ackContent.idElec = idelec;

				}

				if(ep.parent != null)
					System.out.println(ep.currElec+" Node "+node.getID()+" is a child of Node "+ep.parent.getIndex());
				else 
					System.out.println(ep.currElec+" Node "+node.getID()+" is a root");
			}
			
			else if(mess instanceof MessageAck){
				AckContent content = (AckContent) mess.getContent();

				if(content == null && content.idElec.isEqualTo(ep.currElec)){
					System.out.print(ep.currElec+" Node "+node.getID()+" rcv "+content+" from Node"+ mess.getIdSrc());
					ep.waitedNeighbors.remove(mess.getIdSrc());
					System.out.println(" : it is not my child (wait "+ep.waitedNeighbors.size()+")");
				}
				else if(!(content.idElec.isLowerThan(ep.currElec))){
					System.out.print(ep.currElec+" Node "+node.getID()+" rcv "+content+" from Node"+ mess.getIdSrc());
					System.out.println(" : OK");
					if(content.valueLid > ep.ackContent.valueLid){
						ep.ackContent = content;
					}
					ep.waitedNeighbors.remove(mess.getIdSrc());						
				}

				else{
					System.out.print(ep.currElec+" Node "+node.getID()+" rcv "+content+" from Node"+ mess.getIdSrc());
					System.out.println(" NO CARE !!");
				}
			}
			else if(mess instanceof MessageLeader){
				AckContent content = (AckContent) mess.getContent();
				System.out.println("Node "+node.getID() + " :: LEADER "+content);
				if(ep.inElection && !content.idElec.isLowerThan(ep.currElec)){
					System.out.println(ep.currElec+" Node "+node.getID()+" rcv LEADER "+content+" from Node"+ mess.getIdSrc());
					ep.inElection = false;
					ep.pendingAck = false;
					ep.idLeader = content.idLid;
					ep.currElec = null;

					emitter.emit(node, mess);						
				}
			}*/
		}
	}

	public Node getNodeWithId(Long id){
		Node result = null;
		Node n;

		for(int i=0;i< Network.size();i++){
			n = Network.get(i);
			if(n.getID() == id){
				result = n;
			}
		}
		return result;
	}

	public void triggerElection(Node n){
		IDElection idelec;
		ElectionProtocolImpl ep;
		MessageElection msg;
		EmitterImpl em;

		ep = ((ElectionProtocolImpl) n.getProtocol(protocol_id));
		idelec = new IDElection(ep.getMyNumElec(), n.getID());
		if(!ep.inElection || idelec.isHigherThan(ep.currElec)){
			ep.idLeader = NONE;
			ep.inElection = true;
			ep.currElec = idelec;
			ep.parent = null;
			ep.waitedNeighbors = new ArrayList<Long>();
			ep.waitedNeighbors.addAll(ep.neighbors);

			ep.ackContent.idElec = idelec;
			ep.ackContent.idLid = n.getID();
			ep.ackContent.valueLid = ep.myValue;

			System.out.println(ep.currElec+" Node "+n.getID()+" triggers an election");

			ep.setMyNumElec(ep.getMyNumElec() + 1);
			msg = new MessageElection(n.getID(), Emitter.ALL, idelec, protocol_id);
			em = ((EmitterImpl) n.getProtocol(emitter_pid));
			em.emit(n, msg);

		}


	}

	public void sendAck(Node src, Node dest){
		ElectionProtocolImpl ep = (ElectionProtocolImpl) src.getProtocol(protocol_id);
		EmitterImpl emi = (EmitterImpl) src.getProtocol(emitter_pid);
		
		MessageAck msg;
		
		if(dest == parent)
			msg = new MessageAck(src.getID(), dest.getID(), ep.ackContent, protocol_id);
		else
			msg = new MessageAck(src.getID(), dest.getID(), null, protocol_id);
		emi.emit(src, msg);
	}
	
	public boolean hasAllAck(Node node){
		ElectionProtocolImpl ep = (ElectionProtocolImpl)node.getProtocol(protocol_id);
		if(ep.waitedNeighbors.size() == 0)
			return true;
		return false;
	}



}

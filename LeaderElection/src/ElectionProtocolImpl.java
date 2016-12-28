import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.text.Position;
import javax.swing.text.AbstractDocument.Content;
import javax.xml.ws.handler.MessageContext.Scope;

import org.lsmp.djep.vectorJep.function.MSubtract;

import peersim.config.Configuration;
import peersim.core.CommonState;
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

	private static final int DELTA = 5;
	
	private final int protocol_id;
	private final int position_pid;
	private final int emitter_pid;

	private boolean inElection;
	private long idLeader;
	private long valueLeader;

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
		valueLeader = NONE;

		myNumElec = 0;
		pendingAck = false;
		myValue = (int) (CommonState.r.nextDouble() * MAXVAL);

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
			ep.myValue = (int) (CommonState.r.nextDouble() * MAXVAL);
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

		if(event instanceof Message){
			Message mess =(Message) event;

			//If we detect a new neighbor
			if(mess instanceof MessageProbe){
				ArrayList<Long> listNeighbors = (ArrayList<Long>) ep.getNeighbors();
				if(!listNeighbors.contains(mess.getIdSrc())){
					listNeighbors.add(mess.getIdSrc());
					System.out.println("Node "+node.getID()+" new neighbor node "+mess.getIdSrc()+" "+ep.ackContent);
					emitter.emit(node, new MessageLeader(node.getID(), mess.getIdSrc(), new AckContent(null, ep.idLeader, ep.valueLeader), protocol_id) );
				}
				ep.timerNeighbor.put(mess.getIdSrc(), DELTA);
			}

			//If we are invited to an election
			else if(mess instanceof MessageElection){

				IDElection idelec = (IDElection)mess.getContent();
				System.out.println("Node "+node.getID()+" :: MSG ELECTION rcv :"+idelec+" from Node "+mess.getIdSrc());

				//If we are not in election we will participate to it
				if (!ep.inElection){
					ep.currElec = idelec;
					ep.parent = ep.getNodeWithId(mess.getIdSrc());

					ep.waitedNeighbors = new ArrayList<Long>();
					ep.waitedNeighbors.addAll(ep.neighbors);
					ep.waitedNeighbors.remove(ep.parent.getID());
					
					ep.ackContent = new AckContent(idelec, node.getID(), ep.myValue);
					ep.ackContent.idLid = node.getID();
					ep.ackContent.valueLid = ep.myValue;

					//we propagate the election
					MessageElection prop;
					for(int i = 0; i < ep.waitedNeighbors.size(); i++){
						prop = new MessageElection(node.getID(), ep.waitedNeighbors.get(i), idelec, protocol_id);
						emitter.emit(node, prop);								
					}
				}
				//We are in a election process
				else{
					//If we hear an election higher than our then we participate
					if (idelec.isHigherThan(ep.currElec)){
						ep.currElec = idelec;
						ep.parent = ep.getNodeWithId(mess.getIdSrc());

						ep.waitedNeighbors = new ArrayList<Long>();
						ep.waitedNeighbors.addAll(ep.neighbors);
						ep.waitedNeighbors.remove(ep.parent.getID());
						
						ep.ackContent = new AckContent(idelec, node.getID(), ep.myValue);
						ep.ackContent.idLid = node.getID();
						ep.ackContent.valueLid = ep.myValue;

						//we propagate the election
						MessageElection prop;
						for(int i = 0; i < ep.waitedNeighbors.size(); i++){
							prop = new MessageElection(node.getID(), ep.waitedNeighbors.get(i), idelec, protocol_id);
							emitter.emit(node, prop);								
						}
					}
					//If it's the same election, it's can't be our father
					//If it's a lower election, we respond it in order to the src would not wait us
					else{
						MessageAck prop = new MessageAck(node.getID(), mess.getIdSrc(), new AckContent(ep.currElec, NONE, NONE), protocol_id);
						emitter.emit(node, prop);					
					}
				}
				if(ep.parent != null)
					System.out.println(ep.currElec+" Node "+node.getID()+" is a child of Node "+ep.parent.getIndex());
				else 
					System.out.println(ep.currElec+" Node "+node.getID()+" is a root");
			}
			
			else if(mess instanceof MessageAck){
				AckContent content = (AckContent) mess.getContent();
				IDElection idelec = content.idElec;
				System.out.print(ep.currElec+" Node "+node.getID()+" rcv "+content+" from Node"+ mess.getIdSrc());

				//SHOULD NOT BE POSSIBLE We participate to it 
				if (!ep.inElection){
					System.err.println("ERRORR :: "+idelec+" Node "+node.getID()+" is invited by a ACK from node "+mess.getIdSrc());
				}
				else{
					if (!idelec.isEqualTo(ep.currElec)){
						System.err.println("ERRORR :: Node "+node.getID()+" rcv a concurrent ack curr:"+ep.currElec+ " recv :"+idelec+" from node "+mess.getIdSrc());
					}
					else{
						//It's our child
						if(content.idLid != NONE){
							if(content.valueLid > ep.ackContent.valueLid){
								ep.ackContent.valueLid = content.valueLid;
								ep.ackContent.idLid = content.idLid;
							}
						}
						System.out.print(ep.currElec+" Node "+node.getID()+" local leader "+ep.ackContent);
						ep.waitedNeighbors.remove(mess.getIdSrc());
					}
				}
			}
			else if(mess instanceof MessageLeader){
				AckContent content = (AckContent) mess.getContent();
				IDElection idelec = content.idElec;

				System.out.println("Node "+node.getID() + " :: LEADER "+content);

				if (ep.inElection){
					if (idelec.isEqualTo(ep.currElec)){
						System.out.print("Node "+node.getID() + " :: NEW LEADER "+content);
						ep.idLeader = content.idLid;
						ep.valueLeader = content.valueLid;

						ep.inElection = false;
						ep.pendingAck = false;
						ep.currElec = null;
						ep.parent = null;
						ep.pendingAck = false;

						MessageLeader prop;
						for(int i = 0; i < ep.neighbors.size(); i++){
							if(mess.getIdSrc() != ep.neighbors.get(i)){
								prop = new MessageLeader(node.getID(), ep.neighbors.get(i), ep.ackContent, protocol_id);
								emitter.emit(node, prop);
							}
						}
					}
				}
				//It's a concurrent election
				else{
					System.out.print("Node "+node.getID() + " :: CONCURRENT LEADER "+content+" : ");
					if(ep.valueLeader < content.valueLid){
						ep.idLeader = content.idLid;
						ep.valueLeader = content.valueLid;
						System.out.println(" new leader ");
						MessageLeader prop;
						for(int i = 0; i < ep.neighbors.size(); i++){
							if(mess.getIdSrc() != ep.neighbors.get(i)){
								prop = new MessageLeader(node.getID(), ep.neighbors.get(i), ep.ackContent, protocol_id);
								emitter.emit(node, prop);
							}
						}
					}else{
						System.out.println(" stay the same");			
					}
				}
			}
		}
		//Happens before every paint of the nodes
		else if (event == null){
			//Sends a Probe msg to all nodes in scope
			MessageProbe msg = new MessageProbe(node.getID(),Emitter.ALL, protocol_id);
			emitter.emit(node, msg);


			//We delete neighbors which doesn't respond
			for(Map.Entry<Long, Integer> entry : ep.timerNeighbor.entrySet()) { 
			    if (entry.getValue() > 0){
			    	Integer tmp = entry.getValue() - 1;
			    	entry.setValue(tmp);

				    if(entry.getValue() <= 0 ){
						ep.getNeighbors().remove(entry.getKey());

						//If we waited this neighbor for an election we remove it
						if(ep.inElection) {
							ep.waitedNeighbors.remove(entry.getKey());

							//If it was our parent we become the new src of leader election
							if(ep.parent != null && ep.parent.getID() == entry.getKey()){
								System.out.println("Node "+node.getID()+" is the new src");
								if(ep.currElec.getNum() > ep.myNumElec)
									ep.myNumElec = ep.currElec.getNum();
								
								ep.triggerElection(node);
							}
						}

						//If it was the leader we triger a new election
						if(entry.getKey() == idLeader){
							System.out.println("LEADER "+entry.getKey()+"DISCONNECT");
							ep.triggerElection(node);
						}
					
				    }
			    }
			}
			
			if(ep.idLeader == NONE && !ep.inElection){
				System.out.println("NO LEADER");
				ep.triggerElection(node);
			}

			if(!ep.inElection){
				
			}
			else{
				if(ep.waitedNeighbors.size() == 0){
					//if we are the src
					if(node.getID() == ep.currElec.getId()){

						System.out.println(ep.currElec+" Node "+node.getID()+ " root broadcast Leader "+ep.ackContent);						
						ep.idLeader = ep.ackContent.idLid;
						ep.valueLeader = ep.ackContent.valueLid;
						ep.inElection = false;
						ep.pendingAck = false;
						ep.currElec = null;
						ep.parent = null;
						ep.pendingAck = false;
	
						emitter.emit(node, new MessageLeader(node.getID(), Emitter.ALL, ep.ackContent, protocol_id));


					}
					else{
						System.out.println(ep.currElec+" Node "+node.getID()+ " sends ack "+ep.ackContent+" to node "+ep.parent.getID());
						ep.pendingAck = true;
						emitter.emit(node, new MessageAck(node.getID(), parent.getID(), ep.ackContent, protocol_id));
					}
				}
			}
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

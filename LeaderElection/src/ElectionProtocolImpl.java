import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.text.Position;
import javax.xml.ws.handler.MessageContext.Scope;

import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;

public class ElectionProtocolImpl implements ElectionProtocol{
	private static final int MAXVAL = 2000;
	
	private static final String PROBE = "PROBE";
	private static final String ELECTION = "ELECTION";
	private static final String ACK = "ACK";

	private static final String PAR_POSITIONPID = "positionprotocol";
	private static final String PAR_EMITTER = "emitter";

	private static final int DELTA = 1;
	
	private final int protocol_id;
	private final int position_pid;
	private final int emitter_pid;

	private boolean inElection;
	private int idLeader;
	private int myValue;

	private List<Long> neighbors;
	private HashMap<Long, Integer> timerNeighbor;

	public ElectionProtocolImpl(String prefix) {
		// TODO Auto-generated constructor stub
		String tmp[]= prefix.split("\\.");
		protocol_id = Configuration.lookupPid(tmp[tmp.length-1]);
		position_pid = Configuration.getPid(prefix+"."+PAR_POSITIONPID);
		emitter_pid = Configuration.getPid(prefix+"."+PAR_EMITTER);

		inElection = false;
		idLeader = 0;
		myValue = (int) (Math.random() * MAXVAL);
		neighbors = new ArrayList<Long>();
		timerNeighbor = new HashMap<Long, Integer>();
	}

	@Override
	public void processEvent(Node node, int pid, Object event) {
		if(protocol_id != pid){
			throw new RuntimeException("Receive Message for wrong protocol");
		}
		ElectionProtocolImpl ep = (ElectionProtocolImpl) node.getProtocol(protocol_id);


		if(event instanceof Message){
			boolean canDeliver = false;
			MessageLeaderElection mess = (MessageLeaderElection) event;
			Node src = null;
			for(int i=0;i< Network.size();i++){
				Node n= Network.get(i);
				if(n.getID() == mess.getIdSrc()){
					src = n;
				}
			}
			
			if(src != null && src != node){
				PositionProtocolImpl posTmp, posN;
				int scope;
				posN = (PositionProtocolImpl) node.getProtocol(position_pid);
				posTmp = (PositionProtocolImpl) src.getProtocol(position_pid);
				
				scope = ((EmitterImpl)node.getProtocol(emitter_pid)).getScope();

				if( posTmp.getX() < posN.getX() + scope
					&& posTmp.getX() > posN.getX() - scope 
					&& posTmp.getY() < posN.getY() + scope
					&& posTmp.getY() > posN.getY() - scope)
					{
						canDeliver = true;
					}
			}
							
			if (canDeliver){
				if(mess.getTag().equals(PROBE)){
					
					PositionProtocolImpl posH = (PositionProtocolImpl) node.getProtocol(position_pid);

					//Find nodes in the range of host 
					Node tmp;
					PositionProtocolImpl posTmp;

					ArrayList<Long> listNeighbors = (ArrayList<Long>) ep.getNeighbors();
					
					if(!listNeighbors.contains(mess.getIdSrc())){
						listNeighbors.add(mess.getIdSrc());
					}
					ep.timerNeighbor.put(mess.getIdSrc(), DELTA);
				}				
			}
		}
		else if (event == null){
			//Check if a neighbor is still connected
			for(Map.Entry<Long, Integer> entry : ep.timerNeighbor.entrySet()) {			    
			    if (entry.getValue() > 0){
			    	Integer tmp = entry.getValue() - 1;
			    	entry.setValue(tmp);
				    if(entry.getValue() <= 0 ){
				    	ep.neighbors.remove(entry.getKey());
				    }
			    }
			}

			//Send a Probe msg to all nodes in scope
			MessageLeaderElection msg = new MessageLeaderElection(node.getID(),Emitter.ALL, PROBE, null, protocol_id);
			EmitterImpl emitter = (EmitterImpl) node.getProtocol(emitter_pid);
			emitter.emit(node, msg);
		}
	}

	public Object clone(){
		ElectionProtocolImpl ep = null;
		try{
			ep = (ElectionProtocolImpl) super.clone();
			ep.neighbors = new ArrayList<Long>();
			ep.timerNeighbor = new HashMap<Long, Integer>();
			ep.myValue = (int) (Math.random() * MAXVAL);
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

}

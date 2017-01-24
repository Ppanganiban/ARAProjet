import java.util.HashMap;

import peersim.config.Configuration;
import peersim.core.Node;
import peersim.edsim.EDProtocol;

public class PerformanceProtocol implements EDProtocol{
  
  private static final String PAR_ELECTIONPID = "electionprotocol";
  private int protocol_id;
	private int election_pid;
	private double timeWithoutLeader;
	private double nbTimesLeader;
	private double electionTime;
	private double messageOverhead;
	private double timeInElection;
	
	private HashMap<IDElection, Boolean> allElection;



	public PerformanceProtocol(String prefix) {
	  String tmp[] = prefix.split("\\.");
	  protocol_id  = Configuration.lookupPid(tmp[tmp.length-1]);
	  election_pid = Configuration.getPid(prefix +"."+PAR_ELECTIONPID);
	  timeWithoutLeader = 0;
	  nbTimesLeader = 0;
	  electionTime = 0;
	  messageOverhead = 0;
	  timeInElection = 0;
	  allElection = new HashMap<IDElection, Boolean>();
	}
	

	public Object clone(){
	  PerformanceProtocol ep = null;
    try{
      ep = (PerformanceProtocol) super.clone();
      ep.allElection = new HashMap<IDElection, Boolean>();
    }
    catch( CloneNotSupportedException e ) {} // never happens
    return ep;
  }

	@Override
	public void processEvent(Node node, int pid, Object event) {
	  ElectionProtocolImpl ep = (ElectionProtocolImpl) node.getProtocol(election_pid);
	  PerformanceProtocol perf = (PerformanceProtocol) node.getProtocol(protocol_id);

	  
	  if(ep.isInElection()){
	    perf.timeWithoutLeader ++;
	    perf.allElection.put(ep.getCurrElec(), true);
	    
    	perf.timeInElection++;

	    perf.messageOverhead += ep.getSentMSG();
	    ep.setSentMSG(0);
	  }
	  else{
	    if(ep.getIDLeader() == node.getID()){
	      perf.nbTimesLeader++;
	    }
	    perf.messageOverhead += ep.getSentMSG();
      ep.setSentMSG(0);
	  }
	}

  public int getElection_pid() {
    return election_pid;
  }

  public double getTimeWithoutLeader() {
    return timeWithoutLeader;
  }

  public double getElectionTime() {
    return electionTime;
  }

  public double getMessageOverhead() {
    return messageOverhead;
  }

  public double getNbTimesLeader() {
    return nbTimesLeader;
  }

  public double getTimeInElection() {
	return timeInElection;
  }

	public HashMap<IDElection, Boolean> getAllElection() {
		return allElection;
	}
}

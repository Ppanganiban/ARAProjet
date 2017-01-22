import peersim.config.Configuration;
import peersim.core.Node;
import peersim.edsim.EDProtocol;

public class PerformanceProtocol implements EDProtocol{
  
  private static final String PAR_ELECTIONPID = "electionprotocol";
  private int protocol_id;
	private int election_pid;
	private double timeWithoutLeader;
	private double nbElection;
	private double nbTimesLeader;
  private double electionTime;
	private double messageOverhead;

	private IDElection precElection;
	
	public PerformanceProtocol(String prefix) {
	  String tmp[] = prefix.split("\\.");
	  protocol_id  = Configuration.lookupPid(tmp[tmp.length-1]);
	  election_pid = Configuration.getPid(prefix +"."+PAR_ELECTIONPID);
	  timeWithoutLeader = 0;
	  nbElection = 0;
	  nbTimesLeader = 0;
	  electionTime = 0;
	  messageOverhead = 0;
	  precElection = null;
	}
	
	public Object clone(){
	  PerformanceProtocol ep = null;
    try{
      ep = (PerformanceProtocol) super.clone();
      precElection = null;
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
	    if(perf.precElection == null
	        || !perf.precElection.isEqualTo(ep.getCurrElec()))
	    {
	      perf.precElection = ep.getCurrElec();
	      perf.nbElection++;
	    }
	    
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

  public double getNbElection() {
    return nbElection;
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

}

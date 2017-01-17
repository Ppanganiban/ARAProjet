import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;

public class ControlElection implements Control{
  private static final String PAR_ELECTIONPID = "electionprotocol";
  private static final String PAR_TIMESLOW = "time_slow";

  private final int election_pid;
  private final double time_slow;

  public ControlElection(String prefix) {
    election_pid = Configuration.getPid(prefix+"."+PAR_ELECTIONPID);
    time_slow=Configuration.getDouble(prefix+"."+PAR_TIMESLOW);
  }

  @Override
  public boolean execute() {
    //Check neighbors and if there is something to do

    ElectionProtocolImpl ep;
    for(int i = 0; i < Network.size(); i++){
      ep = (ElectionProtocolImpl) Network.get(i).getProtocol(election_pid);
      ep.checkNodeState(Network.get(i));
    }
    try {
      int nb_milisec=(int)time_slow;
      double nb_milisec_double = (double) nb_milisec;
      int nb_nano = (int)((time_slow-nb_milisec_double) * 1000000.0);
      Thread.sleep(nb_milisec, nb_nano);
    } catch (InterruptedException e) {}
    
    return false;
  }

}

import java.util.HashMap;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public class EndController implements Control{
  private static final String PAR_PERFPID = "perfprotocol";
  private final int perf_pid;
 
  public EndController(String prefix) {
    perf_pid = Configuration.getPid(prefix+"."+PAR_PERFPID);
  }
  @Override
  public boolean execute() {
    double F = 0; //fraction of time without leader
    double R = 0; //Election rate
    double T = 0; //Election time
    double L = 0; //Average of leader during the experience
    double M = 0; //Message overhead

    HashMap<IDElection, Boolean> allElection = new HashMap<IDElection, Boolean>();
    PerformanceProtocol perf;
    Node node;
 
    int nbNodeDidElection = 0;

    for(int i = 0; i< Network.size(); i++){
      node = Network.get(i);
      perf = (PerformanceProtocol) node.getProtocol(perf_pid);
      F += perf.getTimeWithoutLeader();

      R += perf.getTimeInElection();
 
      if(perf.getAllElection().size() > 0){
    	  nbNodeDidElection ++;
          M += (perf.getMessageOverhead()/perf.getAllElection().size());

          T += (perf.getTimeInElection()/perf.getAllElection().size());
          
          allElection.putAll(perf.getAllElection());

      }

      L += perf.getNbTimesLeader();
 
    }
 
    M /= nbNodeDidElection;
    T /= nbNodeDidElection;

    F /= Network.size();
    R = R / (Network.size() * CommonState.getEndTime());
    L /= CommonState.getEndTime();

    System.out.println("Fraction of time without leader : "+F+" ms");
    System.out.println("Election rate : "+R);
    System.out.println("Election Time : "+T+" ms");
    System.out.println("Average of leader : "+L);
    System.out.println("Message overhead : "+M);


    return false;
  }

}

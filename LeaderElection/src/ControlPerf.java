import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.edsim.EDSimulator;

public class ControlPerf implements Control{
  private static final String PAR_PERFPID = "perfprotocol";
  private final int perf_pid;
 
  public ControlPerf(String prefix) {
    perf_pid = Configuration.getPid(prefix+"."+PAR_PERFPID);
  }

  @Override
  public boolean execute() {   
    for(int i = 0; i< Network.size(); i++){
      EDSimulator.add(0, null, Network.get(i), perf_pid);
    }
    return false;
  }
}

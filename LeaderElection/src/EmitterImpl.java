import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

public class EmitterImpl implements Emitter{
  private static final String PAR_ELECTIONPID = "electionprotocol";
  private static final String PAR_POSITIONPID = "positionprotocol";

  private final int election_pid;
  private final int position_pid;

  private static int scope;
  private static int latency;

  public EmitterImpl(String prefix) {
    election_pid = Configuration.getPid(prefix+"."+PAR_ELECTIONPID);
    position_pid = Configuration.getPid(prefix+"."+PAR_POSITIONPID);
    scope = Configuration.getInt(prefix+"."+"scope");
    latency = Configuration.getInt(prefix+"."+"latency");
  }


  @Override
  public void emit(Node host, Message m) {
    PositionProtocolImpl posTmp, posN;
    double px, py, ph;
    Node node;
    for(int i = 0; i < Network.size(); i++){
      if (Network.get(i) != host){
        node = Network.get(i);
        posN = (PositionProtocolImpl) node.getProtocol(position_pid);
        posTmp = (PositionProtocolImpl) host.getProtocol(position_pid);

        //Pythagore theorem
        px = posN.getX() - posTmp.getX();
        py = posN.getY() - posTmp.getY();
        ph = Math.sqrt(Math.pow(px, 2) + Math.pow(py, 2));
        
        if(ph <= scope){
          EDSimulator.add(latency, m, node, election_pid);
        }
      }
    }
  }

  public Object clone(){
    EmitterImpl ei = null;
    try{
      ei = (EmitterImpl) super.clone();
    }
    catch( CloneNotSupportedException e ) {} // never happens
    return ei;
  }

  @Override
  public int getLatency() {
    return latency;
  }

  @Override
  public int getScope() {
    return scope;
  }

}
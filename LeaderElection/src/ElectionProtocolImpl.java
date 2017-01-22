import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

public class ElectionProtocolImpl implements ElectionProtocol{
  private static final int MAXVAL         = 2000;
  private static final int NONE           = -1;
  private static final String PAR_EMITTER = "emitter";
  private static final String PAR_POSITION   = "position";

  private static final String PAR_DELTA   = "delta";

  /**Timer for one communication between two nodes*/
  private static int DELTANEIGHBOR;

  /**Timer to wait for receiving an ack of a child*/
  private static int DELTACHILD;

  /**Timer to wait for receiving a probe message from the current leader*/
  private static int DELTALEADER;

  private final int protocol_id;
  private final int emitter_pid;
  private final int position_pid;

  /**Local number of sequence for election*/
  private long myNumElec;
  /**Local value*/
  private int myValue;

  /**True if it's in election process else false*/
  private boolean inElection;
  /**
   * NONE if there is no leader (just at initialization) else the current leader.
   * A leader is the node in the connected graph with the greater value.
   */
  private long idLeader;
  /**Leader's value*/
  private long valueLeader;
  /**ID of the current election*/
  private IDElection currElec;
  public IDElection getCurrElec() {
    return currElec;
  }

  /**ID of the source node of the current election*/
  //We need this variable as a tmp of the id in var currElec.
  //The src elec can change when we lost connection with our parent
  //during the election.
  private long srcElec;
  /**Node of our parent during the current election*/
  private Node parent;
  /**True if we send our ack to our parent, else false*/
  private boolean pendingAck;
  /**Message content uses during an election.*/
  private MessContent ackContent;
  /**List of neighbor's IDs*/
  private List<Long> neighbors;
  /**HashMap<A node ID, current deadline for waiting a new ProbeMess>*/
  private HashMap<Long, Integer> timerNeighbor;
  /**List of neighbor's IDs whom we are waiting an Ack message*/
  private List<Long> waitedNeighbors;
  /**HashMap<A node ID, current deadline for waiting an ACK from this node>*/
  private HashMap<Long, Integer> timerChild;
  /**Highest LeaderMessage which is concurrent received during an election*/
  private MessageLeader pendingMsgLeader;
  /**Node id of the pendingMsgLeader*/
  private long idPendingMsgLeader;
  /**Number of sequence of next probe leader msg.*/
  private long numseqLeader;
  /**
   * 0 if we didn't have a ProbeLeaderMessage then we trigger a new election.
   * Else time to wait.*/
  private int timerLeader;

  private double sentMSG; //Use for performance computing


  public ElectionProtocolImpl(String prefix) {
    String tmp[]  = prefix.split("\\.");
    protocol_id   = Configuration.lookupPid(tmp[tmp.length-1]);
    emitter_pid   = Configuration.getPid(prefix+"."+PAR_EMITTER);
    position_pid  = Configuration.getPid(prefix+"."+PAR_POSITION);

    DELTANEIGHBOR = Configuration.getInt(prefix+"."+PAR_DELTA) + 1;
    DELTACHILD    = DELTANEIGHBOR * Network.size() + 1;
    DELTALEADER   = DELTANEIGHBOR * Network.size() + 1;

    inElection  = false;
    idLeader    = NONE;
    valueLeader = NONE;
    myNumElec   = 0;
    pendingAck  = false;
    myValue     = (int) (CommonState.r.nextDouble() * MAXVAL);

    currElec  = null;
    srcElec   = NONE;
    parent    = null;

    neighbors       = new ArrayList<Long>();
    waitedNeighbors = new ArrayList<Long>();
    timerNeighbor   = new HashMap<Long, Integer>();
    timerChild      = new HashMap<Long, Integer>();
    ackContent      = new MessContent(currElec, NONE, NONE);

    pendingMsgLeader    = null;
    idPendingMsgLeader  = NONE;
    timerLeader         = DELTALEADER;
    numseqLeader        = 0;
  }

  public Object clone(){
    ElectionProtocolImpl ep = null;
    try{
      ep = (ElectionProtocolImpl) super.clone();

      ep.neighbors        = new ArrayList<Long>();
      ep.waitedNeighbors  = new ArrayList<Long>();
      ep.timerChild       = new HashMap<Long, Integer>();
      ep.timerNeighbor    = new HashMap<Long, Integer>();
      ep.myValue          = (int) (CommonState.r.nextDouble() * MAXVAL);
      ep.ackContent       = new MessContent(currElec, NONE, NONE);
      ep.pendingMsgLeader = null;

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
    EmitterImpl emitter     = (EmitterImpl) node.getProtocol(emitter_pid);

    //Called by the monitor every 1 ms
    if (event == null){
      ep.checkNodeState(node);
    }


    else if(event instanceof Message){
      Message mess =(Message) event;
      boolean canBeDeliver = false;

      //Check if we deliver it to the application
      if(mess.getIdDest() == Emitter.ALL || mess.getIdDest() == node.getID())
        canBeDeliver = true;

      if(canBeDeliver){

        /*
         * We add it to our neighbors list and we give it a timer.
         * If it's a new neighbor we exchange with it our leader
         */
        if(mess instanceof MessageProbe){
          ArrayList<Long> listNeighbors = (ArrayList<Long>) ep.getNeighbors();
          if(!listNeighbors.contains(mess.getIdSrc())){
            listNeighbors.add(mess.getIdSrc());
            //System.out.println("Node "+node.getID()+" new neighbor node "+mess.getIdSrc());
            emitter.emit(node,
                          new MessageLeader(node.getID(),
                          mess.getIdSrc(),
                          new MessContent(null, ep.idLeader, ep.valueLeader),
                          protocol_id));
          }
          ep.timerNeighbor.put(mess.getIdSrc(), DELTANEIGHBOR);
        }


        /* 
         * We participate to it if we are not in election process and
         * the election is about the of our leader defunt leader. If it's not
         * the case, we do not respond in order to finish the current election.
         * We will be notify by its leader during its leader phase
         * by a LeaderMessage.
         */
        else if(mess instanceof MessageElection){
          IDElection idelec = (IDElection)mess.getContent();
          //System.out.println("Node "+node.getID()+" :: MSG ELECTION rcv :"+idelec+" from Node "+mess.getIdSrc() + " nbHop:"+idelec.getNbHop());          
          if ((!ep.inElection && idelec.getOldLeader() == ep.idLeader)
              || idelec.isHigherThan(ep.currElec)){

            //System.out.println("Node "+node.getID()+" :: participates to this election :"+idelec+" prev: "+ep.currElec);
            ep.currElec       = idelec;
            ep.srcElec        = idelec.getId();
            ep.parent         = ep.getNodeWithId(mess.getIdSrc());
            ep.inElection     = true;
            ep.ackContent     = new MessContent(idelec, node.getID(), ep.myValue);
            ep.pendingAck     = false;
            ep.numseqLeader   = 0;
            
            ep.timerChild       = new HashMap<Long, Integer>();
            ep.waitedNeighbors  = new ArrayList<Long>();
            ep.waitedNeighbors.addAll(ep.neighbors);
            ep.waitedNeighbors.remove(ep.parent.getID());

            //System.out.println(ep.currElec+" Node "+node.getID()+" is a child of Node "+ep.parent.getIndex());

            //We propagate the election to ours neighbors
            MessageElection prop;
            for(int i = 0; i < ep.waitedNeighbors.size(); i++){
              ep.timerChild.put(ep.waitedNeighbors.get(i),
                                DELTACHILD - (DELTANEIGHBOR * idelec.getNbHop()));
              //System.out.println(ep.currElec+" node "+node.getID()+ " propagates election to node "+ep.waitedNeighbors.get(i));
              prop = new MessageElection(node.getID(),
                                         ep.waitedNeighbors.get(i),
                                         new IDElection(idelec.getNum(),
                                                         idelec.getId(),
                                                         idelec.getNbHop() + 1,
                                                         idelec.getOldLeader()),
                                         protocol_id);
              emitter.emit(node, prop);
            }
            ep.sentMSG += (ep.neighbors.size()-1);

            
          }
        }


        /*
         * We only treat the ack from ours child.
         * We udpate our ack content with the content received.
         * Then we update our list of ack waited.
         */
        else if(mess instanceof MessageAck){
          MessContent content = (MessContent) mess.getContent();
          IDElection idelec   = content.idElec;

          //System.out.println(ep.currElec+" Node "+node.getID()+" rcv "+content+" from Node"+ mess.getIdSrc());
          if (ep.inElection){
            if (idelec.isEqualTo(ep.currElec)){
              if(content.idLid != NONE){
                if(content.valueLid > ep.ackContent.valueLid){
                  ep.ackContent.valueLid  = content.valueLid;
                  ep.ackContent.idLid     = content.idLid;
                }
              }
              ep.timerChild.remove(mess.getIdSrc());
              ep.waitedNeighbors.remove(mess.getIdSrc());
            }
          }
        }


        /*
         * We can received a LeaderMessage even if we are not in election.
         * If this message is not about the current election we put it 
         * inside the pending message leader. Else we update our leader.
         */
        else if(mess instanceof MessageLeader){
          MessContent content = (MessContent) mess.getContent();
          IDElection idelec = content.idElec;

          //System.out.println("Node "+node.getID() + " :: LEADER "+content+ "from node "+mess.getIdSrc());

          if (ep.inElection && ep.currElec.isEqualTo(idelec)){
            //System.out.println("Node "+node.getID() + " :: NEW LEADER "+content);
            ep.idLeader     = content.idLid;
            ep.valueLeader  = content.valueLid;
            ep.timerLeader  = DELTALEADER;

            if(ep.idLeader == node.getID())
              ep.timerLeader = 0;

            ep.numseqLeader   = 0;
            ep.inElection     = false;
            ep.pendingAck     = false;
            ep.currElec       = null;
            ep.srcElec        = NONE;
            ep.parent         = null;
            ep.pendingAck     = false;

            ep.ackContent.idLid     = ep.idLeader;
            ep.ackContent.valueLid  = ep.valueLeader;
            MessageLeader prop;
            for(int i = 0; i < ep.neighbors.size(); i++){
              if(mess.getIdSrc() != ep.neighbors.get(i)){
                prop = new MessageLeader(node.getID(),
                                          ep.neighbors.get(i),
                                          ep.ackContent,
                                          protocol_id);
                emitter.emit(node, prop);
              }
            }
            ep.sentMSG += (ep.neighbors.size()-1);
          }

          //It's a concurrent election
          else{
            if(!ep.inElection){
              //System.out.print("Node "+node.getID() + " :: EVALUATE LEADER "+content+" : ");
              if(ep.valueLeader < content.valueLid){
                ep.idLeader = content.idLid;
                ep.valueLeader = content.valueLid;
                ep.timerLeader = DELTALEADER;

                if(ep.idLeader == node.getID())
                  ep.timerLeader = 0;

                ep.numseqLeader = 0;
                //System.out.println(" new leader ");
                MessageLeader prop;
                for(int i = 0; i < ep.neighbors.size(); i++){
                  if(mess.getIdSrc() != ep.neighbors.get(i)){
                    prop = new MessageLeader(node.getID(),
                                              ep.neighbors.get(i),
                                              new MessContent(null,
                                                              ep.idLeader,
                                                              ep.valueLeader),
                                              protocol_id);
                    emitter.emit(node, prop);
                  }
                }
                ep.sentMSG += (ep.neighbors.size()-1);

              }else{
                //System.out.println(" stay the same");
              }
            }
            else{
              //System.out.println("Node "+node.getID() + " :: already in election process "+ep.currElec+". Put it in pendingMSGLeader "+content+" : ");
              if(ep.pendingMsgLeader == null
                || ((MessContent)ep.pendingMsgLeader.getContent()).valueLid < content.valueLid){
                ep.pendingMsgLeader   = (MessageLeader) mess;
                ep.idPendingMsgLeader = mess.getIdSrc();
              }
            }
          }
        }


        /*
         * We propagate the ProbeLeader message only if it's about our leader
         * and if it's our parent to leader.
         */
        else if (mess instanceof MessageProbeLeader){
          long problid = (long) mess.getContent();
          MessageProbeLeader m = (MessageProbeLeader) mess;
          //System.out.print("Node "+node.getID() + " :: RECV PROBE LEADER "+problid+" from node "+mess.getIdSrc()+" : ");
          if(ep.idLeader == problid
              && node.getID() != problid
              && m.getNumseq() >= ep.numseqLeader){
                  ep.timerLeader = DELTALEADER;
                  emitter.emit(node,
                              new MessageProbeLeader(node.getID(),
                                                      Emitter.ALL,
                                                      problid,
                                                      ep.numseqLeader,
                                                      protocol_id));
                  ep.numseqLeader = m.getNumseq() + 1;
          }
        }
      }
    }
  }

  /**
   * Update neighbors node.
   * If node is in election, it treats pending message leader and initializes first leader as itself.
   * Else, it checks the waited acks.
   * @param node
   */
  public void checkNodeState(Node node){
    EmitterImpl emitter     = (EmitterImpl) node.getProtocol(emitter_pid);
    ElectionProtocolImpl ep = (ElectionProtocolImpl) node.getProtocol(protocol_id);
    
    EDSimulator.add(0, null, node, position_pid);

    //First call
    if(ep.idLeader == NONE){
      //System.out.println("NO LEADER");
      ep.idLeader     = node.getID();
      ep.timerLeader  = 0;
      ep.valueLeader  = ep.myValue;
      ep.ackContent.idLid     = node.getID();
      ep.ackContent.valueLid  = ep.myValue;
    }

    //Sends a Probe msg to all nodes in scope
    MessageProbe msg = new MessageProbe(node.getID(),
                                        Emitter.ALL,
                                        new Long(ep.idLeader),
                                        protocol_id);

    emitter.emit(node, msg);

    //We delete neighbors which didn't respond
    for(Map.Entry<Long, Integer> entry : ep.timerNeighbor.entrySet()) {
      if (entry.getValue() > 0){
        Integer tmp = entry.getValue() - 1;
        entry.setValue(tmp);

        if(entry.getValue() <= 0 ){
          ep.getNeighbors().remove(entry.getKey());
          //System.out.println("Node "+node.getID()+" remove neighbor Node "+entry.getKey());

          //If we waited this neighbor for an election we remove it
          if(ep.inElection) {
            ep.timerChild.remove(entry.getKey());
            ep.waitedNeighbors.remove(entry.getKey());
  
            //If it was our parent we become the new src of leader election
            if(ep.parent != null && ep.parent.getID() == entry.getKey()){
              //System.out.println("Node "+node.getID()+" is the new src");
              ep.srcElec = node.getID();
            }
          }
          else{
            if(ep.idPendingMsgLeader == entry.getKey() ){
              ep.idPendingMsgLeader = NONE;
              ep.pendingMsgLeader = null;
            }
            //If it was the leader we triger a new election
            /*if(entry.getKey() == ep.idLeader){
              //System.out.println("LEADER "+entry.getKey()+" DISCONNECT");
              ep.triggerElection(node);
            }*/
          }    
        }
      }
    }

    if(!ep.inElection){
      //if we had a pending leader msg we treat it
      if (ep.idPendingMsgLeader != NONE){
        MessContent content = (MessContent) ep.pendingMsgLeader.getContent();
        
        //System.out.print("Node "+node.getID() + " :: EVALUATE PENDING LEADER message"+content+" : ");
        if(ep.valueLeader < content.valueLid){
          ep.idLeader     = content.idLid;
          ep.valueLeader  = content.valueLid;
          ep.timerLeader  = DELTALEADER;

          if(ep.idLeader == node.getID()){
            ep.timerLeader = 0;
          }
          //System.out.println(" new leader ");
          MessageLeader prop;
          for(int i = 0; i < ep.neighbors.size(); i++){
            if(ep.pendingMsgLeader.getIdSrc() != ep.neighbors.get(i)){
              prop = new MessageLeader(node.getID(),
                                        ep.neighbors.get(i),
                                        new MessContent(null,
                                                        ep.idLeader,
                                                        ep.valueLeader),
                                        protocol_id);
              emitter.emit(node, prop);
            }
          }
        }else{
          //System.out.println(" stay the same");
        }

        ep.pendingMsgLeader   = null;
        ep.idPendingMsgLeader = NONE;
      }
      if(ep.idLeader == node.getID()){
        if(ep.timerLeader % (DELTALEADER / 2) == 0){
          ep.numseqLeader++;
          emitter.emit(node,
              new MessageProbeLeader(node.getID(),
                                      Emitter.ALL,
                                      node.getID(),
                                      ep.numseqLeader,
                                      protocol_id));
        }
        ep.timerLeader = (ep.timerLeader + 1) % (DELTALEADER / 2);
        //System.out.println("Node "+node.getID()+" is leader. SEND PROBE");
      }else{
        if(ep.timerLeader <= 0){
          //System.out.println("Node "+node.getID()+ " is deconnected to leader "+ep.idLeader);
          ep.triggerElection(node);
        }else{
          //System.out.println("Node "+node.getID()+ " :: timer "+ep.timerLeader+" to leader "+ep.idLeader);
          ep.timerLeader --;
        }
      }
    }
    else{
      ep.checkWaitedAck(node);
    }
  
  }

  /**
   * Trigger an election with node n as src node.
   * Set all variables needed to notice that it is in election.
   * @param n node src
   */
  public void triggerElection(Node n){
    IDElection idelec;
    ElectionProtocolImpl ep;
    MessageElection msg;
    EmitterImpl em;

    ep      = ((ElectionProtocolImpl) n.getProtocol(protocol_id));
    idelec  = new IDElection(ep.getMyNumElec(), n.getID(), 1, ep.idLeader);

    //System.out.println(idelec+" Node "+n.getID()+" triggers an election");
    ep.idLeader         = n.getID();
    ep.valueLeader      = ep.myValue;
    ep.timerLeader      = DELTALEADER;
    ep.numseqLeader     = 0;
    ep.inElection       = true;
    ep.currElec         = idelec;
    ep.srcElec          = n.getID();
    ep.parent           = null;
    ep.timerChild       = new HashMap<Long, Integer>();   
    ep.waitedNeighbors  = new ArrayList<Long>();


    ep.waitedNeighbors.addAll(ep.neighbors);
    ep.myNumElec ++; 

    for (int i = 0; i < ep.waitedNeighbors.size(); i++){
      ep.timerChild.put(ep.waitedNeighbors.get(i), DELTACHILD);
    }

    ep.ackContent.idElec    = idelec;
    ep.ackContent.idLid     = n.getID();
    ep.ackContent.valueLid  = ep.myValue;

    msg = new MessageElection(n.getID(), Emitter.ALL, idelec, protocol_id);
    em  = ((EmitterImpl) n.getProtocol(emitter_pid));
    em.emit(n, msg);
  }

  /**
   * This function check if during an election a node rcv all the ack expected.
   * If it is false, this function does nothing.
   * Else, if it is the node src it will broadcast a leader message, else it sends an ack msg to the parent node
   * @param nodes
   */
  public void checkWaitedAck(Node node){
    ElectionProtocolImpl ep = (ElectionProtocolImpl) node.getProtocol(protocol_id);
    EmitterImpl emitter     = (EmitterImpl) node.getProtocol(emitter_pid);

    if(ep.inElection && !ep.pendingAck){
      for(Map.Entry<Long, Integer> entry : ep.timerChild.entrySet()) {
        Integer tmp = entry.getValue() - 1;
        entry.setValue(tmp);
        //System.out.println("Node "+node.getID()+" :: node "+entry.getKey()+" timerACK "+entry.getValue()+"/"+DELTACHILD);
        if(entry.getValue() <= 0){
          //System.out.println("Node "+node.getID()+" :: node "+entry.getKey()+" is not my child");
          ep.waitedNeighbors.remove(entry.getKey());
        }
      }
    }


    if(ep.waitedNeighbors.size() == 0 && ep.inElection){
      //if we are the src we broadcast LEADER MSG
      if(node.getID() == ep.srcElec){
        //System.out.println(ep.currElec+" Node "+node.getID()+ " root broadcast Leader "+ep.ackContent);
        ep.idLeader     = ep.ackContent.idLid;
        ep.valueLeader  = ep.ackContent.valueLid;
        ep.timerLeader  = DELTALEADER;
        ep.inElection   = false;
        ep.pendingAck   = false;
        ep.currElec     = null;
        ep.srcElec      = NONE;
        ep.parent       = null;
        ep.pendingAck   = false;

        ep.sentMSG += ep.neighbors.size();
        emitter.emit(node, new MessageLeader(node.getID(), Emitter.ALL, ep.ackContent, protocol_id));
      }
      else if (!ep.pendingAck){
        //System.out.println(ep.currElec+" Node "+node.getID()+ " sends ack "+ep.ackContent+" to node "+ep.parent.getID());
        ep.pendingAck = true;
        emitter.emit(node, new MessageAck(node.getID(), parent.getID(), ep.ackContent, protocol_id));
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

  public double getSentMSG() {
    return sentMSG;
  }

  public void setSentMSG(double sentMSG) {
    this.sentMSG = sentMSG;
  }
}

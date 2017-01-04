import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;

public class ElectionProtocolImpl implements ElectionProtocol{
  private static final int MAXVAL = 2000;
  private static final int NONE = -1;

  private static final String PAR_EMITTER = "emitter";
  private static final String PAR_DELTA = "delta";

  private static int DELTANEIGHBOR;
  private static int DELTACHILD;
  private static int DELTALEADER;

  private final int protocol_id;
  private final int emitter_pid;

  private boolean inElection;
  private long idLeader;
  private long valueLeader;
  private IDElection currElec;
  private long srcElec;

  private long myNumElec;
  private int myValue;
  private Node parent;
  private boolean pendingAck;
  private MessContent ackContent;

  private List<Long> neighbors;
  private List<Long> waitedNeighbors;
  private HashMap<Long, Integer> timerNeighbor;
  private HashMap<Long, Integer> timerChild;

  
  private MessageLeader pendingMsgLeader;
  private long idPendingMsgLeader;

  private long parentToLeader;
  private int timerLeader;

  public ElectionProtocolImpl(String prefix) {
    String tmp[]= prefix.split("\\.");
    protocol_id = Configuration.lookupPid(tmp[tmp.length-1]);
    emitter_pid = Configuration.getPid(prefix+"."+PAR_EMITTER);
    DELTANEIGHBOR = Configuration.getInt(prefix+"."+PAR_DELTA) + 1;
    DELTACHILD = DELTANEIGHBOR * Network.size() + 1;
    DELTALEADER = DELTANEIGHBOR * Network.size() + 1;
    inElection = false;
    idLeader = NONE;
    valueLeader = NONE;
    myNumElec = 0;
    pendingAck = false;
    myValue = (int) (CommonState.r.nextDouble() * MAXVAL);

    currElec = null;
    srcElec = NONE;
    parent = null;

    neighbors = new ArrayList<Long>();
    waitedNeighbors = new ArrayList<Long>();
    timerChild = new HashMap<Long, Integer>();
    timerNeighbor = new HashMap<Long, Integer>();
    ackContent = new MessContent(currElec, NONE, NONE);

    pendingMsgLeader = null;
    idPendingMsgLeader = NONE;
    timerLeader = DELTALEADER;
    parentToLeader = NONE;
  }

  public Object clone(){
    ElectionProtocolImpl ep = null;
    try{
      ep = (ElectionProtocolImpl) super.clone();
      ep.neighbors = new ArrayList<Long>();
      ep.waitedNeighbors = new ArrayList<Long>();
      ep.timerChild = new HashMap<Long, Integer>();
      ep.timerNeighbor = new HashMap<Long, Integer>();
      ep.myValue = (int) (CommonState.r.nextDouble() * MAXVAL);
      ep.ackContent = new MessContent(currElec, NONE, NONE);
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
    EmitterImpl emitter = (EmitterImpl) node.getProtocol(emitter_pid);

    if (event == null){
      ep.checkNodeState(node);
    }  
    else if(event instanceof Message){
      Message mess =(Message) event;
      boolean canBeDeliver = false;

      if(mess.getIdDest() == Emitter.ALL || mess.getIdDest() == node.getID())
        canBeDeliver = true;
  
      if(canBeDeliver){

        if(mess instanceof MessageProbe){
          ArrayList<Long> listNeighbors = (ArrayList<Long>) ep.getNeighbors();
          //If it's a new neighbor we exchange the current leader
          if(!listNeighbors.contains(mess.getIdSrc())){
            listNeighbors.add(mess.getIdSrc());
            //System.out.println("Node "+node.getID()+" new neighbor node "+mess.getIdSrc());
            emitter.emit(node, new MessageLeader(node.getID(), mess.getIdSrc(), new MessContent(null, ep.idLeader, ep.valueLeader), protocol_id) );
          }
          ep.timerNeighbor.put(mess.getIdSrc(), DELTANEIGHBOR);
        }

        else if(mess instanceof MessageElection){
          IDElection idelec = (IDElection)mess.getContent();

          //System.out.println("Node "+node.getID()+" :: MSG ELECTION rcv :"+idelec+" from Node "+mess.getIdSrc() + " nbHop:"+idelec.getNbHop());

          /* If we are not in election
           * OR we are in a election process
           * We participate to it only if it is higher than the current
           * If it's not the case, we do not respond in order to finish the current election and it wille be notify during the LEADER PHASE
           */
          if (!ep.inElection){
            //System.out.println("Node "+node.getID()+" :: participates to this election :"+idelec+" prev: "+ep.currElec);
            ep.currElec = idelec;
            ep.srcElec = idelec.getId();
            ep.parent = ep.getNodeWithId(mess.getIdSrc());
            ep.inElection = true;
            ep.ackContent = new MessContent(idelec, node.getID(), ep.myValue);
            ep.pendingAck = false;

            ep.waitedNeighbors = new ArrayList<Long>();
            ep.waitedNeighbors.addAll(ep.neighbors);
            ep.waitedNeighbors.remove(ep.parent.getID());
            ep.timerChild = new HashMap<Long, Integer>();

            //System.out.println(ep.currElec+" Node "+node.getID()+" is a child of Node "+ep.parent.getIndex());

            //We propagate the election to ours neighbors
            MessageElection prop;
            for(int i = 0; i < ep.waitedNeighbors.size(); i++){
              ep.timerChild.put(ep.waitedNeighbors.get(i),
                  DELTACHILD - (DELTANEIGHBOR * idelec.getNbHop())
              );
              //System.out.println(ep.currElec+" node "+node.getID()+ " propagates election to node "+ep.waitedNeighbors.get(i));
              prop = new MessageElection(node.getID(), ep.waitedNeighbors.get(i), new IDElection(idelec.getNum(), idelec.getId(), idelec.getNbHop() + 1), protocol_id);
              emitter.emit(node, prop);                
            }
          }
        }
        
        else if(mess instanceof MessageAck){
          MessContent content = (MessContent) mess.getContent();
          IDElection idelec = content.idElec;
          //System.out.println(ep.currElec+" Node "+node.getID()+" rcv "+content+" from Node"+ mess.getIdSrc());
          /*
           * We only treat the ack from our child
           */
          if (ep.inElection){
            if (idelec.isEqualTo(ep.currElec)){
              if(content.idLid != NONE){
                if(content.valueLid > ep.ackContent.valueLid){
                  ep.ackContent.valueLid = content.valueLid;
                  ep.ackContent.idLid = content.idLid;
                }
              }
              ep.timerChild.remove(mess.getIdSrc());
              ep.waitedNeighbors.remove(mess.getIdSrc());
            }
          }
        }

        //We can rcv a leader MSG 
        else if(mess instanceof MessageLeader){
          MessContent content = (MessContent) mess.getContent();
          IDElection idelec = content.idElec;

          //System.out.println("Node "+node.getID() + " :: LEADER "+content+ "from node "+mess.getIdSrc());

          if (ep.inElection && ep.currElec.isEqualTo(idelec)){
            //System.out.println("Node "+node.getID() + " :: NEW LEADER "+content);
            ep.idLeader = content.idLid;
            ep.valueLeader = content.valueLid;
            ep.timerLeader = DELTALEADER;

            if(ep.idLeader == node.getID())
            	ep.timerLeader = 0;

            ep.parentToLeader = NONE;
            ep.inElection = false;
            ep.pendingAck = false;
            ep.currElec = null;
            ep.srcElec = NONE;
            ep.parent = null;
            ep.pendingAck = false;

            ep.ackContent.idLid = ep.idLeader;
            ep.ackContent.valueLid = ep.valueLeader;
            MessageLeader prop;
            for(int i = 0; i < ep.neighbors.size(); i++){
              if(mess.getIdSrc() != ep.neighbors.get(i)){
                prop = new MessageLeader(node.getID(), ep.neighbors.get(i), ep.ackContent, protocol_id);
                emitter.emit(node, prop);
              }
            }
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

                ep.parentToLeader = NONE;
                //System.out.println(" new leader ");
                MessageLeader prop;
                for(int i = 0; i < ep.neighbors.size(); i++){
                  if(mess.getIdSrc() != ep.neighbors.get(i)){
                    prop = new MessageLeader(node.getID(), ep.neighbors.get(i), new MessContent(null, ep.idLeader, ep.valueLeader), protocol_id);
                    emitter.emit(node, prop);
                  }
                }
              }else{
                //System.out.println(" stay the same");      
              }
            }
            else{
              //System.out.println("Node "+node.getID() + " :: already in election process "+ep.currElec+". Put it in pendingMSGLeader "+content+" : ");
              if(ep.pendingMsgLeader == null
                || ((MessContent)ep.pendingMsgLeader.getContent()).valueLid < content.valueLid){
                ep.pendingMsgLeader = (MessageLeader) mess;
                ep.idPendingMsgLeader = mess.getIdSrc();
              }
              ep.timerChild.remove(mess.getIdSrc());
              ep.waitedNeighbors.remove(mess.getIdSrc());
            }
            
          }
        }
        else if (mess instanceof MessageProbeLeader){
          long problid = (long) mess.getContent();
          //System.out.print("Node "+node.getID() + " :: RECV PROBE LEADER "+problid+" from node "+mess.getIdSrc()+" : ");
          if(ep.idLeader == problid && node.getID() != problid){
            if(ep.parentToLeader == NONE){
              ep.parentToLeader = mess.getIdSrc();
            }
            /*This condition is used if we switch our place with our old parent
            if(ep.idLeader == mess.getIdSrc()){
              ep.parentToLeader = mess.getIdSrc();
            }*/
            if (ep.parentToLeader == mess.getIdSrc()){
              MessageProbeLeader m;
              ep.timerLeader = DELTALEADER;
              //System.out.println(" PROPAGATE");
              for (int i = 0; i < ep.neighbors.size(); i++){
                if(ep.parentToLeader != ep.neighbors.get(i)
                		&& problid != ep.neighbors.get(i)){
                  m = new MessageProbeLeader(node.getID(),
                                            ep.neighbors.get(i),
                                            problid,
                                            protocol_id);
                  emitter.emit(node,m);
                }
              }
            }
            else{
              //System.out.println(" NOT MY PARENT "+ep.parentToLeader);
            }
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
    EmitterImpl emitter  = (EmitterImpl) node.getProtocol(emitter_pid);
    ElectionProtocolImpl ep = (ElectionProtocolImpl) node.getProtocol(protocol_id);

    //First call
    if(ep.idLeader == NONE){
      //System.out.println("NO LEADER");
      ep.idLeader = node.getID();
      ep.timerLeader = 0;
      ep.valueLeader = ep.myValue;
      ep.ackContent.idLid = node.getID();
      ep.ackContent.valueLid = ep.myValue;
    }

    //Sends a Probe msg to all nodes in scope
    MessageProbe msg = new MessageProbe(node.getID(),Emitter.ALL, new Long(ep.idLeader), protocol_id);
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
            if(ep.parentToLeader == entry.getKey()){
              ep.parentToLeader = NONE;
            }
          //If it was the leader we triger a new election
           if(entry.getKey() == ep.idLeader){
              //System.out.println("LEADER "+entry.getKey()+" DISCONNECT");
              ep.triggerElection(node);
            }
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
          ep.idLeader = content.idLid;
          ep.valueLeader = content.valueLid;
 
          ep.timerLeader = DELTALEADER;

          if(ep.idLeader == node.getID()){
            ep.timerLeader = 0;
          }
          //System.out.println(" new leader ");
          MessageLeader prop;
          for(int i = 0; i < ep.neighbors.size(); i++){
            if(ep.pendingMsgLeader.getIdSrc() != ep.neighbors.get(i)){
              prop = new MessageLeader(node.getID(), ep.neighbors.get(i), new MessContent(null, ep.idLeader, ep.valueLeader), protocol_id);
              emitter.emit(node, prop);
            }
          }
        }else{
          //System.out.println(" stay the same");      
        }

        ep.pendingMsgLeader = null;
        ep.idPendingMsgLeader = NONE;
      }
      if(ep.idLeader == node.getID()){
        if(ep.timerLeader % (DELTALEADER/2) == 0)
          emitter.emit(node, new MessageProbeLeader(node.getID(), Emitter.ALL, node.getID(), protocol_id));

        ep.timerLeader = (ep.timerLeader + 1)%(DELTALEADER/2);

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

    ep = ((ElectionProtocolImpl) n.getProtocol(protocol_id));
    idelec = new IDElection(ep.getMyNumElec(), n.getID(), 1);

    //System.out.println(idelec+" Node "+n.getID()+" triggers an election");
    ep.idLeader = n.getID();
    ep.valueLeader = ep.myValue;
    ep.timerLeader = DELTALEADER;
    ep.parentToLeader = NONE;
    ep.inElection = true;
    ep.currElec = idelec;
    ep.srcElec = n.getID();
    ep.myNumElec ++;
    
    ep.parent = null;
    ep.waitedNeighbors = new ArrayList<Long>();
    ep.waitedNeighbors.addAll(ep.neighbors);
    ep.timerChild = new HashMap<Long, Integer>();

    for (int i = 0; i < ep.waitedNeighbors.size(); i++){
      ep.timerChild.put(ep.waitedNeighbors.get(i), DELTACHILD);
    }
    ep.ackContent.idElec = idelec;
    ep.ackContent.idLid = n.getID();
    ep.ackContent.valueLid = ep.myValue;

    msg = new MessageElection(n.getID(), Emitter.ALL, idelec, protocol_id);
    em = ((EmitterImpl) n.getProtocol(emitter_pid));
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
    EmitterImpl emitter = (EmitterImpl) node.getProtocol(emitter_pid);

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
        ep.idLeader = ep.ackContent.idLid;
        ep.valueLeader = ep.ackContent.valueLid;
        ep.timerLeader = DELTALEADER;
        ep.inElection = false;
        ep.pendingAck = false;
        ep.currElec = null;
        ep.srcElec = NONE;
        ep.parent = null;
        ep.pendingAck = false;

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

}

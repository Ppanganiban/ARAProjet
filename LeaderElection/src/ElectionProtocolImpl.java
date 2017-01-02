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

  private static final int DELTA = 3;
  
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

  private long refToLeader;
  private List<Long> neighbors;
  private List<Long> waitedNeighbors;
  private HashMap<Long, Integer> timerNeighbor;

  private MessageLeader pendingMsgLeader;
  private long idPendingMsgLeader;

  public ElectionProtocolImpl(String prefix) {
    String tmp[]= prefix.split("\\.");
    protocol_id = Configuration.lookupPid(tmp[tmp.length-1]);
    emitter_pid = Configuration.getPid(prefix+"."+PAR_EMITTER);

    inElection = false;
    idLeader = NONE;
    valueLeader = NONE;
    myNumElec = 0;
    pendingAck = false;
    myValue = (int) (CommonState.r.nextDouble() * MAXVAL);

    currElec = null;
    srcElec = NONE;
    parent = null;
    refToLeader = NONE;

    neighbors = new ArrayList<Long>();
    waitedNeighbors = new ArrayList<Long>();
    timerNeighbor = new HashMap<Long, Integer>();
    ackContent = new MessContent(currElec, NONE, NONE);

    pendingMsgLeader = null;
    idPendingMsgLeader = NONE;
  }

  public Object clone(){
    ElectionProtocolImpl ep = null;
    try{
      ep = (ElectionProtocolImpl) super.clone();
      ep.neighbors = new ArrayList<Long>();
      ep.waitedNeighbors = new ArrayList<Long>();
      ep.timerNeighbor = new HashMap<Long, Integer>();
      ep.myValue = (int) (CommonState.r.nextDouble() * MAXVAL);
      ep.ackContent = new MessContent(currElec, NONE, NONE);
      ep.pendingMsgLeader = null;
      ep.refToLeader = NONE;
      ep.idPendingMsgLeader = NONE;
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
            System.out.println("Node "+node.getID()+" new neighbor node "+mess.getIdSrc());
            emitter.emit(node, new MessageLeader(node.getID(), mess.getIdSrc(), new MessContent(null, ep.idLeader, ep.valueLeader), protocol_id) );
          }
          ep.timerNeighbor.put(mess.getIdSrc(), DELTA);
        }

        else if(mess instanceof MessageElection){
          IDElection idelec = (IDElection)mess.getContent();
          System.out.println("Node "+node.getID()+" :: MSG ELECTION rcv :"+idelec+" from Node "+mess.getIdSrc());

          /* If we are not in election
           * OR we are in a election process
           * We participate to it only if it is higher than the current
           * If it's not the case, we do not respond in order to finish the current election and it wille be notify during the LEADER PHASE
           */
          if (!ep.inElection || idelec.isHigherThan(ep.currElec)){
            System.out.println("Node "+node.getID()+" :: participates to this election :"+idelec+" prev: "+ep.currElec);
            ep.currElec = idelec;
            ep.srcElec = idelec.getId();
            ep.parent = ep.getNodeWithId(mess.getIdSrc());
            ep.inElection = true;
            ep.waitedNeighbors = new ArrayList<Long>();
            ep.waitedNeighbors.addAll(ep.neighbors);
            ep.waitedNeighbors.remove(ep.parent.getID());
            ep.ackContent = new MessContent(idelec, node.getID(), ep.myValue);
            ep.refToLeader = NONE;

            System.out.println(ep.currElec+" Node "+node.getID()+" is a child of Node "+ep.parent.getIndex());

            //We propagate the election to ours neighbors
            MessageElection prop;
            for(int i = 0; i < ep.waitedNeighbors.size(); i++){
              System.out.println(ep.currElec+" node "+node.getID()+ " propagates election to node "+ep.waitedNeighbors.get(i));
              prop = new MessageElection(node.getID(), ep.waitedNeighbors.get(i), idelec, protocol_id);
              emitter.emit(node, prop);                
            }
          }
          else if (ep.inElection
              && idelec.isEqualTo(ep.currElec)
              && ep.srcElec != node.getID()
              && ep.parent.getID() != mess.getIdSrc()){
              emitter.emit(node, new MessageAck(node.getID(), mess.getIdSrc(), new MessContent(ep.currElec, NONE, NONE), ep.protocol_id));
          }
        }
        
        else if(mess instanceof MessageAck){
          MessContent content = (MessContent) mess.getContent();
          IDElection idelec = content.idElec;
          System.out.println(ep.currElec+" Node "+node.getID()+" rcv "+content+" from Node"+ mess.getIdSrc());

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
              else{
            	  System.out.println("Node "+mess.getIdSrc()+" is not child of Node "+node.getID());
              }
              ep.waitedNeighbors.remove(mess.getIdSrc());
            }
          }
        }

        //We can rcv a leader MSG 
        else if(mess instanceof MessageLeader){
          MessContent content = (MessContent) mess.getContent();
          IDElection idelec = content.idElec;

          System.out.println("Node "+node.getID() + " :: LEADER "+content+ "from node "+mess.getIdSrc());

          if (ep.inElection && ep.currElec.isEqualTo(idelec)){
            System.out.println("Node "+node.getID() + " :: NEW LEADER "+content);
            ep.idLeader = content.idLid;
            ep.valueLeader = content.valueLid;
            ep.refToLeader = mess.getIdSrc(); //the src node of the msg will be our reference to the leader
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
              System.out.print("Node "+node.getID() + " :: EVALUATE LEADER "+content+" : ");
              if(ep.valueLeader < content.valueLid){
                ep.idLeader = content.idLid;
                ep.valueLeader = content.valueLid;
                ep.refToLeader = mess.getIdSrc();

                System.out.println(" new leader ");
                MessageLeader prop;
                for(int i = 0; i < ep.neighbors.size(); i++){
                  if(mess.getIdSrc() != ep.neighbors.get(i)){
                    prop = new MessageLeader(node.getID(), ep.neighbors.get(i), new MessContent(null, ep.idLeader, ep.valueLeader), protocol_id);
                    emitter.emit(node, prop);
                  }
                }
              }else{
                System.out.println(" stay the same");      
              }
            }
            else{
              System.out.println("Node "+node.getID() + " :: already in election process "+ep.currElec+". Put it in pendingMSGLeader "+content+" : ");
              if(ep.pendingMsgLeader == null
                || ((MessContent)ep.pendingMsgLeader.getContent()).valueLid < content.valueLid){
                ep.pendingMsgLeader = (MessageLeader) mess;
                ep.idPendingMsgLeader = mess.getIdSrc();
              }
              ep.waitedNeighbors.remove(mess.getIdSrc());
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
      System.out.println("NO LEADER");
      ep.idLeader = node.getID();
      ep.refToLeader = node.getID();
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
          System.out.println("Node "+node.getID()+" remove neighbor Node "+entry.getKey());
          //If we waited this neighbor for an election we remove it
          if(ep.inElection) {
            ep.waitedNeighbors.remove(entry.getKey());
  
            //If it was our parent we become the new src of leader election
            if(ep.parent != null && ep.parent.getID() == entry.getKey()){
              System.out.println("Node "+node.getID()+" is the new src");
              ep.srcElec = node.getID();
            }
          }
          else{
            if(ep.idPendingMsgLeader == entry.getKey() ){
              ep.idPendingMsgLeader = NONE;
              ep.pendingMsgLeader = null;
            }
          //If it was the leader we triger a new election
            //if(entry.getKey() == ep.idLeader){
              //System.out.println("LEADER "+entry.getKey()+" DISCONNECT");
              //ep.triggerElection(node);
            //}
            if (entry.getKey() == ep.refToLeader){
              System.out.println("Node "+node.getID()+" loses leader reference node "+ep.refToLeader);
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
        
        System.out.print("Node "+node.getID() + " :: EVALUATE PENDING LEADER message"+content+" : ");
        if(ep.valueLeader < content.valueLid){
          ep.idLeader = content.idLid;
          ep.valueLeader = content.valueLid;
          ep.refToLeader = ep.pendingMsgLeader.getIdSrc();
          System.out.println(" new leader ");
          MessageLeader prop;
          for(int i = 0; i < ep.neighbors.size(); i++){
            if(ep.pendingMsgLeader.getIdSrc() != ep.neighbors.get(i)){
              prop = new MessageLeader(node.getID(), ep.neighbors.get(i), new MessContent(null, ep.idLeader, ep.valueLeader), protocol_id);
              emitter.emit(node, prop);
            }
          }
        }else{
          System.out.println(" stay the same");      
        }

        ep.pendingMsgLeader = null;
        ep.idPendingMsgLeader = NONE;
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
    idelec = new IDElection(ep.getMyNumElec(), n.getID());

    System.out.println(idelec+" Node "+n.getID()+" triggers an election");
    ep.idLeader = n.getID();
    ep.valueLeader = ep.myValue;
    ep.inElection = true;
    ep.currElec = idelec;
    ep.srcElec = n.getID();
    ep.myNumElec ++;
    
    ep.parent = null;
    ep.refToLeader = n.getID();
    ep.waitedNeighbors = new ArrayList<Long>();
    ep.waitedNeighbors.addAll(ep.neighbors);
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

    if(ep.waitedNeighbors.size() == 0 && ep.inElection){
      //if we are the src we broadcast LEADER MSG
      if(node.getID() == ep.srcElec){
        System.out.println(ep.currElec+" Node "+node.getID()+ " root broadcast Leader "+ep.ackContent);            
        ep.idLeader = ep.ackContent.idLid;
        ep.refToLeader = node.getID();
        ep.valueLeader = ep.ackContent.valueLid;
        ep.inElection = false;
        ep.pendingAck = false;
        ep.currElec = null;
        ep.srcElec = NONE;
        ep.parent = null;
        ep.pendingAck = false;

        emitter.emit(node, new MessageLeader(node.getID(), Emitter.ALL, ep.ackContent, protocol_id));
      }
      else if (!ep.pendingAck){
        System.out.println(ep.currElec+" Node "+node.getID()+ " sends ack "+ep.ackContent+" to node "+ep.parent.getID());
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

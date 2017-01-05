/**
 * IDElection represents the id of an election.
 * It is composed by a sequence number and the id of the node source.
 * @author PAUL PANGANIBAN & THOMAS MAGALHAES
 *
 */
public class IDElection {
	private long num;
	private long id;
	private int nbHop;
	private long oldLeader;

	public long getNum() {
		return num;
	}

	public long getId() {
		return id;
	}

	public IDElection(long num, long id, int nbHop, long oldLeader){
		this.num = num;
		this.id = id;
		this.nbHop = nbHop;
		this.oldLeader = oldLeader;
	}
	
	public boolean isHigherThan(IDElection idElec){
		if( idElec == null
			||(num > idElec.getNum())
			|| ((num == idElec.getNum()) && (id > idElec.getId()))
			){
			return true;
		}
		return false;
	}

	public boolean isLowerThan(IDElection idElec){
		if( idElec == null
			||(num < idElec.getNum())
			|| ((num == idElec.getNum()) && (id < idElec.getId()))
			){
			return true;
		}
		return false;
	}

	public boolean isEqualTo(IDElection idElec){
		if(idElec != null && num == idElec.getNum() && id == idElec.getId())
			return true;
		return false;
	}

	public void setId(long id){
		this.id = id;
	}
	
	public String toString(){
		return "<"+num+","+id+">";
	}

  public int getNbHop() {
    return nbHop;
  }

  public void setNbHop(int nbHop) {
    this.nbHop = nbHop;
  }

  public long getOldLeader() {
    return oldLeader;
  }

  public void setOldLeader(long oldLeader) {
    this.oldLeader = oldLeader;
  }
}

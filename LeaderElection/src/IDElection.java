/**
 * IDElection represents the id of an election.
 * It is composed by a sequence number and the id of the node source.
 * @author PAUL PANGANIBAN & THOMAS MAGALHAES
 *
 */
public class IDElection {
	private long num;
	private long id;
	
	public long getNum() {
		return num;
	}

	public long getId() {
		return id;
	}

	public IDElection(long num, long id){
		this.num = num;
		this.id = id;
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
}


public class IDElection {
	private long num, id;
	private int nbHop;

	public long getNum() {
		return num;
	}

	public long getId() {
		return id;
	}

	public IDElection(long num, long id, int nbHop){
		this.num = num;
		this.id = id;
		this.nbHop = nbHop;
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
}

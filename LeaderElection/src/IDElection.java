
public class IDElection {
	private long num, id;
	
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

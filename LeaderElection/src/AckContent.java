
public class AckContent {
	public IDElection idElec;
	public long idLid;
	public long valueLid;
	
	public AckContent(IDElection idElec, long idLid, long valueLid){
		this.idElec = idElec;
		this.idLid = idLid;
		this.valueLid = valueLid;
	}
	
	public String toString(){
		return "<ACK "+idElec+" Lid : "+idLid+" value : "+ valueLid+ ">";
	}
}

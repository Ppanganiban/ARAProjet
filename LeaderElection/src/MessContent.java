
public class MessContent {
	public IDElection idElec;
	public long idLid;
	public long valueLid;
	
	public MessContent(IDElection idElec, long idLid, long valueLid){
		this.idElec = idElec;
		this.idLid = idLid;
		this.valueLid = valueLid;
	}
	
	public String toString(){
		return "<ACK "+idElec+" Lid : "+idLid+" value : "+ valueLid+ ">";
	}
}

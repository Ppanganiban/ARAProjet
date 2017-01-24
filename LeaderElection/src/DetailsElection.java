/**
 * Class used for message Election.
 * It contains the id of election, the number of hop done and the old leader
 * @author PAUL PANGANIBAN & THOMAS MAGALHAES
 *
 */
public class DetailsElection {
	private IDElection idelec;
	private int nbHop;
	private long oldLeader;
	
	public DetailsElection(IDElection idelec, int nbHop, long oldLeader) {
		// TODO Auto-generated constructor stub
		this.idelec = idelec;
		this.nbHop	= nbHop;
		this.oldLeader = oldLeader;
	}

	public IDElection getIdelec() {
		return idelec;
	}

	public void setIdelec(IDElection idelec) {
		this.idelec = idelec;
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

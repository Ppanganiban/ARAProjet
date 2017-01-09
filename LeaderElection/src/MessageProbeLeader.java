
public class MessageProbeLeader extends Message{
	private static final String PROBELEADER = "PROBELEADER";
	private long numseq;
	
	public MessageProbeLeader(long idsrc, long iddest, Long idLeader,long numseq, int pid) {
		super(idsrc, iddest, PROBELEADER, idLeader, pid);
		this.setNumseq(numseq);
	}

  public long getNumseq() {
    return numseq;
  }

  public void setNumseq(long numseq) {
    this.numseq = numseq;
  }  
}

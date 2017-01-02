
public class MessageProbeLeader extends Message{
	private static final String PROBELEADER = "PROBELEADER";

	public MessageProbeLeader(long idsrc, long iddest, Long idLeader,int pid) {
		super(idsrc, iddest, PROBELEADER, idLeader, pid);
	}  
}

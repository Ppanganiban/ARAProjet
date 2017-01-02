
public class MessageProbe extends Message{
	private static final String PROBE = "PROBE";

	public MessageProbe(long idsrc, long iddest, Long idLeader,int pid) {
		super(idsrc, iddest, PROBE, idLeader, pid);
	}  
}

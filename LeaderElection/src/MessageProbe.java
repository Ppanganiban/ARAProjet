
public class MessageProbe extends Message{
	private static final String PROBE = "PROBE";
	
	public MessageProbe(long idsrc, long iddest, int pid) {
		super(idsrc, iddest, PROBE, null, pid);
	}

}

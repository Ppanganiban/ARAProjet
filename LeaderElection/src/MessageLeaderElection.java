
public class MessageLeaderElection extends Message{
	private static final String PROBE = "PROBE";
	private static final String ELECTION = "ELECTION";
	private static final String ACK = "ACK";

	public MessageLeaderElection(long idsrc, long iddest, String tag, Object content, int pid) {
		super(idsrc, iddest, tag, content, pid);
		if(!tag.equals(PROBE) && !tag.equals(ELECTION) && !tag.equals(ACK)){
			System.err.println("WRONG MESSAGE TAG");
		}
	}

}

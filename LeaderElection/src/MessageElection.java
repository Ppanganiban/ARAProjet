
public class MessageElection extends Message{
	private static final String ELECTION = "ELECTION";

	public MessageElection(long idsrc, long iddest, DetailsElection details, int pid) {
		super(idsrc, iddest, ELECTION, details, pid);
	}
}


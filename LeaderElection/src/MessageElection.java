
public class MessageElection extends Message{
	private static final String ELECTION = "ELECTION";

	public MessageElection(long idsrc, long iddest, IDElection idElec, int pid) {
		super(idsrc, iddest, ELECTION, idElec, pid);
	}
}


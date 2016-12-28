
public class MessageLeader extends Message{
	private static final String LEADER = "LEADER";

	public MessageLeader(long idsrc, long iddest, AckContent content, int pid) {
		super(idsrc, iddest, LEADER, content, pid);
	}
}

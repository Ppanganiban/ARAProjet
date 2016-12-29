
public class MessageAck extends Message{
	private static final String ACK = "ACK";

	public MessageAck(long idsrc, long iddest, MessContent content, int pid) {
		super(idsrc, iddest, ACK, content, pid);
	}
}

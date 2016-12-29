import peersim.core.Node;
import peersim.transport.Transport;

public class Fifo implements Transport{

	@Override
	public long getLatency(Node src, Node dest) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void send(Node src, Node dest, Object msg, int pid) {
		// TODO Auto-generated method stub
		
	}
	public Object clone(){
		EmitterImpl ei = null;
		try{
			ei = (EmitterImpl) super.clone();
		}
		catch( CloneNotSupportedException e ) {} // never happens
		return ei;
	}
}

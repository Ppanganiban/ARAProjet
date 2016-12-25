import java.util.ArrayList;

import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;

public class EmitterImpl implements Emitter{
	private static final String PAR_ELECTIONPID = "electionprotocol";
	private static final String PAR_POSITIONPID = "positionprotocol";

	private final int protocol_id;
	private final int election_pid;
	private final int position_pid;

	private static int scope;
	private static int latency;

	public EmitterImpl(String prefix) {
		String tmp[]= prefix.split("\\.");
		protocol_id = Configuration.lookupPid(tmp[tmp.length-1]);
		election_pid = Configuration.getPid(prefix+"."+PAR_ELECTIONPID);
		position_pid = Configuration.getPid(prefix+"."+PAR_POSITIONPID);

		scope = Configuration.getInt(prefix+"."+"scope");
		latency = Configuration.getInt(prefix+"."+"latency");
	}


	@Override
	public void emit(Node host, Message msg) {
		for(int i = 0; i < Network.size(); i++){
			if (Network.get(i) != host)
				((EDProtocol) host.getProtocol(election_pid)).processEvent(Network.get(i), election_pid, msg);				
		}
	}

	public Object clone(){
		EmitterImpl ei = null;
		try{
			ei = (EmitterImpl) super.clone();
		}
		catch( CloneNotSupportedException e ) {} // never happens
		return ei;
	}

	@Override
	public int getLatency() {
		return latency;
	}

	@Override
	public int getScope() {
		return scope;
	}

}
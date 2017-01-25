import java.util.HashMap;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public class EndController implements Control{
	private static final String PAR_PERFPID = "perfprotocol";
	private final int perf_pid;

	public EndController(String prefix) {
		perf_pid = Configuration.getPid(prefix+"."+PAR_PERFPID);
	}
	@Override
	public boolean execute() {
		double F = 0; //fraction of time without leader
		double eF = 0;

		double R = 0; //Election rate

		double T = 0; //Election time
		double eT = 0;

		double L = 0; //Average of leader during the experience

		double M = 0; //Message overhead
		double eM = 0;

    HashMap<IDElection, Boolean> allElection = new HashMap<IDElection, Boolean>();

		PerformanceProtocol perf;
		Node node;

		int nbNodeDidElection = 0;

		for(int i = 0; i< Network.size(); i++){
			node = Network.get(i);
			perf = (PerformanceProtocol) node.getProtocol(perf_pid);
			F += perf.getTimeWithoutLeader();

			R += perf.getTimeInElection();

			if(perf.getAllElection().size() > 0){
				nbNodeDidElection ++;
				M += (perf.getMessageOverhead()/perf.getAllElection().size());

				T += (perf.getTimeInElection()/perf.getAllElection().size());

				allElection.putAll(perf.getAllElection());

			}

			L += perf.getNbTimesLeader();

		}

		M /= nbNodeDidElection;
		T /= nbNodeDidElection;
		F /= Network.size()*CommonState.getEndTime();
		R = R / (Network.size() * CommonState.getEndTime());
		L /= CommonState.getEndTime();

		for(int i = 0; i< Network.size(); i++){
			node = Network.get(i);
			perf = (PerformanceProtocol) node.getProtocol(perf_pid);

			eF += Math.pow(perf.getTimeWithoutLeader()/CommonState.getEndTime() - F, 2);

			if(perf.getAllElection().size() > 0){
				eM += Math.pow(perf.getMessageOverhead()/perf.getAllElection().size() - M, 2);
				eT += Math.pow(perf.getTimeInElection()/perf.getAllElection().size() - T, 2);
			}
		}

		eM /= nbNodeDidElection;
		eT /= nbNodeDidElection;
		eF /= Network.size();

		eM = Math.sqrt(eM);
    eT = Math.sqrt(eT);
		eF = Math.sqrt(eF);

/*		
		System.out.println("Fraction of time without leader : "+F/CommonState.getEndTime());
    System.out.println("Standard Deviation : "+eF+"\n");
		System.out.println("Election rate : "+R+"\n");
		System.out.println("Election Time : "+T+" ms");
    System.out.println("Standard Deviation : "+eT+"\n");
		System.out.println("Average of leader : "+L);
    System.out.println("Standard Deviation : "+eL+"\n");
		System.out.println("Message overhead : "+M);
    System.out.println("Standard Deviation : "+eM+"\n");
*/
		System.out.format("%-10.6f%n",F);
		System.out.format("%-10.6f%n\n",eF);
		System.out.format("%-10.6f%n\n",R);
		System.out.format("%-10.6f%n",T);
		System.out.format("%-10.6f%n\n",eT);
		System.out.format("%-10.6f%n\n",L);
		System.out.format("%-10.6f%n",M);
		System.out.format("%-10.6f%n\n",eM);
		return false;
	}

}

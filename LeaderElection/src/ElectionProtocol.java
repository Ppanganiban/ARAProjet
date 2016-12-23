

import java.util.List;

import peersim.edsim.EDProtocol;

public interface ElectionProtocol extends EDProtocol {

	/*Indique si le site est en cours d'élection ou pas*/
	public boolean isInElection();
	
	/*si le site n'est pas en élection renvoie l'indentité du leader, -1 sinon*/
	public long getIDLeader();
	
	/*renvoie la valeur associée au noeud, plus cette valeur est elevée plus le site a des chances d'être élu*/
	public int getMyValue();
	
	
	/*Renvoie la liste courante des Id des voisins directs*/
	public List<Long> getNeighbors();
}

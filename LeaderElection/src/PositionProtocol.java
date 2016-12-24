import peersim.edsim.EDProtocol;

public interface PositionProtocol extends EDProtocol {
	
	/*Renvoie la coordonnée x, 0 <= x <=MaxX*/
	public double getY();
	
	/*Renvoie la coordonnée y, 0 <= y <=MaxY */
	public double getX();
	
	/*Renvoie la vitesse maximale qu'un noeud peut avoir*/
	public int getMaxSpeed();
	
	/*Renvoie l'abscisse maximale (largeur du terrain)*/
	public double getMaxX();
	
	
	/*Renvoie l'ordonnée maximale (largeur du terrain)*/
	public double getMaxY();
	
	
	/*Renvoie le temps d'immobilité*/
	public int getTimePause();
}

import peersim.config.Configuration;
import peersim.core.Node;

public class PositionProtocolImpl implements PositionProtocol{
	private final int protocol_id;

	private static int maxSpeed;
	private static double maxX;
	private static double maxY;
	private static int timePause;

	private double xDest, yDest;
	private double currSpeed;
	private int waitTime; //0 : inMotion , >0 : time to wait
	
	private double x, y; //Current position of the node
	
	public PositionProtocolImpl(String prefix) {
		String tmp[]=prefix.split("\\.");
		protocol_id=Configuration.lookupPid(tmp[tmp.length-1]);

		maxSpeed = Configuration.getInt(prefix+".maxSpeed");
		timePause = Configuration.getInt(prefix+".timePause");
		maxX = Configuration.getDouble(prefix+".maxX");
		maxY = Configuration.getDouble(prefix+".maxY");

		x = Math.random() * maxX;
		y = Math.random() * maxY;

		xDest = Math.random() * maxX;
		yDest = Math.random() * maxY;

		currSpeed = (int)(Math.random() * maxSpeed + 1);
		waitTime = 0;
		
	}

	public Object clone(){
		PositionProtocolImpl pp = null;
		try {
			pp = (PositionProtocolImpl) super.clone();

			pp.x = Math.random() * maxX;
			pp.y = Math.random() * maxY;

			pp.xDest = Math.random() * maxX;
			pp.yDest = Math.random() * maxY;

			pp.currSpeed = (int)(Math.random() * maxSpeed + 1);
			pp.waitTime = 0;
			
		}
		catch( CloneNotSupportedException e ) {} // never happens
		return pp;
	}

	@Override
	public void processEvent(Node node, int pid, Object event) {
		// TODO Auto-generated method stub
		System.out.println("BLABLA");
		if(protocol_id != pid){
			throw new RuntimeException("Receive Message for wrong protocol");
		}
	}

	public int getMaxSpeed() {
		return maxSpeed;
	}

	public double getX() {
		//If the node was in motion
		if(waitTime == 0){
			//we compute the new position of x if it is not at destination
			if(x != xDest && y != yDest){
				double px, py, ph;
				double dx;

				//Pythagore theorem
				px = Math.abs(x - xDest);
				py = Math.abs(y - yDest);
				ph = Math.sqrt(Math.pow(px, 2) + Math.pow(py, 2));

				//Thales theorem
				dx = currSpeed * px / ph;

				if (x > xDest)
					if(x - dx > xDest)
						x -= dx;
					else
						x = xDest;
				else
					if(x + dx < xDest)
						x += dx;
					else
						x = xDest;				
			}
			//We set up the new destination
			else{
				xDest = Math.random() * maxX;
				yDest = Math.random() * maxY;
				currSpeed = (int)(Math.random() * maxSpeed + 1);
				waitTime = timePause;
			}
		}else{
			waitTime --;
		}
		return x;
	}

	public double getY() {
		//If the node was in motion
		if(waitTime == 0){
			//we compute the new position of x if it is not at destination
			if(x != xDest && y != yDest){
				double px, py, ph;
				double dy;

				//Pythagore theorem
				px = Math.abs(x - xDest);
				py = Math.abs(y - yDest);
				ph = Math.sqrt(Math.pow(px, 2) + Math.pow(py, 2));

				//Thales theorem
				dy = currSpeed * py / ph;

				if (y > yDest)
					if(y - dy > yDest)
						y -= dy;
					else
						y = yDest;
				else
					if(y + dy < yDest)
						y += dy;
					else
						y = yDest;				
			}
		}

		//We don't decrement the waittime value because getX did it.
		return y;
	}

	public double getMaxX() {
		return maxX;
	}

	public double getMaxY() {
		return maxY;
	}

	public int getTimePause() {
		return timePause;
	}

}

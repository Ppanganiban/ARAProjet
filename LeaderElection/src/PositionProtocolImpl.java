import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;

public class PositionProtocolImpl implements PositionProtocol{
	private final int protocol_id;

	private static int maxSpeed, minSpeed;
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
    minSpeed = Configuration.getInt(prefix+".minSpeed");
		timePause = Configuration.getInt(prefix+".timePause");
		maxX = Configuration.getDouble(prefix+".maxX");
		maxY = Configuration.getDouble(prefix+".maxY");

		x = CommonState.r.nextDouble() * maxX;
		y = CommonState.r.nextDouble() * maxY;
		xDest = CommonState.r.nextDouble() * maxX;
		yDest = CommonState.r.nextDouble() * maxY;

		currSpeed = (int)(CommonState.r.nextDouble() * (maxSpeed - minSpeed) ) + minSpeed;
		waitTime = 0;
		
	}

	public Object clone(){
		PositionProtocolImpl pp = null;
		try {
			pp = (PositionProtocolImpl) super.clone();

			pp.x = CommonState.r.nextDouble() * maxX;
			pp.y = CommonState.r.nextDouble() * maxY;

			pp.xDest = CommonState.r.nextDouble() * maxX;
			pp.yDest = CommonState.r.nextDouble() * maxY;
			pp.currSpeed = (int)(CommonState.r.nextDouble() * (maxSpeed - minSpeed) ) + minSpeed;
			pp.waitTime = 0;
			
		}
		catch( CloneNotSupportedException e ) {} // never happens
		return pp;
	}

	@Override
	public void processEvent(Node node, int pid, Object event) {
	  PositionProtocolImpl pos = (PositionProtocolImpl) node.getProtocol(protocol_id);
		if(protocol_id != pid){
			throw new RuntimeException("Receive Message for wrong protocol");
		}
		if(pos.waitTime == 0){
			//we compute the new position of x if it is not at destination
			if(pos.x != pos.xDest || pos.y != pos.yDest){
			  double px, py, ph;
				double dx, dy;

				//Pythagore theorem
				px = Math.abs(pos.x - pos.xDest);
				py = Math.abs(pos.y - pos.yDest);
				ph = Math.sqrt(Math.pow(px, 2) + Math.pow(py, 2));

				//Thales theorem
				dx = (pos.currSpeed/1000) * px / ph;
				if (pos.x > pos.xDest)
					if(pos.x - dx > pos.xDest)
					  pos.x -= dx;
					else
					  pos.x = pos.xDest;
				else
					if(pos.x + dx < pos.xDest)
					  pos.x += dx;
					else
					  pos.x = pos.xDest;
				
				dy = (pos.currSpeed/1000) * py / ph;

				if (pos.y > pos.yDest)
					if(pos.y - dy > pos.yDest)
					  pos.y -= dy;
					else
					  pos.y = pos.yDest;
				else
					if(pos.y + dy < pos.yDest)
					  pos.y += dy;
					else
					  pos.y = pos.yDest;
			}
			//We set up the new destination
			else{
			  pos.xDest = CommonState.r.nextDouble() * maxX;
			  pos.yDest = CommonState.r.nextDouble() * maxY;
			  pos.currSpeed = (int)(CommonState.r.nextDouble() * (maxSpeed - minSpeed) ) + minSpeed;
			  pos.waitTime = timePause;
			}
		}else{
		  pos.waitTime --;
		}
	}

	public int getMaxSpeed() {
		return maxSpeed;
	}

	public double getX() {
		return x;
	}

	public double getY() {
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

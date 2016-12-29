import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JFrame;
import javax.swing.JPanel;

import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;

public class Monitor extends JPanel implements Control {

	private static final long serialVersionUID = -4639751772079773440L;

	private static final String PROBE = "PROBE";
	private static final String ELECTION = "ELECTION";
	private static final String ACK = "ACK";

	private static final String PAR_POSITIONPID = "positionprotocol";
	private static final String PAR_ELECTIONPID = "electionprotocol";
	private static final String PAR_EMITTER = "emitter";
	private static final String PAR_TIMESLOW = "time_slow";

	
	private final int position_pid;
	private final int election_pid;
	private final int emitter_pid;
	private final double time_slow;
	
	private final Dimension dimension_frame;
	
	private final Dimension dimension_terrain;
	private  JFrame frame = null;
	public Monitor(String prefix) {
		election_pid = Configuration.getPid(prefix+"."+PAR_ELECTIONPID);

		position_pid=Configuration.getPid(prefix+"."+PAR_POSITIONPID);
		
		emitter_pid=Configuration.getPid(prefix+"."+PAR_EMITTER);

		time_slow=Configuration.getDouble(prefix+"."+PAR_TIMESLOW);

		Node n = Network.get(0);
		PositionProtocol pos = (PositionProtocol) n.getProtocol(position_pid);

		Dimension dim_screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		dim_screen=new Dimension((int)(dim_screen.getWidth()*0.9), (int) (dim_screen.getHeight()*0.9));
		dimension_terrain = new Dimension((int)pos.getMaxX(),  (int)pos.getMaxY());

		int width = dimension_terrain.getWidth() > dim_screen.getWidth() ? (int)dim_screen.getWidth(): (int)dimension_terrain.getWidth();
		int height = dimension_terrain.getHeight() > dim_screen.getHeight() ? (int)dim_screen.getHeight(): (int)dimension_terrain.getHeight();
		
		dimension_frame=new Dimension(width, height);
		
	}
	
	
	private void init(){
		frame = new JFrame();
		frame.setTitle("MANET SYSTEM");
		
		
		
	    frame.setSize(dimension_frame);
	    
	    
	    frame.setLocationRelativeTo(null);               

	    this.setBackground(Color.WHITE);        
	    this.setSize(frame.getSize());
	    frame.getContentPane().add( this);  
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    frame.setVisible(true);
	}
	
	
	@Override
	protected void paintComponent(Graphics g) {
		g.setColor(this.getBackground());
		g.fillRect(0, 0, this.getWidth(), this.getHeight());
		for(int i = 0 ; i< Network.size() ; i++){
			Node n= Network.get(i);
			PositionProtocol pos = (PositionProtocol) n.getProtocol(position_pid);
			ElectionProtocol elec = (ElectionProtocol) n.getProtocol(election_pid);
			Emitter emitter = (Emitter) n.getProtocol(emitter_pid);
			int size = 10;
			int center_x=toGraphicX( pos.getX());
			int center_y=toGraphicY(pos.getY());
			
			
			int x_node=center_x-(size/2);
			int y_node=center_y-(size/2);
			
			
			g.setColor(Color.CYAN);
						
			int size_scope = toGraphicX(emitter.getScope());
			int x_scope=center_x-size_scope;
			int y_scope=center_y-size_scope;
			g.drawOval(x_scope,y_scope, size_scope*2, size_scope*2);
			
			
			g.setColor(Color.BLACK);
			g.drawString("Node"+n.getID(), x_node+size, y_node);
			g.drawString("value="+elec.getMyValue(), x_node+size, y_node+10);
			g.drawString("Leader="+elec.getIDLeader(), x_node+size, y_node+20);
	
			Long[] neighbors = new Long[elec.getNeighbors().size()];
			neighbors=elec.getNeighbors().toArray(neighbors);
			
			for(Long id : neighbors){
				Node neighbor = getNodefromId(id);
				PositionProtocol pos_neigh = (PositionProtocol) neighbor.getProtocol(position_pid);
				int center_x_neighbor=toGraphicX( pos_neigh.getX());
				int center_y_neighbor=toGraphicY(pos_neigh.getY());
				g.drawLine(center_x, center_y, center_x_neighbor, center_y_neighbor);
			}
			
			if(elec.isInElection()){
				g.setColor(Color.PINK);
			}else if(elec.getIDLeader() == n.getID()){
				g.setColor(Color.red);
			}else{	
				g.setColor(Color.GREEN);
			}
			g.fillOval(x_node,y_node, size, size);
		}
	}
	
	private Node getNodefromId(long id) {
		for(int i=0;i< Network.size();i++){
			Node n= Network.get(i);
			if(n.getID() == id){
				return n;
			}
		}
		throw new RuntimeException("Unknwon Id :"+id);
	}

	private int toGraphicX(double x_terrain){
		double res = (x_terrain * dimension_frame.getWidth()) / dimension_terrain.getWidth();
		return (int)res;
	}
	
	private int toGraphicY(double y_terrain){
		double res = (y_terrain * dimension_frame.getHeight()) / dimension_terrain.getHeight();
		return (int)res;
	}
	
 public static boolean firstElection = false;
 
	@Override
	public boolean execute() {
		/*if(firstElection){
			ElectionProtocolImpl ep;
			Node n;
			for(int i = 0; i < Network.size(); i++){
				n = Network.get(i);
				ep = ((ElectionProtocolImpl) n.getProtocol(election_pid));
				ep.triggerElection(n);
			}
			firstElection = false;
		}*/
		
		for(int i = 0; i < Network.size(); i++){
			EDSimulator.add(0, null, Network.get(i), election_pid);
		}
		for(int i = 0; i < Network.size(); i++){
			EDSimulator.add(0, null, Network.get(i), position_pid);
		}

		if(frame == null){
			init();			
			firstElection = true;
			ElectionProtocolImpl ep;
			Node n;
			for(int i = 0; i < Network.size(); i++){
				n = Network.get(i);
				ep = ((ElectionProtocolImpl) n.getProtocol(election_pid));
				ep.triggerElection(n);
			}
		}
		this.repaint();
		try {
			int nb_milisec=(int)time_slow;
			double nb_milisec_double = (double) nb_milisec;
			int nb_nano = (int)((time_slow-nb_milisec_double) * 1000000.0);
			Thread.sleep(nb_milisec, nb_nano);
		} catch (InterruptedException e) {}
		return false;
	}

}

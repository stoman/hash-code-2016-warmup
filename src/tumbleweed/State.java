package tumbleweed;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

public class State {
	public Input in;
	ArrayList<List<Delivery>> droneSchedules = new ArrayList<>();
	
	public State(Input in) {
		this.in = in;
		//TODO: generate empty state
		for (int i = 0; i < in.D; i++) {
			droneSchedules.add(new LinkedList<>());
		}
	}
	
	public String toString() {
		int Q = 0;
		
		int[] prod_nums = new int[in.P];
		StringBuilder result = new StringBuilder();
		for (int d = 0; d < droneSchedules.size(); ++d) {
			for (int i = 0; i < droneSchedules.get(d).size(); ++i) {
				for (int p = 0; p < in.P; ++p)
					prod_nums[p] = 0;
				for (int j = 0; j < droneSchedules.get(d).get(i).products.size(); ++j) 
					prod_nums[droneSchedules.get(d).get(i).products.get(j)]++;
				
				for (int p = 0; p < in.P; ++p)
					if (prod_nums[p] > 0) {
						result.append(d + " L " + droneSchedules.get(d).get(i).warehouse + " " + p + " " + prod_nums[p] + "\n");
						Q++;
					}
				for (int p = 0; p < in.P; ++p)
					if (prod_nums[p] > 0){
						result.append(d + " D " + droneSchedules.get(d).get(i).orderid + " " + p + " " + prod_nums[p] + "\n");
						Q++;
					}
			}
			
		}
		return Q + "\n" + result.toString();
	}
	
	public String visualize() {
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < in.C; i++) {
			sb.append("\"O" + i + "-" + in.c_l[i] + "\" ");
			sb.append(in.c_x[i]).append(" ").append(in.c_y[i]).append(" ").append(0).append("\n");
		}
		for (int i = 0; i < in.W; i++) {
			sb.append("\"W").append(i).append("\" ");
			sb.append(in.w_x[i]).append(" ").append(in.w_y[i]).append(" ").append(1).append("\n");
		}
		return sb.toString();
	}
	
	
	public String visualizeFreq() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < in.C; i++) {
			int sum = 0;
			for (int j = 0; j < in.c_l[i]; j++) {
				sum += in.weights[in.c_products[i][j]];
			}
			sb.append(sum).append('\n');
		}
		return sb.toString();
	}
	
	static ArrayList<List<Delivery>> currentSchedule;
	
	static int distTime(int x1, int y1, int x2, int y2) {
		int dist0 = (x2 - x1)*(x2 - x1);
		dist0 += (y2-y1)*(y2-y1);
		return (int)Math.ceil(Math.sqrt(dist0));
	}
	
	static int getTime(Input in, Delivery d, Drone drone) {
		int time = 0;
		time += distTime(drone.posX,drone.posY, in.w_x[d.warehouse],in.w_y[d.warehouse]);
		time += distTime(in.w_x[d.warehouse],in.w_y[d.warehouse],in.orders[d.orderid].x,in.orders[d.orderid].y);
		time += 2*d.numberOfProducts();
		return time;
	}
	
	static int getTime(Input in, List<Delivery> deliveries, List<Drone> drones) {
		currentSchedule = new ArrayList<>();
		for (int i = 0; i < in.D; i++) {
			currentSchedule.add(new LinkedList<>());
		}
		TreeSet<Drone> owndrones = new TreeSet<Drone>();
		for (Drone d : drones) {
			owndrones.add(new Drone(d));
		}
		int done = 0;
		int endTime = Integer.MIN_VALUE;
		while (done != deliveries.size()) {
			Drone d = owndrones.pollFirst();
			int best = Integer.MAX_VALUE;
			Delivery bestDel = null;
			for (Delivery del : deliveries) {
				if (del.done){
					continue;
				}
				int time = getTime(in,del,d);
				if (time < best) {
					best = time;
					bestDel = del;
				}
			}
			currentSchedule.get(d.id).add(bestDel);
			d.time += best;
			endTime = Math.max(endTime, d.time);
			d.posX = in.orders[bestDel.orderid].x;
			d.posY = in.orders[bestDel.orderid].x;
			owndrones.add(d);
			bestDel.done = true;
			done++;
		}
		
		for (Delivery d : deliveries) {
			d.done = false;
		}
		return endTime;
	}
	
	public static State solve(Input in) {
		State solved = new State(in);
		List<List<Delivery>> deliveries = in.planDeliveries();
		TreeSet<Drone> droneFinished = new TreeSet<>();
		for (int i=0; i < in.D; i++ ) {
			//fixme 0tes warehouse
			droneFinished.add(new Drone(i,0,in.w_x[0],in.w_y[0]));
		}
		
		while (!deliveries.isEmpty() && droneFinished.first().time < in.deadline) {
			List<Drone> freeDrones = new LinkedList<Drone>();
			int best = Integer.MAX_VALUE;
			List<List<Delivery>> bestSchedule = null;
			List<Delivery> bestOrder = null;
			for (Drone d : droneFinished) {
				// try for every subset of the drohnes
				freeDrones.add(d);
				for (List<Delivery> order : deliveries) {
					int timeForOrder = getTime(in,order, freeDrones);
					if (timeForOrder < best) {
						bestOrder = order;
						bestSchedule = currentSchedule;
						best = timeForOrder;
					}
	 			}
			}
			TreeSet<Drone> oldDroneFinished = droneFinished;
			droneFinished = new TreeSet<Drone>();
			while (!oldDroneFinished.isEmpty()) {
				Drone d = oldDroneFinished.pollFirst();
				List<Delivery> droneSchedule = bestSchedule.get(d.id);
				
				for (Delivery del : droneSchedule) {
					int time = getTime(in,del,d);
					d.time += time;
					d.posX = in.orders[del.orderid].x;
					d.posY = in.orders[del.orderid].x;
					solved.droneSchedules.get(d.id).add(del);
				}
				droneFinished.add(d);
			}
			deliveries.remove(bestOrder);
		}
		return solved;
	}
	
	static class Drone implements Comparable<Drone> {
		int id;
		int posX;
		int posY;
		int time;
		
		
		public Drone(Drone o) {
			id = o.id;
			posX = o.posX;
			posY = o.posY;
			time = o.time;
		}
		
		public Drone(int id, int posX, int posY, int time) {
			super();
			this.id = id;
			this.posX = posX;
			this.posY = posY;
			this.time = time;
		}
		
		public int compareTo(Drone o) {
			if (time == o.time){
				return (id - o.id);
			}
			return time - o.time;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Drone)) {
				return false;
			}
			Drone o = (Drone)obj;
			return o.id != id;
		}

		public String toString() {
			return "drone " + id + " at " + posX + "|" + posY + ", time: " + time;
		}
	}
}

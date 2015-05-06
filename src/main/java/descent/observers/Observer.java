package descent.observers;

import java.util.HashSet;

import peersim.config.Configuration;
import peersim.config.MissingParameterException;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import descent.observers.program.VarianceAndArcCountProgram;
import descent.rps.IDynamic;
import descent.rps.IRandomPeerSampling;

/**
 * Created by julian on 3/15/15.
 */
public class Observer implements Control {

	private static final String PAR_PROTOCOL = "protocol";

	// =============================================
	// C T O R
	// =============================================

	private int pid;
	private final ObserverProgram program;

	public Observer(String name) {
		this.pid = Configuration.lookupPid(Configuration.getString(name + "."
				+ PAR_PROTOCOL));

		this.program = new VarianceAndArcCountProgram();
		// this.program = new DebugProgram();
		// this.program = new PythonNetworkProgram();

	}

	// =============================================
	// E X E C
	// =============================================

	int counter = 0;
	final HashSet<Integer> alreadyCalculated = new HashSet<Integer>();

	public boolean execute() {

		if (!this.isLast) {
			final int STEP = 10;
			final DictGraph observer = DictGraph.getSingleton(Network.size());
			observer.reset();

			int max = Integer.MIN_VALUE;
			int min = Integer.MAX_VALUE;
			int count = 0, disconnected = 0;

			for (int i = 0; i < Network.size(); i++) {
				Node n = Network.get(i);
				IDynamic d = (IDynamic) n.getProtocol(pid);
				if (d.isUp()) {
					count += 1;
					IRandomPeerSampling pss = (IRandomPeerSampling) n
							.getProtocol(pid);
					observer.addStrict(n, pss);
					final int size = pss.getAliveNeighbors().size();
					if (size < min) {
						min = size;
					}
					if (size > max) {
						max = size;
					}
					if (size == 0) {
						disconnected++;
					}
				}
			}

			this.program.tick(CommonState.getTime(), observer);

			if (CommonState.getTime() == (CommonState.getEndTime() - 1)) {
				this.program.onLastTick(observer);
				this.isLast = true;
			}

		}

		return false;
	}

	boolean isLast = false;
	int lastSize = -1;
	int lastCount = 0;
	int lastCountTemp = 0;
	double firstVar = -1;

	private String print(int[] list) {
		StringBuilder sb = new StringBuilder();
		for (int i : list) {
			sb.append(i);
			sb.append("\n");
		}
		return sb.toString();
	}

	private double avgPathLength(DictGraph observer) {
		System.err.print("avg path length: [");
		double total = 0;
		total += observer.avgReachablePaths(0).avg; // 1
		System.err.print("#");
		total += observer.avgReachablePaths(observer.size() / 2).avg; // 2
		System.err.print("#");
		total += observer.avgReachablePaths(observer.size() / 3).avg; // 3
		System.err.print("#");
		total += observer.avgReachablePaths(observer.size() / 4).avg; // 4
		System.err.print("#");
		total += observer.avgReachablePaths(observer.size() / 8).avg; // 5
		System.err.print("#");
		total += observer.avgReachablePaths(observer.size() / 16).avg; // 6
		System.err.print("#");
		total += observer.avgReachablePaths(observer.size() - 1).avg; // 7
		System.err.print("#");
		// total += observer.avgReachablePaths(randomId()).avg; // 8
		// System.err.println("#");
		// total += observer.avgReachablePaths(randomId()).avg; // 9
		// System.err.println("#");
		// total += observer.avgReachablePaths(randomId()).avg; // 10
		System.err.println("]");
		return total / 7.0;
	}

	/**
	 *
	 * @param observer
	 * @return
	 */
	private double avgPathLengthRand(DictGraph observer) {
		System.err.println("avg path length: [");
		double total = 0;
		total += observer.avgReachablePaths(randomId()).avg; // 1
		System.err.println("#");
		total += observer.avgReachablePaths(randomId()).avg; // 2
		System.err.println("#");
		total += observer.avgReachablePaths(randomId()).avg; // 3
		System.err.println("#");
		total += observer.avgReachablePaths(randomId()).avg; // 4
		System.err.println("#");
		total += observer.avgReachablePaths(randomId()).avg; // 5
		System.err.println("#");
		total += observer.avgReachablePaths(randomId()).avg; // 6
		System.err.println("#");
		total += observer.avgReachablePaths(randomId()).avg; // 7
		System.err.println("#");
		// total += observer.avgReachablePaths(randomId()).avg; // 8
		// System.err.println("#");
		// total += observer.avgReachablePaths(randomId()).avg; // 9
		// System.err.println("#");
		// total += observer.avgReachablePaths(randomId()).avg; // 10
		System.err.println("]");
		return total / 7.0;
	}

	public static String printArray(int[] list) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.length; i++) {
			sb.append(list[i]);
			sb.append("\n");
		}
		return sb.toString();
	}

	/**
	 *
	 * @return
	 */
	private long randomId() {
		return Network.get(CommonState.r.nextInt(Network.size())).getID();
	}
}

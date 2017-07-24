package descent.observers;

import descent.observers.structure.DictGraph;
import descent.observers.structure.IObserverProgram;
import peersim.core.CommonState;

/**
 * This Program will print the network as Python-graph
 *
 * Created by julian on 02/05/15.
 */
public class PPythonNetwork implements IObserverProgram {

	final int step = 100;
	boolean isFirst = true;

	/**
	 *
	 * @param currentTick
	 * @param observer
	 */
	public void tick(long currentTick, DictGraph observer) {

		if (CommonState.getTime() >= 100){ //&& CommonState.getTime() % step == 0) {
			boolean imp = this.isFirst;
			this.isFirst = false;
			System.out
					.println("#=================START=================== step:"
							+ CommonState.getTime());
			System.out.println(observer.networkxDigraph(
					DictGraph.NetworkX.Draw,
					"g" + (CommonState.getTime()), imp));
			System.out.println("#=================END===================");
		}

	}

	/**
	 *
	 * @param observer
	 */
	public void onLastTick(DictGraph observer) {
		/*
		 * System.out.println("#=================START=================== step:"
		 * + CommonState.getTime());
		 * System.out.println(observer.networkxDigraph(
		 * DictGraph.NetworkX.Connectedness, "g" + (MAX_SIZE - observer.size()),
		 * true));
		 * System.out.println("#=================END===================");
		 */
	}
}

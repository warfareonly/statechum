package statechum.xmachine.model.testset;

import java.util.*;
import java.util.Map.Entry;

import statechum.JUConstants;
import statechum.analysis.learning.RPNIBlueFringeLearner;
import statechum.analysis.learning.TestFSMAlgo.FSMStructure;

import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.DirectedSparseGraph;



public class WMethod {

	private DirectedSparseGraph machineGraph;
	private FSMStructure fsm;
	private int numberOfExtraStates;
	private PrefixFreeCollection fullTestSet;
	private List<List<String>> transitionCover, characterisationSet;
	
	public WMethod(DirectedSparseGraph g, int numberOfExtraStates){
		this.machineGraph = g;
		this.fsm = null;
		this.numberOfExtraStates = numberOfExtraStates;
	}
	
	public WMethod(FSMStructure fsm, int numberOfExtraStates){
		this.machineGraph = null;
		this.fsm = fsm;
		this.numberOfExtraStates = numberOfExtraStates;
	}
	
	public Collection<List<String>> getFullTestSet(){
		if (fullTestSet == null)
			computeTestSet();
		return fullTestSet.getData();
	}
	/*
	public Set<List<String>> getFullTestSetStrings(){
		Iterator<Set<List<String>>> testSetIt = fullTestSet.iterator();
		HashSet<List<String>> testSet = new HashSet<List<String>>();
		while(testSetIt.hasNext()){
			Collection test = testSetIt.next();
			ArrayList<String> string = new ArrayList<String>();
			for(Object element:test){
				if(element instanceof Edge){
					Edge e = (Edge)element;
					Set labelSet = (Set)e.getUserDatum(JUConstants.LABEL);
					string.add(labelSet.toArray()[0].toString());
				}
				else 
					string.add((String)element);
			}
			testSet.add(string);
		}
		return testSet;
	}
	*/
	public List<List<String>> getCharacterisationSet() {
		return characterisationSet;
	}

	public List<List<String>> getTransitionCover() {
		return transitionCover;
	}

	/** Builds fsm structures corresponding to the Jung graph passed as an argument. The aim is
	 * to use the constructed structure for the comparison of different Jung graphs.
	 * 
	 * @param g graph from which to extract data
	 * @return the class storing transition information.
	 */
	public static FSMStructure getGraphData(DirectedSparseGraph g)
	{
		Iterator<DirectedSparseEdge> edgeIt = (Iterator<DirectedSparseEdge>)g.getEdges().iterator();
		FSMStructure extractedFSM = new FSMStructure();
		while(edgeIt.hasNext())
		{
			DirectedSparseEdge edge = edgeIt.next();
			Vertex fromVertex = edge.getSource(), toVertex = edge.getDest();
			String from = (String)fromVertex.getUserDatum(JUConstants.LABEL),
				to = (String)toVertex.getUserDatum(JUConstants.LABEL);
			Map<String,String> labelToTargetState = extractedFSM.trans.get(from);
			if (labelToTargetState == null)
			{
				labelToTargetState = new HashMap<String,String>();extractedFSM.trans.put(from, labelToTargetState);
			}
			createLabelToStateMap((Set<String>)edge.getUserDatum(JUConstants.LABEL),to,labelToTargetState);
		}
		
		Iterator<Vertex> vertexIt = (Iterator<Vertex>)g.getVertices().iterator();
		while(vertexIt.hasNext())
		{
			Vertex v = vertexIt.next();
			String name = (String)v.getUserDatum(JUConstants.LABEL);
			if (name == null)
				throw new IllegalArgumentException("unlabelled state encountered");
			
			if (extractedFSM.accept.containsKey(name))
				throw new IllegalArgumentException("multiple states with the same name "+name);
			
			extractedFSM.accept.put(name, 
					new Boolean(v.getUserDatum(JUConstants.ACCEPTED).toString()));
			Object initp = v.getUserDatum(JUConstants.PROPERTY);
			if (initp != null)
			{
				if (!JUConstants.INIT.equals(initp.toString()))
					throw new IllegalArgumentException("invalid init property");

				if (extractedFSM.init != null)
					throw new IllegalArgumentException("duplicate initial state "+name);

				extractedFSM.init = name;
			}
		}
		if (extractedFSM.init == null)
			throw new IllegalArgumentException("missing initial state");
		
		return extractedFSM;
	}
	
	/** Given a set of labels and a target state, this method adds to a supplied map an association 
	 * of every label with the specified target state.
	 * 
	 * @param labels labels
	 * @param to target state
	 * @param map a map associating state <i>to</i> with each of the labels. If this is <i>null</i>, a new map is created.
	 * @return an updated map.
	 */ 
	public static Map<String,String> createLabelToStateMap(Collection<String> labels,String to,Map<String,String> map)
	{
		Map<String,String> result = (map == null)? new HashMap<String,String>() : map;
		for(String label:labels)
		{
			if (result.containsKey(label))
				throw new IllegalArgumentException("nondeterminism detected for label "+label);
			result.put(label,to);
		}
		return result;
	}

	public static int tracePath(FSMStructure fsm, List<String> path)
	{
		return tracePath(fsm,path,fsm.init);
	}
	
	public static int tracePath(FSMStructure fsm, List<String> path, String startState)
	{
		String current = startState;
		int pos = -1;
		for(String label:path)
		{
			++pos;
			Map<String,String> exitingTrans = fsm.trans.get(current);
			if (exitingTrans == null || (current = exitingTrans.get(label)) == null)
				return pos;
		}
		return fsm.accept.get(current).booleanValue()? RPNIBlueFringeLearner.USER_ACCEPTED:pos;
	}


	/** converts a given sequence into
	 * a fundamental test sequence.
	 * 
	 * @return truncated sequence
	 */
	public static List<String> truncateSequence(FSMStructure fsm, List<String> path)
	{
		int pos = tracePath(fsm, path);
		List<String> seq = path;
		assert(pos == RPNIBlueFringeLearner.USER_ACCEPTED || pos < path.size());
		if (pos >= 0)
				seq = path.subList(0, pos+1);// up to a rejected position plus one
		return seq;
	}
	
	/** Given a database of sequences and a test sequence, converts a given sequence into
	 * a fundamental test sequence and appends a the result to the <em>sequences</em> set 
	 * if the fundamental test sequence is not a prefix of an existing sequence in that set.  
	 */
	public static void appendSequence(FSMStructure fsm, PrefixFreeCollection sequences, List<String> path)
	{
		List<String> seq = truncateSequence(fsm, path);
		sequences.addSequence(seq);
	}
	
	public static void appendAllSequences(FSMStructure fsm, PrefixFreeCollection sequences, List<List<String>> paths)
	{
		for(List<String> path:paths)
			appendSequence(fsm, sequences, path);
	}
	
	private void computeTestSet()
	{
		if (fsm == null)
			fsm = getGraphData(machineGraph);
		Set<String> alphabet =  computeAlphabet(fsm);
		List<List<String>> partialSet = computeStateCover(fsm), Phi = makeSingleton(alphabet);
		characterisationSet = computeWSet(fsm);if (characterisationSet.isEmpty()) characterisationSet.add(Arrays.asList(new String[]{}));
		transitionCover = cross(partialSet,Phi);transitionCover.addAll(partialSet);

		fullTestSet = new SlowPrefixFreeCollection();
		
		appendAllSequences(fsm, fullTestSet, cross(partialSet,characterisationSet));
		for(int i=0;i<=this.numberOfExtraStates;i++)
		{
			partialSet=cross(partialSet,Phi);
			appendAllSequences(fsm, fullTestSet, cross(partialSet,characterisationSet));
		}
	}
	
	/** Checks if the supplied FSM has unreachable states.
	 * 
	 * @param fsm the machine to check
	 * @return true if there are any unreachable states.
	 */
	public static boolean checkUnreachableStates(FSMStructure fsm)
	{
		return computeStateCover(fsm).size() != fsm.accept.size();
	}
	
	/** Checks if the supplied FSM has equivalent states. */
	public static boolean checkEquivalentStates(FSMStructure fsm)
	{
		try
		{
			computeWSet(fsm);
		}
		catch(EquivalentStatesException e)
		{
			return true;
		}
		return false;
	}
	
	public static List<List<String>> cross(Collection<List<String>> a, Collection<List<String>> b){
		List<List<String>> returnVect = new LinkedList<List<String>>();
		for(List<String> elemA:a){
			for(List<String> elemB:b) {
				List<String> cross = new LinkedList<String>();
				cross.addAll(elemA);
				cross.addAll(elemB);
				returnVect.add( cross );
			}
		}
		return returnVect;
	}

	public static LinkedHashSet<String> computeAlphabet(DirectedSparseGraph g)
	{
		LinkedHashSet<String> alphabet = new LinkedHashSet<String>();

		Iterator<Vertex> vertexIt = (Iterator<Vertex>)g.getVertices().iterator();
		while(vertexIt.hasNext())
		{
			Vertex v = vertexIt.next();
			Iterator<DirectedSparseEdge>outEdgeIt = v.getOutEdges().iterator();
			while(outEdgeIt.hasNext()){
				DirectedSparseEdge outEdge = outEdgeIt.next();
				alphabet.addAll( (Set<String>)outEdge.getUserDatum(JUConstants.LABEL) );
			}
		}
		return alphabet;
	}

	public static Set<String> computeAlphabet(FSMStructure fsm)
	{
		LinkedHashSet<String> alphabet = new LinkedHashSet<String>();
		for(Entry<String,Map<String,String>> row:fsm.trans.entrySet())
			alphabet.addAll(row.getValue().keySet());
		return alphabet;
	}
	
	public static List<List<String>> makeSingleton(Collection<String> stimuli){
		List<List<String>> functionList = new LinkedList<List<String>>();
		for(String stim:stimuli){
			List<String> path = new LinkedList<String>();
			path.add(stim);
			functionList.add(path);
		}
		return functionList;
	}
	
	public static List<List<String>> computeStateCover(FSMStructure fsm){
		Queue<String> fringe = new LinkedList<String>();
		Set<String> statesInFringe = new HashSet<String>();// in order not to iterate through the list all the time.
		Map<String,LinkedList<String>> stateToPath = new HashMap<String,LinkedList<String>>();stateToPath.put(fsm.init, new LinkedList<String>());
		fringe.add(fsm.init);statesInFringe.add(fsm.init);
		while(!fringe.isEmpty())
		{
			String currentState = fringe.remove();
			LinkedList<String> currentPath = stateToPath.get(currentState);
			Map<String,String> targets = fsm.trans.get(currentState);
			if(targets != null && !targets.isEmpty())
				for(Entry<String,String> labelstate:targets.entrySet())
				{
					String target = labelstate.getValue();
					if (!statesInFringe.contains(target))
					{
						LinkedList<String> newPath = (LinkedList<String>)currentPath.clone();newPath.add(labelstate.getKey());
						stateToPath.put(target,newPath);
						fringe.offer(target);
						statesInFringe.add(target);
					}
				}
		}
		List<List<String>> outcome = new LinkedList<List<String>>();outcome.addAll(stateToPath.values());
		return outcome;
	}
	
	public static class EquivalentStatesException extends Error 
	{
		/**
		 *  For serialization.
		 */
		private static final long serialVersionUID = 6988899034488999997L;
		
		private final String stateA, stateB;
		
		public String getA()
		{
			return stateA;
		}
		
		public String getB()
		{
			return stateB;
		}
		
		public EquivalentStatesException(String a, String b)
		{
			stateA = a;stateB = b;
		}
	}
	
	/** Computes a characterising set, assuming that there are no unreachable states (with unreachable states, 
	 * it will take a bit longer to perform the computation. 
	 * 
	 * @param fsm the machine to process
	 * @param alphabet
	 * @return characterising set
	 */
	public static List<List<String>> computeWSet(FSMStructure fsm) throws EquivalentStatesException
	{
		
		Map<String,Integer> equivalenceClasses = new LinkedHashMap<String,Integer>(), newEquivClasses = new LinkedHashMap<String,Integer>();
		Map<Map<String,Integer>,Integer> sortedRows = new HashMap<Map<String,Integer>,Integer>();
		Map<String,Map<String,List<String>>> Wdata = new HashMap<String,Map<String,List<String>>>();
		
		for(Entry<String,Boolean> stateEntry:fsm.accept.entrySet()) 
			equivalenceClasses.put(stateEntry.getKey(), 0);
		for(Entry<String,Integer> stateA:equivalenceClasses.entrySet())
		{
			Map<String,List<String>> row = new HashMap<String,List<String>>();
			Wdata.put(stateA.getKey(), row);

			Iterator<Entry<String,Integer>> stateB_It = equivalenceClasses.entrySet().iterator();
			while(stateB_It.hasNext())
			{
				Entry<String,Integer> stateB = stateB_It.next();if (stateB.getKey().equals(stateA.getKey())) break; // we only process a triangular subset.
				row.put(stateB.getKey(), new LinkedList<String>());
			}
		}
		
		int equivalenceClassNumber = 0,oldEquivalenceClassNumber=0;
		do
		{
			oldEquivalenceClassNumber = equivalenceClassNumber;
			Map<String,Map<String,Integer>> newMap = new HashMap<String,Map<String,Integer>>();
			equivalenceClassNumber = 0;sortedRows.clear();newEquivClasses.clear();
			for(Entry<String,Boolean> stateEntry:fsm.accept.entrySet())
			{
				Map<String,Integer> map = new HashMap<String,Integer>();
				Map<String,String> labelNSmap = fsm.trans.get(stateEntry.getKey());
				if (labelNSmap != null)
					for(Entry<String,String> labelstate:labelNSmap.entrySet())
						map.put(labelstate.getKey(), equivalenceClasses.get(labelstate.getValue()));
				
				newMap.put(stateEntry.getKey(), map);
				if (!sortedRows.containsKey(map))
				{
					equivalenceClassNumber++;
					sortedRows.put(map,equivalenceClassNumber);newEquivClasses.put(stateEntry.getKey(), equivalenceClassNumber);
				}
				else
					newEquivClasses.put(stateEntry.getKey(), sortedRows.get(map));
			}

			for(Entry<String,Integer> stateA:equivalenceClasses.entrySet())
			{
				Iterator<Entry<String,Integer>> stateB_It = equivalenceClasses.entrySet().iterator();
				while(stateB_It.hasNext())
				{
					Entry<String,Integer> stateB = stateB_It.next();if (stateB.getKey().equals(stateA.getKey())) break; // we only process a triangular subset.

					if (stateA.getValue().equals(stateB.getValue()) &&
							!newEquivClasses.get(stateA.getKey()).equals(newEquivClasses.get(stateB.getKey())))
					{// the two states used to be in the same equivalence class, now they are in different ones, hence we populate the matrix.
						
						List<String> Wsequence = null;
						if (Wdata.get(stateA.getKey()).containsKey(stateB.getKey()))
							Wsequence = Wdata.get(stateA.getKey()).get(stateB.getKey());// depending on the ordering in the matrix, either (A,B) or (B,A) should be defined.
						else
							Wsequence = Wdata.get(stateB.getKey()).get(stateA.getKey());
						assert Wsequence != null : "In states ("+stateA.getKey()+","+stateB.getKey()+") Wsequence is null";
						assert Wsequence.isEmpty() : "In states ("+stateA.getKey()+","+stateB.getKey()+") Wsequence is non-empty and contains "+Wsequence;
						
						// the two states used to be equivalent but not any more, find the different element
						Map<String,Integer> mapA = newMap.get(stateA.getKey()), mapB = newMap.get(stateB.getKey());
						Iterator<Entry<String,Integer>> mapAiter = mapA.entrySet().iterator();
						String label = null;
						while(mapAiter.hasNext() && label == null)
						{
							Entry<String,Integer> enA = mapAiter.next();
							if (!mapB.entrySet().contains(enA))
								label = enA.getKey();
						}
						
						Iterator<Entry<String,Integer>> mapBiter = mapB.entrySet().iterator();
						while(mapBiter.hasNext() && label == null)
						{
							Entry<String,Integer> enB = mapBiter.next();
							if (!mapA.entrySet().contains(enB))
								label = enB.getKey();
						}
						assert label != null;
						String toA = null;if (fsm.trans.containsKey(stateA.getKey())) toA = fsm.trans.get(stateA.getKey()).get(label);
						String toB = null;if (fsm.trans.containsKey(stateB.getKey())) toB = fsm.trans.get(stateB.getKey()).get(label);
						Wsequence.add(label);
						if (toA != null && toB != null) // these can be null at the first iteration, where states are distinguished based on their response to inputs rather then on the states they lead to.
						{
							List<String> Wprevious = null;
							if (Wdata.get(toA).containsKey(toB))
								Wprevious = Wdata.get(toA).get(toB);// depending on the ordering in the matrix, either (A,B) or (B,A) should be defined.
							else
								Wprevious = Wdata.get(toB).get(toA);
							assert Wprevious != null : "In states ("+stateA.getKey()+","+stateB.getKey()+") previous pair ("+toA+","+toB+") has a null sequence";
							assert !Wprevious.isEmpty() : "In states ("+stateA.getKey()+","+stateB.getKey()+") previous pair ("+toA+","+toB+") was not separated";
							
							Wsequence.addAll(Wprevious);
						}
					}
				}			
			}			

			equivalenceClasses = newEquivClasses;newEquivClasses = new LinkedHashMap<String,Integer>();
		}
		while(equivalenceClassNumber > oldEquivalenceClassNumber);

		List<List<String>> result = new LinkedList<List<String>>();
		if (oldEquivalenceClassNumber == fsm.accept.entrySet().size() )
		{
			for(Entry<String,Integer> stateA:equivalenceClasses.entrySet())
			{
				Iterator<Entry<String,Integer>> stateB_It = equivalenceClasses.entrySet().iterator();
				while(stateB_It.hasNext())
				{
					Entry<String,Integer> stateB = stateB_It.next();if (stateB.getKey().equals(stateA.getKey())) break; // we only process a triangular subset.
					List<String> seq = Wdata.get(stateA.getKey()).get(stateB.getKey());
					if (!seq.isEmpty()) 
						result.add(seq);
				}
			}			
		}
		else
		{// report equivalent states
			for(Entry<String,Integer> stateA:equivalenceClasses.entrySet())
			{
				Iterator<Entry<String,Integer>> stateB_It = equivalenceClasses.entrySet().iterator();
				while(stateB_It.hasNext())
				{
					Entry<String,Integer> stateB = stateB_It.next();if (stateB.getKey().equals(stateA.getKey())) break; // we only process a triangular subset.
					if (stateA.getValue().equals(stateB.getValue()) && !stateA.getKey().equals(stateB.getKey()))
						throw new EquivalentStatesException(stateA.getKey(),stateB.getKey());
				}
			}
			assert false: "equivalent states were not found";
		}
		return result;
	}
}
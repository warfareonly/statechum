/* Copyright (c) 2013 The University of Sheffield.
 * 
 * This file is part of StateChum.
 * 
 * StateChum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * StateChum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with StateChum.  If not, see <http://www.gnu.org/licenses/>.
 */

package statechum.analysis.learning.experiments.PairSelection;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import statechum.Configuration;
import statechum.Configuration.STATETREE;
import statechum.Configuration.ScoreMode;
import statechum.DeterministicDirectedSparseGraph.CmpVertex;
import statechum.DeterministicDirectedSparseGraph.VertexID;
import statechum.GlobalConfiguration;
import statechum.GlobalConfiguration.G_PROPERTIES;
import statechum.JUConstants;
import statechum.Label;
import statechum.ProgressIndicator;
import statechum.Trace;
import statechum.analysis.learning.DrawGraphs;
import statechum.analysis.learning.DrawGraphs.RBoxPlot;
import statechum.analysis.learning.DrawGraphs.SquareBagPlot;
import statechum.analysis.learning.MarkovUniversalLearner;
import statechum.analysis.learning.MarkovUniversalLearner.UpdatableOutcome;
import statechum.analysis.learning.PairScore;
import statechum.analysis.learning.StatePair;
import statechum.analysis.learning.experiments.ExperimentRunner;
import statechum.analysis.learning.experiments.PaperUAS;
import statechum.analysis.learning.experiments.PairSelection.PairQualityLearner.LearnerThatCanClassifyPairs;
import statechum.analysis.learning.experiments.PairSelection.PairQualityLearner.LearnerThatUsesWekaResults.TrueFalseCounter;
import statechum.analysis.learning.experiments.mutation.DiffExperiments.MachineGenerator;
import statechum.analysis.learning.observers.ProgressDecorator.LearnerEvaluationConfiguration;
import statechum.analysis.learning.rpnicore.AMEquivalenceClass;
import statechum.analysis.learning.rpnicore.AbstractLearnerGraph;
import statechum.analysis.learning.rpnicore.LearnerGraph;
import statechum.analysis.learning.rpnicore.LearnerGraph.ScoreComputationCallback;
import statechum.analysis.learning.rpnicore.AbstractPathRoutines;
import statechum.analysis.learning.rpnicore.LearnerGraphCachedData;
import statechum.analysis.learning.rpnicore.LearnerGraphND;
import statechum.analysis.learning.rpnicore.MergeStates;
import statechum.analysis.learning.rpnicore.PairScoreComputation;
import statechum.analysis.learning.rpnicore.RandomPathGenerator;
import statechum.analysis.learning.rpnicore.RandomPathGenerator.RandomLengthGenerator;
import statechum.analysis.learning.rpnicore.Transform;
import statechum.analysis.learning.rpnicore.Transform.ConvertALabel;
import statechum.collections.ArrayMapWithSearch;
import statechum.collections.HashMapWithSearch;
import statechum.model.testset.PTASequenceEngine.FilterPredicate;

public class MarkovPassivePairSelection extends PairQualityLearner
{
	public static long computeScoreUsingMarkovFanouts(LearnerGraph graph, LearnerGraphND origInverse, MarkovUniversalLearner Markov, Set<Label> alphabet, StatePair p)
	{
		long currentScore=0;//comparePredictedFanouts(graph,origInverse,Markov,p.getR(),p.getQ(),alphabet,new LinkedList<Label>(),2);
		// The one below compares states based on actual outgoing transitions, the one above only uses Markov predictions, current outgoing are taken into account when I count inconsistencies.
		Map<Label,CmpVertex> transitionsFromBlue = graph.transitionMatrix.get(p.getQ());
		for(Entry<Label,CmpVertex> outgoing:graph.transitionMatrix.get(p.getR()).entrySet())
		{
			CmpVertex targetFromBlue = transitionsFromBlue.get(outgoing.getKey());
			if (targetFromBlue != null)
			{// we have matching outgoing transitions
				currentScore+=comparePredictedFanouts(graph,origInverse,Markov,outgoing.getValue(),targetFromBlue,alphabet,new LinkedList<Label>(),2);
			}
		}
		
		return currentScore;
	}

	/** PTA is supposed to be built using walks over a reference graph. If these are random walks, it is possible that some transitions will not be covered. 
	 * For the learning purposes, this is significant because this could make some states more easily identifiable.
	 *  
	 * @param pta walks through the reference graph
	 * @param reference graph to trim 
	 * @return trimmed copy of the reference graph.
	 */
	public static Map<CmpVertex,Set<Label>> identifyUncoveredTransitions(LearnerGraph pta,LearnerGraph reference)
	{
		Map<CmpVertex,Set<Label>> outcome = new TreeMap<CmpVertex,Set<Label>>();
		StatePair reference_pta = new StatePair(reference.getInit(),pta.getInit());
		LinkedList<StatePair> pairsToExplore = new LinkedList<StatePair>();pairsToExplore.add(reference_pta);
		for(Entry<CmpVertex,Map<Label,CmpVertex>> entry:reference.transitionMatrix.entrySet())
			outcome.put(entry.getKey(), new TreeSet<Label>(entry.getValue().keySet()));
		Set<CmpVertex> visitedInTree = new HashSet<CmpVertex>();
		while(!pairsToExplore.isEmpty())
		{
			reference_pta = pairsToExplore.pop();
			Map<Label,CmpVertex> transitions=pta.transitionMatrix.get(reference_pta.secondElem);
			outcome.get(reference_pta.firstElem).removeAll(transitions.keySet());
			for(Entry<Label,CmpVertex> target:transitions.entrySet())
				if (target.getValue().isAccept())
				{
					if (visitedInTree.contains(target.getValue()))
						throw new IllegalArgumentException("PTA is not a tree");
					visitedInTree.add(target.getValue());
					CmpVertex nextGraphState = reference.transitionMatrix.get(reference_pta.firstElem).get(target.getKey());
					if (nextGraphState == null)
						throw new IllegalArgumentException("coverage has more transitions than the original graph");
					pairsToExplore.add(new StatePair(nextGraphState, target.getValue()));
				}
		}
		
		return outcome;
	}
	
	
	public static LearnerGraph trimUncoveredTransitions(LearnerGraph pta,LearnerGraph reference)
	{
		Configuration shallowCopy = reference.config.copy();shallowCopy.setLearnerCloneGraph(false);
		LearnerGraph outcome = new LearnerGraph(shallowCopy);AbstractLearnerGraph.copyGraphs(reference, outcome);

		for(Entry<CmpVertex,Set<Label>> entry:identifyUncoveredTransitions(pta, reference).entrySet())
		{
			Map<Label,CmpVertex> map = outcome.transitionMatrix.get(entry.getKey());
			for(Label lbl:entry.getValue()) map.remove(lbl);
		}
		
		return outcome;
	}
	
	public static LearnerGraph buildFirstOrderMarkovGraph(LearnerGraph graph, LearnerGraph referenceGraph, MarkovUniversalLearner m)
	{
		LearnerGraph outcome = new LearnerGraph(graph.config);
		if (m.getChunkLen() < 2)
			throw new IllegalArgumentException("not enough data for a first-order Markov model");
		
		LearnerGraph trimmedReference = trimUncoveredTransitions(graph,referenceGraph);
		
		
		Map<Label,CmpVertex> states = new TreeMap<Label,CmpVertex>();
		for(Label l:graph.getCache().getAlphabet())
		{
			CmpVertex state = AbstractLearnerGraph.generateNewCmpVertex(VertexID.parseID(l.toString()), graph.config);states.put(l,state);
			outcome.transitionMatrix.put(state, outcome.createNewRow());
		}
		for(Entry<Label,CmpVertex> state:states.entrySet())
		{
			for(Label label:graph.getCache().getAlphabet())
			{
				UpdatableOutcome transition = m.get_Markov_model().get(new Trace(Arrays.asList(new Label[]{state.getKey(),label}),true));
				if (transition != null)
					if (transition == UpdatableOutcome.positive) outcome.transitionMatrix.get(state.getValue()).put(label,states.get(label));
			}
		}		

		List<List<Label>> uniqueSequences = new LinkedList<List<Label>>();
		for(Label l1:graph.getCache().getAlphabet())
		{
			boolean nonUnique = false;
			Label unique = null;
			for(Label lbl:graph.getCache().getAlphabet())
			{
				if (m.get_Markov_model().containsKey(new Trace(Arrays.asList(new Label[]{l1,lbl}),true)))
				{
					if (unique == null)
						unique = lbl;
					else
					{
						nonUnique = true;break;
					}
				}
			}
			if (unique != null && !nonUnique)
				uniqueSequences.add(Arrays.asList(new Label[]{l1,unique}));
			
		}
		
		List<List<Label>> sequencesUnique2=new LinkedList<List<Label>>();
		for(List<Label> prefix:uniqueSequences)
			{
				boolean nonUnique = false;
				List<Label> unique = null;
				for(Label lbl:graph.getCache().getAlphabet())
				{
					List<Label> seq = new LinkedList<Label>(prefix);seq.add(lbl);
					if (m.get_Markov_model().containsKey(new Trace(seq,true)))
					{
						if (unique == null)
							unique = seq;
						else
						{
							nonUnique = true;break;
						}
					}
				}
				
				if (nonUnique == false && unique!= null)
					sequencesUnique2.add(unique);
			}
		
		for(List<Label> seq:sequencesUnique2)
		{
			System.out.println(seq);
			for(CmpVertex vert:trimmedReference.transitionMatrix.keySet())
			{
				CmpVertex target=trimmedReference.getVertex(vert,seq);
				if (target != null)
				{
					System.out.println("\t -> "+target);
					CmpVertex stateOfInterest = trimmedReference.getVertex(vert,seq.subList(0, 2));
					System.out.println("\t "+trimmedReference.transitionMatrix.get(stateOfInterest));
				}
				
			}
		}
		/*
		int singleout=0, total=0;
		Collection<Label> singleLabels = new LinkedList<Label>();
		for(Entry<Label,CmpVertex> state:states.entrySet())
		{
			if (!outcome.transitionMatrix.get(state.getValue()).isEmpty()) ++total;
			if (outcome.transitionMatrix.get(state.getValue()).size() == 1)
			{
				singleout++;
				singleLabels.add(state.getKey());
			}
				
		}		
		System.out.println("total: "+total+" single: "+singleout);
		for(Label lbl:singleLabels)
		{
			Label nextLabel = outcome.transitionMatrix.get(states.get(lbl)).keySet().iterator().next();
			System.out.println(lbl+"-"+nextLabel);
			for(Entry<CmpVertex,Map<Label,CmpVertex>> entry:referenceGraph.transitionMatrix.entrySet())
			{
				if (entry.getValue().containsKey(lbl))
				{
					Map<Label,CmpVertex> targets =  referenceGraph.transitionMatrix.get(entry.getValue().get(lbl));
					if (!targets.containsKey(nextLabel))
						System.out.println();
					System.out.println("\t"+entry.getKey()+" : "+entry.getValue()+" from there "+targets+" and finally to "+targets.get(nextLabel));
				}
			}
		}*/
		int countTriples = 0, triplesUnique = 0;
		long totalTripleInPTA=0;
		

		List<Long> uniqueFreq = new LinkedList<Long>(), nonUniqueFreq=new LinkedList<Long>();
		List<Label> uniqueElem = new LinkedList<Label>(), nonUniqueElem = new LinkedList<Label>();
		List<Long> tripleCount=new LinkedList<Long>();
		
		for(Entry<Trace,UpdatableOutcome> entry:m.get_Markov_model().entrySet())
		{
			
			if (entry.getKey().getList().size() == 1 && entry.getValue() == UpdatableOutcome.positive)
			{
				long countTripleInPTA=0;
				for(CmpVertex v:graph.transitionMatrix.keySet())
					if (graph.getVertex(v, entry.getKey().getList()) != null)
						++countTripleInPTA;
				tripleCount.add(countTripleInPTA);totalTripleInPTA+=countTripleInPTA;
				++countTriples;
				if (checkSeqIsUnique(trimmedReference, entry.getKey().getList()))
				{
					uniqueFreq.add((long)m.get_Markov_model_occurence().get(entry.getKey()).firstElem);uniqueElem.add(entry.getKey().getList().get(0));
					++triplesUnique;
				}
				else
				{
					nonUniqueFreq.add((long)m.get_Markov_model_occurence().get(entry.getKey()).firstElem);nonUniqueElem.add(entry.getKey().getList().get(0));
				}
				Collections.sort(uniqueFreq);Collections.sort(nonUniqueFreq);
			}
			
		}
		/*
		long maxFreq = Math.max(uniqueFreq.get(uniqueFreq.size()-1).longValue(),nonUniqueFreq.get(nonUniqueFreq.size()-1).longValue());
		
		System.out.println("Inconsistency in the PTA: "+computeInconsistency(graph,m));
		long sumOfInconsistencyValues = 0, sumOfCountValues = 0;
		for(int i=0;i<uniqueFreq.size();++i)
		{
			Label label = uniqueElem.get(i);long count = uniqueFreq.get(i);
			long value = computeInconsistencyForMergingLabel(graph,label,m);sumOfInconsistencyValues+=value;sumOfCountValues+=count;
			System.out.println("Unique "+count+" inconsistency "+value);
		}
		for(int i=0;i<nonUniqueFreq.size();++i)
		{
			Label label = nonUniqueElem.get(i);long count = nonUniqueFreq.get(i);
			long value = computeInconsistencyForMergingLabel(graph,label,m);sumOfInconsistencyValues+=value;sumOfCountValues+=count;
			System.out.println("NON Unique "+count+" inconsistency "+value);
		}
		
		long inconsistencyAverage = sumOfInconsistencyValues/(uniqueFreq.size()+nonUniqueFreq.size()), countAverage = sumOfCountValues/(uniqueFreq.size()+nonUniqueFreq.size());
		System.out.println("inconsistency average: "+inconsistencyAverage+" countAverage: "+countAverage);
		final double divCount = 1.3, divInconsistency = 2;
		for(int i=0;i<uniqueFreq.size();++i)
		{
			Label label = uniqueElem.get(i);long count = uniqueFreq.get(i);
			long value = computeInconsistencyForMergingLabel(graph,label,m);
			if (!(count < countAverage/divCount && value >= 0 && value < inconsistencyAverage/divInconsistency))
				System.out.println("disregarded "+count+" "+value);
		}
		for(int i=0;i<nonUniqueFreq.size();++i)
		{
			Label label = nonUniqueElem.get(i);long count = nonUniqueFreq.get(i);long value = computeInconsistencyForMergingLabel(graph,label,m);
			if (count < countAverage/divCount && value >= 0 && value < inconsistencyAverage/divInconsistency)
				System.out.println("failed to disregard "+count+" "+value);
		}
		 */
		
		int attemptToUpdateMarkov=0;
		double scoreAfterBigMerge=1;
		do
		{
			long maxCount = 0;
			System.out.println("Traces in Markov: "+m.get_Markov_model().size());
			for(Entry<Trace,UpdatableOutcome> entry:m.get_Markov_model().entrySet())
				if (entry.getKey().getList().size() == 1 && entry.getValue() == UpdatableOutcome.positive)
				{
					long countInPTA=m.get_Markov_model_occurence().get(entry.getKey()).firstElem;
					if (countInPTA > maxCount)
						maxCount = countInPTA;
				}
			
			
			Map<Double,List<Label>> thresholdToInconsistency = new TreeMap<Double,List<Label>>();
			
			for(Entry<Trace,UpdatableOutcome> markovEntry:m.get_Markov_model().entrySet())
				if (markovEntry.getKey().getList().size() == 1 && markovEntry.getValue() == UpdatableOutcome.positive)
				{
					Label lbl = markovEntry.getKey().getList().get(0);
					long countInPTA=m.get_Markov_model_occurence().get(markovEntry.getKey()).firstElem;
					if (countInPTA < maxCount/4)
					{
						double value = computeInconsistencyForMergingLabel(graph,lbl,m);
						if (value >= 0)
						{
							List<Label> labelsForThisInconsistency = thresholdToInconsistency.get(value);
							if (labelsForThisInconsistency == null)
							{
								labelsForThisInconsistency = new LinkedList<Label>();thresholdToInconsistency.put(value, labelsForThisInconsistency); 
							}
							labelsForThisInconsistency.add(lbl);
						}
					}
				}
			
			System.out.println("Starting merging");
			Set<Label> validLabelsToMerge = new TreeSet<Label>(uniqueElem);
			for(double threshold:thresholdToInconsistency.keySet())
			{
				Set<Label> smallValueUniques = new TreeSet<Label>();
				for(Entry<Double,List<Label>> entry:thresholdToInconsistency.entrySet())
					if (entry.getKey() <= threshold)
						smallValueUniques.addAll(entry.getValue());
					else
						break;
				LinkedList<AMEquivalenceClass<CmpVertex,LearnerGraphCachedData>> verticesToMerge = new LinkedList<AMEquivalenceClass<CmpVertex,LearnerGraphCachedData>>();
				List<StatePair> pairsList = LearnerThatCanClassifyPairs.buildVerticesToMerge(graph,new LinkedList<Label>(),smallValueUniques);
				scoreAfterBigMerge = MarkovUniversalLearner.REJECT;
				LearnerGraph merged = null;
				if (!pairsList.isEmpty())
				{
					int score = graph.pairscores.computePairCompatibilityScore_general(null, pairsList, verticesToMerge);
					if (score < 0)
						scoreAfterBigMerge = MarkovUniversalLearner.REJECT;
					else
					{
						merged = MergeStates.mergeCollectionOfVertices(graph, null, verticesToMerge);
						scoreAfterBigMerge = computeInconsistency(merged, m);
					}
				}
				
				if (merged != null)
					System.out.println("After big merge ("+threshold+"): "+scoreAfterBigMerge+" inconsistencies, "+merged.getStateNumber()+" states, originally "+graph.getStateNumber()+ " " +((!validLabelsToMerge.containsAll(smallValueUniques))?"INVALID":"VALID"));
			}
			
			{
				List<Label> whatToMerge = thresholdToInconsistency.entrySet().iterator().next().getValue();
				List<StatePair> pairsList = LearnerThatCanClassifyPairs.buildVerticesToMerge(graph,new LinkedList<Label>(),whatToMerge);
				scoreAfterBigMerge = MarkovUniversalLearner.REJECT;
				LearnerGraph merged = null;
				if (!pairsList.isEmpty())
				{
					LinkedList<AMEquivalenceClass<CmpVertex,LearnerGraphCachedData>> verticesToMerge = new LinkedList<AMEquivalenceClass<CmpVertex,LearnerGraphCachedData>>();
					int score = graph.pairscores.computePairCompatibilityScore_general(null, pairsList, verticesToMerge);
					if (score < 0)
						scoreAfterBigMerge = MarkovUniversalLearner.REJECT;
					else
					{
						merged = MergeStates.mergeCollectionOfVertices(graph, null, verticesToMerge);
						scoreAfterBigMerge = computeInconsistency(merged, m);
					}
				}
				
				if (merged != null)
					System.out.println("Iteration "+attemptToUpdateMarkov+" : "+scoreAfterBigMerge+" inconsistencies, "+merged.getStateNumber()+" states, originally "+graph.getStateNumber()+ " " +((!validLabelsToMerge.containsAll(whatToMerge))?"INVALID":"VALID"));

				m.predictTransitionsAndUpdateMarkov(merged);
				++attemptToUpdateMarkov;
			}
		}
		while(scoreAfterBigMerge > 0);
		System.out.println("triples : "+countTriples+" unique elems: "+triplesUnique+" average in PTA: "+(totalTripleInPTA/(double)tripleCount.size()));
		System.out.println("Unique Freq: "+uniqueFreq);
		System.out.println("Non unique Freq: "+nonUniqueFreq);

		
		
		return outcome;
	}

	public void constructMapFromLabelsToStateGroups(LearnerGraph tentativeGraph, Collection<Label> transitionsFromTheSameState)
	{
		Map<Label,Collection<CmpVertex>> labelToStates = 
				tentativeGraph.config.getTransitionMatrixImplType() == STATETREE.STATETREE_ARRAY? new ArrayMapWithSearch<Label,Collection<CmpVertex>>() : new TreeMap<Label,Collection<CmpVertex>>();
		Map<Label,Collection<CmpVertex>> labelFromStates = 
				tentativeGraph.config.getTransitionMatrixImplType() == STATETREE.STATETREE_ARRAY? new ArrayMapWithSearch<Label,Collection<CmpVertex>>() : new TreeMap<Label,Collection<CmpVertex>>();
					
		for(Label lbl:transitionsFromTheSameState) labelFromStates.put(lbl,new ArrayList<CmpVertex>());
		for(Entry<CmpVertex,Map<Label,CmpVertex>> entry:tentativeGraph.transitionMatrix.entrySet())
			if (entry.getKey().isAccept())
				for(Entry<Label,CmpVertex> transition:entry.getValue().entrySet())
				{
					Collection<CmpVertex> statesToMerge = labelToStates.get(transition.getKey());
					if (statesToMerge != null && transition.getValue().isAccept()) statesToMerge.add(transition.getValue());

					Collection<CmpVertex> sourceStatesToMerge = labelFromStates.get(transition.getKey());
					if (sourceStatesToMerge != null && transition.getValue().isAccept()) sourceStatesToMerge.add(entry.getKey());
				}
		
	}
	
	public static double computeInconsistencyForMergingLabel(LearnerGraph graph, Label label, MarkovUniversalLearner m)
	{
		double outcome = 0;
		
		LinkedList<AMEquivalenceClass<CmpVertex,LearnerGraphCachedData>> verticesToMerge = new LinkedList<AMEquivalenceClass<CmpVertex,LearnerGraphCachedData>>();
		List<StatePair> pairsList = LearnerThatCanClassifyPairs.buildVerticesToMerge(graph,new LinkedList<Label>(),Arrays.asList(new Label[]{label}));
		if (!pairsList.isEmpty())
		{
			int score = graph.pairscores.computePairCompatibilityScore_general(null, pairsList, verticesToMerge);
			if (score < 0)
				outcome = -1;
			else
			{
				LearnerGraph merged = MergeStates.mergeCollectionOfVertices(graph, null, verticesToMerge);
				outcome = computeInconsistency(merged, m);
			}
		}
		
		return outcome;
	}
	
	public static double computeInconsistency(LearnerGraph graph, MarkovUniversalLearner m)
	{
		Configuration shallowCopy = graph.config.copy();shallowCopy.setLearnerCloneGraph(false);
		LearnerGraphND Inverse_Graph = new LearnerGraphND(shallowCopy);
		AbstractPathRoutines.buildInverse(graph,LearnerGraphND.ignoreNone,Inverse_Graph);  // do the inverse to the tentative graph 
		return m.computeConsistency(Inverse_Graph, graph, m.getChunkLen());
	}
	
	protected static boolean checkSeqIsUnique(LearnerGraph referenceGraph, List<Label> seq)
	{
		boolean outcome = false;
		int count=0;
		for(CmpVertex v:referenceGraph.transitionMatrix.keySet())
		{
			if (referenceGraph.getVertex(v,seq) != null)
			{
				++count;
				if (count > 1)
					break;
			}
		}
		if (count == 1)
			outcome = true;
		
		return outcome;
	}
	protected static boolean checkSeqUniqueTarget(LearnerGraph referenceGraph, List<Label> seq)
	{
		boolean targetUnique = true;
		CmpVertex target = null;
		for(CmpVertex v:referenceGraph.transitionMatrix.keySet())
		{
			CmpVertex currTarget = referenceGraph.getVertex(v,seq);
			if (currTarget != null)
			{
				if (target != null)
				{
					targetUnique = false;
					break;
				}
				target = currTarget;
			}
		}
		
		return targetUnique && target != null;
	}
	
	protected static long comparePredictedFanouts(LearnerGraph graph, LearnerGraphND origInverse, MarkovUniversalLearner Markov, CmpVertex red, CmpVertex blue, Set<Label> alphabet, List<Label> pathLenBeyondCurrentState,int stepNumber)
	{
		if (!red.isAccept() || !blue.isAccept())
			return 0;
		
		long scoreCurrentFanout = 0, score = 0;
		Map<Label,UpdatableOutcome> outgoing_red_probabilities=Markov.predictTransitionsFromState(origInverse,red,alphabet,pathLenBeyondCurrentState,Markov.getChunkLen());
		Map<Label,UpdatableOutcome> outgoing_blue_probabilities=Markov.predictTransitionsFromState(origInverse,blue,alphabet,pathLenBeyondCurrentState,Markov.getChunkLen());
		for(Entry<Label,UpdatableOutcome> entry:outgoing_red_probabilities.entrySet())
		{
			UpdatableOutcome outcomeBlue = outgoing_blue_probabilities.get(entry.getKey());
			if (outcomeBlue == null && entry.getValue() == UpdatableOutcome.negative) 
				++scoreCurrentFanout; // red negative, blue absent, hence the two are consistent
			if (outcomeBlue == entry.getValue()) // or if the two are consistent
			{
				if (stepNumber > 1)
				{
					LinkedList<Label> pathBeyond = new LinkedList<Label>(pathLenBeyondCurrentState);pathBeyond.add(entry.getKey());
					score+=comparePredictedFanouts(graph,origInverse,Markov,red,blue,alphabet,pathBeyond,stepNumber-1);
				}
				++scoreCurrentFanout;
			}
		}
			
		for(Entry<Label,UpdatableOutcome> entry:outgoing_blue_probabilities.entrySet())
		{
			UpdatableOutcome outcomeRed = outgoing_red_probabilities.get(entry.getKey());
			if (outcomeRed == null && entry.getValue() == UpdatableOutcome.negative) 
				++scoreCurrentFanout; // blue negative, red absent, hence the two are consistent
			if (outcomeRed == entry.getValue()) // or if the two are consistent
			{
				if (stepNumber > 1)
				{
					LinkedList<Label> pathBeyond = new LinkedList<Label>(pathLenBeyondCurrentState);pathBeyond.add(entry.getKey());
					score+=comparePredictedFanouts(graph,origInverse,Markov,red,blue,alphabet,pathBeyond,stepNumber-1);
				}
				++scoreCurrentFanout;
			}
		}
		
		if (scoreCurrentFanout*4 < (outgoing_red_probabilities.size()+outgoing_blue_probabilities.size())*3)
			scoreCurrentFanout = 0;
		else
			scoreCurrentFanout+=score;
		return scoreCurrentFanout;
	}
	
	/** Identifies states <i>steps</i> away from the root state and labels the first of them red and others blue. The aim is to permit Markov predictive power to be used on arbitrary states, 
	 * without this we cannot predict anything in the vicinity of the root state. 
	 */ 
	public static void labelStatesAwayFromRoot(LearnerGraph graph, int steps)
	{
		graph.clearColours();graph.getInit().setColour(null);
		
		Set<CmpVertex> visited = new HashSet<CmpVertex>();
		Collection<CmpVertex> frontLine = new LinkedList<CmpVertex>(), nextLine = new LinkedList<CmpVertex>(), previousFrontLine = null;
		
		frontLine.add(graph.getInit());visited.add(graph.getInit());
		for(int line=0;line < steps;++line)
		{
			for(CmpVertex vert:frontLine)
				for(CmpVertex next:graph.transitionMatrix.get(vert).values())
					if (!visited.contains(next))
					{
						nextLine.add(next);visited.add(next);
					}
			
			previousFrontLine = frontLine;frontLine = nextLine;nextLine=new LinkedList<CmpVertex>();
		}
		for(CmpVertex blue:frontLine) blue.setColour(JUConstants.BLUE);
		if (frontLine.isEmpty())
			throw new IllegalArgumentException("no states beyond the steps");
		graph.additionalExplorationRoot = previousFrontLine;
		frontLine.iterator().next().setColour(JUConstants.RED);
	}

	public static long computeScoreBasedOnMarkov(LearnerGraph original,StatePair pair,MarkovUniversalLearner Markov)
	{
		assert pair.getQ() != pair.getR();
		assert original.transitionMatrix.containsKey(pair.firstElem);
		assert original.transitionMatrix.containsKey(pair.secondElem);
		Map<CmpVertex,List<CmpVertex>> mergedVertices = original.config.getTransitionMatrixImplType() == STATETREE.STATETREE_ARRAY?
				new ArrayMapWithSearch<CmpVertex,List<CmpVertex>>(original.getStateNumber()):
				new HashMapWithSearch<CmpVertex,List<CmpVertex>>(original.getStateNumber());
		Configuration shallowCopy = original.config.copy();shallowCopy.setLearnerCloneGraph(false);
		LearnerGraph result = new LearnerGraph(original,shallowCopy);
		assert result.transitionMatrix.containsKey(pair.firstElem);
		assert result.transitionMatrix.containsKey(pair.secondElem);

		long pairScore = original.pairscores.computePairCompatibilityScore_internal(pair,mergedVertices);
		if (pairScore < 0)
			throw new IllegalArgumentException("elements of the pair are incompatible");

		if ((pair.getR().getDepth() < Markov.getChunkLen()-1 || pair.getQ().getDepth() < Markov.getChunkLen()-1) && pairScore <= 0)
			return Long.MIN_VALUE;// block mergers into the states for which no statistical information is available if there are not common transitions.

		Map<CmpVertex,Collection<Label>> labelsAdded = new TreeMap<CmpVertex,Collection<Label>>();

		Collection<Label> redLabelsAdded = new TreeSet<Label>();labelsAdded.put(pair.getR(), redLabelsAdded);
		redLabelsAdded.addAll(result.transitionMatrix.get(pair.getR()).keySet());

		
		// make a loop
		for(Entry<CmpVertex,Map<Label,CmpVertex>> entry:original.transitionMatrix.entrySet())
		{
			for(Entry<Label,CmpVertex> rowEntry:entry.getValue().entrySet())
				if (rowEntry.getValue() == pair.getQ())
				{
					// the transition from entry.getKey() leads to the original blue state, record it to be rerouted.
					result.transitionMatrix.get(entry.getKey()).put(rowEntry.getKey(), pair.getR());

					Collection<Label> newLabelsAdded = labelsAdded.get(entry.getKey());
					if (newLabelsAdded == null)
					{
						newLabelsAdded = new TreeSet<Label>();labelsAdded.put(entry.getKey(), newLabelsAdded);
					}
					newLabelsAdded.add(rowEntry.getKey());

				}
		}
		
		Set<CmpVertex> ptaVerticesUsed = new HashSet<CmpVertex>();
		Set<Label> inputsUsed = new HashSet<Label>();

		// I iterate over the elements of the original graph in order to be able to update the target one.
		for(Entry<CmpVertex,Map<Label,CmpVertex>> entry:original.transitionMatrix.entrySet())
		{
			CmpVertex vert = entry.getKey();
			Map<Label,CmpVertex> resultRow = result.transitionMatrix.get(vert);// the row we'll update
			if (mergedVertices.containsKey(vert))
			{// there are some vertices to merge with this one.
				Collection<Label> newLabelsAddedToVert = labelsAdded.get(entry.getKey());
				if (newLabelsAddedToVert == null)
				{
					newLabelsAddedToVert = new TreeSet<Label>();labelsAdded.put(entry.getKey(), newLabelsAddedToVert);
				}

				inputsUsed.clear();inputsUsed.addAll(entry.getValue().keySet());// the first entry is either a "derivative" of a red state or a branch of PTA into which we are now merging more states.
				for(CmpVertex toMerge:mergedVertices.get(vert))
				{// for every input, I'll have a unique target state - this is a feature of PTA
				 // For this reason, every if multiple branches of PTA get merged, there will be no loops or parallel edges.
				// As a consequence, it is safe to assume that each input/target state combination will lead to a new state
				// (as long as this combination is the one _not_ already present from the corresponding red state).
					boolean somethingWasAdded = false;
					for(Entry<Label,CmpVertex> input_and_target:original.transitionMatrix.get(toMerge).entrySet())
						if (!inputsUsed.contains(input_and_target.getKey()))
						{
							// We are adding a transition to state vert with label input_and_target.getKey() and target state input_and_target.getValue();
							resultRow.put(input_and_target.getKey(), input_and_target.getValue());
							
							newLabelsAddedToVert.add(input_and_target.getKey());
							
							inputsUsed.add(input_and_target.getKey());
							ptaVerticesUsed.add(input_and_target.getValue());somethingWasAdded = true;
							// Since PTA is a tree, a tree rooted at ptaVerticesUsed will be preserved in a merged automaton, however 
							// other parts of a tree could be merged into it. In this case, each time there is a fork corresponding to 
							// a step by that other chunk which the current tree cannot follow, that step will end in a tree and a root
							// of that tree will be added to ptaVerticesUsed.
						}
					assert somethingWasAdded : "RedAndBlueToBeMerged was not set correctly at an earlier stage";
				}
			}
		}
		
		// Now we have a graph with all the transitions added (but old ones are not removed, no point doing this). Check if there are any new inconsistencies with 
		// transitions in the vicinity of the added ones. For instance, where a path has been folded in with some transitions sticking out, those new ones
		// may be inconsistent with predictions, based on the transitions in the red part of the graph.

		// mapping map to store all paths leave each state in different length
		double tentativeScore=0;
		LearnerGraphND Inverse_Graph = new LearnerGraphND(shallowCopy);
		AbstractPathRoutines.buildInverse(result,LearnerGraphND.ignoreNone,Inverse_Graph);  // do the inverse to the tentative graph 
		for(Entry<CmpVertex,Collection<Label>> entry:labelsAdded.entrySet())
			if (!entry.getValue().isEmpty())
			{
				double numberOfInconsistencies = Markov.checkFanoutInconsistency(Inverse_Graph,result,entry.getKey(),Markov.getChunkLen());
				tentativeScore-=numberOfInconsistencies;
			}

		return (long)tentativeScore;
	}

	public static class LearnerRunner implements Callable<ThreadResult>
	{
		protected final Configuration config;
		protected final ConvertALabel converter;
		protected final int states,sample;
		protected boolean onlyUsePositives, pickUniqueFromInitial;
		protected final int seed;
		protected int chunkLen=3;
		protected final int traceQuantity;
		protected int lengthMultiplier = 1;
		protected String selectionID;

		public void setSelectionID(String value)
		{
			selectionID = value;
		}
		
		public void setLengthMultiplier(int value)
		{
			lengthMultiplier = value;
		}
		
		/** Whether to filter the collection of traces such that only positive traces are used. */
		public void setOnlyUsePositives(boolean value)
		{
			onlyUsePositives = value;
		}
		
		/** Where a transition that can be uniquely identifying an initial state be used both for mergers and for building a partly-merged PTA. */
		public void setPickUniqueFromInitial(boolean value)
		{
			pickUniqueFromInitial = value;
		}
		
		public void setChunkLen(int len)
		{
			chunkLen = len;
		}
		
		public LearnerRunner(int argStates, int argSample, int argSeed, int nrOfTraces, Configuration conf, ConvertALabel conv)
		{
			states = argStates;sample = argSample;config = conf;seed = argSeed;traceQuantity=nrOfTraces;converter=conv;
		}
		
		class UnusualVertices implements Comparable<UnusualVertices>
		{
			final public long score;
			final public List<CmpVertex> vertices;
			final public List<StatePair> verticesToMerge;
			
			public UnusualVertices(long s, List<CmpVertex> v, List<StatePair> p) 
			{
				score = s;vertices=v;verticesToMerge=p;
			}

			@Override
			public int compareTo(UnusualVertices o) {
				return (int)(score - o.score);
			}

			/* (non-Javadoc)
			 * @see java.lang.Object#hashCode()
			 */
			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + getOuterType().hashCode();
				result = prime * result + (int)score;
				result = prime * result
						+ ((vertices == null) ? 0 : vertices.hashCode());
				return result;
			}

			/* (non-Javadoc)
			 * @see java.lang.Object#equals(java.lang.Object)
			 */
			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (!(obj instanceof UnusualVertices))
					return false;
				UnusualVertices other = (UnusualVertices) obj;
				if (!getOuterType().equals(other.getOuterType()))
					return false;
				if (score != other.score)
					return false;
				if (vertices == null) {
					if (other.vertices != null)
						return false;
				} else if (!vertices.equals(other.vertices))
					return false;
				return true;
			}

			private LearnerRunner getOuterType() {
				return LearnerRunner.this;
			}
		}

		public static List<StatePair> getVerticesToMergeFor(LearnerGraph graph,List<List<List<Label>>> pathsToMerge)
		{
			List<StatePair> listOfPairs = new LinkedList<StatePair>();
			for(List<List<Label>> lotOfPaths:pathsToMerge)
			{
				CmpVertex firstVertex = graph.getVertex(lotOfPaths.get(0));
				for(List<Label> seq:lotOfPaths)
					listOfPairs.add(new StatePair(firstVertex,graph.getVertex(seq)));
			}
			return listOfPairs;
		}
		
		@Override
		public ThreadResult call() throws Exception 
		{
			final int alphabet = 2*states;
			LearnerGraph referenceGraph = null;
			ThreadResult outcome = new ThreadResult();
			Label uniqueFromInitial = null;
			MachineGenerator mg = new MachineGenerator(states, 400 , (int)Math.round((double)states/5));mg.setGenerateConnected(true);
			do
			{
				referenceGraph = mg.nextMachine(alphabet,seed, config, converter).pathroutines.buildDeterministicGraph();// reference graph has no reject-states, because we assume that undefined transitions lead to reject states.
				if (pickUniqueFromInitial)
				{
					Map<Label,CmpVertex> uniques = uniqueFromState(referenceGraph);
					if(!uniques.isEmpty())
					{
						Entry<Label,CmpVertex> entry = uniques.entrySet().iterator().next();
						referenceGraph.setInit(entry.getValue());uniqueFromInitial = entry.getKey();
					}
				}
			}
			while(pickUniqueFromInitial && uniqueFromInitial == null);
			
			LearnerEvaluationConfiguration learnerEval = new LearnerEvaluationConfiguration(config);learnerEval.setLabelConverter(converter);
			final Collection<List<Label>> testSet = PaperUAS.computeEvaluationSet(referenceGraph,states*3,states*alphabet);
			
			for(int attempt=0;attempt<2;++attempt)
			{// try learning the same machine a few times
				LearnerGraph pta = new LearnerGraph(config);
				RandomPathGenerator generator = new RandomPathGenerator(referenceGraph,new Random(attempt),5,null);
				// test sequences will be distributed around 
				final int pathLength = generator.getPathLength();
				// The total number of elements in test sequences (alphabet*states*traceQuantity) will be distributed around (random(pathLength)+1). The total size of PTA is a product of these two.
				// For the purpose of generating long traces, we construct as many traces as there are states but these traces have to be rather long,
				// that is, length of traces will be (random(pathLength)+1)*sequencesPerChunk/states and the number of traces generated will be the same as the number of states.
				final int tracesToGenerate = makeEven(traceQuantity);
				final Random rnd = new Random(seed*31+attempt);
				generator.generateRandomPosNeg(tracesToGenerate, 1, false, new RandomLengthGenerator() {
										
						@Override
						public int getLength() {
							return (rnd.nextInt(pathLength)+1)*lengthMultiplier;
						}
		
						@Override
						public int getPrefixLength(int len) {
							return len;
						}
					});

				if (onlyUsePositives)
					pta.paths.augmentPTA(generator.getAllSequences(0).filter(new FilterPredicate() {
						@Override
						public boolean shouldBeReturned(Object name) {
							return ((statechum.analysis.learning.rpnicore.RandomPathGenerator.StateName)name).accept;
						}
					}));
				else
					pta.paths.augmentPTA(generator.getAllSequences(0));// the PTA will have very few reject-states because we are generating few sequences and hence there will be few negative sequences.
					// In order to approximate the behaviour of our case study, we need to compute which pairs are not allowed from a reference graph and use those as if-then automata to start the inference.
				//pta.paths.augmentPTA(referenceGraph.wmethod.computeNewTestSet(referenceGraph.getInit(),1));
		
				List<List<Label>> sPlus = generator.getAllSequences(0).getData(new FilterPredicate() {
					@Override
					public boolean shouldBeReturned(Object name) {
						return ((statechum.analysis.learning.rpnicore.RandomPathGenerator.StateName)name).accept;
					}
				});
				List<List<Label>> sMinus= generator.getAllSequences(0).getData(new FilterPredicate() {
					@Override
					public boolean shouldBeReturned(Object name) {
						return !((statechum.analysis.learning.rpnicore.RandomPathGenerator.StateName)name).accept;
					}
				});
				assert sPlus.size() > 0;
				assert sMinus.size() > 0;
				final MarkovUniversalLearner m= new MarkovUniversalLearner(chunkLen);
				m.createMarkovLearner(sPlus, sMinus);
				
				pta.clearColours();
				synchronized (AbstractLearnerGraph.syncObj) {
					//PaperUAS.computePTASize(selectionID+" attempt: "+attempt+" with unique: ", pta, referenceGraph);
				}
				
				if (!onlyUsePositives)
				{
					assert pta.getStateNumber() > pta.getAcceptStateNumber() : "graph with only accept states but onlyUsePositives is not set";
				
				}
				else assert pta.getStateNumber() == pta.getAcceptStateNumber() : "graph with negatives but onlyUsePositives is set";
				
				LearnerMarkovPassive learnerOfPairs = null;
				LearnerGraph actualAutomaton = null;
				
				final Configuration deepCopy = pta.config.copy();deepCopy.setLearnerCloneGraph(true);
				LearnerGraph ptaCopy = new LearnerGraph(deepCopy);LearnerGraph.copyGraphs(pta, ptaCopy);

				if (pickUniqueFromInitial)
				{
					pta = mergeStatesForUnique(pta,uniqueFromInitial);
					learnerOfPairs = new LearnerMarkovPassive(learnerEval,referenceGraph,pta);learnerOfPairs.setMarkovModel(m);
					learnerOfPairs.setLabelsLeadingFromStatesToBeMerged(Arrays.asList(new Label[]{uniqueFromInitial}));
					
					actualAutomaton = learnerOfPairs.learnMachine(new LinkedList<List<Label>>(),new LinkedList<List<Label>>());

					LinkedList<AMEquivalenceClass<CmpVertex,LearnerGraphCachedData>> verticesToMerge = new LinkedList<AMEquivalenceClass<CmpVertex,LearnerGraphCachedData>>();
					List<StatePair> pairsList = LearnerThatCanClassifyPairs.buildVerticesToMerge(actualAutomaton,learnerOfPairs.getLabelsLeadingToStatesToBeMerged(),learnerOfPairs.getLabelsLeadingFromStatesToBeMerged());
					if (!pairsList.isEmpty())
					{
						int score = actualAutomaton.pairscores.computePairCompatibilityScore_general(null, pairsList, verticesToMerge);
						if (score < 0)
						{
							learnerOfPairs = new LearnerMarkovPassive(learnerEval,referenceGraph,pta);learnerOfPairs.setMarkovModel(m);
							learnerOfPairs.setLabelsLeadingFromStatesToBeMerged(Arrays.asList(new Label[]{uniqueFromInitial}));
							actualAutomaton = learnerOfPairs.learnMachine(new LinkedList<List<Label>>(),new LinkedList<List<Label>>());
							score = actualAutomaton.pairscores.computePairCompatibilityScore_general(null, pairsList, verticesToMerge);
							throw new RuntimeException("last merge in the learning process was not possible");
						}
						actualAutomaton = MergeStates.mergeCollectionOfVertices(actualAutomaton, null, verticesToMerge);
					}
				}
				else
				{// not merging based on a unique transition from an initial state
					//learnerEval.config.setGeneralisationThreshold(1);
					learnerOfPairs = new LearnerMarkovPassive(learnerEval,referenceGraph,pta);learnerOfPairs.setMarkovModel(m);
					//learnerOfPairs.setPairsToMerge(checkVertices(pta, referenceGraph, m));
					final LearnerGraph finalReferenceGraph = referenceGraph;
					pta.setScoreComputationCallback(new ScoreComputationCallback() {
						LearnerGraph coregraph = null;
						Set<Label> callbackAlphabet = null;
						LearnerGraphND origInverse = null;
						
						@Override
						public void initComputation(LearnerGraph graph) {
							coregraph = graph;
							//labelStatesAwayFromRoot(coregraph,m.getChunkLen()-1);
							callbackAlphabet = coregraph.learnerCache.getAlphabet(); 
							// mapping map to store all paths leave each state in different length
							final Configuration shallowCopy = coregraph.config.copy();shallowCopy.setLearnerCloneGraph(false);
							origInverse = new LearnerGraphND(shallowCopy);
							AbstractPathRoutines.buildInverse(coregraph,LearnerGraphND.ignoreNone,origInverse);  // do the inverse to the tentative graph 
							
						}
						
						@Override
						public long overrideScoreComputation(PairScore p) {
							long score = p.getScore();//computeScoreUsingMarkovFanouts(coregraph,origInverse,m,callbackAlphabet,p);//p.getScore();
							m.constructMarkovTentative(coregraph);
							if (  m.computeMarkovScoring(p,coregraph,m.get_extension_model(),m.getChunkLen()) < 0)
								score = -1;
							// computeScoreBasedOnMarkov(coregraph,p,m) < 0 || 
							if (score >= 0 && MarkovUniversalLearner.computeScoreSicco(coregraph, p) < 0)
								score = -1;
							if (score >= 0 && computeScoreBasedOnMarkov(coregraph,p,m) < 0)
								score =-1;
							if (score >= 0 && ( (p.getR().getDepth() < m.getChunkLen()-1 || p.getQ().getDepth() < m.getChunkLen()-1 ) || p.getScore() < 2) )
								score=-1;
							/*
							if ( (p.getR().getDepth() < m.getChunkLen()-1 || p.getQ().getDepth() < m.getChunkLen()-1 ) && computeScoreUsingMarkovFanouts(coregraph,origInverse,m,alphabet,p) <= 0)
								score =-1;
							*/
							/*
							long score = computeScoreUsingMarkovFanouts(coregraph,origInverse,m,alphabet,p);
							
							score = computeScoreBasedOnMarkov(coregraph,p,m,score);
							*/
/*
							ArrayList<PairScore> pairOfInterest = new ArrayList<PairScore>(1);pairOfInterest.add(p);
							List<PairScore> correctPairs = new ArrayList<PairScore>(1), wrongPairs = new ArrayList<PairScore>(1);
							SplitSetOfPairsIntoRightAndWrong(coregraph, finalReferenceGraph, pairOfInterest, correctPairs, wrongPairs);

							if (
									(score >= 0 && correctPairs.isEmpty()) )
									//|| (score < 0 && !correctPairs.isEmpty()))
							{
								System.out.println(p+" "+score+" INCORRECT"+" with fanouts it is "+computeScoreUsingMarkovFanouts(coregraph,origInverse,m,callbackAlphabet,p)+" using inconsistencies it will be "+computeScoreBasedOnMarkov(coregraph,p,m)+" Sicco reports "+computeScoreSicco(coregraph, p));
								
//								Visualiser.updateFrame(coregraph.transform.trimGraph(3, p.getQ()), coregraph.transform.trimGraph(3, p.getR()));
								Visualiser.updateFrame(coregraph.transform.trimGraph(5, coregraph.getInit()),finalReferenceGraph);
								computeScoreUsingMarkovFanouts(coregraph,origInverse,m,callbackAlphabet,p);
								computeScoreBasedOnMarkov(coregraph,p,m);
								
								//computeScoreUsingMarkovFanouts(coregraph,origInverse,m,alphabet,p);
								//Visualiser.waitForKey();
								//computeScoreBasedOnMarkov(coregraph,p,m);
								//System.out.println(p+" "+score+((score>=0 && correctPairs.isEmpty())?" INCORRECT":" correct"));
							}
					*/	
							return score;
						}

					});

					//actualAutomaton = learnerOfPairs.learnMachine(new LinkedList<List<Label>>(),new LinkedList<List<Label>>());
				}
				SampleData dataSample = new SampleData(null,null);
				dataSample.difference = new DifferenceToReferenceDiff(0, 0);
				dataSample.differenceForReferenceLearner = new DifferenceToReferenceDiff(0, 0);
				/*
				VertID rejectVertexID = null;
				for(CmpVertex v:actualAutomaton.transitionMatrix.keySet())
					if (!v.isAccept())
					{
						assert rejectVertexID == null : "multiple reject vertices in learnt automaton, such as "+rejectVertexID+" and "+v;
						rejectVertexID = v;break;
					}
				if (rejectVertexID == null)
					rejectVertexID = actualAutomaton.nextID(false);
				actualAutomaton.pathroutines.completeGraphPossiblyUsingExistingVertex(rejectVertexID);// we need to complete the graph, otherwise we are not matching it with the original one that has been completed.
				dataSample.difference = estimateDifference(referenceGraph,actualAutomaton,testSet);
				pta.setScoreComputationCallback(null);
				ptaCopy.setScoreComputationCallback(new ScoreComputationCallback() {
					LearnerGraph coregraph = null;
					
					@Override
					public void initComputation(LearnerGraph graph) {
						coregraph = graph;
					}
					
					@Override
					public long overrideScoreComputation(PairScore p) {
						long score = p.getScore();
						if (MarkovUniversalLearner.computeScoreSicco(coregraph,p) < 0)
							score = -1;
						return score;
					}
				});
				*/
				buildFirstOrderMarkovGraph(ptaCopy,referenceGraph,m);
				////Visualiser.updateFrame(referenceGraph, buildFirstOrderMarkovGraph(pta,referenceGraph,m));
				//Visualiser.waitForKey();
				LearnerGraph outcomeOfReferenceLearner = new ReferenceLearner(learnerEval,referenceGraph,ptaCopy).learnMachine(new LinkedList<List<Label>>(),new LinkedList<List<Label>>());
				dataSample.differenceForReferenceLearner = estimateDifference(referenceGraph, outcomeOfReferenceLearner,testSet);
				//System.out.println("actual: "+actualAutomaton.getStateNumber()+" from reference learner: "+outcomeOfReferenceLearner.getStateNumber()+ " difference actual is "+dataSample.difference+ " difference ref is "+dataSample.differenceForReferenceLearner);
				outcome.samples.add(dataSample);
			}
			
			return outcome;
		}

		// Delegates to a specific estimator
		DifferenceToReference estimateDifference(LearnerGraph reference, LearnerGraph actual,@SuppressWarnings("unused") Collection<List<Label>> testSet)
		{
			//return DifferenceToReferenceLanguageBCR.estimationOfDifference(reference, actual, testSet);
			return DifferenceToReferenceDiff.estimationOfDifferenceDiffMeasure(reference, actual, config, 1);//estimationOfDifferenceFmeasure(reference, actual,testSet);
		}
	}
	

	/** An extension of {@Link PairScore} with Markov distance. */
	public static class PairScoreWithDistance extends PairScore
	{
		private double distance;
		
		public PairScoreWithDistance(PairScore p, double d) {
			super(p.getQ(), p.getR(), p.getScore(), p.getAnotherScore());distance = d;
		}
		
		double getDistanceScore()
		{
			return distance;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			long temp;
			temp = Double.doubleToLongBits(distance);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			PairScoreWithDistance other = (PairScoreWithDistance) obj;
			if (Double.doubleToLongBits(distance) != Double
					.doubleToLongBits(other.distance))
				return false;
			return true;
		}
		
		@Override
		public String toString()
		{
			return "[ "+getQ().getStringId()+"("+getQ().isAccept()+","+getQ().getDepth()+"), "+getR().getStringId()+"("+getR().isAccept()+","+getR().getDepth()+") : "+getScore()+","+getAnotherScore()+","+distance+" ]";
		}
	}
	
	/** Uses the supplied classifier to rank pairs. */
	public static class LearnerMarkovPassive extends LearnerThatCanClassifyPairs
	{
		protected Map<Long,TrueFalseCounter> pairQuality = null;
		private int num_states;
		private int numtraceQuantity;
		private int num_seed;
		private int lengthMultiplier;
		public MarkovUniversalLearner Markov;

		public void setPairQualityCounter(Map<Long,TrueFalseCounter> argCounter)
		{
			pairQuality = argCounter;
		}
		
		List<List<List<Label>>> pairsToMerge = null;
		
		public void setPairsToMerge(List<List<List<Label>>> pairs)
		{
			pairsToMerge = pairs;
		}
		
		public List<List<List<Label>>> getPairsToMerge()
		{
			return pairsToMerge;
		}
		
		public void  setlengthMultiplier(int setlengthMultiplier)
		{
			lengthMultiplier = setlengthMultiplier;
		}
		
		public int  getlengthMultiplier()
		{
			return lengthMultiplier;
		}

		public void set_States(int states) {
			num_states=	states;		
		}
		public MarkovUniversalLearner Markov() {
			return Markov;			
		}
			
		public void setMarkovModel(MarkovUniversalLearner m) {
			Markov=m;
			
		}

		public void set_traceQuantity(int traceQuantity) {
			numtraceQuantity=traceQuantity;			
		}
		
		public int get_States() {
			return num_states;		
		}
	
		public int get_traceQuantity() {
			return numtraceQuantity;			
		}
		
		public void set_seed(int i) {
			num_seed=i;
			
		}
		
		public int get_seed() {
			return num_seed;
			
		}

		
		/** During the evaluation of the red-blue pairs, where all pairs are predicted to be unmergeable, one of the blue states will be returned as red. */
		protected boolean classifierToChooseWhereNoMergeIsAppropriate = false;
		
		/** Used to select next red state based on the subjective quality of the subsequent set of red-blue pairs, as determined by the classifier. */
		protected boolean useClassifierToChooseNextRed = false;
		
		public void setUseClassifierForRed(boolean classifierForRed)
		{
			useClassifierToChooseNextRed = classifierForRed;
		}
		
		public void setUseClassifierToChooseNextRed(boolean classifierToBlockAllMergers)
		{
			classifierToChooseWhereNoMergeIsAppropriate = classifierToBlockAllMergers;
		}

		/** Where a pair has a zero score but Weka is not confident that this pair should not be merged, where this flag, such a pair will be assumed to be unmergeable. Where there is a clearly wrong pair
		 * detected by Weka, its blue state will be marked red, where no pairs are clearly appropriate for a merger and all of them have zero scores, this flag will cause a blue state in one of them to be marked red.  
		 */
		protected boolean blacklistZeroScoringPairs = false;
		
		
		public void setBlacklistZeroScoringPairs(boolean value)
		{
			blacklistZeroScoringPairs = value;
		}
		
		public LearnerMarkovPassive(LearnerEvaluationConfiguration evalCnf,final LearnerGraph argReferenceGraph, final LearnerGraph argInitialPTA) 
		{
			super(evalCnf,argReferenceGraph,argInitialPTA);
		}
		
		/** This method orders the supplied pairs in the order of best to merge to worst to merge. 
		 * We do not simply return the best pair because the next step is to check whether pairs we think are right are classified correctly.
		 * <p/> 
		 * Pairs are supposed to be the ones from {@link LearnerThatCanClassifyPairs#filterPairsBasedOnMandatoryMerge(Stack, LearnerGraph)} where all those not matching mandatory merge conditions are not included.
		 * Inclusion of such pairs will not affect the result but it would be pointless to consider such pairs.
		 * @param learnerGraph 
		 * @param l 
		 * @param m 
		 */
		protected ArrayList<PairScore> classifyPairs(Collection<PairScore> pairs, LearnerGraph tentativeGraph)
		{
			boolean allPairsNegative = true;
			for(PairScore p:pairs)
			{
				assert p.getScore() >= 0;
				
				if (p.getQ().isAccept() || p.getR().isAccept()) // if any are rejects, add with a score of zero, these will always work because accept-reject pairs will not get here and all rejects can be merged.
				{
					allPairsNegative = false;break;
				}
			}
			ArrayList<PairScore> possibleResults = new ArrayList<PairScore>(pairs.size()),nonNegPairs = new ArrayList<PairScore>(pairs.size());
			if (allPairsNegative)
				possibleResults.addAll(pairs);
			else
			{
				for(PairScore p:pairs)
				{
					assert p.getScore() >= 0;
					if (!p.getQ().isAccept() || !p.getR().isAccept()) // if any are rejects, add with a score of zero, these will always work because accept-reject pairs will not get here and all rejects can be merged.
						possibleResults.add(new PairScoreWithDistance(p,0));
					else
						nonNegPairs.add(p);// meaningful pairs, will check with the classifier
				}
				
				for(PairScore p:nonNegPairs)
				{
					//double d=//m.get_extension_model().pairscores.computePairCompatibilityScore(p);//
					//		m.computeMMScoreImproved(p, tentativeGraph, l);
					//if(d > 0.0)
					//	possibleResults.add(new PairScoreWithDistance(p, d));
					
					//long score = computeScoreBasedOnMarkov(tentativeGraph,p,Markov,0);
					double d = Markov.computeMMScoreImproved(p, tentativeGraph);
					if(d > 0.0)
						possibleResults.add(new PairScoreWithDistance(p, d));
					/*long pairScore = classifyPairBasedOnUnexpectedTransitions(p,tentativeGraph,Markov);
					if (pairScore >= 0)
						possibleResults.add(p);
						*/
				}
			
					
				Collections.sort(possibleResults, new Comparator<PairScore>(){
	
					@Override
					public int compare(PairScore o1, PairScore o2) {
						int outcome = sgn( ((PairScoreWithDistance)o2).getDistanceScore() - ((PairScoreWithDistance)o1).getDistanceScore());  
						if (outcome != 0)
							return outcome;
						return o2.compareTo(o1);
					}}); 
			}				
			return possibleResults;
		}
		
	
		protected double computeBadPairsEstimation(PairScore p, LearnerGraph tentativeGraph, Map<CmpVertex, Map<Label, Double>> l) 
		{
			double bad=0.0;
			for(Label L:tentativeGraph.getCache().getAlphabet())
			{
				double pF = 0,pS=0;
				if(l.get(p.firstElem).get(L)==null)
					 pF=0.0;
				else
					pF= l.get(p.firstElem).get(L);

				if(l.get(p.secondElem).get(L)==null)
					pS=0.0;
				else
					pS= l.get(p.secondElem).get(L);
				bad+=Math.min(pF,pS);
			}
			return bad;
		}
		
		/** Where there does not seem to be anything useful to merge, return the pair clearly incorrectly labelled. */
		protected PairScore getPairToBeLabelledRed(Collection<PairScore> pairs, LearnerGraph tentativeGraph, MarkovUniversalLearner m)
		{
			for(PairScore p:pairs)
			{
				assert p.getScore() >= 0;
				
				if (!p.getQ().isAccept() || !p.getR().isAccept()) // if any are rejects, add with a score of zero, these will always work because accept-reject pairs will not get here and all rejects can be merged.
					return null;// negatives can always be merged.
			}
			
			// if we are here, none of the pairs are clear candidates for mergers.

			PairScore pairBestToReturnAsRed = null;
			long worstPairScore=0;
			for(PairScore p:pairs)
			{
				/*
				long pairScore = classifyPairBasedOnUnexpectedTransitions(p,tentativeGraph,Markov);
				if (pairScore >= 0)
					return null;
				*/
				double d=m.computeMMScoreImproved(p, tentativeGraph);
				if (d>0)
					return null;
				return p;
				/*
//				double d=m.get_extension_model().pairscores.computePairCompatibilityScore(p);
//				double d=howbadPairs(p, tentativeGraph, l, learnerGraph, m);
				if (worstPairScore == 0 || worstPairScore < pairScore)
				{
					worstPairScore = pairScore;
					pairBestToReturnAsRed = p;
				}
				*/
			}

			System.out.println("to be marked as red: "+pairBestToReturnAsRed+" with score "+worstPairScore);
			return pairBestToReturnAsRed;
		}

		public ArrayList<PairScoreWithDistance> SplitSetOfPairsIntoWrong(LearnerGraph graph, Collection<PairScore> pairs)
		{						
			ArrayList<PairScoreWithDistance>  WrongPairs= new ArrayList<PairScoreWithDistance>();
			for(PairScore p:pairs)
			{	
				if(p.firstElem.isAccept()==true && p.secondElem.isAccept()==true)
				{
					double d=Markov.computeMMScoreImproved(p, graph);
					if(d == MarkovUniversalLearner.REJECT)
					{
						WrongPairs.add(new PairScoreWithDistance(p, d));
				 	}
				}
			}
			
			Collections.sort(WrongPairs, new Comparator<PairScoreWithDistance>(){
				
				@Override
				public int compare(PairScoreWithDistance o1,PairScoreWithDistance o2) {
					return -o2.compareTo(o1);// ensures an entry with the lowest score is first.
				}
			});
			
			return WrongPairs;
		}

		public static String refToString(Object obj)
		{
			return obj == null?"null":obj.toString();
		}
		@Override 
		public Stack<PairScore> ChooseStatePairs(final LearnerGraph graph)
		{
			Stack<PairScore> outcome = graph.pairscores.chooseStatePairs(new PairScoreComputation.RedNodeSelectionProcedure()
			{

				// Here I could use a learner based on metrics of both tentative reds and the perceived quality of the red-blue pairs obtained if I choose any given value.
				// This can be accomplished by doing a clone of the graph and running chooseStatePairs on it with decision procedure that 
				// (a) applies the same rule (of so many) to choose pairs and
				// (b) checks that deadends are flagged. I could iterate this process for a number of decision rules, looking locally for the one that gives best quality of pairs
				// for a particular pairscore decision procedure.
				@Override
				public CmpVertex selectRedNode(LearnerGraph coregraph, @SuppressWarnings("unused") Collection<CmpVertex> reds, Collection<CmpVertex> tentativeRedNodes) 
				{
					CmpVertex redVertex = null;
					if (useClassifierToChooseNextRed) 
						redVertex = LearnerThatUsesWekaResults.selectRedNodeUsingNumberOfNewRedStates(coregraph, tentativeRedNodes);
					else 
						redVertex = tentativeRedNodes.iterator().next();
					
					return redVertex;
				}
				
				@Override
				public CmpVertex resolvePotentialDeadEnd(LearnerGraph coregraph, @SuppressWarnings("unused") Collection<CmpVertex> reds, List<PairScore> pairs) 
				{/*
					List<PairScore> correctPairs = new ArrayList<PairScore>(pairs.size()), wrongPairs = new ArrayList<PairScore>(pairs.size());
					SplitSetOfPairsIntoRightAndWrong(coregraph, referenceGraph, pairs, correctPairs, wrongPairs);

					Map<CmpVertex, Map<Label, UpdatablePairDouble>> l =constructExtensionGraph(coregraph);
					PairScore tentativePair = getPairToBeLabelledRed(pairs,coregraph,l,Markov);
					if (tentativePair == null)
					{
						System.out.println("merge permitted");
						return null;
					}
					
					if (wrongPairs.contains(tentativePair))
						System.out.println("correct choice "+tentativePair);
					else
					{
						System.out.println("WRONG choice "+tentativePair);
						//classifyPairBasedOnUnexpectedTransitions(tentativePair,coregraph);
					}
					return tentativePair.getQ();*/
					
					return null;
				}
/*
				private StatePair classify_bad(List<PairScore> filterbad)
				{
					Map <CmpVertex,Integer> reds=new TreeMap<CmpVertex,Integer>();
					Map <CmpVertex,Integer> blues=new TreeMap<CmpVertex,Integer>();

					for(PairScore P:filterbad)
					{
						if(!blues.containsKey(P.getQ()))
							blues.put(P.getQ(), 1);
						else
						{
							blues.put(P.getQ(), blues.get(P.getQ())+1);
						}
						
						if(!reds.containsKey(P.getR()))
							reds.put(P.getR(), 1);
						else
						{
							reds.put(P.getR(), reds.get(P.getR())+1);
						}						
					}
					for(PairScore P:filterbad)
					{
						if(blues.get(P.getQ())==1 && reds.get(P.getR())==1)
						{
					         return P;
						}
					}					
					return filterbad.get(0);
				}
				*/
			});
			
			if (!outcome.isEmpty())
			{
				PairScore result = null;
				result=classifyingByMarkovScore(outcome, graph);
				assert result!=null;
				assert result.getScore()>=0;
/*
 				List<PairScore> correctPairs = new ArrayList<PairScore>(1), wrongPairs = new ArrayList<PairScore>(1);
				List<PairScore> pairs = new ArrayList<PairScore>(1);pairs.add(result);
				SplitSetOfPairsIntoRightAndWrong(graph, referenceGraph, pairs, correctPairs, wrongPairs);
				if (!correctPairs.isEmpty())
					System.out.println("merge correct");
				else
				{
					System.out.println("merge WRONG "+result);
//					Visualiser.updateFrame(graph.transform.trimGraph(3, graph.getInit()), referenceGraph);
//					Visualiser.waitForKey();
				}
				*/
				outcome.clear();outcome.push(result);
			}	
			return outcome;

		}
		
		public PairScore classifyingByMarkovScore(Stack<PairScore> outcome, LearnerGraph graph)
		{
			return pickPairQSMLike(outcome);
			/*
			Map<CmpVertex, Map<Label, UpdatableOutcome>> l = constructExtensionGraph(graph);
			PairScore result = null;
			
			ArrayList<PairScore> possibleResults = classifyPairs(outcome,graph);
 			if(!possibleResults.isEmpty())
			{
 				result = possibleResults.iterator().next();
			}
			
 			if(result==null)
 			{
 				result = pickPairQSMLike(outcome);// no pairs have been provided by the modified algorithm, hence using the default one.
 			}
 			assert result != null;
			return result;
			*/
		}
		
		
		public static PairScore pickPairDISLike(Collection<PairScoreWithDistance> pairs)
		{
			assert pairs != null;
			PairScoreWithDistance bestPair=null;
			for(PairScoreWithDistance P:pairs)
			{
				if(bestPair == null || P.getAnotherScore() > bestPair.getAnotherScore())
					bestPair=P;
				else if(P.getAnotherScore() == bestPair.getAnotherScore())
				{
					if(P.getDistanceScore() > bestPair.getDistanceScore())
						bestPair=P;	
                    
					else if(Math.abs(P.getQ().getDepth()-P.getR().getDepth()) < Math.abs(bestPair.getQ().getDepth()-bestPair.getR().getDepth()))
							bestPair=P;	
				}
			}
			return bestPair;
		}
		
		public List<PairScore> pickPairToRed(Collection<PairScore> pairs)
		{
			assert pairs != null;
			List<PairScore> bad =new ArrayList<PairScore>();
			PairScore badPair=null;
			for(PairScore P:pairs)
			{
				if(badPair == null)
					badPair=P;
				else if(P.getScore() < badPair.getScore())
					badPair=P;
			}
			bad.add(badPair);

			for(PairScore P:pairs)
			{
				if(badPair.getScore()==P.getScore() && !bad.contains(P))
				bad.add(P);
			}
			return bad;
		}
	}
	
	public static void updateGraph(final RBoxPlot<Long> gr_PairQuality, Map<Long,TrueFalseCounter> pairQuality)
	{
		if (gr_PairQuality != null)
		{
			for(Entry<Long,TrueFalseCounter> entry:pairQuality.entrySet())
				gr_PairQuality.add(entry.getKey(), 100*entry.getValue().trueCounter/((double)entry.getValue().trueCounter+entry.getValue().falseCounter));
		}		
	}

	/** Records scores of pairs that are correctly classified and misclassified. */
	protected static void updateStatistics( Map<Long,TrueFalseCounter> pairQuality, LearnerGraph tentativeGraph, LearnerGraph referenceGraph, Collection<PairScore> pairsToConsider)
	{
		if (!pairsToConsider.isEmpty() && pairQuality != null)
		{
			List<PairScore> correctPairs = new ArrayList<PairScore>(pairsToConsider.size()), wrongPairs = new ArrayList<PairScore>(pairsToConsider.size());
			SplitSetOfPairsIntoRightAndWrong(tentativeGraph, referenceGraph, pairsToConsider, correctPairs, wrongPairs);

			
			for(PairScore pair:pairsToConsider)
			{
				if (pair.getQ().isAccept() && pair.getR().isAccept() && pair.getScore() < 150)
					synchronized(pairQuality)
					{
						TrueFalseCounter counter = pairQuality.get(pair.getScore());
						if (counter == null)
						{
							counter = new TrueFalseCounter();pairQuality.put(pair.getScore(),counter);
						}
						if (correctPairs.contains(pair))
							counter.trueCounter++;
						else
							counter.falseCounter++;
					}
			}
		}
	}

	public static void main(String args[]) throws Exception
	{
		try
		{
			runExperiment();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		finally
		{
			DrawGraphs.end();
		}
	}
	
	
	@SuppressWarnings("null")
	public static void runExperiment() throws Exception
	{
		DrawGraphs gr = new DrawGraphs();
		Configuration config = Configuration.getDefaultConfiguration().copy();config.setAskQuestions(false);config.setDebugMode(false);config.setGdLowToHighRatio(0.7);config.setRandomPathAttemptFudgeThreshold(1000);
		config.setTransitionMatrixImplType(STATETREE.STATETREE_LINKEDHASH);config.setLearnerScoreMode(ScoreMode.COMPATIBILITY);
		ConvertALabel converter = new Transform.InternStringLabel();
		GlobalConfiguration.getConfiguration().setProperty(G_PROPERTIES.LINEARWARNINGS, "false");
		final int ThreadNumber = ExperimentRunner.getCpuNumber();	
		ExecutorService executorService = Executors.newFixedThreadPool(ThreadNumber);
		final int minStateNumber = 20;
		final int samplesPerFSM = 10;
		final int rangeOfStateNumbers = 4;
		final int stateNumberIncrement = 4;
		final int traceQuantity=5;
		
/*
		LearnerRunner oneExperimentRunner = new LearnerRunner(minStateNumber,0,traceQuantity+2,traceQuantity, config, converter);
		oneExperimentRunner.setPickUniqueFromInitial(false);
		oneExperimentRunner.setOnlyUsePositives(false);oneExperimentRunner.setLengthMultiplier(50);
		//learnerRunner.setSelectionID(selection+"_states"+states+"_sample"+sample);
		oneExperimentRunner.call();
*/
		// Stores tasks to complete.
		CompletionService<ThreadResult> runner = new ExecutorCompletionService<ThreadResult>(executorService);
		for(final int lengthMultiplier:new int[]{150})
		for(final boolean onlyPositives:new boolean[]{false})
			{
				for(final boolean useUnique:new boolean[]{false})
				{
					String selection;
					for(final boolean selectingRed:new boolean[]{false})
					for(final boolean classifierToBlockAllMergers:new boolean[]{false})
					for(final double threshold:new double[]{1.0})
					{
						final boolean zeroScoringAsRed = false;
						selection = "TRUNK;EVALUATION;"+";threshold="+threshold+
								";onlyPositives="+onlyPositives+";selectingRed="+selectingRed+";classifierToBlockAllMergers="+classifierToBlockAllMergers+";zeroScoringAsRed="+zeroScoringAsRed+";lengthMultiplier="+lengthMultiplier+";";

						final int totalTaskNumber = traceQuantity;
						final RBoxPlot<Long> gr_PairQuality = new RBoxPlot<Long>("Correct v.s. wrong","%%",new File("percentage_score"+selection.substring(0, 80)+".pdf"));
						final RBoxPlot<String> gr_QualityForNumberOfTraces = new RBoxPlot<String>("traces","%%",new File("quality_traces"+selection.substring(0, 80)+".pdf"));
						SquareBagPlot gr_NewToOrig = new SquareBagPlot("orig score","score with learnt selection",new File("new_to_orig"+selection.substring(0, 80)+".pdf"),0,1,true);
						final Map<Long,TrueFalseCounter> pairQualityCounter = new TreeMap<Long,TrueFalseCounter>();
						try
						{
							int numberOfTasks = 0;
							for(int states=minStateNumber;states < minStateNumber+rangeOfStateNumbers;states+=stateNumberIncrement)
								for(int sample=0;sample<samplesPerFSM;++sample)
								{
									LearnerRunner learnerRunner = new LearnerRunner(states,sample,totalTaskNumber+numberOfTasks,traceQuantity, config, converter);
									learnerRunner.setPickUniqueFromInitial(useUnique);
									learnerRunner.setOnlyUsePositives(onlyPositives);learnerRunner.setLengthMultiplier(lengthMultiplier);
									learnerRunner.setSelectionID(selection+"_states"+states+"_sample"+sample);
									runner.submit(learnerRunner);
									++numberOfTasks;
								}
							ProgressIndicator progress = new ProgressIndicator(new Date()+" evaluating "+numberOfTasks+" tasks for "+selection, numberOfTasks);
							for(int count=0;count < numberOfTasks;++count)
							{
								ThreadResult result = runner.take().get();// this will throw an exception if any of the tasks failed.
								if (gr_NewToOrig != null)
								{
									for(SampleData sample:result.samples)
										gr_NewToOrig.add(sample.differenceForReferenceLearner.getValue(),sample.difference.getValue());
								}
								
								for(SampleData sample:result.samples)
									if (sample.differenceForReferenceLearner.getValue() > 0)
										gr_QualityForNumberOfTraces.add(traceQuantity+"",sample.difference.getValue()/sample.differenceForReferenceLearner.getValue());
								progress.next();
							}
							if (gr_PairQuality != null)
							{
								synchronized(pairQualityCounter)
								{
									updateGraph(gr_PairQuality,pairQualityCounter);
									//gr_PairQuality.drawInteractive(gr);
									//gr_NewToOrig.drawInteractive(gr);
									//if (gr_QualityForNumberOfTraces.size() > 0)
									//	gr_QualityForNumberOfTraces.drawInteractive(gr);
								}
							}
							if (gr_PairQuality != null) gr_PairQuality.drawPdf(gr);
						}
						catch(Exception ex)
						{
							IllegalArgumentException e = new IllegalArgumentException("failed to compute, the problem is: "+ex);e.initCause(ex);
							if (executorService != null) { executorService.shutdownNow();executorService = null; }
							throw e;
						}
						if (gr_NewToOrig != null) gr_NewToOrig.drawPdf(gr);
						if (gr_QualityForNumberOfTraces != null) gr_QualityForNumberOfTraces.drawPdf(gr);
					}
				}
			}
		if (executorService != null) { executorService.shutdown();executorService = null; }
	

	}
}
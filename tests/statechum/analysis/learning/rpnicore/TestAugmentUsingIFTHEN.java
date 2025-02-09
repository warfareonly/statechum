/* Copyright (c) 2006, 2007, 2008 Neil Walkinshaw and Kirill Bogdanov
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

package statechum.analysis.learning.rpnicore;

import static statechum.Helper.checkForCorrectException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.ParameterizedWithName;
import org.junit.runners.ParameterizedWithName.ParametersToString;

import edu.uci.ics.jung.graph.impl.DirectedSparseGraph;
import statechum.Configuration;
import statechum.Helper;
import statechum.JUConstants;
import statechum.Configuration.QuestionGeneratorKind;
import statechum.DeterministicDirectedSparseGraph.CmpVertex;
import statechum.DeterministicDirectedSparseGraph.VertexID;
import statechum.DeterministicDirectedSparseGraph.VertID.VertKind;
import statechum.Helper.whatToRun;
import statechum.JUConstants.PAIRCOMPATIBILITY;
import statechum.Label;
import statechum.analysis.learning.AbstractOracle;
import statechum.analysis.learning.StatePair;
import statechum.analysis.learning.TestStateMerging;
import statechum.analysis.learning.rpnicore.AMEquivalenceClass.IncompatibleStatesException;
import statechum.analysis.learning.rpnicore.LTL_to_ba.UnrecognisedLabelException;
import statechum.analysis.learning.rpnicore.LearnerGraph.NonExistingPaths;
import statechum.analysis.learning.rpnicore.PathRoutines.EdgeAnnotation;
import statechum.analysis.learning.rpnicore.Transform.AugmentFromIfThenAutomatonException;
import statechum.analysis.learning.rpnicore.WMethod.DifferentFSMException;
import statechum.analysis.learning.rpnicore.WMethod.VERTEX_COMPARISON_KIND;
import statechum.apps.QSMTool;
import statechum.model.testset.PTASequenceEngine;
import static statechum.analysis.learning.rpnicore.FsmParser.buildLearnerGraph;

@RunWith(ParameterizedWithName.class)
final public class TestAugmentUsingIFTHEN extends TestWithMultipleConfigurations
{
	@org.junit.runners.Parameterized.Parameters
	public static Collection<Object[]> data() 
	{
		return TestWithMultipleConfigurations.data();
	}
	
	@ParametersToString
	public static String parametersToString(Configuration config)
	{
		return TestWithMultipleConfigurations.parametersToString(config);
	}

	public TestAugmentUsingIFTHEN(Configuration conf)
	{
		super(conf);
	}
	
	/** Tests merging of the two automata depicted on page 18 of "why_nondet_does_not_matter.xoj" */
	@Test
	public final void testAugmentFromMax1_AB()
	{
		LearnerGraph gr = buildLearnerGraph("H-a->A-a->B-b->C\nH-c->B\nH-d->B", "testAugmentFromMax1_gr",mainConfiguration,converter);
		LearnerGraph max = buildLearnerGraph("I-a->D-a-#E\nI-d-#E\nI-c->F-b->G", "testAugmentFromMax1_max",mainConfiguration,converter);
		LearnerGraph result = Transform.augmentFromMAX(gr, max, true, true, mainConfiguration, true);
		TestEquivalenceChecking.checkM("H-a->A-a-#BE\nH-d-#BE\nH-c->BF-b->C", result, mainConfiguration,converter);
		Assert.assertEquals(5,result.getStateNumber());
	}
	
	/** Tests merging of the two automata on page 18 of "why_nondet_does_not_matter.xoj" */
	@Test
	public final void testAugmentFromMax1_nonoverride()
	{
		final LearnerGraph gr = buildLearnerGraph("H-a->A-a->B-b->C\nH-c->B\nH-d->B", "testAugmentFromMax1_gr",mainConfiguration,converter);
		final LearnerGraph max = buildLearnerGraph("I-a->D-a-#E\nI-d-#E\nI-c->F-b->G", "testAugmentFromMax1_max",mainConfiguration,converter);
		checkForCorrectException(new whatToRun() {	public @Override void run() throws NumberFormatException 
		{
			Transform.augmentFromMAX(gr, max, false, true,mainConfiguration, true);
		}}, IllegalArgumentException.class, "incompatible");
	}
	
	/** Tests merging of the two automata on page 18 of "why_nondet_does_not_matter.xoj" */
	@Test
	public final void testAugmentFromMax1_BA()
	{
		String automatonWithReject = "I-a->D-a-#E\nI-d-#E\nI-c->F-b->G";
		LearnerGraph gr = buildLearnerGraph(automatonWithReject, "testAugmentFromMax1_max",mainConfiguration,converter);
		LearnerGraph max = buildLearnerGraph("H-a->A-a->B-b->C\nH-c->B\nH-d->B", "testAugmentFromMax1_gr",mainConfiguration,converter);
		LearnerGraph result = Transform.augmentFromMAX(gr, max, true, true,mainConfiguration, true);
		Assert.assertNull(result);
	}

	/** Tests merging of the two automata on page 17 of "why_nondet_does_not_matter.xoj" */
	@Test
	public final void testAugmentFromMax2_AB()
	{
		LearnerGraph gr = buildLearnerGraph("A-b->A-a->C-b->C-a->E-b-#G", "testAugmentFromMax2_gr",mainConfiguration,converter);
		LearnerGraph max = buildLearnerGraph("B-b->D-b->F-a->F-b->B", "testAugmentFromMax2_max",mainConfiguration,converter);
		LearnerGraph result = Transform.augmentFromMAX(gr, max, true, true,mainConfiguration, true);
		Assert.assertNull(result);
	}

	/** Tests merging of the two automata on page 17 of "why_nondet_does_not_matter.xoj" */
	@Test
	public final void testAugmentFromMax3_AB()
	{
		LearnerGraph gr = buildLearnerGraph("A-b->A-a->C-b->C-a->E-b-#G\n"+
				"A-c->A\nC-c->C", "testAugmentFromMax3_gr",mainConfiguration,converter);
		LearnerGraph max = buildLearnerGraph("B-b->D-b->F-a->F-b->B\n"+
				"B-c->D-c->F-c->B", "testAugmentFromMax3_max",mainConfiguration,converter);
		LearnerGraph result = Transform.augmentFromMAX(gr, max, true, true,mainConfiguration, true);
		Assert.assertNull(result);
	}
	
	@Test
	public final void testAugmentFromMax4_AB()
	{
		String origGraph = "A-b->A-a->A-c->B-c->C\n";
		LearnerGraph gr = buildLearnerGraph(origGraph, "testAugmentFromMax4_gr",mainConfiguration,converter);
		LearnerGraph max = buildLearnerGraph("E-a->F-a->G-a->H", "testAugmentFromMax4_max",mainConfiguration,converter);
		LearnerGraph result = Transform.augmentFromMAX(gr, max, true, true,mainConfiguration,true);
		Assert.assertNull(result);
	}

	@Test
	public final void testAugmentFromMax5_AB()
	{
		LearnerGraph gr = buildLearnerGraph("A-b->A-a->A-c->B-c->C\n", "testAugmentFromMax4_gr",mainConfiguration,converter);
		LearnerGraph max = buildLearnerGraph("E-a->F-a->G-a->H-a-#I", "testAugmentFromMax5_max",mainConfiguration,converter);
		LearnerGraph result = Transform.augmentFromMAX(gr, max, true, true,mainConfiguration,true);
		TestEquivalenceChecking.checkM("AE-a->AF-a->AG-a->AH-a-#I\n"+
				"AE-b->P-c->B-c->C\nP-a->P-b->P\nAE-c->B\nAF-b->P\nAF-c->B\nAG-b->P\nAG-c->B\nAH-b->P\nAH-c->B", result, mainConfiguration,converter);
	}
	
	@Test
	public final void testAugmentFromMax6_AB()
	{
		LearnerGraph gr = buildLearnerGraph("A-b->A-a->A-c->B-c->C\n", "testAugmentFromMax4_gr",mainConfiguration,converter);
		LearnerGraph max = new LearnerGraph(mainConfiguration);max.getInit().setAccept(false);
		LearnerGraph result = Transform.augmentFromMAX(gr, max, true, true,mainConfiguration,true);
		Assert.assertNull(WMethod.checkM(max, result));
	}
	
	@Test
	public final void testAugmentFromMax6_AB_nooverride()
	{
		final LearnerGraph gr = buildLearnerGraph("A-b->A-a->A-c->B-c->C\n", "testAugmentFromMax4_gr",mainConfiguration,converter);
		final LearnerGraph max = new LearnerGraph(mainConfiguration);max.getInit().setAccept(false);
		checkForCorrectException(new whatToRun() {	public @Override void run() throws NumberFormatException 
			{
				Transform.augmentFromMAX(gr, max, false, true,mainConfiguration, true);
			}}, IllegalArgumentException.class, "incompatible");
	}

	@Test
	public final void testAugmentFromMax6_BA()
	{
		LearnerGraph gr = new LearnerGraph(mainConfiguration);gr.getInit().setAccept(false);
		LearnerGraph max = buildLearnerGraph("A-b->A-a->A-c->B-c->C\n", "testAugmentFromMax4_gr",mainConfiguration,converter);
		LearnerGraph result = Transform.augmentFromMAX(gr, max, true, true,mainConfiguration,true);
		Assert.assertNull(result);
	}

	@Test
	public final void testAugmentFromMax7_AB()
	{
		LearnerGraph gr = buildLearnerGraph("A-b->A-a->C-b->C-a->E-b-#G", "testAugmentFromMax2_gr",mainConfiguration,converter);
		LearnerGraph max = buildLearnerGraph("B-b->D-b->F-a->F-b->B\nD-a-#E", "testAugmentFromMax7_max",mainConfiguration,converter);
		LearnerGraph result = Transform.augmentFromMAX(gr, max, true, true,mainConfiguration, true);
		TestEquivalenceChecking.checkM("AB-b->AD-b->AF-b->AB\nAF-a->CF-b->CB-b->CD-b->CF-a->EF-b-#G\n"+
				"AB-a->C-b->C-a->E-b-#G\nCB-a->E\nAD-a-#H\nCD-a-#H", result, mainConfiguration,converter);
	}

	@Test
	public final void testAugmentFromMax7_AB_nooverride()
	{
		final LearnerGraph gr = buildLearnerGraph("A-b->A-a->C-b->C-a->E-b-#G", "testAugmentFromMax2_gr",mainConfiguration,converter);
		final LearnerGraph max = buildLearnerGraph("B-b->D-b->F-a->F-b->B\nD-a-#E", "testAugmentFromMax7_max",mainConfiguration,converter);
		checkForCorrectException(new whatToRun() {	public @Override void run() throws NumberFormatException 
			{
				Transform.augmentFromMAX(gr, max, false, true,mainConfiguration, true);
			}}, IllegalArgumentException.class, "incompatible");
	}
	
	@Test
	public final void testAugmentFromMax8_a()
	{
		LearnerGraph gr = buildLearnerGraph("A-b->A-a->C-b->C-a->E-b-#G", "testAugmentFromMax2_gr",mainConfiguration,converter);
		LearnerGraph max = new LearnerGraph(mainConfiguration);
		LearnerGraph result = Transform.augmentFromMAX(gr, max, true, true,mainConfiguration, true);
		Assert.assertNull(result);
	}
	
	@Test
	public final void testAugmentFromMax8_b()
	{
		LearnerGraph gr = buildLearnerGraph("A-b->A-a->C-b->C-a->E-b-#G", "testAugmentFromMax2_gr",mainConfiguration,converter);
		LearnerGraph max = buildLearnerGraph("A-a->A-b->A-c->A-d->A", "testAugmentFromMax7_max",mainConfiguration,converter);
		LearnerGraph result = Transform.augmentFromMAX(gr, max, true, true,mainConfiguration, true);
		Assert.assertNull(result);
	}
	
	static void compareGraphs(LearnerGraph A, LearnerGraph B)
	{
		DifferentFSMException ex= WMethod.checkM_and_colours(A, B, VERTEX_COMPARISON_KIND.NONE);
		Assert.assertNull(ex==null?"":ex.toString(),ex);
		
		// reachability of all states ensures that transition structures are isomorphic.
		Assert.assertEquals(A.getStateNumber(),A.pathroutines.computeShortPathsToAllStates().size());
		Assert.assertEquals(B.getStateNumber(),B.pathroutines.computeShortPathsToAllStates().size());
	}
	
	private static final String ifthenA = "A-a->B-a->C-b->B / P-b->Q-a->R / S-c->S / T-d->N / S=THEN=C=THEN=P / B=THEN=T";
	private static final String ifthenB = "A-a->B-a->C-b->B / P-b->Q-a->R / S-c->S / T-d->N / S=THEN=C=THEN=P / B=THEN=T=THEN=A";
	
	@Test
	public final void testCheckIFTHEN1()
	{
		Transform.checkTHEN_disjoint_from_IF(buildLearnerGraph(ifthenA,"ifthen",mainConfiguration,converter));
	}
	
	@Test
	public final void testCheckIFTHEN2()
	{
		Transform.checkTHEN_disjoint_from_IF(buildLearnerGraph(ifthenB,"ifthen",mainConfiguration,converter));
	}
	
	@Test
	public final void testCheckIFTHEN_fail0a()
	{
		Helper.checkForCorrectException(new whatToRun() { public @Override void run() {
			Transform.checkTHEN_disjoint_from_IF(new LearnerGraph(mainConfiguration));
		}}, IllegalArgumentException.class,"no THEN states");
	}
	
	@Test
	public final void testCheckIFTHEN_fail0b()
	{
		Helper.checkForCorrectException(new whatToRun() { public @Override void run() {
			LearnerGraph graph = new LearnerGraph(mainConfiguration);graph.getInit().setAccept(false);
			Transform.checkTHEN_disjoint_from_IF(graph);
		}}, IllegalArgumentException.class,"no THEN states");
	}
	
	@Test
	public final void testCheckIFTHEN_fail1()
	{
		Helper.checkForCorrectException(new whatToRun() { public @Override void run() {
			Transform.checkTHEN_disjoint_from_IF(buildLearnerGraph("A-a->B-a->C-b->B / P-b->Q-a->R / S-c->S / T-d->N","testCheckIFTHEN_fail1",mainConfiguration,converter));
		}}, IllegalArgumentException.class,"no THEN states");
	}
	
	@Test
	public final void testCheckIFTHEN_fail2()
	{
		Helper.checkForCorrectException(new whatToRun() { public @Override void run() {
			Transform.checkTHEN_disjoint_from_IF(buildLearnerGraph("A-a->B-a->C-b->B / P-b->Q-a->R / S-c->S / T-d->N / S=THEN=C=THEN=P","testCheckIFTHEN_fail2",mainConfiguration,converter));
		}}, IllegalArgumentException.class,"unreachable");
	}
	
	@Test
	public final void testCheckIFTHEN_fail3()
	{
		Helper.checkForCorrectException(new whatToRun() { public @Override void run() {
			Transform.checkTHEN_disjoint_from_IF(buildLearnerGraph("A-a->B-a->C-b->B / P-b->Q-a->B / S-c->S / T-d->N / S=THEN=C=THEN=P / B=THEN=T","testCheckIFTHEN_fail2",mainConfiguration,converter));
		}}, IllegalArgumentException.class,"are shared between");
	}
	
	@Test
	public final void testCheckIFTHEN_fail4()
	{
		Helper.checkForCorrectException(new whatToRun() { public @Override void run() {
			Transform.checkTHEN_disjoint_from_IF(buildLearnerGraph("A-a->B-a->C-b->B / P-b->Q-a->Q / S-c->S / T-d->N / S=THEN=C=THEN=P / S=THEN=T","testCheckIFTHEN_fail2",mainConfiguration,converter));
		}}, IllegalArgumentException.class,"do not belong");
	}
	
	@Test
	public final void testbuildIfThenAutomata1()
	{
		String ltlFormula = "!([](a->X[]b))";
		Collection<LearnerGraph> automata = Transform.buildIfThenAutomata(Arrays.asList(new String[]{
				QSMTool.cmdLTL+" "+ltlFormula}), buildLearnerGraph("A-a->B-b->C-c->D", "testbuildIfThenAutomata1", mainConfiguration,converter).pathroutines.computeAlphabet(),mainConfiguration,converter);
		Iterator<LearnerGraph> graphIter = automata.iterator();

		LearnerGraph topGraph = graphIter.next(), expectedTop = buildLearnerGraph("I-a->A-b->A / I-b->IA-a->A / I-c->IA-b->IA-c->IA / P-c-#P1 / P-a-#P2 / A = THEN = P / " +
				"I - transition_to_THEN ->P","!("+ltlFormula+")",mainConfiguration,converter);
		topGraph.addTransition(topGraph.transitionMatrix.get(topGraph.getInit()), AbstractLearnerGraph.generateNewLabel("transition_to_THEN",mainConfiguration,converter), topGraph.findVertex(VertexID.parseID("P"+(topGraph.vertPositiveID-1))));
		graphIter = automata.iterator();
		compareGraphs(expectedTop,graphIter.next());
		Assert.assertFalse(graphIter.hasNext());
	}
	
	/** Same as above, but more automata. */
	@Test
	public final void testbuildIfThenAutomata2()
	{
		String ltlFormulaA = "a", ltlFormulaB = "b";

		Collection<LearnerGraph> automata = Transform.buildIfThenAutomata(Arrays.asList(new String[]{
				QSMTool.cmdLTL+" "+ltlFormulaA,
				QSMTool.cmdIFTHENAUTOMATON+" graphA A-a->B / P-a->P == THEN == A",
				QSMTool.cmdLTL+" "+ltlFormulaB,
				QSMTool.cmdIFTHENAUTOMATON+" graphB "+ifthenA
			}), buildLearnerGraph("A-a->B-b->C-c->D-d->E", "testbuildIfThenAutomata1", mainConfiguration,converter).pathroutines.computeAlphabet(),mainConfiguration,converter);
		Iterator<LearnerGraph> graphIter = automata.iterator();

		LearnerGraph topGraph = graphIter.next(), expectedTop = buildLearnerGraph("I-c->A / I-d->A / A-a->A-b->A-c->A-d->A / P2#-b-P-a-#P1 / I = THEN = P / " +
				"I - transition_to_THEN ->P","!("+ltlFormulaA+"||"+ltlFormulaB+")",mainConfiguration,converter);
		topGraph.addTransition(topGraph.transitionMatrix.get(topGraph.getInit()), AbstractLearnerGraph.generateNewLabel("transition_to_THEN",mainConfiguration,converter), topGraph.findVertex(VertexID.parseID("P"+(topGraph.vertPositiveID-1))));
		LearnerGraph next = null;
		compareGraphs(expectedTop, topGraph);Assert.assertEquals("LTL",topGraph.getName());
		
		next=graphIter.next();Assert.assertEquals("graphA", next.getName());
		next.addTransition(next.transitionMatrix.get(next.getInit()), AbstractLearnerGraph.generateNewLabel("transition_to_THEN",mainConfiguration,converter), next.findVertex("P"));
		compareGraphs(buildLearnerGraph("A-a->B / P-a->P == THEN == A-transition_to_THEN->P","1",mainConfiguration,converter),next);
		
		next=graphIter.next();Assert.assertEquals("graphB", next.getName());
		next.addTransition(next.transitionMatrix.get(next.getInit()), AbstractLearnerGraph.generateNewLabel("transition_to_THEN_P",mainConfiguration,converter), next.findVertex("P"));
		next.addTransition(next.transitionMatrix.get(next.getInit()), AbstractLearnerGraph.generateNewLabel("transition_to_THEN_S",mainConfiguration,converter), next.findVertex("S"));
		next.addTransition(next.transitionMatrix.get(next.getInit()), AbstractLearnerGraph.generateNewLabel("transition_to_THEN_T",mainConfiguration,converter), next.findVertex("T"));
		compareGraphs(buildLearnerGraph(ifthenA+" / A-transition_to_THEN_P->P / A-transition_to_THEN_S->S / A-transition_to_THEN_T->T","2",mainConfiguration,converter),next);
		Assert.assertFalse(graphIter.hasNext());
	}
	
	/** No LTL but some automata. */
	@Test
	public final void testbuildIfThenAutomata3()
	{
		Collection<LearnerGraph> automata = Transform.buildIfThenAutomata(Arrays.asList(new String[]{
				QSMTool.cmdIFTHENAUTOMATON+" graphA A-a->B / P-a->P == THEN == A",
				QSMTool.cmdIFTHENAUTOMATON+" graphB "+ifthenA
			}), buildLearnerGraph("A-a->B-b->C-c->D-d->E", "testbuildIfThenAutomata1", mainConfiguration,converter).pathroutines.computeAlphabet(),mainConfiguration,converter);
		Iterator<LearnerGraph> graphIter = automata.iterator();

		LearnerGraph next = null;
		
		next=graphIter.next();Assert.assertEquals("graphA", next.getName());
		next.addTransition(next.transitionMatrix.get(next.getInit()), AbstractLearnerGraph.generateNewLabel("transition_to_THEN",mainConfiguration,converter), next.findVertex("P"));
		compareGraphs(buildLearnerGraph("A-a->B / P-a->P == THEN == A-transition_to_THEN->P","1",mainConfiguration,converter),next);
		
		next=graphIter.next();Assert.assertEquals("graphB", next.getName());
		next.addTransition(next.transitionMatrix.get(next.getInit()), AbstractLearnerGraph.generateNewLabel("transition_to_THEN_P",mainConfiguration,converter), next.findVertex("P"));
		next.addTransition(next.transitionMatrix.get(next.getInit()), AbstractLearnerGraph.generateNewLabel("transition_to_THEN_S",mainConfiguration,converter), next.findVertex("S"));
		next.addTransition(next.transitionMatrix.get(next.getInit()), AbstractLearnerGraph.generateNewLabel("transition_to_THEN_T",mainConfiguration,converter), next.findVertex("T"));
		compareGraphs(buildLearnerGraph(ifthenA+" / A-transition_to_THEN_P->P / A-transition_to_THEN_S->S / A-transition_to_THEN_T->T","2",mainConfiguration,converter),next);
		Assert.assertFalse(graphIter.hasNext());
	}

	/** An automaton without a name. */
	@Test
	public final void testbuildIfThenAutomata_fail1()
	{
		Helper.checkForCorrectException(new whatToRun() { public @Override void run() {
		Transform.buildIfThenAutomata(Arrays.asList(new String[]{
				QSMTool.cmdLTL+" !a",
				QSMTool.cmdIFTHENAUTOMATON+" graphA"}), buildLearnerGraph("A-a->B-b->C-c->D", "testbuildIfThenAutomata1", mainConfiguration,converter).pathroutines.computeAlphabet(),mainConfiguration,converter);
		}}, IllegalArgumentException.class,"missing automata name");
	}
	
	@Test
	public final void testbuildIfThenAutomata_fail2()
	{
		Helper.checkForCorrectException(new whatToRun() { public @Override void run() {
			Transform.buildIfThenAutomata(Arrays.asList(new String[]{
				QSMTool.cmdIFTHENAUTOMATON+" graphA A-t->B / P-a->P == THEN == A"
			}), buildLearnerGraph("A-a->B-b->C-c->D-d->E", "testbuildIfThenAutomata1", mainConfiguration,converter).pathroutines.computeAlphabet(),mainConfiguration,converter);
		
		}}, UnrecognisedLabelException.class,"unrecognised label t");
	}
	
	/** Tests the construction of a PTA from questions. */
	@Test
	public final void testBuildPTAofQuestions1()
	{
		Configuration config = mainConfiguration.copy();config.setLearnerCloneGraph(false);config.setQuestionGenerator(QuestionGeneratorKind.CONVENTIONAL_IMPROVED);
		LearnerGraph graph = buildLearnerGraph(
				"A1-a->B1-b->A2-a->B2-b->A3-a->B3-b->A4-a->B4-b->A5 /"+
				"A2-c->A21-c->A22-e->A23 /"+
				"A3-c->A31-c->A32 / A31-d-#A34 /"+
				"A4-c->A41-c->A42-f-#A43 /"+
				"A5-c->A51-c->A52"
				, "testBuildPTAofQuestions1",config,converter);
		StatePair pair = new StatePair(graph.findVertex("A1"),graph.findVertex("A2"));
		LearnerGraph merged = MergeStates.mergeAndDeterminize_general(graph, pair);
		compareGraphs(buildLearnerGraph("A1-a->B1-b->A1-c->C-d-#R4 / C-c->CC / CC-f-#R3 / CC-e->D", "expected",config,converter),merged);
		PTASequenceEngine questions = ComputeQuestions.getQuestionPta(pair, graph, merged, null);
		LearnerGraph updatedGraphExpected = new LearnerGraph(graph,config),
			updatedGraphActual = ComputeQuestions.constructGraphWithQuestions(pair, graph, merged);

		for(List<Label> path:questions.getData())
			updatedGraphExpected.paths.augmentPTA(path,merged.paths.getVertex(path).isAccept(),false,null);

		Set<List<Label>> expectedQuestions = TestFSMAlgo.buildSet(new String[][] {
				new String[]{"c", "d"},
				new String[]{"c", "c", "e"},
				new String[]{"c", "c", "f"}
		},config,converter), actualQuestions = new LinkedHashSet<List<Label>>();actualQuestions.addAll(questions.getData());
		Assert.assertEquals(expectedQuestions,actualQuestions);
		//updatedGraphExpected.paths.augmentPTA(asList("c", "d"),false,false,null);
		//updatedGraphExpected.paths.augmentPTA(asList("c", "c", "e"),true,false,null);
		//updatedGraphExpected.paths.augmentPTA(asList("c", "c", "f"),false,false,null);
		compareGraphs(updatedGraphExpected,updatedGraphActual);                                               
	}

	/** Tests that PTA of questions is correctly generated by verifying that generated 
	 * questions cover all new transitions.
	 * @param fsm machine to experiment with (states <em>A</em> and <em>B</em> are merged)
	 * @param name how the machine is to be called.
	 */
	private void checkQuestionAugmentation(String fsm, String name)
	{
		Configuration config = mainConfiguration.copy();config.setLearnerCloneGraph(false);
		LearnerGraph graph = buildLearnerGraph(fsm,name,config,converter);
		StatePair pair = new StatePair(graph.findVertex("A"),graph.findVertex("B"));
		LearnerGraph merged = MergeStates.mergeAndDeterminize_general(graph, pair);
		PTASequenceEngine questions = ComputeQuestions.computeQS_general(pair, graph, merged, new ComputeQuestions.QuestionGeneratorQSMLikeWithLoops());
		LearnerGraph updatedGraphExpected = new LearnerGraph(graph,config),updatedGraphActual = new LearnerGraph(graph,config);
		updatedGraphActual.transitionMatrix.putAll(((NonExistingPaths)questions.getFSM()).getNonExistingTransitionMatrix());
		updatedGraphActual.learnerCache.invalidate();
		
		for(List<Label> path:questions.getData())
			updatedGraphExpected.paths.augmentPTA(path,merged.paths.getVertex(path).isAccept(),false,null);

		compareGraphs(updatedGraphExpected,updatedGraphActual);                                               
	}
	
	/** Using a machine from TestRpniLearner. */
	@Test
	public final void testBuildPTAofQuestions2()
	{
		checkQuestionAugmentation("A-a->B-a->C-b->D\n"+
				"A-b->E",
				"testPairCompatible1");
	}

	@Test
	public final void testBuildPTAofQuestions3()
	{
		checkQuestionAugmentation("A-p->B\n"+
				"A-a->P1-c->B1-b->C1-e->D1\n"+
				"B-a->P2-c->B\n"+
				"A-b->C2-e->D2\n"+
				"B-b->C3-e->D3",
				"testPairCompatible_general_A");
	}

	@Test
	public final void testBuildPTAofQuestions4()
	{
		checkQuestionAugmentation("A-p->B\n"+
				"A-a->B\nA-b->B\nA-e->B\n"+
				"B-e->B4-c->D3-a->T1\n"+
				"B4-d->C3-e->T1\n"+
				"B-c->D1-a->T2\n"+
				"B-b->B5-c->D2-a->T3\n"+
				"B-a->B1-d->C1-e->T4\n"+
				"B1-a->B2-a->B3-d->C2-e->T5",
				"testPairCompatible_general_B");
	}

	@Test
	public final void testBuildPTAofQuestions5()
	{
		checkQuestionAugmentation("A-p->B\n"+
				"A-a->B\nA-b->B\nA-e->B\n"+
				"B4-c->D3-a->T1\n"+
				"B-e->B10-e->B11-e->B12-e->B13-e->B14-e->B15-e->B4-d->C3-e->T1\n"+
				"B-c->D1-a->T2\n"+
				"B-b->B5-c->D2-a->T3\n"+
				"B-a->B1-d->C1-e->T4\n"+
				"B1-a->B2-a->B3-d->C2-e->T5",
				"testPairCompatible_general_C");
	}

	/** Using a machine from TestRpniLearner. */
	@Test
	public final void testBuildPTAofQuestions6()
	{
		checkQuestionAugmentation(TestStateMerging.testGeneralD_fsm,
				"testPairCompatible5");
	}
	
	private static final String ifthenC = "A-a->B-b->C-a->B / P-a->Q-b->R / S-c->S / T-d->N / S=THEN=C=THEN=P / B=THEN=T";

	/** Nothing to augment. */
	@Test
	public final void testPerformAugment1() throws IncompatibleStatesException
	{
		LearnerGraph graph = buildLearnerGraph("A-c->B", "testPerformAugment1",mainConfiguration,converter);
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph(ifthenC, "ifthenC", mainConfiguration,converter)};
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 5);
		compareGraphs(buildLearnerGraph("A-c->B", "testPerformAugment1",mainConfiguration,converter), graph);
	}
	
	/** One state is augmented. */
	@Test
	public final void testPerformAugment2a() throws IncompatibleStatesException
	{
		LearnerGraph graph = buildLearnerGraph("A-a->B", "testPerformAugment2a",mainConfiguration,converter);
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph(ifthenC, "ifthenC", mainConfiguration,converter)};
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 5);
		compareGraphs(buildLearnerGraph("A-a->B-d->C", "testPerformAugment2b",mainConfiguration,converter), graph);
	}
	
	/** Nothing is augmented because the collection of properties is empty. */
	@Test
	public final void testPerformAugment2b() throws IncompatibleStatesException
	{
		LearnerGraph graph = buildLearnerGraph("A-a->B", "testPerformAugment2a",mainConfiguration,converter);
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{};
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 5);
		compareGraphs(buildLearnerGraph("A-a->B", "testPerformAugment1",mainConfiguration,converter), graph);
	}
	
	/** Cannot augment: depth is zero. */
	@Test
	public final void testPerformAugment3() throws IncompatibleStatesException
	{
		LearnerGraph graph = buildLearnerGraph("A-a->B", "testPerformAugment2a",mainConfiguration,converter);
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph(ifthenC, "ifthenC", mainConfiguration,converter)}; 
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 0);
		compareGraphs(buildLearnerGraph("A-a->BC", "testPerformAugment3",mainConfiguration,converter), graph);
	}
	
	/** Two states are augmented. */
	@Test
	public final void testPerformAugment4() throws IncompatibleStatesException
	{
		LearnerGraph graph = buildLearnerGraph("A-a->B-b->C", "testPerformAugment4a",mainConfiguration,converter);
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph(ifthenC, "ifthenC", mainConfiguration,converter)};
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 1);
		//Visualiser.updateFrame(graph, null);
		compareGraphs(buildLearnerGraph("A-a->B-d->U / B-b->C-a->Q / C-c->R", "testPerformAugment4b",mainConfiguration,converter), graph);
	}
	
	/** Given a name of a vertex in the map from states to paths, it will look up the path in the supplied graph
	 * and return its depth.
	 * 
	 * @param whereToLook graph in which to follow paths
	 * @param stateCover map from vertices to paths
	 * @param vertexName name of a vertex to look for
	 * @return depth of the vertex
	 */
	private static int getDepthOfVertex(LearnerGraph whereToLook, Map<CmpVertex,List<Label>> stateCover,
			String vertexName)
	{
		return whereToLook.getVertex(stateCover.get(VertexID.parseID(vertexName))).getDepth();
	}
	
	/** Based on {@link TestAugmentUsingIFTHEN#testPerformAugment5a()}, but constructs an automaton
	 * using PTA. This method additionally checks that the depth
	 * of the created states is set correctly. 
	 */
	@Test
	public final void testPerformAugment5_CheckDepth() throws IncompatibleStatesException
	{
		LearnerGraph graph = new LearnerGraph(mainConfiguration);
		graph.paths.augmentPTA(Arrays.asList(new Label[]{
				AbstractLearnerGraph.generateNewLabel("a", mainConfiguration, converter),
				AbstractLearnerGraph.generateNewLabel("b", mainConfiguration, converter)
				}), true, false, null);
		
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph(ifthenC, "ifthenC", mainConfiguration,converter)}; 
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 2);
		LearnerGraph expectedGraph = buildLearnerGraph("A-a->B-d->N1 / B-b->C-a->B1-b->C1/ B1-d->N2 / C-c->S1-c->S2", "testPerformAugment5a",mainConfiguration,converter);
		compareGraphs(expectedGraph, graph);
		Map<CmpVertex,List<Label>> stateCover =expectedGraph.pathroutines.computeShortPathsToAllStates(expectedGraph.getInit());
		Assert.assertEquals(0,getDepthOfVertex(graph,stateCover,"A"));
		Assert.assertEquals(1,getDepthOfVertex(graph,stateCover,"B"));
		Assert.assertEquals(2,getDepthOfVertex(graph,stateCover,"C"));
		Assert.assertEquals(3,getDepthOfVertex(graph,stateCover,"B1"));
		Assert.assertEquals(4,getDepthOfVertex(graph,stateCover,"C1"));
		Assert.assertEquals(3,getDepthOfVertex(graph,stateCover,"S1"));
		Assert.assertEquals(4,getDepthOfVertex(graph,stateCover,"S2"));
		Assert.assertEquals(2,getDepthOfVertex(graph,stateCover,"N1"));
		Assert.assertEquals(4,getDepthOfVertex(graph,stateCover,"N2"));
	}
	
	/** Two states are augmented a bit further - two steps. This method additionally checks that the depth
	 * of the created states is set correctly. 
	 */
	@Test
	public final void testPerformAugment5a() throws IncompatibleStatesException
	{
		LearnerGraph graph = buildLearnerGraph("A-a->B-b->C", "testPerformAugment4a",mainConfiguration,converter);
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph(ifthenC, "ifthenC", mainConfiguration,converter)}; 
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 2);
		compareGraphs(buildLearnerGraph("A-a->B-d->N1 / B-b->C-a->B1-b->C1/ B1-d->N2 / C-c->S1-c->S2", "testPerformAugment5a",mainConfiguration,converter), graph);
	}

	/** Two states are augmented a bit further - three steps. */
	@Test
	public final void testPerformAugment5b() throws IncompatibleStatesException
	{
		LearnerGraph graph = buildLearnerGraph("A-a->B-b->C", "testPerformAugment4a",mainConfiguration,converter);
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph(ifthenC, "ifthenC", mainConfiguration,converter)}; 
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 3);
		compareGraphs(buildLearnerGraph("A-a->B-d->N1 / B-b->C-a->B1-b->C1-a->B2/ B1-d->N2 / C-c->S1-c->S2-c->S3 / C1-c->S11", "testPerformAugment5b",mainConfiguration,converter), graph);
	}
	
	/** Two states are augmented a bit further - four steps. */
	@Test
	public final void testPerformAugment5c() throws IncompatibleStatesException
	{
		LearnerGraph graph = buildLearnerGraph("A-a->B-b->C", "testPerformAugment4a",mainConfiguration,converter);
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph(ifthenC, "ifthenC", mainConfiguration,converter)}; 
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 4);
		compareGraphs(buildLearnerGraph("A-a->B-d->N1 / B-b->C-a->B1-b->C1-a->B2-b->C2/ B1-d->N2 / B2-d->N3 / C-c->S1-c->S2-c->S3-c->S4 / C1-c->S11-c->S12", "testPerformAugment5c",mainConfiguration,converter), graph);
	}
	
	/** Two states are augmented a bit further - five steps. */
	@Test
	public final void testPerformAugment5d() throws IncompatibleStatesException
	{
		LearnerGraph graph = buildLearnerGraph("A-a->B-b->C", "testPerformAugment4a",mainConfiguration,converter);
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph(ifthenC, "ifthenC", mainConfiguration,converter)}; 
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 5);
		compareGraphs(buildLearnerGraph("A-a->B-d->N1 / B-b->C-a->B1-b->C1-a->B2-b->C2-a->B3/ B1-d->N2 / B2-d->N3 / C-c->S1-c->S2-c->S3-c->S4-c->S5"+
				"/ C1-c->S11-c->S12-c->S13 / C2-c->S21", "testPerformAugment5d",mainConfiguration,converter), graph);
	}
	
	
	/** Non-existing vertices in a tentative automaton. */
	@Test
	public final void testPerformAugment_fail0()
	{
		final LearnerGraph graph = buildLearnerGraph("A-a->B-b->C", "testPerformAugment_fail0a",mainConfiguration,converter);
		graph.transitionMatrix.put(AbstractLearnerGraph.generateNewCmpVertex(new VertexID(VertKind.NONEXISTING,90),mainConfiguration),graph.createNewRow());
		Helper.checkForCorrectException(new whatToRun() { public @Override void run() throws IncompatibleStatesException {
			LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph("A-a->B-a->B /  T-b->N / B=THEN=T", "testPerformAugment_fail0b", mainConfiguration,converter)}; 
			Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 2);
		}},IllegalArgumentException.class,"non-existing vertices");
	}
	
	/** Contradiction between a new state and a graph, first when everything is ok. */
	@Test
	public final void testPerformAugment6a() throws IncompatibleStatesException
	{
		LearnerGraph graph = buildLearnerGraph("A-a->B", "testPerformAugment6a",mainConfiguration,converter);
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph("A-a->B-a->B /  T-b-#N / B=THEN=T", "testPerformAugment_fail1", mainConfiguration,converter)}; 
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 2);
		compareGraphs(buildLearnerGraph("A-a->B-b-#N1", "testPerformAugment6a",mainConfiguration,converter), graph);
	}
	
	/** Contradiction between a new state and a graph, first when everything is ok. */
	@Test
	public final void testPerformAugment6b() throws IncompatibleStatesException
	{
		LearnerGraph graph = buildLearnerGraph("A-a->B-b-#C", "testPerformAugment6b",mainConfiguration,converter);
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph("A-a->B-a->B /  T-b-#N / B=THEN=T", "testPerformAugment_fail1", mainConfiguration,converter)}; 
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 2);
		compareGraphs(buildLearnerGraph("A-a->B-b-#N1", "testPerformAugment6a",mainConfiguration,converter), graph);
	}

	/** Contradiction between a new state and a graph, first when everything is ok. */
	@Test
	public final void testPerformAugment6c() throws IncompatibleStatesException
	{
		LearnerGraph graph = buildLearnerGraph("A-a->B-b-#C / B-a->D", "testPerformAugment6c",mainConfiguration,converter);
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph("A-a->B-a->B /  T-b-#N / B=THEN=T", "testPerformAugment_fail1", mainConfiguration,converter)}; 
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 2);
		compareGraphs(buildLearnerGraph("A-a->B-b-#N1 / B-a->D-b-#N2", "testPerformAugment6c",mainConfiguration,converter), graph);
	}
	
	/** Contradiction between a new state and a graph, first when everything is ok. */
	@Test
	public final void testPerformAugment6d() throws IncompatibleStatesException
	{
		LearnerGraph graph = buildLearnerGraph("A-a->B-b-#C / B-a->D-b-#N2", "testPerformAugment6d",mainConfiguration,converter);
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph("A-a->B-a->B /  T-b-#N / B=THEN=T", "testPerformAugment_fail1", mainConfiguration,converter)}; 
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 2);
		compareGraphs(buildLearnerGraph("A-a->B-b-#N1 / B-a->D-b-#N2", "testPerformAugment6c",mainConfiguration,converter), graph);
	}
	
	
	/** Contradiction between a new state and a graph. */
	@Test
	public final void testPerformAugment_fail1()
	{
		final LearnerGraph graph = buildLearnerGraph("A-a->B-b->C", "testPerformAugment_fail1a",mainConfiguration,converter);
		Helper.checkForCorrectException(new whatToRun() { public @Override void run() throws IncompatibleStatesException {
			LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph("A-a->B-a->B /  T-b-#N / B=THEN=T", "testPerformAugment_fail1", mainConfiguration,converter)}; 
			Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 2);
		}},AugmentFromIfThenAutomatonException.class,"cannot merge a tentative state");
	}
	
	/** Contradiction between a new state and a graph after unrolling the property a few times. */
	@Test
	public final void testPerformAugment_fail2()
	{
		final LearnerGraph graph = buildLearnerGraph("A-a->B-a->C-a->D-b->E", "testPerformAugment_fail1a",mainConfiguration,converter);
		Helper.checkForCorrectException(new whatToRun() { public @Override void run() throws IncompatibleStatesException {
			LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph("A-a->B-a->B /  T-b-#N / B=THEN=T", "testPerformAugment_fail1", mainConfiguration,converter)}; 
			Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 2);
		}},AugmentFromIfThenAutomatonException.class,"cannot merge a tentative state");
	}
	
	/** Contradiction between states added by THEN graphs, first when everything is ok. 
	 * @throws IncompatibleStatesException */
	@Test
	public final void testPerformAugment7() throws IncompatibleStatesException
	{
		final LearnerGraph graph = buildLearnerGraph("A-a->B", "testPerformAugment_fail1a",mainConfiguration,converter);
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph("A-a->B-a->B /  T-c->T1-c->T2-c-#T3 / R-c->R1-c->R2-b->R3 / R=THEN=B=THEN=T", "testPerformAugment_fail1", mainConfiguration,converter)}; 
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 2);
		compareGraphs(buildLearnerGraph("A-a->B-c->T1-c->T2", "testPerformAugment6b",mainConfiguration,converter), graph);
	}
	
	/** Contradiction between states added by THEN graphs, first when everything is ok. 
	 * @throws IncompatibleStatesException */
	@Test
	public final void testPerformAugment8() throws IncompatibleStatesException
	{
		final LearnerGraph graph = buildLearnerGraph("A-a->B", "testPerformAugment_fail1a",mainConfiguration,converter);
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph("A-a->B-a->B /  T-c->T1-c->T2-c-#T3 / R-c->R1-c->R2-b->R3 / R=THEN=B=THEN=T", "testPerformAugment_fail1", mainConfiguration,converter)}; 
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 3);
		compareGraphs(buildLearnerGraph("A-a->B-c->T1-c->T2-c-#T3 / T2-b->T4", "testPerformAugment6b",mainConfiguration,converter), graph);
	}
	
	/** Contradiction between states added by THEN graphs, first when everything is ok. 
	 * @throws IncompatibleStatesException */
	@Test
	public final void testPerformAugment9() throws IncompatibleStatesException
	{
		final LearnerGraph graph = buildLearnerGraph("A-a->B", "testPerformAugment_fail1a",mainConfiguration,converter);
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph("A-a->B-a->B /  T-c->T1-c->T2-c-#T3 / R-c->R1-c->R2-b->R3 / R=THEN=B=THEN=T", "testPerformAugment_fail1", mainConfiguration,converter)}; 
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 7);
		compareGraphs(buildLearnerGraph("A-a->B-c->T1-c->T2-c-#T3 / T2-b->T4", "testPerformAugment6b",mainConfiguration,converter), graph);
	}
	
	/** Not yet a contradiction between states added by THEN graphs - the depth of exploration is 
	 * too low to hit it. 
	 * @throws IncompatibleStatesException */
	@Test
	public final void testPerformAugment10() throws IncompatibleStatesException
	{
		final LearnerGraph graph = buildLearnerGraph("A-a->B", "testPerformAugment_fail1a",mainConfiguration,converter);
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph("A-a->B-a->B /  T-c->T1-c->T2-b-#T3 / R-c->R1-c->R2-b->R3 / R=THEN=B=THEN=T", "testPerformAugment_fail1", mainConfiguration,converter)}; 
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 2);
		compareGraphs(buildLearnerGraph("A-a->B-c->T1-c->T2", "testPerformAugment6b",mainConfiguration,converter), graph);
	}
	
	/** Contradiction between states added by THEN graphs. */
	@Test
	public final void testPerformAugment_fail3()
	{
		final LearnerGraph graph = buildLearnerGraph("A-a->B", "testPerformAugment_fail1a",mainConfiguration,converter);
		Helper.checkForCorrectException(new whatToRun() { public @Override void run() throws IncompatibleStatesException {
			LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph("A-a->B-a->B /  T-c->T1-c->T2-b-#T3 / R-c->R1-c->R2-b->R3 / R=THEN=B=THEN=T", "testPerformAugment_fail1", mainConfiguration,converter)};
			Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 3);
		}},AugmentFromIfThenAutomatonException.class,"cannot merge a tentative state");
	}
	
	/** Incompatibility between an existing state and a new one. */
	@Test
	public final void testPerformAugment_fail4()
	{
		final LearnerGraph graph = buildLearnerGraph("A-a->B-b-#C", "testPerformAugment_fail4a",mainConfiguration,converter);
		Helper.checkForCorrectException(new whatToRun() { public @Override void run() throws IncompatibleStatesException {
			LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph("A-a->B-a->B /  T-b->N / B=THEN=T", "testPerformAugment_fail4", mainConfiguration,converter)}; 
			Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 2);
		}},AugmentFromIfThenAutomatonException.class,"cannot merge a tentative state");
	}
	
	/** Reject-states cannot be extended even if they match a property. 
	 * @throws AugmentFromIfThenAutomatonException */
	@Test
	public final void testPerformAugment_reject1() throws AugmentFromIfThenAutomatonException
	{
		final LearnerGraph graph = buildLearnerGraph("A-a-#B", "testPerformAugment_reject1a",mainConfiguration,converter);
		LearnerGraph ifthen = buildLearnerGraph("A-a->B-a->B /  T-b->N / B=THEN=T", "testPerformAugment_fail4", mainConfiguration,converter);
		ifthen.findVertex("T").setAccept(false);
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{ifthen}; 
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 2);
		compareGraphs(buildLearnerGraph("A-a-#B", "testPerformAugment_reject1b",mainConfiguration,converter), graph);
	}
	
	/** Another example of a contradiction between a tentative graph and the property. */
	@Test
	public final void testPerformAugment_fail5()
	{
		final LearnerGraph graph = buildLearnerGraph("A-a->B-b-#C", "testPerformAugment_fail5a",mainConfiguration,converter);
		Helper.checkForCorrectException(new whatToRun() { public @Override void run() throws IncompatibleStatesException {
			LearnerGraph ifthen = buildLearnerGraph("A-a->B-a->B /  T-b->N-s->R / B=THEN=T", "testPerformAugment_fail5", mainConfiguration,converter);
			LearnerGraph[] ifthenCollection = new LearnerGraph[]{ifthen}; 
			Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 2);
		}},AugmentFromIfThenAutomatonException.class,"cannot merge a tentative");
	}
	
	/** Dummy property - dummy means satisfied as long as the initial state of a tentative automaton is accept. */
	@Test
	public final void testPerformAugment11() throws IncompatibleStatesException
	{
		LearnerGraph graph = buildLearnerGraph("A-a->B", "testPerformAugment11a",mainConfiguration,converter);
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph("A-a->B / P-b->P / A==THEN==P", "testPerformAugment11", mainConfiguration,converter)}; 
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 4);
		compareGraphs(buildLearnerGraph("A-a->B / A-b->C-b->D-b->E-b->F", "testPerformAugment11b",mainConfiguration,converter), graph);
	}
	
	/** Dummy limited property. */
	@Test
	public final void testPerformAugment12() throws IncompatibleStatesException
	{
		LearnerGraph graph = buildLearnerGraph("A-a->B", "testPerformAugment11a",mainConfiguration,converter);
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph("A-a->B / P-b->Q-c->R / A==THEN==P", "testPerformAugment11", mainConfiguration,converter)}; 
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 4);
		compareGraphs(buildLearnerGraph("A-a->B / A-b->C-c->D", "testPerformAugment11b",mainConfiguration,converter), graph);
	}
	
	/** Dummy limited property and the first automaton does not match at all - which does not matter since property is dummy. */
	@Test
	public final void testPerformAugment13() throws IncompatibleStatesException
	{
		LearnerGraph graph = buildLearnerGraph("A-s->B", "testPerformAugment11a",mainConfiguration,converter);
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph("A-a->B / P-b->Q-c->R / A==THEN==P", "testPerformAugment11", mainConfiguration,converter)}; 
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 4);
		compareGraphs(buildLearnerGraph("A-s->B / A-b->C-c->D", "testPerformAugment11b",mainConfiguration,converter), graph);
	}
	
	/** Infinitely matching property - this test is similar to a testPerformAugment5 series. */
	@Test
	public final void testPerformAugment14() throws IncompatibleStatesException
	{
		LearnerGraph graph = buildLearnerGraph("A-a->B-b->C-a->B", "testPerformAugment14a",mainConfiguration,converter);
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph("A-a->B-b->C / P-a->Q-b->P-c->S / C==THEN==P", "testPerformAugment14", mainConfiguration,converter)}; 
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 400);
		compareGraphs(buildLearnerGraph("A-a->B-b->C-a->B / C-c->D", "testPerformAugment14b",mainConfiguration,converter), graph);
	}

	/** Two properties where there is one which matches after a while and another short one which depends on the result of the match of the first one. */ 
	@Test
	public final void testPerformAugment15() throws IncompatibleStatesException
	{
		LearnerGraph graph = buildLearnerGraph("A-a->B-b->A", "testPerformAugment15a",mainConfiguration,converter);
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{
				buildLearnerGraph("A-a->B-b->C-a->D-b->E / P-a->Q-c->S / E==THEN==P", "testPerformAugment15c", mainConfiguration,converter),
				buildLearnerGraph("A-a->B-c->C / P-d->Q / C==THEN==P", "testPerformAugment15d", mainConfiguration,converter)				
		}; 
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 400);
		compareGraphs(buildLearnerGraph("bA-a->bB-b->bA / bB-c->bC-d->bD", "testPerformAugment15b",mainConfiguration,converter), graph);
	}
	
	/** Two properties where there is one which matches after a while and another short one which depends on the result of the match of the first one. */ 
	@Test
	public final void testPerformAugment16a() throws IncompatibleStatesException
	{
		LearnerGraph graph = buildLearnerGraph("A-a->B-b->A", "testPerformAugment15a",mainConfiguration,converter);
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{
				buildLearnerGraph("A-a->B-b->C-a->D-b->E / B-c->F-d->G / P-a->Q-c->S / T-e->U / E==THEN==P / G==THEN==T", "testPerformAugment16c", mainConfiguration,converter),
				buildLearnerGraph("1A-a->1B-c->1C / 1P-d->1Q / 1C==THEN==1P", "testPerformAugment16a", mainConfiguration,converter)				
		}; 
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 400);
		compareGraphs(buildLearnerGraph("bA-a->bB-b->bA / bB-c->bC-d->bD-e->bE", "testPerformAugment16a",mainConfiguration,converter), graph);
	}
	
	/** Two properties where there is one which matches after a while and another short one which depends on the result of the match of the first one. */ 
	@Test
	public final void testPerformAugment16b() throws IncompatibleStatesException
	{
		LearnerGraph graph = buildLearnerGraph("A-a->B-b->A", "testPerformAugment15a",mainConfiguration,converter);
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{
				buildLearnerGraph("A-a->B-b->C-a->D-b->E / B-c->F-d->G / P-a->Q-c->S / T-e->U / E==THEN==P / G==THEN==T /"+
						"D-c->D2-d->R / RB1-d->RB2 / R == THEN == RB1 ", "testPerformAugment16c", mainConfiguration,converter),
				buildLearnerGraph("1A-a->1B-c->1C / 1P-d->1Q / 1C==THEN==1P", "testPerformAugment16a", mainConfiguration,converter)				
		}; 
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 400);
		compareGraphs(buildLearnerGraph("bA-a->bB-b->bA / bB-c->bC-d->bD-e->bE / bD-d->bF", "testPerformAugment16b",mainConfiguration,converter), graph);
	}
	
	
	static String ifthen_ab_to_c = "A0-a->B0-b->C0-a->B0 / C0-b->A0 / D0-c->E0 / C0 == THEN == D0",
		ifthen_a_to_c = "A1-a->B1-b->A1 / C1-c->D1 / B1 == THEN == C1",
		ifthen_c_to_cc = "A2-a->A2-b->A2-c->B2-c->B2 / B2-a->A2 / B2-b->A2 / C2-c->D2 / B2 == THEN == C2",
		ifthen_ccc_to_ab = "A3-a->A3-b->A3-c->B3-c->C3-c->D3-c->C3 / B3-a->A3 / B3-b->A3 / C3-a->A3 / C3-b->A3 / D3-a->A3 / D3-b->A3 / E3-a->F3-b->G3 / D3 == THEN == E3";
	
	/** Three if-then automata, two of which recursively expand each other. */
	@Test
	public final void testPerformAugment17() throws IncompatibleStatesException
	{
		LearnerGraph graph = buildLearnerGraph("A-b->B-c->A-c->B", "testPerformAugment17",mainConfiguration,converter);
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{
				buildLearnerGraph(ifthen_a_to_c, "ifthen_a_to_c", mainConfiguration,converter),
				buildLearnerGraph(ifthen_c_to_cc, "ifthen_c_to_cc", mainConfiguration,converter),			
				buildLearnerGraph(ifthen_ccc_to_ab, "ifthen_ccc_to_ab", mainConfiguration,converter)
		};
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 9);
		//Visualiser.updateFrame(graph, null);Visualiser.waitForKey();
		compareGraphs(buildLearnerGraph("A-b->B-c->A-c->B / "+
				"A-a->A1-b->B1 / "+
				"B-a->A2-b->B2 / "+
				"A1-c->C1-c->C2-c->C3-c->C4-c->C5-c->C6-c->C7-c->C8 / "+
				"C3-a->A3-b->B3 / "+"C5-a->A3 / "+"C7-a->A4 / "
				, "testPerformAugment17b",mainConfiguration,converter), graph);
	}
	
	/** Three if-then automata, two of which recursively expand each other. */
	@Test
	public final void testPerformAugment18() throws IncompatibleStatesException
	{
		LearnerGraph graph = buildLearnerGraph("A-b->B-c->A-c->B", "testPerformAugment17",mainConfiguration,converter);
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{
				buildLearnerGraph(ifthen_ab_to_c, "ifthen_ab_to_c", mainConfiguration,converter),
				buildLearnerGraph(ifthen_c_to_cc, "ifthen_c_to_cc", mainConfiguration,converter),			
				buildLearnerGraph(ifthen_ccc_to_ab, "ifthen_ccc_to_ab", mainConfiguration,converter)
		};
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 9);
		compareGraphs(buildLearnerGraph("A-b->B-c->A-c->B / "+
				"A-a->A1-b->B1 / "+
				"B-a->A2-b->B2 / "+
				"B1-c->C1-c->C2-c->C3-c->C4-c->C5-c->C6-c->C7 / "+
				"C3-a->A3-b->B3 / "+"C5-a->A3 "
				, "testPerformAugment17b",mainConfiguration,converter), graph);
	}
	
	/** Three if-then automata, two of which recursively expand each other - very similar to the above but operates on a tree. */
	@Test
	public final void testPerformAugment19() throws IncompatibleStatesException
	{
		LearnerGraph graph = buildLearnerGraph("A-a->B-b->C-a->D-b->E-a->F-b->G", "testPerformAugment19",mainConfiguration,converter);
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{
				buildLearnerGraph(ifthen_ab_to_c, "ifthen_ab_to_c", mainConfiguration,converter),
				buildLearnerGraph(ifthen_c_to_cc, "ifthen_c_to_cc", mainConfiguration,converter),			
				buildLearnerGraph(ifthen_ccc_to_ab, "ifthen_ccc_to_ab", mainConfiguration,converter)
		};
		Transform.augmentFromIfThenAutomaton(graph, null, ifthenCollection, 7);
		//Visualiser.updateFrame(graph, null);Visualiser.waitForKey();
		compareGraphs(buildLearnerGraph("A-a->B-b->C-a->D-b->E-a->F-b->G / "+
				"C-c->C11-c->C12-c->C13-c->C14-c->C15-c->C16-c->C17 / C13-a->13A-b->13B / C15-a->15A-b->15B /"+
				"E-c->C21-c->C22-c->C23-c->C24-c->C25-c->C26-c->C27 / C23-a->23A-b->23B / C25-a->25A-b->25B /"+
				"G-c->C31-c->C32-c->C33-c->C34-c->C35-c->C36-c->C37 / C33-a->33A-b->33B / C35-a->35A-b->35B"
				, "testPerformAugment17b",mainConfiguration,converter), graph);
	}
	
	
	/** Tests how properties can answer questions. 
	 * @throws IncompatibleStatesException */
	@Test
	public final void testQuestionAnswering1() throws IncompatibleStatesException
	{
		LearnerGraph graph = buildLearnerGraph(
				"A1-a->B1-b->A2-a->B2-b->A3-a->B3-b->A4-a->B4-b->A5 /"+
				"A2-c->A21-c->A22-e->A23 /"+
				"A3-c->A31-c->A32 / A31-d-#A34 /"+
				"A4-c->A41-c->A42-f-#A43 /"+
				"A5-c->A51-c->A52"
				, "testBuildPTAofQuestions1",mainConfiguration,converter);
		StatePair pair = new StatePair(graph.findVertex("A1"),graph.findVertex("A2"));
		LearnerGraph merged = MergeStates.mergeAndDeterminize_general(graph, pair);
		compareGraphs(buildLearnerGraph("A1-a->B1-b->A1-c->C-d-#R4/C-c->CC/CC-f-#R3/CC-e->D", "testQuestionAnswering2b",mainConfiguration,converter),merged);
		PTASequenceEngine questions = ComputeQuestions.computeQS_general(pair, graph, merged, new ComputeQuestions.QuestionGeneratorQSMLikeWithLoops());
		// the IF part we're augmenting with is a dummy one
		LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph("A-s->B / P-c->Q-d-#R / S-c->S1-c->S2-e->S3 / S==THEN==A==THEN==P", "testQuestionAnswering1", mainConfiguration,converter)}; 
		Transform.augmentFromIfThenAutomaton(graph, (NonExistingPaths)questions.getFSM(), ifthenCollection, 0);
		List<List<Label>> questionList = questions.getData();
		Assert.assertEquals(1,questionList.size());
		Assert.assertEquals(labelList(new String[]{"c","c","f"}), questionList.iterator().next());
	}

	@Test
	public final void testConversionOfAssociationsToTransitions1()
	{
		Configuration config = mainConfiguration.copy();config.setLearnerCloneGraph(false);
		LearnerGraph graph = buildLearnerGraph("A-a->B / P-b->Q-c->R / ", "testConversionOfAssociationsToTransitions1a", config,converter);
                DirectedSparseGraph graphAfterConversion = graph.pathroutines.getGraph();
                PathRoutines.convertPairAssociationsToTransitions(graphAfterConversion,graph, config,converter);
		graph.pairCompatibility.compatibility.clear();
		LearnerGraph obtainedGraph = new LearnerGraph(graphAfterConversion,config);
		WMethod.checkM_and_colours(buildLearnerGraph("A-a->B / P-b->Q-c->R / ",
				"testConversionOfAssociationsToTransitions1b",config,converter),obtainedGraph ,VERTEX_COMPARISON_KIND.DEEP);
		Assert.assertNull(graphAfterConversion.getUserDatum(JUConstants.VERTEX));
		Assert.assertTrue(
				((EdgeAnnotation)graphAfterConversion.getUserDatum(JUConstants.EDGE)).isEmpty());
	}
	
	@Test
	public final void testConversionOfAssociationsToTransitions2()
	{
		Configuration config = mainConfiguration.copy();config.setLearnerCloneGraph(false);
		LearnerGraph graph = buildLearnerGraph("A-a->B / P-b->Q-c->R / A==THEN==P / B=INCOMPATIBLE=Q=MERGED=R", "testConversionOfAssociationsToTransitions2a", config,converter);
                DirectedSparseGraph graphAfterConversion = graph.pathroutines.getGraph();
		PathRoutines.convertPairAssociationsToTransitions(graphAfterConversion,graph, config,converter);
		graph.pairCompatibility.compatibility.clear();
		LearnerGraph obtainedGraph = new LearnerGraph(graphAfterConversion,config);
		WMethod.checkM_and_colours(buildLearnerGraph("A-a->B / P-b->Q-c->R / "+
				"A-"+PathRoutines.associationPrefix+PAIRCOMPATIBILITY.THEN.name()+"->P / "+
				"P-"+PathRoutines.associationPrefix+PAIRCOMPATIBILITY.THEN.name()+"->A / "+
				
				"B-"+PathRoutines.associationPrefix+PAIRCOMPATIBILITY.INCOMPATIBLE.name()+"->Q / "+
				"Q-"+PathRoutines.associationPrefix+PAIRCOMPATIBILITY.INCOMPATIBLE.name()+"->B / "+
				
				"Q-"+PathRoutines.associationPrefix+PAIRCOMPATIBILITY.MERGED.name()+"->R / "+
				"R-"+PathRoutines.associationPrefix+PAIRCOMPATIBILITY.MERGED.name()+"->Q / ", 
				"testConversionOfAssociationsToTransitions2b",config,converter), obtainedGraph,VERTEX_COMPARISON_KIND.DEEP);

		Assert.assertNull(graphAfterConversion.getUserDatum(JUConstants.VERTEX));
		Assert.assertEquals("{A={"+PathRoutines.associationPrefix+PAIRCOMPATIBILITY.THEN.name()+"={P=java.awt.Color[r=255,g=255,b=0]}}, " +
				"B={"+PathRoutines.associationPrefix+PAIRCOMPATIBILITY.INCOMPATIBLE.name()+"={Q=java.awt.Color[r=255,g=255,b=0]}}, " +
				"P={"+PathRoutines.associationPrefix+PAIRCOMPATIBILITY.THEN.name()+"={A=java.awt.Color[r=255,g=255,b=0]}}, " +
				"Q={"+PathRoutines.associationPrefix+PAIRCOMPATIBILITY.INCOMPATIBLE.name()+"={B=java.awt.Color[r=255,g=255,b=0]}, "+PathRoutines.associationPrefix+PAIRCOMPATIBILITY.MERGED.name()+"={R=java.awt.Color[r=255,g=255,b=0]}}, " +
				"R={"+PathRoutines.associationPrefix+PAIRCOMPATIBILITY.MERGED.name()+"={Q=java.awt.Color[r=255,g=255,b=0]}}}",
				((EdgeAnnotation)graphAfterConversion.getUserDatum(JUConstants.EDGE)).toString());

	}

	@RunWith(ParameterizedWithName.class)
	public static final class TestQuestionPTA extends TestWithMultipleConfigurations
	{
		@Parameters
		public static Collection<Object[]> data() 
		{
			return TestWithMultipleConfigurations.data();
		}
		
		@ParametersToString
		public static String parametersToString(Configuration config)
		{
			return TestWithMultipleConfigurations.parametersToString(config);
		}

		public TestQuestionPTA(Configuration conf)
		{
			super(conf);
		}

		private final static String ifthen_sc = "I-s->A-c->B / P-d-#R / P-c->T1-e->T2 / P-a->T / P==THEN==B",// "if" part requires "s c" to be confirmed and rules out "s c d" and "s c c e"
			ifthen_sc_unsat = "I-s->A-c-#B / P-d-#R / P-c->T1-e->T2 / P-a->T / P==THEN==B",// "if" part attempts to match a reject-state - this will not be the case
		ifthen_s = "I-s->B / P-c->Q-d-#R / P-a->T / P-b->T1 / P==THEN==B",// the B state in these "if" graphs corresponds to A.._U1 series in "graphWithAppendixAfterMerging"
		graphWithAppendixAfterMerging = "A-s->A1 / "+
		"A1-a->B1-b->A2-a->B2-b->A3-a->B3-b->A4-a->B4-b->A5 /"+
		"A2-c->A21-c->A22-e->A23 /"+
		"A3-c->A31-c->A32 / A31-d-#A34 /"+
		"A4-c->A41-c->A42-f-#A43 /"+
		"A5-c->A51-c->A52";
	
		LearnerGraph graph = null;
		private StatePair pair = null;
		private LearnerGraph merged =null;
		private PTASequenceEngine questions = null;
		private LearnerGraph origGraph = null;
		
		@Before
		public final void beforeTest()
		{
			mainConfiguration.setLearnerCloneGraph(false);
			origGraph = FsmParser.buildLearnerGraph(graphWithAppendixAfterMerging, "graphWithAppendixAfterMerging",mainConfiguration,converter);
			graph = new LearnerGraph(origGraph,mainConfiguration);
			
			pair = new StatePair(graph.findVertex("A1"),graph.findVertex("A2"));
			merged = MergeStates.mergeAndDeterminize_general(graph, pair);
			
			//Visualiser.updateFrame(graph, merged);Visualiser.waitForKey();
			compareGraphs(buildLearnerGraph("A-s->A1-a->B1-b->A1-c->C-d-#R4/C-c->CC/CC-f-#R3/CC-e->D", "testQuestionAnswering2b",mainConfiguration,converter),merged);
			questions = ComputeQuestions.computeQS_general(pair, graph, merged, new ComputeQuestions.QuestionGeneratorQSMLikeWithLoops());
			Assert.assertEquals(3,questions.getData().size());// whether questions are correctly generated is tested in "testQuestionAnswering2"
		}
		
		/** Tests that the question part is matched to the IF part only if it matches the THEN part.
		 * @throws IncompatibleStatesException */
		@Test
		public final void testQuestionAnswering2() throws IncompatibleStatesException
		{
			LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph(ifthen_sc, "ifthen_sc", mainConfiguration,converter)}; 
			Transform.augmentFromIfThenAutomaton(graph, (NonExistingPaths)questions.getFSM(), ifthenCollection, 0);
			List<List<Label>> questionList = questions.getData();
			Assert.assertEquals(3,questionList.size());
			Set<List<Label>> expected = TestFSMAlgo.buildSet(new String[][]{new String[]{"s","c","d"},new String[]{"s","c","c","f"},new String[]{"s","c","c","e"}},mainConfiguration,converter);
			Set<List<Label>> actual = new LinkedHashSet<List<Label>>();actual.addAll(questionList);
			Assert.assertEquals(expected,actual);
		}
		
		/** Tests that the question part is matched to the IF part only if it matches the THEN part.
		 * @throws IncompatibleStatesException */
		@Test
		public final void testQuestionAnswering3a() throws IncompatibleStatesException
		{
			LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph(ifthen_s, "ifthen_s", mainConfiguration,converter)}; 
			Transform.augmentFromIfThenAutomaton(graph, (NonExistingPaths)questions.getFSM(), ifthenCollection, 0);
			List<List<Label>> questionList = questions.getData();
			Assert.assertEquals(2,questionList.size());
			Set<List<Label>> expected = TestFSMAlgo.buildSet(new String[][]{new String[]{"s","c","c","f"},new String[]{"s","c","c","e"}},mainConfiguration,converter);
			Set<List<Label>> actual = new LinkedHashSet<List<Label>>();actual.addAll(questionList);
			Assert.assertEquals(expected,actual);
		
			// now do the same again - should not change anything. 
			ifthenCollection = new LearnerGraph[]{buildLearnerGraph(ifthen_s, "ifthen_s", mainConfiguration,converter)};
			Transform.augmentFromIfThenAutomaton(graph, (NonExistingPaths)questions.getFSM(), ifthenCollection, 0);
			compareGraphs(new LearnerGraph(origGraph,mainConfiguration),graph);// check that augment did not modify the automaton
			questionList = questions.getData();
			Assert.assertEquals(2,questionList.size());
			actual = new LinkedHashSet<List<Label>>();actual.addAll(questionList);
			Assert.assertEquals(expected,actual);
		}
		
		/** Same as above but do the two (identical) things at the same time. */
		@Test
		public final void testQuestionAnswering3b() throws IncompatibleStatesException
		{
			LearnerGraph[] ifthenCollection = new LearnerGraph[]{
					buildLearnerGraph(ifthen_s, "ifthen_s", mainConfiguration,converter), 
					buildLearnerGraph(ifthen_s, "ifthen_s", mainConfiguration,converter)}; 
			Transform.augmentFromIfThenAutomaton(graph, (NonExistingPaths)questions.getFSM(), ifthenCollection, 0);
			List<List<Label>> questionList = questions.getData();
			Assert.assertEquals(2,questionList.size());
			Set<List<Label>> expected = TestFSMAlgo.buildSet(new String[][]{new String[]{"s","c","c","f"},new String[]{"s","c","c","e"}},mainConfiguration,converter);
			Set<List<Label>> actual = new LinkedHashSet<List<Label>>();actual.addAll(questionList);
			Assert.assertEquals(expected,actual);
		
			// now do the same again - should not change anything. 
			ifthenCollection = new LearnerGraph[]{
					buildLearnerGraph(ifthen_s, "ifthen_s", mainConfiguration,converter),
					buildLearnerGraph(ifthen_s, "ifthen_s", mainConfiguration,converter)};
			Transform.augmentFromIfThenAutomaton(graph, (NonExistingPaths)questions.getFSM(), ifthenCollection, 0);
			compareGraphs(new LearnerGraph(origGraph,mainConfiguration),graph);// check that augment did not modify the automaton
			questionList = questions.getData();
			Assert.assertEquals(2,questionList.size());
			actual = new LinkedHashSet<List<Label>>();actual.addAll(questionList);
			Assert.assertEquals(expected,actual);
		}
		
		/** Tests that the question part is matched to the IF part only if it matches the THEN part. 
		 * Note that "if_then_sc" depends on the outcome of "if_then_s".
		 * 
		 * @throws IncompatibleStatesException */
		@Test
		public final void testQuestionAnswering4a() throws IncompatibleStatesException
		{
			LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph(ifthen_s, "ifthen_s", mainConfiguration,converter)}; 
			Transform.augmentFromIfThenAutomaton(graph, (NonExistingPaths)questions.getFSM(), ifthenCollection, 0);
			ifthenCollection = new LearnerGraph[]{buildLearnerGraph(ifthen_sc, "ifthen_sc", mainConfiguration,converter)};
			Transform.augmentFromIfThenAutomaton(graph, (NonExistingPaths)questions.getFSM(), ifthenCollection, 0);
			
			compareGraphs(new LearnerGraph(origGraph,mainConfiguration),graph);// check that augment did not modify the automaton
			List<List<Label>> questionList = questions.getData();
			Assert.assertEquals(1,questionList.size());
			Set<List<Label>> expected = TestFSMAlgo.buildSet(new String[][]{new String[]{"s","c","c","f"}},mainConfiguration,converter);
			Set<List<Label>> actual = new LinkedHashSet<List<Label>>();actual.addAll(questionList);
			Assert.assertEquals(expected,actual);
		}
		
		/** Same as above but do the two (identical) things at the same time.
		 * Note that "if_then_sc" depends on the outcome of "if_then_s".
		 * 
		 * @throws IncompatibleStatesException */
		@Test
		public final void testQuestionAnswering4b() throws IncompatibleStatesException
		{
			LearnerGraph[] ifthenCollection = new LearnerGraph[]{
					buildLearnerGraph(ifthen_s, "ifthen_s", mainConfiguration,converter), 
					buildLearnerGraph(ifthen_sc, "ifthen_sc", mainConfiguration,converter)
			};
			Transform.augmentFromIfThenAutomaton(graph, (NonExistingPaths)questions.getFSM(), ifthenCollection, 0);
			
			compareGraphs(new LearnerGraph(origGraph,mainConfiguration),graph);// check that augment did not modify the automaton
			List<List<Label>> questionList = questions.getData();
			Assert.assertEquals(1,questionList.size());
			Set<List<Label>> expected = TestFSMAlgo.buildSet(new String[][]{new String[]{"s","c","c","f"}},mainConfiguration,converter);
			Set<List<Label>> actual = new LinkedHashSet<List<Label>>();actual.addAll(questionList);
			Assert.assertEquals(expected,actual);
		}
		
		/** Tests that the question part is matched to the IF part only if it has been answered by a user.
		 * First, with IF part which cannot be satisfied.
		 * 
		 * @throws IncompatibleStatesException */
		@Test
		public final void testQuestionAnswering5a() throws IncompatibleStatesException
		{
			LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph(ifthen_sc, "ifthen_sc", mainConfiguration,converter)}; 
			Transform.augmentFromIfThenAutomaton(graph, (NonExistingPaths)questions.getFSM(), ifthenCollection, 0);
			compareGraphs(new LearnerGraph(origGraph,mainConfiguration),graph);// check that augment did not modify the automaton
			graph.learnerCache.questionsPTA=questions;
			Assert.assertEquals(3,questions.getData().size());
			Transform.augmentFromIfThenAutomaton(graph, (NonExistingPaths)questions.getFSM(), ifthenCollection, 0);
			compareGraphs(new LearnerGraph(origGraph,mainConfiguration),graph);// check that augment did not modify the automaton
	
			List<List<Label>> questionList = questions.getData();
			Assert.assertEquals(3,questionList.size());
			Set<List<Label>> expected = TestFSMAlgo.buildSet(new String[][]{new String[]{"s","c","d"},new String[]{"s","c","c","f"},new String[]{"s","c","c","e"}},mainConfiguration,converter);
			Set<List<Label>> actual = new LinkedHashSet<List<Label>>();actual.addAll(questionList);
			Assert.assertEquals(expected,actual);
		}
		
		/** Tests that the question part is matched to the IF part only if it has been answered by a user.
		 * First, with IF part which cannot be satisfied.
		 * 
		 * @throws IncompatibleStatesException */
		@Test
		public final void testQuestionAnswering5b() throws IncompatibleStatesException
		{
			LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph(ifthen_sc_unsat, "ifthen_sc", mainConfiguration,converter)}; 
			Transform.augmentFromIfThenAutomaton(graph, (NonExistingPaths)questions.getFSM(), ifthenCollection, 0);
			compareGraphs(new LearnerGraph(origGraph,mainConfiguration),graph);// check that augment did not modify the automaton
			graph.learnerCache.questionsPTA=questions;
			Assert.assertEquals(3,questions.getData().size());
			Transform.augmentFromIfThenAutomaton(graph, (NonExistingPaths)questions.getFSM(), ifthenCollection, 0);
			compareGraphs(new LearnerGraph(origGraph,mainConfiguration),graph);// check that augment did not modify the automaton
	
			List<List<Label>> questionList = questions.getData();
			Assert.assertEquals(3,questionList.size());
			Set<List<Label>> expected = TestFSMAlgo.buildSet(new String[][]{new String[]{"s","c","d"},new String[]{"s","c","c","f"},new String[]{"s","c","c","e"}}, mainConfiguration,converter);
			Set<List<Label>> actual = new LinkedHashSet<List<Label>>();actual.addAll(questionList);
			Assert.assertEquals(expected,actual);
		}
		
		/** Tests that the question part is matched to the IF part only if it has been answered by a user.
		 * @throws IncompatibleStatesException */
		@Test
		public final void testQuestionAnswering6() throws IncompatibleStatesException
		{
			LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph(ifthen_sc, "ifthen_sc", mainConfiguration,converter)}; 
			Transform.augmentFromIfThenAutomaton(graph, (NonExistingPaths)questions.getFSM(), ifthenCollection, 0);
			compareGraphs(new LearnerGraph(origGraph,mainConfiguration),graph);// check that augment did not modify the automaton
			graph.learnerCache.questionsPTA=questions;
			Assert.assertNotNull(graph.transform.AugmentNonExistingMatrixWith(labelList(new String[]{"s","c","d"}), false));
			Assert.assertEquals(2,questions.getData().size());
			Transform.augmentFromIfThenAutomaton(graph, (NonExistingPaths)questions.getFSM(), ifthenCollection, 0);
			compareGraphs(new LearnerGraph(origGraph,mainConfiguration),graph);// check that augment did not modify the automaton
	
			List<List<Label>> questionList = questions.getData();
			Assert.assertEquals(1,questionList.size());
			Set<List<Label>> expected = TestFSMAlgo.buildSet(new String[][]{new String[]{"s","c","c","f"}},mainConfiguration,converter);
			Set<List<Label>> actual = new LinkedHashSet<List<Label>>();actual.addAll(questionList);
			Assert.assertEquals(expected,actual);
		}
		
		/** Tests that the question part is matched to the IF part only if it has been answered by a user.
		 * @throws IncompatibleStatesException */
		@Test
		public final void testQuestionAnswering7() throws IncompatibleStatesException
		{
			LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph(ifthen_sc, "ifthen_sc", mainConfiguration,converter)}; 
			Transform.augmentFromIfThenAutomaton(graph, (NonExistingPaths)questions.getFSM(), ifthenCollection, 0);
			compareGraphs(new LearnerGraph(origGraph,mainConfiguration),graph);// check that augment did not modify the automaton
			graph.learnerCache.questionsPTA=questions;
			Assert.assertNotNull(graph.transform.AugmentNonExistingMatrixWith(labelList(new String[]{"s","c"}), true));
			Assert.assertEquals(3,questions.getData().size());
			Transform.augmentFromIfThenAutomaton(graph, (NonExistingPaths)questions.getFSM(), ifthenCollection, 0);
			compareGraphs(new LearnerGraph(origGraph,mainConfiguration),graph);// check that augment did not modify the automaton
	
			List<List<Label>> questionList = questions.getData();
			Assert.assertEquals(1,questionList.size());
			Set<List<Label>> expected = TestFSMAlgo.buildSet(new String[][]{new String[]{"s","c","c","f"}},mainConfiguration,converter);
			Set<List<Label>> actual = new LinkedHashSet<List<Label>>();actual.addAll(questionList);
			Assert.assertEquals(expected,actual);
		}
		
		/** Tests that recursive properties are correctly handled. */
		@Test
		public final void testQuestionAnswering8() throws IncompatibleStatesException
		{
			LearnerGraph[] ifthenCollection = new LearnerGraph[]{buildLearnerGraph(ifthen_sc, "ifthen_sc", mainConfiguration,converter)}; 
			Transform.augmentFromIfThenAutomaton(graph, (NonExistingPaths)questions.getFSM(), ifthenCollection, 0);
			compareGraphs(new LearnerGraph(origGraph,mainConfiguration),graph);// check that augment did not modify the automaton
			graph.learnerCache.questionsPTA=questions;
			Assert.assertNotNull(graph.transform.AugmentNonExistingMatrixWith(labelList(new String[]{"s","c"}), true));
			Assert.assertEquals(3,questions.getData().size());
			Transform.augmentFromIfThenAutomaton(graph, (NonExistingPaths)questions.getFSM(), ifthenCollection, 0);
			compareGraphs(new LearnerGraph(origGraph,mainConfiguration),graph);// check that augment did not modify the automaton
	
			List<List<Label>> questionList = questions.getData();
			Assert.assertEquals(1,questionList.size());
			Set<List<Label>> expected = TestFSMAlgo.buildSet(new String[][]{new String[]{"s","c","c","f"}},mainConfiguration,converter);
			Set<List<Label>> actual = new LinkedHashSet<List<Label>>();actual.addAll(questionList);
			Assert.assertEquals(expected,actual);
		}
		
		/** Three if-then automata, two of which recursively expand each other - very similar to the above but operates on a tree. */
		@Test
		public final void testQuestionAnswering9() throws IncompatibleStatesException
		{
			origGraph = FsmParser.buildLearnerGraph("I-c->I-s->A-c->A-a->B-b->C-a->D-b->E-a->F-b->G / "+
					"C-c->C11-c->C12-c->C13-c->C14-c->C15-c->C16-c->C17 / C13-a->13A-b->13B / C15-a->15A-b->15B /"+
					"E-c->C21-c->C22-c->C23-c->C24-c->C25-c->C26-c->C27 / C23-a->23A-b->23B / C25-a->25A-b->25B /"+
					"G-c->C31-c->C32-c->C33-c->C34-c->C35-c->C36-c->C37 / C33-a->33A-b->33B / C35-a->35A-b->35B", "testQuestionAnswering9",mainConfiguration,converter);
			graph = new LearnerGraph(origGraph,mainConfiguration);
			LearnerGraph[] ifthenCollection = new LearnerGraph[]{
					buildLearnerGraph(ifthen_ab_to_c, "ifthen_ab_to_c", mainConfiguration,converter),
					buildLearnerGraph(ifthen_c_to_cc, "ifthen_c_to_cc", mainConfiguration,converter),			
					buildLearnerGraph(ifthen_ccc_to_ab, "ifthen_ccc_to_ab", mainConfiguration,converter)
			};
			pair = new StatePair(graph.findVertex("I"),graph.findVertex("A"));
			merged = MergeStates.mergeAndDeterminize_general(graph, pair);
			LearnerGraph expectedMergedGraph = buildLearnerGraph("A-s->A-c->A-a->B-b->C-a->D-b->E-a->F-b->G / "+
					"C-c->C11-c->C12-c->C13-c->C14-c->C15-c->C16-c->C17 / C13-a->13A-b->13B / C15-a->15A-b->15B /"+
					"E-c->C21-c->C22-c->C23-c->C24-c->C25-c->C26-c->C27 / C23-a->23A-b->23B / C25-a->25A-b->25B /"+
					"G-c->C31-c->C32-c->C33-c->C34-c->C35-c->C36-c->C37 / C33-a->33A-b->33B / C35-a->35A-b->35B", "testQuestionAnswering9b",mainConfiguration,converter);
			//Visualiser.updateFrame(graph, merged);Visualiser.waitForKey();
			compareGraphs(expectedMergedGraph,merged);
			questions = ComputeQuestions.computeQS_general(pair, graph, merged, new ComputeQuestions.QuestionGeneratorQSMLikeWithLoops());
			graph.learnerCache.questionsPTA=questions;
			Assert.assertEquals(19,questions.getData().size());
			Transform.augmentFromIfThenAutomaton(graph, (NonExistingPaths)questions.getFSM(), ifthenCollection, 0);
			compareGraphs(new LearnerGraph(origGraph,mainConfiguration),graph);// check that augment did not modify the automaton
			List<List<Label>> questionList = questions.getData();
			Assert.assertEquals(13,questionList.size());
		}

		final LearnerGraph 
			ifthen1=buildLearnerGraph("I-a->A-b->B / P-c->C / P==THEN==B","ifthenA",mainConfiguration,converter),
			ifthen2=buildLearnerGraph("I-a->I-b->I-c->A / A-a->I / A-c->A / A-b->B / B-a->I / B-b->I / B-c->A / P-a->C / P==THEN==B","ifthenA",mainConfiguration,converter),
			ifthen3=buildLearnerGraph("I-a->I-b->I-c->A / A-a->I / A-c->A / A-b->B / B-a->I / B-b->I / B-c->A / P-a-#C / P==THEN==B","ifthenA",mainConfiguration,converter);
		
		/** Tests <em>mapPathToConfirmedElements</em> with an empty sequence. */
		@Test
		public final void testMapPathToConfirmedElements1Ea()
		{
			LearnerGraph hardFacts = new LearnerGraph(Configuration.getDefaultConfiguration());hardFacts.initPTA();
			Assert.assertTrue(PathRoutines.mapPathToConfirmedElements(hardFacts,new LinkedList<Label>(),new LearnerGraph[]{})
					.isEmpty());
		}
		
		/** Tests <em>mapPathToConfirmedElements</em> with a sequence containing a non-existing element. */
		@Test
		public final void testMapPathToConfirmedElements1Eb()
		{// questions are [[s, c, d], [s, c, c, f], [s, c, c, e]] where only s exists in the original graph
			LearnerGraph hardFacts = new LearnerGraph(mainConfiguration);hardFacts.initPTA();
			List<Boolean> result = PathRoutines.mapPathToConfirmedElements(hardFacts,
					AbstractLearnerGraph.buildList(Arrays.asList(new String[]{
					"u"}),mainConfiguration,converter),new LearnerGraph[]{});
			Assert.assertEquals(Arrays.asList(new Boolean[]{null}),result);
		}
		

		/** Tests <em>mapPathToConfirmedElements</em> when there are no ifthen automata (empty set of automata). */
		@Test
		public final void testMapPathToConfirmedElements1Ec()
		{// questions are [[s, c, d], [s, c, c, f], [s, c, c, e]] where only s exists in the original graph
			LearnerGraph hardFacts = new LearnerGraph(mainConfiguration);hardFacts.initPTA();
			hardFacts.paths.augmentPTA(labelList(new String[]{"s","t"}), true, false, JUConstants.BLUE);
			List<Boolean> result = PathRoutines.mapPathToConfirmedElements(hardFacts,
					AbstractLearnerGraph.buildList(Arrays.asList(new String[]{
					"s","v","j"}),mainConfiguration,converter),new LearnerGraph[]{});
			Assert.assertEquals(Arrays.asList(new Boolean[]{true,null,null}),result);
		}

		/** Tests <em>mapPathToConfirmedElements</em> with an empty sequence. */
		@Test
		public final void testMapPathToConfirmedElements1Na()
		{
			LearnerGraph hardFacts = new LearnerGraph(mainConfiguration);hardFacts.initPTA();
			Assert.assertTrue(PathRoutines.mapPathToConfirmedElements(hardFacts,new LinkedList<Label>(),null)
					.isEmpty());
		}
		
		/** Tests <em>mapPathToConfirmedElements</em> with a sequence containing a non-existing element. */
		@Test
		public final void testMapPathToConfirmedElements1Nb()
		{// questions are [[s, c, d], [s, c, c, f], [s, c, c, e]] where only s exists in the original graph
			LearnerGraph hardFacts = new LearnerGraph(mainConfiguration);hardFacts.initPTA();
			List<Boolean> result = PathRoutines.mapPathToConfirmedElements(hardFacts,
					labelList(new String[]{"u"}),null);
			Assert.assertEquals(Arrays.asList(new Boolean[]{null}),result);
		}
		

		/** Tests <em>mapPathToConfirmedElements</em> when there are no ifthen automata (null argument). */
		@Test
		public final void testMapPathToConfirmedElements1Nc()
		{// questions are [[s, c, d], [s, c, c, f], [s, c, c, e]] where only s exists in the original graph
			LearnerGraph hardFacts = new LearnerGraph(mainConfiguration);hardFacts.initPTA();
			hardFacts.paths.augmentPTA(labelList(new String[]{"s","t"}), 
					true, false, JUConstants.BLUE);
			List<Boolean> result = PathRoutines.mapPathToConfirmedElements(hardFacts,
					labelList(new String[]{"s","v","j"}),null);
			Assert.assertEquals(Arrays.asList(new Boolean[]{true,null,null}),result);
		}

		/** Tests <em>mapPathToConfirmedElements</em>. */
		@Test
		public final void testMapPathToConfirmedElements2a()
		{// questions are [[s, c, d], [s, c, c, f], [s, c, c, e]] where only s exists in the original graph
			LearnerGraph hardFacts = new LearnerGraph(mainConfiguration);hardFacts.initPTA();
			hardFacts.paths.augmentPTA(labelList(new String[]{"s","t"}), true, false, JUConstants.BLUE);
			List<Boolean> result = PathRoutines.mapPathToConfirmedElements(hardFacts,labelList(new String[]{
					"s"}), new LearnerGraph[]{});
			Assert.assertEquals(Arrays.asList(new Boolean[]{true}),result);
		}
		
		/** Tests <em>mapPathToConfirmedElements</em>. */
		@Test
		public final void testMapPathToConfirmedElements2b()
		{// questions are [[s, c, d], [s, c, c, f], [s, c, c, e]] where only s exists in the original graph
			LearnerGraph hardFacts = new LearnerGraph(mainConfiguration);hardFacts.initPTA();
			hardFacts.paths.augmentPTA(labelList(new String[]{"s","t"}),
					true, false, JUConstants.BLUE);
			List<Boolean> result = PathRoutines.mapPathToConfirmedElements(hardFacts,labelList(new String[]{
					"s"}), new LearnerGraph[]{ifthen1,ifthen2});
			Assert.assertEquals(Arrays.asList(new Boolean[]{true}),result);
		}

		/** Tests <em>mapPathToConfirmedElements</em>. */
		@Test
		public final void testMapPathToConfirmedElements3a()
		{// questions are [[s, c, d], [s, c, c, f], [s, c, c, e]] where only s exists in the original graph
			LearnerGraph hardFacts = new LearnerGraph(mainConfiguration);hardFacts.initPTA();
			hardFacts.paths.augmentPTA(labelList(new String[]{"s","t"}), 
					true, false, JUConstants.BLUE);
			List<Boolean> result = PathRoutines.mapPathToConfirmedElements(hardFacts,labelList(new String[]{
					"s","t"}), new LearnerGraph[]{ifthen1,ifthen2});
			Assert.assertEquals(Arrays.asList(new Boolean[]{true,true}),result);
		}

		/** Tests <em>mapPathToConfirmedElements</em>. */
		@Test
		public final void testMapPathToConfirmedElements3b()
		{// questions are [[s, c, d], [s, c, c, f], [s, c, c, e]] where only s exists in the original graph
			LearnerGraph hardFacts = new LearnerGraph(mainConfiguration);hardFacts.initPTA();
			hardFacts.paths.augmentPTA(labelList(new String[]{"s","t"}),
					false, false, JUConstants.BLUE);
			List<Boolean> result = PathRoutines.mapPathToConfirmedElements(hardFacts,labelList(new String[]{
					"s","t"}), new LearnerGraph[]{ifthen1,ifthen2});
			Assert.assertEquals(Arrays.asList(new Boolean[]{true,false}),result);
		}

		/** Tests <em>mapPathToConfirmedElements</em>. */
		@Test
		public final void testMapPathToConfirmedElements4a()
		{// questions are [[s, c, d], [s, c, c, f], [s, c, c, e]] where only s exists in the original graph
			LearnerGraph hardFacts = new LearnerGraph(mainConfiguration);hardFacts.initPTA();
			hardFacts.paths.augmentPTA(labelList(new String[]{"s","t"}),
					true, false, JUConstants.BLUE);
			List<Boolean> result = PathRoutines.mapPathToConfirmedElements(hardFacts,labelList(new String[]{
					"s","t","q"}), new LearnerGraph[]{ifthen1,ifthen2});
			Assert.assertEquals(Arrays.asList(new Boolean[]{true,true,null}),result);
		}

		/** Tests <em>mapPathToConfirmedElements</em>. */
		@Test
		public final void testMapPathToConfirmedElements4b()
		{// questions are [[s, c, d], [s, c, c, f], [s, c, c, e]] where only s exists in the original graph
			LearnerGraph hardFacts = new LearnerGraph(mainConfiguration);hardFacts.initPTA();
			hardFacts.paths.augmentPTA(labelList(new String[]{"s","t"}),
					false, false, JUConstants.BLUE);
			List<Boolean> result = PathRoutines.mapPathToConfirmedElements(hardFacts,labelList(new String[]{
					"s","t","q"}), new LearnerGraph[]{ifthen1,ifthen2});
			Assert.assertEquals(Arrays.asList(new Boolean[]{true,false,null}),result);
		}

		/** Tests <em>mapPathToConfirmedElements</em>. 
		 * Path matches if-then but "then" element does not confirm anything. 
		 */
		@Test
		public final void testMapPathToConfirmedElements5a()
		{// questions are [[s, c, d], [s, c, c, f], [s, c, c, e]] where only s exists in the original graph
			LearnerGraph hardFacts = new LearnerGraph(mainConfiguration);hardFacts.initPTA();
			hardFacts.paths.augmentPTA(AbstractLearnerGraph.buildList(Arrays.asList(new String[]{"s","t"}),mainConfiguration,converter),
					true, false, JUConstants.BLUE);
			List<Boolean> result = PathRoutines.mapPathToConfirmedElements(hardFacts,labelList(new String[]{
					"a","b","q"}), new LearnerGraph[]{ifthen1,ifthen2});
			Assert.assertEquals(Arrays.asList(new Boolean[]{null,null,null}),result);
		}

		/** Tests <em>mapPathToConfirmedElements</em>. 
		 * Path matches if-then and "then" element confirms a path
		 */
		@Test
		public final void testMapPathToConfirmedElements5b()
		{// questions are [[s, c, d], [s, c, c, f], [s, c, c, e]] where only s exists in the original graph
			LearnerGraph hardFacts = new LearnerGraph(mainConfiguration);hardFacts.initPTA();
			hardFacts.paths.augmentPTA(labelList(new String[]{"s","t"}),
					true, false, JUConstants.BLUE);
			List<Boolean> result = PathRoutines.mapPathToConfirmedElements(hardFacts,labelList(new String[]{
					"a","b","c"}), new LearnerGraph[]{ifthen1,ifthen2});
			Assert.assertEquals(Arrays.asList(new Boolean[]{null,null,true}),result);
		}

		/** Tests <em>mapPathToConfirmedElements</em>. 
		 * Path matches if-then and "then" element confirms a path
		 */
		@Test
		public final void testMapPathToConfirmedElements5c()
		{// questions are [[s, c, d], [s, c, c, f], [s, c, c, e]] where only s exists in the original graph
			LearnerGraph hardFacts = new LearnerGraph(mainConfiguration);hardFacts.initPTA();
			hardFacts.paths.augmentPTA(labelList(new String[]{"a","b"}),
					true, false, JUConstants.BLUE);
			List<Boolean> result = PathRoutines.mapPathToConfirmedElements(hardFacts,labelList(new String[]{
					"a","b","s"}), new LearnerGraph[]{ifthen1,ifthen2});
			Assert.assertEquals(Arrays.asList(new Boolean[]{true,true,null}),result);
		}
		
		/** Tests <em>mapPathToConfirmedElements</em>. 
		 * Path matches if-then and "then" element confirms a path
		 */
		@Test
		public final void testMapPathToConfirmedElements5d()
		{// questions are [[s, c, d], [s, c, c, f], [s, c, c, e]] where only s exists in the original graph
			LearnerGraph hardFacts = new LearnerGraph(mainConfiguration);hardFacts.initPTA();
			hardFacts.paths.augmentPTA(labelList(new String[]{"a","t"}),
					true, false, JUConstants.BLUE);
			List<Boolean> result = PathRoutines.mapPathToConfirmedElements(hardFacts,labelList(new String[]{
					"a","b","c"}), new LearnerGraph[]{ifthen1,ifthen2});
			Assert.assertEquals(Arrays.asList(new Boolean[]{true,null,true}),result);
		}

		/** Tests <em>mapPathToConfirmedElements</em>. 
		 * Path matches if-then and "then" element confirms a path
		 */
		@Test
		public final void testMapPathToConfirmedElements5e()
		{// questions are [[s, c, d], [s, c, c, f], [s, c, c, e]] where only s exists in the original graph
			LearnerGraph hardFacts = new LearnerGraph(mainConfiguration);hardFacts.initPTA();
			hardFacts.paths.augmentPTA(labelList(new String[]{"a","b"}),
					true, false, JUConstants.BLUE);
			List<Boolean> result = PathRoutines.mapPathToConfirmedElements(hardFacts,labelList(new String[]{
					"a","b","c"}), new LearnerGraph[]{ifthen1,ifthen2});
			Assert.assertEquals(Arrays.asList(new Boolean[]{true,true,true}),result);
		}

		/** Tests <em>mapPathToConfirmedElements</em>. 
		 * Path matches if-then and "then" element confirms a path
		 */
		@Test
		public final void testMapPathToConfirmedElements5f()
		{// questions are [[s, c, d], [s, c, c, f], [s, c, c, e]] where only s exists in the original graph
			LearnerGraph hardFacts = new LearnerGraph(mainConfiguration);hardFacts.initPTA();
			hardFacts.paths.augmentPTA(labelList(new String[]{"a","b"}),
					true, false, JUConstants.BLUE);
			List<Boolean> result = PathRoutines.mapPathToConfirmedElements(hardFacts,labelList(new String[]{
					"a","b","f"}), new LearnerGraph[]{ifthen1,ifthen2});
			Assert.assertEquals(Arrays.asList(new Boolean[]{true,true,null}),result);
		}

		/** Tests <em>mapPathToConfirmedElements</em>. 
		 */
		@Test
		public final void testMapPathToConfirmedElements6a()
		{// questions are [[s, c, d], [s, c, c, f], [s, c, c, e]] where only s exists in the original graph
			LearnerGraph hardFacts = new LearnerGraph(mainConfiguration);hardFacts.initPTA();
			hardFacts.paths.augmentPTA(labelList(new String[]{"a","s"}),
					true, false, JUConstants.BLUE);
			List<Boolean> result = PathRoutines.mapPathToConfirmedElements(hardFacts,labelList(new String[]{
					"a","b","c","c"}), new LearnerGraph[]{ifthen1,ifthen2});
			Assert.assertEquals(Arrays.asList(new Boolean[]{true,null,true,null}),result);
		}

		/** Tests <em>mapPathToConfirmedElements</em>. */
		@Test
		public final void testMapPathToConfirmedElements6b()
		{// questions are [[s, c, d], [s, c, c, f], [s, c, c, e]] where only s exists in the original graph
			LearnerGraph hardFacts = new LearnerGraph(mainConfiguration);hardFacts.initPTA();
			hardFacts.paths.augmentPTA(labelList(new String[]{"a","s"}),
					true, false, JUConstants.BLUE);
			List<Boolean> result = PathRoutines.mapPathToConfirmedElements(hardFacts,labelList(new String[]{
					"a","b","c","b"}), new LearnerGraph[]{ifthen1,ifthen2});
			Assert.assertEquals(Arrays.asList(new Boolean[]{true,null,true,null}),result);
		}

		/** Tests <em>mapPathToConfirmedElements</em>. */
		@Test
		public final void testMapPathToConfirmedElements6c()
		{// questions are [[s, c, d], [s, c, c, f], [s, c, c, e]] where only s exists in the original graph
			LearnerGraph hardFacts = new LearnerGraph(mainConfiguration);hardFacts.initPTA();
			hardFacts.paths.augmentPTA(labelList(new String[]{"a","s"}), 
					true, false, JUConstants.BLUE);
			List<Boolean> result = PathRoutines.mapPathToConfirmedElements(hardFacts,labelList(new String[]{
					"a","b","c","b","a"}), new LearnerGraph[]{ifthen1,ifthen2});
			Assert.assertEquals(Arrays.asList(new Boolean[]{true,null,true,null,true}),result);
		}

		/** Tests <em>mapPathToConfirmedElements</em>. */
		@Test
		public final void testMapPathToConfirmedElements6d()
		{// questions are [[s, c, d], [s, c, c, f], [s, c, c, e]] where only s exists in the original graph
			LearnerGraph hardFacts = new LearnerGraph(mainConfiguration);hardFacts.initPTA();
			hardFacts.paths.augmentPTA(labelList(new String[]{"a","s"}),
					true, false, JUConstants.BLUE);
			List<Boolean> result = PathRoutines.mapPathToConfirmedElements(hardFacts,labelList(new String[]{
					"a","b","c","b","s"}), new LearnerGraph[]{ifthen1,ifthen2});
			Assert.assertEquals(Arrays.asList(new Boolean[]{true,null,true,null,null}),result);
		}

		/** Tests <em>mapPathToConfirmedElements</em>. */
		@Test
		public final void testMapPathToConfirmedElements6e()
		{// questions are [[s, c, d], [s, c, c, f], [s, c, c, e]] where only s exists in the original graph
			LearnerGraph hardFacts = new LearnerGraph(mainConfiguration);hardFacts.initPTA();
			hardFacts.paths.augmentPTA(labelList(new String[]{"a","s"}),
					true, false, JUConstants.BLUE);
			List<Boolean> result = PathRoutines.mapPathToConfirmedElements(hardFacts,labelList(new String[]{
					"a","b","c","c","b","s"}), new LearnerGraph[]{ifthen1,ifthen2});
			Assert.assertEquals(Arrays.asList(new Boolean[]{true,null,true,null,null,null}),result);
		}

		/** Tests <em>mapPathToConfirmedElements</em>. */
		@Test
		public final void testMapPathToConfirmedElements6f()
		{// questions are [[s, c, d], [s, c, c, f], [s, c, c, e]] where only s exists in the original graph
			LearnerGraph hardFacts = new LearnerGraph(mainConfiguration);hardFacts.initPTA();
			hardFacts.paths.augmentPTA(labelList(new String[]{"a","s"}), 
					true, false, JUConstants.BLUE);
			List<Boolean> result = PathRoutines.mapPathToConfirmedElements(hardFacts,labelList(new String[]{
					"a","b","c","c","b","a"}), new LearnerGraph[]{ifthen1,ifthen2});
			Assert.assertEquals(Arrays.asList(new Boolean[]{true,null,true,null,null,true}),result);
		}

		/** Tests <em>mapPathToConfirmedElements</em>. */
		@Test
		public final void testMapPathToConfirmedElements7a()
		{// questions are [[s, c, d], [s, c, c, f], [s, c, c, e]] where only s exists in the original graph
			LearnerGraph hardFacts = new LearnerGraph(mainConfiguration);hardFacts.initPTA();
			hardFacts.paths.augmentPTA(labelList(new String[]{"a","b"}),
					true, false, JUConstants.BLUE);
			List<Boolean> result = PathRoutines.mapPathToConfirmedElements(hardFacts,labelList(new String[]{
					"a","b","c","c","b","a"}), new LearnerGraph[]{ifthen1,ifthen3});
			Assert.assertEquals(Arrays.asList(new Boolean[]{true,true,true,null,null,false}),result);
		}

		/** Tests <em>mapPathToConfirmedElements</em>. */
		@Test
		public final void testMapPathToConfirmedElements7b()
		{// questions are [[s, c, d], [s, c, c, f], [s, c, c, e]] where only s exists in the original graph
			LearnerGraph hardFacts = new LearnerGraph(mainConfiguration);hardFacts.initPTA();
			hardFacts.paths.augmentPTA(labelList(new String[]{"a","b"}),
					true, false, JUConstants.BLUE);
			List<Boolean> result = PathRoutines.mapPathToConfirmedElements(hardFacts,labelList(new String[]{
					"a","b","c","c","b","a","u"}), new LearnerGraph[]{ifthen1,ifthen2});
			Assert.assertEquals(Arrays.asList(new Boolean[]{true,true,true,null,null,true,null}),result);
		}

		/** Tests <em>mapPathToConfirmedElements</em>. */
		@Test
		public final void testMapPathToConfirmedElements7c()
		{// questions are [[s, c, d], [s, c, c, f], [s, c, c, e]] where only s exists in the original graph
			final LearnerGraph hardFacts = new LearnerGraph(mainConfiguration);hardFacts.initPTA();
			hardFacts.paths.augmentPTA(labelList(new String[]{"a","b"}),
					true, false, JUConstants.BLUE);
			Helper.checkForCorrectException(new whatToRun() { public @Override void run() {
				PathRoutines.mapPathToConfirmedElements(hardFacts,labelList(new String[]{
					"a","b","c","c","b","a","u"}), new LearnerGraph[]{ifthen1,ifthen3});
			}},IllegalArgumentException.class,"is invalid: either of true/false");
		}

		/** Tests <em>identifyTheOnlyChoice</em>. */
		@Test
		public final void testIdentifyTheOnlyChoice1()
		{
			Helper.checkForCorrectException(new whatToRun() { public @Override void run() {
				PathRoutines.identifyTheOnlyChoice(Arrays.asList(new Boolean[]{}));
			}},IllegalArgumentException.class,"an empty path");
		}
		

		/** Tests <em>identifyTheOnlyChoice</em>. */
		@Test
		public final void testIdentifyTheOnlyChoice2()
		{
			Assert.assertEquals(Integer.valueOf(2),PathRoutines.identifyTheOnlyChoice(Arrays.asList(new Boolean[]{true,true,false})));
		}
		
		/** Tests <em>identifyTheOnlyChoice</em>. */
		@Test
		public final void testIdentifyTheOnlyChoice3()
		{
			Assert.assertEquals(Integer.valueOf(AbstractOracle.USER_ACCEPTED),PathRoutines.identifyTheOnlyChoice(Arrays.asList(new Boolean[]{true,true,true})));
		}
		
		/** Tests <em>identifyTheOnlyChoice</em>. */
		@Test
		public final void testIdentifyTheOnlyChoice4()
		{
			Assert.assertNull(PathRoutines.identifyTheOnlyChoice(Arrays.asList(new Boolean[]{true,true,null})));
		}
		
		/** Tests <em>identifyTheOnlyChoice</em>. */
		@Test
		public final void testIdentifyTheOnlyChoice5()
		{
			Assert.assertNull(PathRoutines.identifyTheOnlyChoice(Arrays.asList(new Boolean[]{true,false,true})));
		}
		
		/** Tests <em>identifyTheOnlyChoice</em>. */
		@Test
		public final void testIdentifyTheOnlyChoice6()
		{
			Assert.assertNull(PathRoutines.identifyTheOnlyChoice(Arrays.asList(new Boolean[]{true,null,true})));
		}
		
		/** Tests <em>identifyTheOnlyChoice</em>. */
		@Test
		public final void testIdentifyTheOnlyChoice7()
		{
			Assert.assertNull(PathRoutines.identifyTheOnlyChoice(Arrays.asList(new Boolean[]{false,true,true})));
		}
		
		/** Tests <em>identifyTheOnlyChoice</em>. */
		@Test
		public final void testIdentifyTheOnlyChoice8()
		{
			Assert.assertNull(PathRoutines.identifyTheOnlyChoice(Arrays.asList(new Boolean[]{null,true,true})));
		}
		
		/** Tests <em>identifyTheOnlyChoice</em>. */
		@Test
		public final void testIdentifyTheOnlyChoice9()
		{
			Assert.assertNull(PathRoutines.identifyTheOnlyChoice(Arrays.asList(new Boolean[]{null,null,true})));
		}
		
		/** Tests <em>identifyTheOnlyChoice</em>. */
		@Test
		public final void testIdentifyTheOnlyChoice10()
		{
			Assert.assertNull(PathRoutines.identifyTheOnlyChoice(Arrays.asList(new Boolean[]{null,false,true})));
		}
		
		/** Tests marking of questions as answered. */
		@Test
		public final void testQuestionMarking1()
		{
			graph.learnerCache.questionsPTA = questions;
			Assert.assertNotNull(graph.transform.AugmentNonExistingMatrixWith(labelList(new String[]{"s","c"}), true));
			Assert.assertEquals(3,questions.getData().size());
			Assert.assertNotNull(graph.transform.AugmentNonExistingMatrixWith(labelList(new String[]{"s","c","d"}), false));
			Assert.assertEquals(2,questions.getData().size());
		}
		
		/** Tests marking of questions as answered. */
		@Test
		public final void testQuestionMarking_unmatched()
		{
			graph.learnerCache.questionsPTA = questions;
			Assert.assertFalse(graph.transform.AugmentNonExistingMatrixWith(labelList(new String[]{"s","c"}), false).booleanValue());
			Assert.assertFalse(graph.transform.AugmentNonExistingMatrixWith(labelList(new String[]{"s","c","c"}), false).booleanValue());
			Assert.assertFalse(graph.transform.AugmentNonExistingMatrixWith(labelList(new String[]{"s","c","d"}), true).booleanValue());
		}
		
		/** The integration of test of questions, similar to <em>testQuestionAnswering4</em> but 
		 * using different methods. */
		@Test
		public final void testQuestions_and_marking1a()
		{
			//Visualiser.updateFrame(graph, null);
			//Visualiser.updateFrame(ComputeQuestions.constructGraphWithQuestions(pair,graph,merged), buildLearnerGraph(ifthen_s, "ifthen_s",config));
			//Visualiser.updateFrame(ComputeQuestions.constructGraphWithQuestions(pair,graph,merged),buildLearnerGraph(ifthen_sc, "ifthen_sc",config));
			List<List<Label>> qs = ComputeQuestions.computeQS(pair, graph, merged, new LearnerGraph[] {
					buildLearnerGraph(ifthen_s, "ifthen_s",mainConfiguration,converter),
					buildLearnerGraph(ifthen_sc, "ifthen_sc",mainConfiguration,converter)
			});
			compareGraphs(new LearnerGraph(origGraph,mainConfiguration),graph);// check that augment did not modify the automaton
			Assert.assertEquals(1,qs.size());
			Set<List<Label>> expected = TestFSMAlgo.buildSet(new String[][]{new String[]{"s","c","c","f"}},mainConfiguration,converter);
			Set<List<Label>> actual = new LinkedHashSet<List<Label>>();actual.addAll(qs);
			Assert.assertEquals(expected,actual);
		}
		
		/** Almost the same as the above, but the order of elements in the array of properties is different. */
		@Test
		public final void testQuestions_and_marking1b()
		{
			List<List<Label>> qs = ComputeQuestions.computeQS(pair, graph, merged, new LearnerGraph[] {
					buildLearnerGraph(ifthen_sc, "ifthen_sc",mainConfiguration,converter),
					buildLearnerGraph(ifthen_s, "ifthen_s",mainConfiguration,converter)
			});
			compareGraphs(new LearnerGraph(origGraph,mainConfiguration),graph);// check that augment did not modify the automaton
			Assert.assertEquals(1,qs.size());
			Set<List<Label>> expected = TestFSMAlgo.buildSet(new String[][]{new String[]{"s","c","c","f"}},mainConfiguration,converter);
			Set<List<Label>> actual = new LinkedHashSet<List<Label>>();actual.addAll(qs);
			Assert.assertEquals(expected,actual);
		}
		
		/** The integration of test of questions, similar to <em>testQuestionAnswering6</em> but 
		 * using different methods. */
		@Test
		public final void testQuestions_and_marking2()
		{
			LearnerGraph[] properties = new LearnerGraph[]{
					buildLearnerGraph(ifthen_sc, "ifthen_sc",mainConfiguration,converter)};
			List<List<Label>> qs = ComputeQuestions.computeQS(pair, graph, merged, properties);
			compareGraphs(new LearnerGraph(origGraph,mainConfiguration),graph);// check that augment did not modify the automaton
			Assert.assertEquals(3,qs.size());
			
			Assert.assertNotNull(graph.transform.AugmentNonExistingMatrixWith(labelList(new String[]{"s","c"}), true));
			qs = ComputeQuestions.RecomputeQS(pair, graph, merged, properties);
			compareGraphs(new LearnerGraph(origGraph,mainConfiguration),graph);// check that augment did not modify the automaton
			Assert.assertEquals(1,qs.size());

			Set<List<Label>> expected = TestFSMAlgo.buildSet(new String[][]{new String[]{"s","c","c","f"}},mainConfiguration,converter);
			Set<List<Label>> actual = new LinkedHashSet<List<Label>>();actual.addAll(qs);
			Assert.assertEquals(expected,actual);
		}

		/** Interaction between <em>computeQS</em> and <em>recomputeQS</em>. */
		@Test
		public final void testQuestions_and_marking3()
		{
			LearnerGraph [] properties = new LearnerGraph[] {
					buildLearnerGraph(ifthen_sc, "ifthen_sc",mainConfiguration,converter)};
			List<List<Label>> qs = ComputeQuestions.computeQS(pair, graph, merged, properties);
			compareGraphs(new LearnerGraph(origGraph,mainConfiguration),graph);// check that augment did not modify the automaton
			Assert.assertEquals(3,qs.size());

			Assert.assertNotNull(graph.transform.AugmentNonExistingMatrixWith(labelList(new String[]{"s","c"}), true));
			// the above call should not affect computeQS
			qs = ComputeQuestions.computeQS(pair, graph, merged, properties);
			compareGraphs(new LearnerGraph(origGraph,mainConfiguration),graph);// check that augment did not modify the automaton
			Assert.assertEquals(3,qs.size());
		}
	}
}

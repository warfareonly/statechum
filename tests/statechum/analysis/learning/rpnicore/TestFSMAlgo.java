/* Copyright (c) 2006, 2007, 2008 Neil Walkinshaw and Kirill Bogdanov
 * 
 * This file is part of StateChum
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

import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.ParameterizedWithName;
import org.junit.runners.ParameterizedWithName.ParametersToString;

import statechum.Configuration;
import statechum.DeterministicDirectedSparseGraph;
import statechum.JUConstants;
import statechum.DeterministicDirectedSparseGraph.CmpVertex;
import statechum.DeterministicDirectedSparseGraph.VertexID;
import statechum.Label;
import statechum.analysis.learning.StatePair;
import statechum.analysis.learning.Visualiser;
import statechum.analysis.learning.rpnicore.AMEquivalenceClass.IncompatibleStatesException;
import statechum.analysis.learning.rpnicore.Transform.ConvertALabel;
import statechum.analysis.learning.smt.SmtLabelRepresentation;
import statechum.analysis.learning.smt.SmtLabelRepresentation.AbstractState;
import statechum.analysis.learning.rpnicore.WMethod.VERTEX_COMPARISON_KIND;
import statechum.apps.QSMTool;
import statechum.collections.ArrayOperations;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.DirectedSparseGraph;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static statechum.Helper.whatToRun;
import static statechum.analysis.learning.rpnicore.FsmParser.buildLearnerGraph;
import static statechum.analysis.learning.rpnicore.FsmParser.buildLearnerGraphND;
import static statechum.analysis.learning.smt.SmtLabelRepresentation.INITMEM;

@RunWith(ParameterizedWithName.class)
public class TestFSMAlgo extends TestWithMultipleConfigurations
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

	public TestFSMAlgo(Configuration argConfig)
	{
		super(argConfig);
		mainConfiguration.setAllowedToCloneNonCmpVertex(true);
	}
	
	org.w3c.dom.Document doc = null;
	
	SmtLabelRepresentation lbls = null;
	
	/** Make sure that whatever changes a test have made to the 
	 * configuration, next test is not affected.
	 */
	@Before
	public final void beforeTest()
	{
		config = mainConfiguration.copy();

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try
		{
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);factory.setXIncludeAware(false);
			factory.setExpandEntityReferences(false);factory.setValidating(false);// we do not have a schema to validate against-this does not seem necessary for the simple data format we are considering here.
			doc = factory.newDocumentBuilder().newDocument();
		}
		catch(ParserConfigurationException e)
		{
			statechum.Helper.throwUnchecked("failed to construct DOM document",e);
		}
	
		lbls = new SmtLabelRepresentation(config,converter);
		lbls.parseCollection(Arrays.asList(new String[]{
				QSMTool.cmdOperation+" "+INITMEM+" "+SmtLabelRepresentation.OP_DATA.PRE+ " varDeclP_N",
				QSMTool.cmdOperation+" "+INITMEM+" "+SmtLabelRepresentation.OP_DATA.PRE+ " varDeclQ_N",
				QSMTool.cmdOperation+" "+INITMEM+" "+SmtLabelRepresentation.OP_DATA.POST+ " initCond_N",
				QSMTool.cmdOperation+" "+"a"+" "+SmtLabelRepresentation.OP_DATA.PRE+ " somePrecondA_N",
				QSMTool.cmdOperation+" "+"a"+" "+SmtLabelRepresentation.OP_DATA.POST+ " somePostcondA_N",
				QSMTool.cmdOperation+" "+"b"+" "+SmtLabelRepresentation.OP_DATA.PRE+ " somePrecondB_N",
				QSMTool.cmdOperation+" "+"b"+" "+SmtLabelRepresentation.OP_DATA.POST+ " somePostcondB_N",
				QSMTool.cmdOperation+" "+"c"+" "+SmtLabelRepresentation.OP_DATA.PRE+ " somePrecondC_N",
				QSMTool.cmdOperation+" "+"c"+" "+SmtLabelRepresentation.OP_DATA.POST+ " somePostcondC_N"}));
	}

	/** The configuration to use when running tests. */
	Configuration config = null;

	@Test
	public final void completeComputeAlphabet0()
	{
		Set<Label> alphabet = DeterministicDirectedSparseGraph.computeAlphabet(new DirectedSparseGraph());
		Assert.assertTrue(alphabet.isEmpty());
	}

	/** Tests alphabet computation in the presence of unreachable states. */
	@Test
	public final void testComputeFSMAlphabet1()
	{
		Set<Label> expected = new TreeSet<Label>();
		expected.addAll(labelList(new String[] {"p"}));
		LearnerGraphND g = buildLearnerGraphND("A-p->A","testComputeFSMAlphabet1",config,converter);
		Assert.assertEquals(expected, g.pathroutines.computeAlphabet());
		Assert.assertEquals(expected, DeterministicDirectedSparseGraph.computeAlphabet(g.pathroutines.getGraph()));
	}

	@Test
	public final void testComputeFSMAlphabet2()
	{
		LearnerGraphND g = buildLearnerGraphND("A-a->A<-b-A", "completeComputeAlphabet3",config,converter);
		Collection<Label> expected = new HashSet<Label>();expected.addAll(labelList(new String[] {"a","b"}));
		Assert.assertEquals(expected, g.pathroutines.computeAlphabet());
		Assert.assertEquals(expected, DeterministicDirectedSparseGraph.computeAlphabet(g.pathroutines.getGraph()));				
	}
	
	/** Tests alphabet computation in the presence of unreachable states. */
	@Test
	public final void testComputeFSMAlphabet3()
	{
		Collection<Label> expected = new TreeSet<Label>();expected.addAll(labelList(new String[]{"p","d","b","c","a"}));
		LearnerGraphND g = buildLearnerGraphND("A-p->A-b->B-c->B-a-#C\nQ-d->S-c->S","testComputeFSMAlphabet3",config,converter);
		Assert.assertEquals(expected, g.pathroutines.computeAlphabet());
		Assert.assertEquals(expected, DeterministicDirectedSparseGraph.computeAlphabet(g.pathroutines.getGraph()));				
	}


	@Test
	public final void testComputeFSMAlphabet4() {
		LearnerGraphND g = buildLearnerGraphND("A-p->A-b->B-c->B-a->C\nQ-d->S-a-#T","testComputeFSMAlphabet4",config,converter);
		Collection<Label> expected = new HashSet<Label>();expected.addAll(labelList(new String[]{"p","d","b","c","a"}));
		Assert.assertEquals(expected, g.pathroutines.computeAlphabet());
		Assert.assertEquals(expected, DeterministicDirectedSparseGraph.computeAlphabet(g.pathroutines.getGraph()));				
	}

	@Test
	public final void completeComputeAlphabet5()
	{
		LearnerGraphND g = buildLearnerGraphND("A-a->A-b->B-c->B-a->C\nQ-a->S\nA-c->A\nB-b->B\nC-a->C-b->C-c->C\nQ-b->Q-c->Q\nS-a->S-b->S-c->S", "completeComputeAlphabet5",config,converter);
		Collection<Label> expected = new HashSet<Label>();expected.addAll(labelList(new String[] {"a","b","c"}));
		Assert.assertEquals(expected, new LearnerGraphND(g,config).pathroutines.computeAlphabet());
		Assert.assertEquals(expected, DeterministicDirectedSparseGraph.computeAlphabet(g.pathroutines.getGraph()));				

		LearnerGraph clone = new LearnerGraph(g,config);
		Assert.assertFalse( clone.pathroutines.completeGraph(VertexID.parseID("REJ")));
		Assert.assertFalse(DeterministicDirectedSparseGraph.completeGraph(g.pathroutines.getGraph(),"REJ"));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public final void testFindVertex0()
	{
		DeterministicDirectedSparseGraph.findVertex(JUConstants.JUNKVERTEX, null, new DirectedSparseGraph());
	}

	@Test
	public final void testFindVertex1()
	{
		Assert.assertNull(DeterministicDirectedSparseGraph.findVertex(JUConstants.JUNKVERTEX, "bb", new DirectedSparseGraph()));
	}
	
	@Test
	public final void testFindVertex2()
	{
		DirectedSparseGraph g = buildLearnerGraphND("A-a->A-b->B-c->B-a->C\nQ-d->S", "testFindVertex2",config,converter).pathroutines.getGraph();
		//Visualiser.updateFrame(g, g);Visualiser.waitForKey();
		Assert.assertNull(DeterministicDirectedSparseGraph.findVertex(JUConstants.JUNKVERTEX, "bb", g));
	}
		
	@Test
	public final void testFindVertex3()
	{
		DirectedSparseGraph g = buildLearnerGraphND("A-a->A-b->B-c->B-a->C\nQ-d->S", "testFindVertex3",config,converter).pathroutines.getGraph();
		//Visualiser.updateFrame(g, null);Visualiser.waitForKey();
		Assert.assertNull(DeterministicDirectedSparseGraph.findVertex(JUConstants.LABEL, "D", g));
	}

	@Test
	public final void testFindVertex4a()
	{
		Vertex v = DeterministicDirectedSparseGraph.findVertex(JUConstants.INITIAL, "anything", buildLearnerGraphND("A-a->A-b->B-c->B-a->C\nQ-d->S", "testFindVertex4a",config,converter).pathroutines.getGraph());
		Assert.assertNull(v);
	}

	@Test
	public final void testFindVertex4b()
	{
		Vertex v =  DeterministicDirectedSparseGraph.findVertex(JUConstants.INITIAL, true, buildLearnerGraphND("A-a->A-b->B-c->B-a->C\nQ-d->S", "testFindVertex4b",config,converter).pathroutines.getGraph());
		Assert.assertEquals(VertexID.parseID("A"), v.getUserDatum(JUConstants.LABEL));
	}

	@Test
	public final void testFindVertex5()
	{
		Vertex v =  DeterministicDirectedSparseGraph.findVertex(JUConstants.LABEL, VertexID.parseID("A"), buildLearnerGraphND("A-a->A-b->B-c->B-a->C\nQ-d->S", "testFindVertex5",config,converter).pathroutines.getGraph());
		Assert.assertEquals(VertexID.parseID("A"), v.getUserDatum(JUConstants.LABEL));
	}
	
	@Test
	public final void testFindVertex6()
	{
		Vertex v =  DeterministicDirectedSparseGraph.findVertex(JUConstants.LABEL, VertexID.parseID("C"), buildLearnerGraphND("A-a->A-b->B-c->B-a->C\nQ-d->S", "testFindVertex6",config,converter).pathroutines.getGraph());
		Assert.assertEquals(VertexID.parseID("C"), v.getUserDatum(JUConstants.LABEL));
	}
	
	@Test
	public final void testFindVertex7()
	{
		Vertex v = DeterministicDirectedSparseGraph.findVertex(JUConstants.LABEL, VertexID.parseID("S"), 
				buildLearnerGraphND("A-a->A-b->B-c->B-a->C\nQ-d->S", "testFindVertex7",config,converter).pathroutines.getGraph());
		Assert.assertEquals(VertexID.parseID("S"), v.getUserDatum(JUConstants.LABEL));
	}
	
	@Test
	public final void testFindVertex8()
	{
		Vertex v = DeterministicDirectedSparseGraph.findVertex(JUConstants.LABEL, VertexID.parseID("Q"), 
				buildLearnerGraphND("A-a->A-b->B-c->B-a->C\nQ-d->S", "testFindVertex8",config,converter).pathroutines.getGraph());
		Assert.assertEquals(VertexID.parseID("Q"), v.getUserDatum(JUConstants.LABEL));
	}

	
	@Test
	public final void testFindInitial1()
	{
		Vertex v = DeterministicDirectedSparseGraph.findInitial(
				buildLearnerGraphND("A-a->A-b->B-c->B-a->C\nQ-d->S", "testFindInitial",config,converter).pathroutines.getGraph());
		Assert.assertEquals(VertexID.parseID("A"), v.getUserDatum(JUConstants.LABEL));
	}
	
	@Test
	public final void testFindInitial2()
	{
		Vertex v = DeterministicDirectedSparseGraph.findInitial(new DirectedSparseGraph());
		Assert.assertNull(v);
	}


	private final boolean checkIncompatible(LearnerGraph gr,StatePair pair)
	{
		return !AbstractLearnerGraph.checkCompatible(pair.getQ(), pair.getR(), gr.pairCompatibility);
	}
	
	/** Adding A, B as incompatible states. */
	@Test
	public final void testAddToIncompatibles1()
	{
		LearnerGraph grf = buildLearnerGraph("A-a->A-b->B-c->B-a->C\nQ-d->S", "testFindInitial",config,converter);
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("A"))));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("B"))));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("B"),grf.findVertex("A"))));
		
		grf.addToCompatibility(grf.findVertex("B"),grf.findVertex("A"), JUConstants.PAIRCOMPATIBILITY.INCOMPATIBLE);
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("A"))));
		Assert.assertTrue(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("B"))));
		Assert.assertTrue(checkIncompatible(grf,new StatePair(grf.findVertex("B"),grf.findVertex("A"))));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("C"))));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("C"),grf.findVertex("A"))));
	}
	
	/** Adding B, A as incompatible states. */
	@Test
	public final void testAddToIncompatibles2()
	{
		LearnerGraph grf = buildLearnerGraph("A-a->A-b->B-c->B-a->C\nQ-d->S", "testFindInitial",config,converter);
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("A"))));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("B"))));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("B"),grf.findVertex("A"))));
		
		grf.addToCompatibility(grf.findVertex("A"),grf.findVertex("B"),JUConstants.PAIRCOMPATIBILITY.INCOMPATIBLE);
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("A"))));
		Assert.assertTrue(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("B"))));
		Assert.assertTrue(checkIncompatible(grf,new StatePair(grf.findVertex("B"),grf.findVertex("A"))));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("C"))));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("C"),grf.findVertex("A"))));
	}
	
	/** Adding B, A as incompatible states twice. */
	@Test
	public final void testAddToIncompatibles3()
	{
		LearnerGraph grf = buildLearnerGraph("A-a->A-b->B-c->B-a->C\nQ-d->S", "testFindInitial",config,converter);
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("A"))));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("B"))));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("B"),grf.findVertex("A"))));
		
		grf.addToCompatibility(grf.findVertex("A"),grf.findVertex("B"),JUConstants.PAIRCOMPATIBILITY.INCOMPATIBLE);
		grf.addToCompatibility(grf.findVertex("A"),grf.findVertex("B"),JUConstants.PAIRCOMPATIBILITY.INCOMPATIBLE);
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("A"))));
		Assert.assertTrue(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("B"))));
		Assert.assertTrue(checkIncompatible(grf,new StatePair(grf.findVertex("B"),grf.findVertex("A"))));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("C"))));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("C"),grf.findVertex("A"))));
	}
	
	/** Adding B, A as incompatible states twice. */
	@Test
	public final void testAddToIncompatibles4()
	{
		LearnerGraph grf = buildLearnerGraph("A-a->A-b->B-c->B-a->C\nQ-d->S", "testFindInitial",config,converter);
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("A"))));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("B"))));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("B"),grf.findVertex("A"))));
		
		grf.addToCompatibility(grf.findVertex("A"),grf.findVertex("B"),JUConstants.PAIRCOMPATIBILITY.INCOMPATIBLE);
		grf.addToCompatibility(grf.findVertex("B"),grf.findVertex("A"),JUConstants.PAIRCOMPATIBILITY.INCOMPATIBLE);
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("A"))));
		Assert.assertTrue(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("B"))));
		Assert.assertTrue(checkIncompatible(grf,new StatePair(grf.findVertex("B"),grf.findVertex("A"))));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("C"))));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("C"),grf.findVertex("A"))));
	}
	
	/** Adding B, A as incompatible states twice. */
	@Test
	public final void testAddToIncompatibles5()
	{
		LearnerGraph grf = buildLearnerGraph("A-a->A-b->B-c->B-a->C\nQ-d->S", "testFindInitial",config,converter);
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("A"))));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("B"))));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("B"),grf.findVertex("A"))));
		
		grf.addToCompatibility(grf.findVertex("A"),grf.findVertex("B"),JUConstants.PAIRCOMPATIBILITY.INCOMPATIBLE);
		grf.addToCompatibility(grf.findVertex("C"),grf.findVertex("A"),JUConstants.PAIRCOMPATIBILITY.INCOMPATIBLE);
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("A"))));
		Assert.assertTrue(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("B"))));
		Assert.assertTrue(checkIncompatible(grf,new StatePair(grf.findVertex("B"),grf.findVertex("A"))));
		Assert.assertTrue(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("C"))));
		Assert.assertTrue(checkIncompatible(grf,new StatePair(grf.findVertex("C"),grf.findVertex("A"))));
	}
	
	/** Removing B, A as incompatible states. */
	@Test
	public final void testRemoveFromIncompatibles1()
	{
		LearnerGraph grf = buildLearnerGraph("A-a->A-b->B-c->B-a->C\nQ-d-#S", "testRemoveFromIncompatibles1",config,converter);
		grf.addToCompatibility(grf.findVertex("A"),grf.findVertex("B"),JUConstants.PAIRCOMPATIBILITY.INCOMPATIBLE);
		Assert.assertTrue(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("B"))));
		Assert.assertTrue(checkIncompatible(grf,new StatePair(grf.findVertex("B"),grf.findVertex("A"))));
		grf.removeFromIncompatibles(grf.findVertex("A"),grf.findVertex("B"));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("B"))));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("B"),grf.findVertex("A"))));
	}
	
	/** Removing B, A as incompatible states twice. */
	@Test
	public final void testRemoveFromIncompatibles2()
	{
		LearnerGraph grf = buildLearnerGraph("A-a->A-b->B-c->B-a->C\nQ-d-#S", "testRemoveFromIncompatibles1",config,converter);
		grf.addToCompatibility(grf.findVertex("A"),grf.findVertex("B"),JUConstants.PAIRCOMPATIBILITY.INCOMPATIBLE);
		Assert.assertTrue(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("B"))));
		Assert.assertTrue(checkIncompatible(grf,new StatePair(grf.findVertex("B"),grf.findVertex("A"))));
		grf.removeFromIncompatibles(grf.findVertex("A"),grf.findVertex("B"));
		grf.removeFromIncompatibles(grf.findVertex("A"),grf.findVertex("B"));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("B"))));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("B"),grf.findVertex("A"))));
	}
	
	/** Removing B, A as incompatible states. */
	@Test
	public final void testRemoveFromIncompatibles3()
	{
		LearnerGraph grf = buildLearnerGraph("A-a->A-b->B-c->B-a->C\nQ-d-#S", "testRemoveFromIncompatibles1",config,converter);
		grf.addToCompatibility(grf.findVertex("A"),grf.findVertex("B"),JUConstants.PAIRCOMPATIBILITY.INCOMPATIBLE);
		grf.removeFromIncompatibles(grf.findVertex("A"),grf.findVertex("S"));
		grf.removeFromIncompatibles(grf.findVertex("A"),grf.findVertex("C"));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("C"))));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("C"),grf.findVertex("A"))));
		Assert.assertTrue(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("B"))));
		Assert.assertTrue(checkIncompatible(grf,new StatePair(grf.findVertex("B"),grf.findVertex("A"))));
		Assert.assertTrue(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("S"))));
		Assert.assertTrue(checkIncompatible(grf,new StatePair(grf.findVertex("S"),grf.findVertex("A"))));
	}
	
	/** Tests that removing a vertex from a collection of incompatibles may also remove a row. */
	@Test
	public final void testRemoveFromIncompatibles4()
	{
		LearnerGraph grf = buildLearnerGraph("A-a->A-b->B-c->B-a->C\nQ-d-#S", "testRemoveFromIncompatibles1",config,converter);
		grf.addToCompatibility(grf.findVertex("A"),grf.findVertex("B"),JUConstants.PAIRCOMPATIBILITY.INCOMPATIBLE);
		grf.removeFromIncompatibles(grf.findVertex("A"),grf.findVertex("B"));
		Assert.assertTrue(grf.pairCompatibility.compatibility.isEmpty());
	}
	
	/** Checking that copying a graph clones the array. */
	@Test
	public final void testIncompatibles5()
	{
		LearnerGraph grf = buildLearnerGraph("A-a->A-b->B-c->B-a->C\nQ-d->S", "testFindInitial",config,converter);

		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("A"))));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("B"))));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("B"),grf.findVertex("A"))));
		LearnerGraph graph2 = new LearnerGraph(grf,config);
		
		grf.addToCompatibility(grf.findVertex("B"),grf.findVertex("A"),JUConstants.PAIRCOMPATIBILITY.INCOMPATIBLE);
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("A"))));
		Assert.assertTrue(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("B"))));
		Assert.assertTrue(checkIncompatible(grf,new StatePair(grf.findVertex("B"),grf.findVertex("A"))));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("C"))));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("C"),grf.findVertex("A"))));

		Assert.assertFalse(checkIncompatible(graph2,new StatePair(graph2.findVertex("A"),graph2.findVertex("A"))));
		Assert.assertFalse(checkIncompatible(graph2,new StatePair(graph2.findVertex("A"),graph2.findVertex("B"))));
		Assert.assertFalse(checkIncompatible(graph2,new StatePair(graph2.findVertex("B"),graph2.findVertex("A"))));
		Assert.assertFalse(checkIncompatible(graph2,new StatePair(graph2.findVertex("A"),graph2.findVertex("C"))));
		Assert.assertFalse(checkIncompatible(graph2,new StatePair(graph2.findVertex("C"),graph2.findVertex("A"))));
		
		graph2.addToCompatibility(grf.findVertex("C"),grf.findVertex("A"),JUConstants.PAIRCOMPATIBILITY.INCOMPATIBLE);
		
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("A"))));
		Assert.assertTrue(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("B"))));
		Assert.assertTrue(checkIncompatible(grf,new StatePair(grf.findVertex("B"),grf.findVertex("A"))));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("A"),grf.findVertex("C"))));
		Assert.assertFalse(checkIncompatible(grf,new StatePair(grf.findVertex("C"),grf.findVertex("A"))));

		Assert.assertFalse(checkIncompatible(graph2,new StatePair(graph2.findVertex("A"),graph2.findVertex("A"))));
		Assert.assertFalse(checkIncompatible(graph2,new StatePair(graph2.findVertex("A"),graph2.findVertex("B"))));
		Assert.assertFalse(checkIncompatible(graph2,new StatePair(graph2.findVertex("B"),graph2.findVertex("A"))));
		Assert.assertTrue(checkIncompatible(graph2,new StatePair(graph2.findVertex("A"),graph2.findVertex("C"))));
		Assert.assertTrue(checkIncompatible(graph2,new StatePair(graph2.findVertex("C"),graph2.findVertex("A"))));
	}
	
	/** Tests that construction of incompatibles from information in equivalence classes works. 
	 * @throws IncompatibleStatesException */
	@Test
	public final void testConstuctionOfIncompatibles1() throws IncompatibleStatesException
	{
		LearnerGraphND gr = buildLearnerGraphND("S-a->A-b->C-c->G\nS-a->B-b->D-c->H\nB-b->E-c->I\nS-a->F", "testConstuctionOfIncompatiblesA",config,converter);
		gr.addToCompatibility(gr.findVertex("F"), gr.findVertex("D"),JUConstants.PAIRCOMPATIBILITY.INCOMPATIBLE);
		LearnerGraphND expected = buildLearnerGraphND("S-a->A-b->C-c->G", "testConstuctionOfIncompatiblesB",config,converter);
		expected.addToCompatibility(expected.findVertex("A"), expected.findVertex("C"),JUConstants.PAIRCOMPATIBILITY.INCOMPATIBLE);
		Assert.assertNull(WMethod.checkM_and_colours(expected,gr.pathroutines.buildDeterministicGraph(),VERTEX_COMPARISON_KIND.DEEP));
	}
	
	/** Tests that construction of incompatibles from information in equivalence classes works. 
	 * @throws IncompatibleStatesException */
	@Test
	public final void testConstuctionOfIncompatibles2() throws IncompatibleStatesException
	{
		LearnerGraphND gr = buildLearnerGraphND("S-a->A-b->C-c->G\nS-a->B-b->D-c->H\nB-b->E-c->I\nS-a->F", "testConstuctionOfIncompatiblesA",config,converter);
		gr.addToCompatibility(gr.findVertex("B"), gr.findVertex("H"),JUConstants.PAIRCOMPATIBILITY.INCOMPATIBLE);gr.addToCompatibility(gr.findVertex("B"), gr.findVertex("D"),JUConstants.PAIRCOMPATIBILITY.INCOMPATIBLE);
		LearnerGraphND expected = buildLearnerGraphND("S-a->A-b->C-c->G", "testConstuctionOfIncompatiblesB",config,converter);
		expected.addToCompatibility(expected.findVertex("A"), expected.findVertex("C"),JUConstants.PAIRCOMPATIBILITY.INCOMPATIBLE);expected.addToCompatibility(gr.findVertex("A"), gr.findVertex("G"),JUConstants.PAIRCOMPATIBILITY.INCOMPATIBLE);
		Assert.assertNull(WMethod.checkM_and_colours(expected,gr.pathroutines.buildDeterministicGraph(),VERTEX_COMPARISON_KIND.DEEP));
	}
	
	/** Builds a set of sequences from a two-dimensional array, where each element corresponds to a sequence.
	 * 
	 * @param data source data
	 * @return a set of sequences to apply to an RPNI learner
	 */
	public static Set<List<Label>> buildSet(String [][] data,Configuration config, ConvertALabel converter)
	{
		Set<List<Label>> result = new HashSet<List<Label>>();
		for(String []seq:data)
		{
			List<Label> labelSeq = new LinkedList<Label>();
			for(String s:seq) 
				labelSeq.add(AbstractLearnerGraph.generateNewLabel(s,config,converter));
			result.add(labelSeq);
		}
		return result;
	}
	
	/** Builds a set of sequences from a two-dimensional array, where each element corresponds to a sequence.
	 * 
	 * @param data source data
	 * @param config configuration determining the type of label to build
	 * @param converter label converter to use
	 * @return a set of sequences to apply to an RPNI learner
	 */
	public static List<List<Label>> buildList(String [][] data, Configuration config, ConvertALabel converter)
	{
		List<List<Label>> result = new LinkedList<List<Label>>();
		for(String []seq:data)
		{
			result.add(AbstractLearnerGraph.buildList(Arrays.asList(seq),config,converter));
		}
		return result;
	}

	/** Builds a set of sequences from a two-dimensional array, where each element corresponds to a sequence.
	 * 
	 * @param data source data
	 * @param config configuration determining the type of label to build
	 * @return a set of sequences to apply to an RPNI learner
	 */
	public static List<List<String>> buildList(String [][] data)
	{
		List<List<String>> result = new LinkedList<List<String>>();
		for(String []seq:data)
		{
			result.add(Arrays.asList(seq));
		}
		return result;
	}

	/** Builds a map from an array, where each element corresponds to a pair of a string array 
	 * (representing a sequence) and a string (representing flags associated with this sequence).
	 * 
	 * @param data source data
	 * @return a string->string map
	 */
	public static Map<String,String> buildStringMap(Object [][] data)
	{
		Map<String,String> result = new HashMap<String,String>();
		for(Object[] str:data)
		{
			if (str.length != 2)
				throw new IllegalArgumentException("more than two elements in sequence "+str);
			if (str[0] == null || str[1] == null || !(str[0] instanceof String[]) || !(str[1] instanceof String))
				throw new IllegalArgumentException("invalid data in array");
			result.put(ArrayOperations.seqToString(Arrays.asList((String[])str[0])),(String)str[1]);
		}
		return result;
	}
	
	@Test
	public final void testBuildSet1()
	{
		assertTrue(buildSet(new String[] []{},config,converter).isEmpty());
	}

	@Test
	public final void testBuildSet2()
	{
		Set<List<String>> expectedResult = new HashSet<List<String>>();
		expectedResult.add(new LinkedList<String>());
		assertTrue(expectedResult.equals(buildSet(new String[] []{new String[]{}},config,converter)));
	}

	@Test
	public final void testBuildSet3A()
	{
		Set<List<Label>> expectedResult = new HashSet<List<Label>>();
		expectedResult.add(labelList(new String[]{"a","b","c"}));
		expectedResult.add(new LinkedList<Label>());
		assertTrue(expectedResult.equals(buildSet(new String[] []{new String[]{},new String[]{"a","b","c"}},config,converter)));
	}

	@Test
	public final void testBuildSet3B()
	{
		Set<List<Label>> expectedResult = new HashSet<List<Label>>();
		expectedResult.add(labelList(new String[]{"a","b","c"}));
		assertTrue(expectedResult.equals(buildSet(new String[] []{new String[]{"a","b","c"}},config,converter)));
	}

	@Test
	public final void testBuildSet4()
	{
		Set<List<Label>> expectedResult = new HashSet<List<Label>>();
		expectedResult.add(labelList(new String[]{"a","b","c"}));
		expectedResult.add(new LinkedList<Label>());
		expectedResult.add(labelList(new String[]{"g","t"}));
		expectedResult.add(labelList(new String[]{"h","q","i"}));
		assertTrue(expectedResult.equals(buildSet(new String[] []{
				new String[]{"a","b","c"},new String[]{"h","q","i"}, new String[] {},new String[]{"g","t"} },config,converter)));
	}

	@Test
	public final void testBuildStringMap1()
	{
		Map<String,String> expectedResult = new HashMap<String,String>();
		
		assertTrue(expectedResult.equals(buildStringMap(new Object[][]{
		})));
	}
	
	@Test
	public final void testBuildStringMap2()
	{
		Map<String,String> expectedResult = new HashMap<String,String>();
		expectedResult.put("a","value2");expectedResult.put("b","value3");
		
		assertTrue(expectedResult.equals(buildStringMap(new Object[][]{
				new Object[]{new String[]{"a"},"value2"},
				new Object[]{new String[]{"b"},"value3"}
		})));
	}
	
	@Test
	public final void testBuildStringMap3()
	{
		Map<String,String> expectedResult = new HashMap<String,String>();
		expectedResult.put("a","value1");expectedResult.put("strC","value2");expectedResult.put("b","value3");
		
		assertTrue(expectedResult.equals(buildStringMap(new Object[][]{
				new Object[]{new String[]{"strC"},"value2"},
				new Object[]{new String[]{"a"},"value1"},
				new Object[]{new String[]{"b"},"value3"}
		})));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public final void testBuildStringMap4()
	{
		Map<String,String> expectedResult = new HashMap<String,String>();
		expectedResult.put("a","value1");expectedResult.put("strC","value2");expectedResult.put("b","value3");
		
		assertTrue(expectedResult.equals(buildStringMap(new Object[][]{
				new Object[]{new String[]{"strC"},"value1"},
				new Object[]{new String[]{"a"}},// an invalid sequence
				new Object[]{new String[]{"b"},"value3"}
		})));
	}

	@Test(expected = IllegalArgumentException.class)
	public final void testBuildStringMap5()
	{
		Map<String,String> expectedResult = new HashMap<String,String>();
		expectedResult.put("a","value1");expectedResult.put("strC","value2");expectedResult.put("b","value3");
		
		assertTrue(expectedResult.equals(buildStringMap(new Object[][]{
				new Object[]{new String[]{"strC"},"value1"},
				new Object[]{},// an invalid sequence - too few elements
				new Object[]{new String[]{"b"},"value3"}
		})));
	}

	@Test(expected = IllegalArgumentException.class)
	public final void testBuildStringMap6()
	{
		Map<String,String> expectedResult = new HashMap<String,String>();
		expectedResult.put("a","value1");expectedResult.put("strC","value2");expectedResult.put("b","value3");
		
		assertTrue(expectedResult.equals(buildStringMap(new Object[][]{
				new Object[]{new String[]{"strC"},"value1"},
				new Object[]{new String[]{"a"},"c","d"},// an invalid sequence - too many elements
				new Object[]{new String[]{"b"},"value3"}
		})));
	}

	@Test(expected = IllegalArgumentException.class)
	public final void testBuildStringMap7()
	{
		Map<String,String> expectedResult = new HashMap<String,String>();
		expectedResult.put("a","value1");expectedResult.put("strC","value2");expectedResult.put("b","value3");
		
		assertTrue(expectedResult.equals(buildStringMap(new Object[][]{
				new Object[]{new String[]{"strC"},"value1"},
				new Object[]{new Object(),"c"},// an invalid sequence - wrong type of the first element
				new Object[]{new String[]{"b"},"value3"}
		})));
	}

	@Test(expected = IllegalArgumentException.class)
	public final void testBuildStringMap8()
	{
		Map<String,String> expectedResult = new HashMap<String,String>();
		expectedResult.put("a","value1");expectedResult.put("strC","value2");expectedResult.put("b","value3");
		
		assertTrue(expectedResult.equals(buildStringMap(new Object[][]{
				new Object[]{new String[]{"strC"},"value1"},
				new Object[]{"text","c"},// an invalid sequence - wrong type of the first element
				new Object[]{new String[]{"b"},"value3"}
		})));
	}

	@Test(expected = IllegalArgumentException.class)
	public final void testBuildStringMap9()
	{
		Map<String,String> expectedResult = new HashMap<String,String>();
		expectedResult.put("a","value1");expectedResult.put("strC","value2");expectedResult.put("b","value3");
		
		assertTrue(expectedResult.equals(buildStringMap(new Object[][]{
				new Object[]{new String[]{"strC"},"value1"},
				new Object[]{new String[]{"a"},new Object()},// an invalid sequence - wrong type of the second element
				new Object[]{new String[]{"b"},"value3"}
		})));
	}

	@Test(expected = IllegalArgumentException.class)
	public final void testBuildStringMap10()
	{
		Map<String,String> expectedResult = new HashMap<String,String>();
		expectedResult.put("a","value1");expectedResult.put("strC","value2");expectedResult.put("b","value3");
		
		assertTrue(expectedResult.equals(buildStringMap(new Object[][]{
				new Object[]{new String[]{"strC"},"value1"},
				new Object[]{null,"value"},// an invalid sequence - null in the first element
				new Object[]{new String[]{"b"},"value3"}
		})));
	}

	@Test(expected = IllegalArgumentException.class)
	public final void testBuildStringMap11()
	{
		Map<String,String> expectedResult = new HashMap<String,String>();
		expectedResult.put("a","value1");expectedResult.put("strC","value2");expectedResult.put("b","value3");
		
		assertTrue(expectedResult.equals(buildStringMap(new Object[][]{
				new Object[]{new String[]{"strC"},"value1"},
				new Object[]{new String[]{"a"}, null},// an invalid sequence - null in the second element
				new Object[]{new String[]{"b"},null}
		})));
	}
	
	public final void checkForCorrectException(final int [][]tTable, final int []vFrom, String exceptionString)
	{
		statechum.Helper.checkForCorrectException(new whatToRun() { public @Override void run() {
			LearnerGraph.convertTableToFSMStructure(tTable, vFrom, -1	,config,converter);
		}}, IllegalArgumentException.class,exceptionString);
	}
	
	/** Zero-sized array. */
	@Test
	public final void testConvertTableToFSMStructure0()
	{
		int [][]table = new int[][] {
		};
		checkForCorrectException(table, new int[0], "array is zero-sized");
	}

	/** Zero-sized alphabet. */
	@Test
	public final void testConvertTableToFSMStructure1a()
	{
		int [][]table = new int[][] {
			{}, 
			{1,1}
		};
		checkForCorrectException(table, new int[]{0,1}, "alphabet is zero-sized");
	}
	
	/** "rows of inconsistent size" */
	@Test
	public final void testConvertTableToFSMStructure1b()
	{
		int [][]table = new int[][] {
			{}, 
			{1,1}
		};
		checkForCorrectException(table, new int[]{1,0}, "rows of inconsistent size");
	}
	
	/** "rows of inconsistent size" */
	@Test
	public final void testConvertTableToFSMStructure2()
	{
		int [][]table = new int[][] {
				{1,0,1,0}, 
				{0,1}
			};
		checkForCorrectException(table, new int[]{0,1}, "rows of inconsistent size");
	}
	
	/** Reject number in vfrom. */
	@Test
	public final void testConvertTableToFSMStructure3()
	{
		int [][]table = new int[][] {
				{1,0,1,0}, 
				{0,1,0,1}
			};
		checkForCorrectException(table, new int[]{0,-1}, "reject number in vFrom");
	}
	
	/** Transition to illegal state 6 */
	@Test
	public final void testConvertTableToFSMStructure4a()
	{
		int [][]table = new int[][] {
			{0,	1,	-1,	2}, 
			{0, 3,	0,	-1},
			{0,0,0,6},
			{-1,-1,-1,-1}
		};
		checkForCorrectException(table, new int[]{0,1,2,3}, "leads to an invalid state");
	}
	
	/** Transition to illegal state -4 */
	@Test
	public final void testConvertTableToFSMStructure4b()
	{
		int [][]table = new int[][] {
			{0,	1,	-1,	2}, 
			{0, 3,	0,	-1},
			{0,0,0,-4},
			{-1,-1,-1,-1}
		};
		checkForCorrectException(table, new int[]{0,1,2,3}, "leads to an invalid state");
	}

	@Test
	public final void testConvertTableToFSMStructure_missing_elements_in_vFrom()
	{
		int [][]table = new int[][] {
			{0,	1,	-1,	3}, 
			{0, 3,	0,	-1},
			{0,0,0,6},
			{-1,-1,-1,-1}
		};
		checkForCorrectException(table, new int[]{0,1}, "Some states in the transition table are not included in vFrom");
	}

	@Test
	public final void testConvertTableToFSMStructure5()
	{
		int [][]table = new int[][] {
			{0,	1,	-1,	3}, 
			{0, 3,	0,	-1},
			{0,0,0,6},
			{-1,-1,-1,-1}
		};
		LearnerGraph fsm = LearnerGraph.convertTableToFSMStructure(table, new int[]{0,1,3}, -1	,config,converter);
		Assert.assertNull(WMethod.checkM(fsm, fsm.findVertex("S0"),
				buildLearnerGraph("S0-i0->S0-i1->S1\nS0-i3->S2\nS1-i0->S0\nS1-i1->S3\nS1-i2->S0", "testConvertTableToFSMStructure5",config,converter), 
				fsm.findVertex("S0"),WMethod.VERTEX_COMPARISON_KIND.NONE, true));
	}
	
	@Test
	public final void testConvertTableToFSMStructure6()
	{
		int [][]table = new int[][] {
			{0,	1,	-1,	3}, 
			{0, 3,	0,	-1},
			{0,0,0,6},
			{-1,-1,-1,-1}
		};
		LearnerGraph fsm = LearnerGraph.convertTableToFSMStructure(table, new int[]{1,0,3}, -1	,config,converter);
		Assert.assertNull(WMethod.checkM(fsm, fsm.findVertex("S0"), 
				buildLearnerGraph("S0-i0->S0-i1->S1\nS0-i3->S2\nS1-i0->S0\nS1-i1->S3\nS1-i2->S0", "testConvertTableToFSMStructure6",config,converter), 
				fsm.findVertex("S0"),WMethod.VERTEX_COMPARISON_KIND.NONE, true));
	}

	@Test
	public final void testConvertTableToFSMStructure7()
	{
		int [][]table = new int[][] {
			{0,	1,	-1,	3}, 
			{0, 3,	0,	-1},
			{0,0,0,6},
			{-1,-1,-1,-1}
		};
		LearnerGraph fsm = LearnerGraph.convertTableToFSMStructure(table, new int[]{3,0,1}, -1	,config,converter);
		Assert.assertNull(WMethod.checkM(fsm, fsm.findVertex("S0"), 
				buildLearnerGraph("S0-i0->S0-i1->S1\nS0-i3->S2\nS1-i0->S0\nS1-i1->S3\nS1-i2->S0", "testConvertTableToFSMStructure7",config,converter), 
				fsm.findVertex("S0"),WMethod.VERTEX_COMPARISON_KIND.NONE, true));
	}
	
	@Test
	public final void testConvertTableToFSMStructure8()
	{
		int [][]table = new int[][] {
			{0,	1,	-1,	3}, 
			{0, 3,	0,	-1},
			{0,0,0,6},
			{-1,-1,-1,-1}
		};
		LearnerGraph fsm = LearnerGraph.convertTableToFSMStructure(table, new int[]{3,0,1,0,1,1}, -1	,config,converter);
		Assert.assertNull(WMethod.checkM(fsm, fsm.findVertex("S0"), 
				buildLearnerGraph("S0-i0->S0-i1->S1\nS0-i3->S2\nS1-i0->S0\nS1-i1->S3\nS1-i2->S0", "testConvertTableToFSMStructure8",config,converter), 
				fsm.findVertex("S0"),WMethod.VERTEX_COMPARISON_KIND.NONE, true));
	}

	@Test
	public final void testGetNonRepeatingNumbers0()
	{
		int data[] = DeterministicDirectedSparseGraph.getNonRepeatingNumbers(0, 0); 
		Assert.assertEquals(0,data.length);
	}
	
	@Test
	public final void testGetNonRepeatingNumbers1()
	{
		int data[] = DeterministicDirectedSparseGraph.getNonRepeatingNumbers(1, 0); 
		Assert.assertEquals(1,data.length);Assert.assertEquals(0, data[0]);
	}
	
	@Test
	public final void testGetNonRepeatingNumbers2()
	{
		int data[] = DeterministicDirectedSparseGraph.getNonRepeatingNumbers(2, 0); 
		Assert.assertEquals(2,data.length);
		if (data[0] == 0)
			Assert.assertEquals(1, data[1]);
		else
		{
			Assert.assertEquals(1, data[0]);Assert.assertEquals(0, data[1]);
		}
	}
	
	@Test
	public final void testGetNonRepeatingNumbers3()
	{
		final int size = 200;
		int data[] = DeterministicDirectedSparseGraph.getNonRepeatingNumbers(size, 1); 
		Assert.assertEquals(size,data.length);
		boolean values[] = new boolean[size];
		for(int i=0;i<size;++i) { Assert.assertFalse(values[data[i]]);values[data[i]]=true; }
		//System.out.println(Arrays.toString(data));
	}

	@Test
	public final void computeShortPathsToAllStates1()
	{
		LearnerGraphND graph = buildLearnerGraphND("A-a->B\nA-a->C","computeShortPathsToAllStates1",config,converter);
		Map<CmpVertex,List<Label>> expected = new TreeMap<CmpVertex,List<Label>>();
		expected.put(graph.findVertex("A"), labelList(new String[]{}));
		expected.put(graph.findVertex("B"), labelList(new String[]{"a"}));
		expected.put(graph.findVertex("C"), labelList(new String[]{"a"}));
		Assert.assertEquals(expected,graph.pathroutines.computeShortPathsToAllStates(graph.findVertex("A")));
	}
	
	@Test
	public final void computeShortPathsToAllStates2()
	{
		LearnerGraphND graph = buildLearnerGraphND("A-a->B\nA-a->C-b-#D","computeShortPathsToAllStates1",config,converter);
		Map<CmpVertex,List<Label>> expected = new TreeMap<CmpVertex,List<Label>>();
		expected.put(graph.findVertex("A"), labelList(new String[]{}));
		expected.put(graph.findVertex("B"), labelList(new String[]{"a"}));
		expected.put(graph.findVertex("C"), labelList(new String[]{"a"}));
		expected.put(graph.findVertex("D"), labelList(new String[]{"a","b"}));
		Assert.assertEquals(expected,graph.pathroutines.computeShortPathsToAllStates(graph.findVertex("A")));
	}
	
	@Test
	public final void computeShortPathsToAllStates3()
	{
		LearnerGraphND graph = buildLearnerGraphND("A-a->B\nA-a->C-b-#D","computeShortPathsToAllStates1",config,converter);
		Map<CmpVertex,List<Label>> expected = new TreeMap<CmpVertex,List<Label>>();
		expected.put(graph.findVertex("B"), labelList(new String[]{}));
		Assert.assertEquals(expected,graph.pathroutines.computeShortPathsToAllStates(graph.findVertex("B")));
	}
	
	@Test
	public final void computeShortPathsToAllStates4()
	{
		LearnerGraphND graph = buildLearnerGraphND("A-a->B\nA-a->C-b-#D","computeShortPathsToAllStates1",config,converter);
		Map<CmpVertex,List<Label>> expected = new TreeMap<CmpVertex,List<Label>>();
		expected.put(graph.findVertex("C"), labelList(new String[]{}));
		expected.put(graph.findVertex("D"), labelList(new String[]{"b"}));
		Assert.assertEquals(expected,graph.pathroutines.computeShortPathsToAllStates(graph.findVertex("C")));
	}
	
	/** Extracts PTA states from a collection of abstract states. */
	private static Set<CmpVertex> extractStates(Collection<AbstractState> abstractStates)
	{
		Set<CmpVertex> result = new TreeSet<CmpVertex>();
		for(AbstractState state:abstractStates) result.add(state.vertex);
		return result;
	}
	
	/** Tests <em>buildVertexToEqClassMap</em>. */
	@Test
	public final void testBuildVertexToEqClassMap1a()
	{
		LearnerGraph graph = buildLearnerGraph("A-a->B-b->A\nA-b->C-b->D","testBuildVertexToEqClassMap1a",config,converter);
		Assert.assertNull(graph.getVertexToAbstractState());
		
		lbls.buildVertexToAbstractStateMap(graph,null,true);
		Assert.assertEquals(4,graph.getVertexToAbstractState().size());

		for(String vertex:new String[]{"A","B","C","D"})
		{
			Set<CmpVertex> expectedSet = new TreeSet<CmpVertex>();expectedSet.add(graph.findVertex(VertexID.parseID(vertex)));
			Assert.assertEquals(expectedSet,extractStates(graph.getVertexToAbstractState().get(graph.findVertex(VertexID.parseID(vertex)))));
		}
		
		// Now update the map and check that it did not change
		lbls.buildVertexToAbstractStateMap(graph,null,true);
		Assert.assertEquals(4,graph.getVertexToAbstractState().size());

		for(String vertex:new String[]{"A","B","C","D"})
		{
			Set<CmpVertex> expectedSet = new TreeSet<CmpVertex>();expectedSet.add(graph.findVertex(VertexID.parseID(vertex)));
			Assert.assertEquals(expectedSet,extractStates(graph.getVertexToAbstractState().get(graph.findVertex(VertexID.parseID(vertex)))));
		}
	}
	
	/** Tests <em>buildVertexToEqClassMap</em>. */
	@Test
	public final void testBuildVertexToEqClassMap1()
	{
		LearnerGraph graph = buildLearnerGraph("A-a->B-b->A\nA-b->C-b-#D","testBuildVertexToEqClassMap1b",config,converter);
		Assert.assertNull(graph.getVertexToAbstractState());
		
		lbls.buildVertexToAbstractStateMap(graph,null,true);
		Assert.assertEquals(3,graph.getVertexToAbstractState().size());

		for(String vertex:new String[]{"A","B","C"})
		{
			Set<CmpVertex> expectedSet = new TreeSet<CmpVertex>();expectedSet.add(graph.findVertex(VertexID.parseID(vertex)));
			Assert.assertEquals(expectedSet,extractStates(graph.getVertexToAbstractState().get(graph.findVertex(VertexID.parseID(vertex)))));
		}
		
		// Now update the map and check that it did not change
		lbls.buildVertexToAbstractStateMap(graph,null,true);
		Assert.assertEquals(3,graph.getVertexToAbstractState().size());

		for(String vertex:new String[]{"A","B","C"})
		{
			Set<CmpVertex> expectedSet = new TreeSet<CmpVertex>();expectedSet.add(graph.findVertex(VertexID.parseID(vertex)));
			Assert.assertEquals(expectedSet,extractStates(graph.getVertexToAbstractState().get(graph.findVertex(VertexID.parseID(vertex)))));
		}
	}
	
	/** Tests <em>buildVertexToEqClassMap</em>. */
	@Test
	public final void testBuildVertexToEqClassMap2()
	{
		LearnerGraph graph = buildLearnerGraph("A-a->B-a->C-a->D\nC-b->C1\nD-b->D1-b->D2","testBuildVertexToEqClassMap2",config,converter);
		LearnerGraph mergedAB = MergeStates.mergeAndDeterminize_general(graph, new StatePair(graph.findVertex("A"),graph.findVertex("B")));
		
		Assert.assertNull(graph.getVertexToAbstractState());
		Assert.assertNull(mergedAB.getVertexToAbstractState());

		lbls.buildVertexToAbstractStateMap(graph,null,true);
		Assert.assertEquals(7,graph.getVertexToAbstractState().size());
		for(String vertex:new String[]{"A","B","C","D","C1","D1","D2"})
		{
			Set<CmpVertex> expectedSet = new TreeSet<CmpVertex>();expectedSet.add(graph.findVertex(VertexID.parseID(vertex)));
			Assert.assertEquals(expectedSet,extractStates(graph.getVertexToAbstractState().get(graph.findVertex(VertexID.parseID(vertex)))));
		}
	}
	
	/** Tests <em>buildVertexToEqClassMap</em>. */
	@Test
	public final void testBuildVertexToEqClassMap3()
	{
		LearnerGraph graph = buildLearnerGraph("A-a->B-a->C-a->D\nC-b->C1\nD-b->D1-b->D2","testBuildVertexToEqClassMap2",config,converter);
		lbls.buildVertexToAbstractStateMap(graph,null,true);
		LearnerGraph mergedAB = MergeStates.mergeAndDeterminize_general(graph, new StatePair(graph.findVertex("A"),graph.findVertex("B")));
		Assert.assertNotNull(graph.getVertexToAbstractState());
		System.out.println();
		lbls.buildVertexToAbstractStateMap(mergedAB,graph,true);
		Assert.assertNotNull(mergedAB.getVertexToAbstractState());
		
		Set<CmpVertex> expectedSet = new TreeSet<CmpVertex>();
		
		expectedSet.clear();
		for(String vertex:new String[]{"A","B","C","D"}) expectedSet.add(graph.findVertex(VertexID.parseID(vertex)));
		Assert.assertEquals(expectedSet,extractStates(mergedAB.getVertexToAbstractState().get(graph.findVertex(VertexID.parseID("A")))));
		
		expectedSet.clear();
		for(String vertex:new String[]{"C1","D1"}) expectedSet.add(graph.findVertex(VertexID.parseID(vertex)));
		Assert.assertEquals(expectedSet,extractStates(mergedAB.getVertexToAbstractState().get(graph.findVertex(VertexID.parseID("C1")))));

		expectedSet.clear();
		for(String vertex:new String[]{"D2"}) expectedSet.add(graph.findVertex(VertexID.parseID(vertex)));
		Assert.assertEquals(expectedSet,extractStates(mergedAB.getVertexToAbstractState().get(graph.findVertex(VertexID.parseID("D2")))));
		
		// Now update the matrix and check that it did not change
		lbls.buildVertexToAbstractStateMap(mergedAB, null,true);

		expectedSet.clear();
		for(String vertex:new String[]{"A","B","C","D"}) expectedSet.add(graph.findVertex(VertexID.parseID(vertex)));
		Assert.assertEquals(expectedSet,extractStates(mergedAB.getVertexToAbstractState().get(graph.findVertex(VertexID.parseID("A")))));
		
		expectedSet.clear();
		for(String vertex:new String[]{"C1","D1"}) expectedSet.add(graph.findVertex(VertexID.parseID(vertex)));
		Assert.assertEquals(expectedSet,extractStates(mergedAB.getVertexToAbstractState().get(graph.findVertex(VertexID.parseID("C1")))));

		expectedSet.clear();
		for(String vertex:new String[]{"D2"}) expectedSet.add(graph.findVertex(VertexID.parseID(vertex)));
		Assert.assertEquals(expectedSet,extractStates(mergedAB.getVertexToAbstractState().get(graph.findVertex(VertexID.parseID("D2")))));
	}
	
	/** Tests <em>buildVertexToEqClassMap</em>. */
	@Test
	public final void testBuildVertexToEqClassMap4()
	{
		LearnerGraph graph = buildLearnerGraph("A-a->B-a->C-a->D\nC-b->C1\nD-b->D1-b->D2","testBuildVertexToEqClassMap2",config,converter);
		lbls.buildVertexToAbstractStateMap(graph,null,true);
		LearnerGraph mergedAB = MergeStates.mergeAndDeterminize_general(graph, new StatePair(graph.findVertex("A"),graph.findVertex("B")));
		lbls.buildVertexToAbstractStateMap(mergedAB, graph,true);
		LearnerGraph mergedAll = MergeStates.mergeAndDeterminize_general(mergedAB, new StatePair(mergedAB.findVertex("A"),mergedAB.findVertex("C1")));
		lbls.buildVertexToAbstractStateMap(mergedAll, mergedAB,true);
		Assert.assertNotNull(graph.getVertexToAbstractState());
		Assert.assertNotNull(mergedAB.getVertexToAbstractState());
		Assert.assertNotNull(mergedAll.getVertexToAbstractState());

		Set<CmpVertex> expectedSet = new TreeSet<CmpVertex>();
		
		expectedSet.clear();
		for(String vertex:new String[]{"A","B","C","D","C1","D1","D2"}) expectedSet.add(graph.findVertex(VertexID.parseID(vertex)));
		Assert.assertEquals(expectedSet,extractStates(mergedAll.getVertexToAbstractState().get(graph.findVertex(VertexID.parseID("A")))));

		// Now update the matrix and check that it did not change
		lbls.buildVertexToAbstractStateMap(mergedAll, null,true);		
		expectedSet.clear();
		for(String vertex:new String[]{"A","B","C","D","C1","D1","D2"}) expectedSet.add(graph.findVertex(VertexID.parseID(vertex)));
		Assert.assertEquals(expectedSet,extractStates(mergedAll.getVertexToAbstractState().get(graph.findVertex(VertexID.parseID("A")))));
	}
	
	/** Similar to the above, but this time a new vertex is added to the intermediate graph and the matrix is updated. */
	@Test
	public final void testBuildVertexToEqClassMap5()
	{
		LearnerGraph graph = buildLearnerGraph("A-a->B-a->C-a->D\nC-b->C1\nD-b->D1-b->D2","testBuildVertexToEqClassMap2",config,converter);
		lbls.buildVertexToAbstractStateMap(graph, null,true);
		LearnerGraph mergedAB = MergeStates.mergeAndDeterminize_general(graph, new StatePair(graph.findVertex("A"),graph.findVertex("B")));
		lbls.buildVertexToAbstractStateMap(mergedAB, graph,true);
		CmpVertex newVertex = AbstractLearnerGraph.generateNewCmpVertex(VertexID.parseID("D3"), mergedAB.config);
		mergedAB.transitionMatrix.put(newVertex, mergedAB.createNewRow());
		mergedAB.addTransition(mergedAB.transitionMatrix.get(mergedAB.findVertex("D2")), AbstractLearnerGraph.generateNewLabel("b",config,converter), newVertex);
		lbls.buildVertexToAbstractStateMap(mergedAB, null,true);// update map
		LearnerGraph mergedAll = MergeStates.mergeAndDeterminize_general(mergedAB, new StatePair(mergedAB.findVertex("A"),mergedAB.findVertex("C1")));
		lbls.buildVertexToAbstractStateMap(mergedAll, mergedAB,true);

		Assert.assertNotNull(graph.getVertexToAbstractState());
		Assert.assertNotNull(mergedAB.getVertexToAbstractState());
		Assert.assertNotNull(mergedAll.getVertexToAbstractState());
		
		Set<CmpVertex> expectedSet = new TreeSet<CmpVertex>();
		
		expectedSet.clear();
		for(String vertex:new String[]{"A","B","C","D","C1","D1","D2"}) expectedSet.add(graph.findVertex(VertexID.parseID(vertex)));
		for(String vertex:new String[]{"D3"}) expectedSet.add(mergedAB.findVertex(VertexID.parseID(vertex)));
		Assert.assertEquals(expectedSet,extractStates(mergedAll.getVertexToAbstractState().get(graph.findVertex(VertexID.parseID("A")))));
	}

	@Test
	public final void testCountEdges1()
	{
		LearnerGraphND graph = buildLearnerGraphND("A-a->A-b->B-b->C-c->D / A-a->E-a->F-d->F","testCountEdges1",config,converter);
		Assert.assertEquals(7, graph.pathroutines.countEdges());
	}
	
	@Test
	public final void testCountEdges2()
	{
		LearnerGraph graph = buildLearnerGraph("A-a->A-b->B-b2->C-c->D / A-a2->E-a->F-d->F","testCountEdges2",config,converter);
		Assert.assertEquals(7, graph.pathroutines.countEdges());
	}
	
	@Test
	public final void testCountEdges3()
	{
		LearnerGraph graph = new LearnerGraph(config);
		Assert.assertEquals(0, graph.pathroutines.countEdges());
	}
	
	@Test
	public final void testCountEdges4()
	{
		LearnerGraphND graph = new LearnerGraphND(config);
		Assert.assertEquals(0, graph.pathroutines.countEdges());
	}

	@Test
	public final void testToADL1()
	{
		config.setUseOrderedEntrySet(true);
		LearnerGraph graph = new LearnerGraph(config);
		graph.initEmpty();
		Assert.assertEquals("0 0\n",graph.pathroutines.toADL());
	}
	
	@Test
	public final void testToADL2()
	{
		config.setUseOrderedEntrySet(true);
		LearnerGraph graph = new LearnerGraph(config);
		graph.initPTA();
		Assert.assertEquals("1 0\nP1000 true true\n",graph.pathroutines.toADL());
	}
	
	@Test
	public final void testToADL3()
	{
		config.setUseOrderedEntrySet(true);
		LearnerGraph graph = new LearnerGraph(config);
		graph.initPTA();graph.getInit().setAccept(false);
		Assert.assertEquals("1 0\nP1000 true false\n",graph.pathroutines.toADL());
	}
	
	@Test
	public final void testToADL4()
	{
		config.setUseOrderedEntrySet(true);
		LearnerGraphND graph = buildLearnerGraphND("A-a->A-a->B","testtoADL4",config,converter);
		graph.getInit().setAccept(false);
		Assert.assertEquals("2 2\nA true false\nB false true\nA A a\nA B a\n",graph.pathroutines.toADL());
	}
	
	@Test
	public final void testToADL5()
	{
		config.setUseOrderedEntrySet(true);
		LearnerGraphND graph = buildLearnerGraphND("A-a->A-a->B / A-b->B","testtoADL4",config,converter);
		graph.getInit().setAccept(false);
		Assert.assertEquals("2 3\nA true false\nB false true\nA A a\nA B a\nA B b\n",graph.pathroutines.toADL());
	}
	
	@Test
	public final void testToADL6()
	{
		config.setUseOrderedEntrySet(true);
		LearnerGraphND graph = buildLearnerGraphND("A-a->A-a->B / A-b->B / B-b->A / B-c-#C","testtoADL4",config,converter);
		graph.getInit().setAccept(false);
		Assert.assertEquals("3 5\nA true false\nB false true\nC false false\nA A a\nA B a\nA B b\nB A b\nB C c\n",graph.pathroutines.toADL());
	}
	
	@Test
	public final void testToADL7()
	{
		config.setUseOrderedEntrySet(true);
		LearnerGraphND graph = buildLearnerGraphND("A-a->A-a->B / A-b->B / B-b->A / B-c-#C","testtoADL4",config,converter);
		Assert.assertEquals("3 5\nA true true\nB false true\nC false false\nA A a\nA B a\nA B b\nB A b\nB C c\n",graph.pathroutines.toADL());
	}
	
	@BeforeClass
	public static void initJungViewer() // initialisation - once only for all tests in this class
	{		
		Visualiser.disposeFrame();
	}

	@AfterClass
	public static void cleanUp()
	{
		Visualiser.disposeFrame();
	}
}

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

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import statechum.Configuration;
import statechum.DeterministicDirectedSparseGraph.CmpVertex;
import statechum.DeterministicDirectedSparseGraph.VertexID;
import statechum.DeterministicDirectedSparseGraph.VertID.VertKind;
import statechum.Label;
import statechum.analysis.learning.experiments.ExperimentRunner;
import statechum.analysis.learning.experiments.ExperimentRunner.HandleProcessIO;
import statechum.analysis.learning.rpnicore.AMEquivalenceClass.IncompatibleStatesException;
import statechum.analysis.learning.rpnicore.Transform.ConvertALabel;
import statechum.apps.QSMTool;

/** This one runs LTL2BA and parses its output.
 * <p>
 * Data from LTL2BA has to arrive in the following format:
 * <pre>
never {
accept_init :
	if
	:: (!close) -> goto accept_S1
	:: (1) -> goto accept_S4
	:: (!save) -> goto accept_S2
	:: (!save && edit) -> goto accept_S3
	fi;
accept_S1 :
	if
	:: (!close) -> goto accept_S1
	:: (1) -> goto accept_S4
	fi;
accept_S4 :
	if
	:: (!save && !edit && !close) -> goto accept_S4
	:: (!save && !edit && !close && load) -> goto accept_S1
	fi;
accept_S2 :
	if
	:: (!save) -> goto accept_S2
	:: (!save && edit) -> goto accept_S3
	fi;
accept_S3 :
	if
	:: (!save) -> goto accept_S3
	:: (1) -> goto accept_S2
	fi;
}
</pre>
 *
 * Another example is below (note that "if" statements can be replaced with "skip"
 * !(((edit) R (!save)) && G(save -> X((edit) R (!save))))
<pre>
never {
T0_init :
	if
	:: (!edit) || (save) -> goto T0_S1
	:: (save) -> goto accept_all
	:: (1) -> goto T1_S2
	fi;
T0_S1 :
	if
	:: (!edit) -> goto T0_S1
	:: (save) -> goto accept_all
	fi;
T1_S2 :
	if
	:: (1) -> goto T1_S2
	:: (save) -> goto T0_S1
	fi;
accept_all :
	skip
}
</pre>
 * This automatically excludes a few words from appearing
 * as event labels.
 * 
 * @author kirill
 *
 */
public class LTL_to_ba {
	/** What is forbidden in LTL formulas - whether we're dealing with purely safety properties
	 * is checked in the process of parsing of the output of LTL2BA. 
	 */
	public static final Pattern ltlForbiddenWords;
	
	static
	{
		StringBuffer expr = new StringBuffer();
		boolean first = true;
		for(String str:new String[]{":","/\\*","\\*/"}) // this one is to ensure that parsing is regular.
		{
			if (!first) expr.append("|");else first=false;
			expr.append("(.*");expr.append(str);expr.append(".*)");
		}
		ltlForbiddenWords = Pattern.compile(expr.toString());
	}
	
	/** Refer to the description of dumpStreams in ExperimentRunner class for details on this. */
	public static final int timeBetweenHearbeats=20;

	/** Concatenates LTL and checks for forbidden words. */
	public static StringBuffer concatenateLTL(Collection<String> ltl)
	{
		StringBuffer ltlCombined = new StringBuffer();
		boolean first = true;
		for(String str:ltl)
			if (str.startsWith(QSMTool.cmdLTL))
			{
				String formula = str.substring(QSMTool.cmdLTL.length()).trim();
				if (ltlForbiddenWords.matcher(formula).find())
					throw new IllegalArgumentException("expression "+formula+" contains a forbidden word");
				if (formula.length() > 0)
				{
					if (!first) ltlCombined.append(" || ");else first=false;
					ltlCombined.append(formula);
				}
			}
		return ltlCombined;
	}
	
	public static final String 
		baStart="never", 
		baError = "ltl2ba",baSimpleComment="\\s*/\\*.*\\*/\\n*";
	
	private final Configuration config;
	private final ConvertALabel converter;
	protected final LearnerGraphND matrixFromLTL;
	
	public LearnerGraphND getLTLgraph()
	{
		return matrixFromLTL;
	}
	
	/** Constructs class which will use LTL to augment the supplied graph.
	 * 
	 * @param cnf configuration to use
	 * @param conv converter to use to intern labels.
	 */
	public LTL_to_ba(Configuration cnf, ConvertALabel conv)
	{
		config = cnf;converter = conv;
		matrixFromLTL = new LearnerGraphND(config);
	}
		
	private static final int lexSTART =8;
	private static final int lexEND =11;
	private static final int lexFALSE =9;
	private static final int lexCOMMENT =10;
	private static final int lexIF =6;
	private static final int lexFI =7;
	private static final int lexSTATE = 4;
	private static final int lexSTATELABEL = 5;
	private static final int lexTRANSITION = 1;
	private static final int lexTRANSITIONLABEL =2;
	private static final int lexTRANSITIONTARGET =3;
	private static final int lexSKIP = 12;

	public static class Lexer 
	{
		private String text = null;
		private Matcher lexer = null;
	
		private String lastMatch = null;
		private int lastMatchNumber = -1;
		private final Pattern pattern;
		
		/** Constructs the lexer.
		 * 
		 * @param grammar grammar to deal with
		 * @param textToAnalyse what to lexically analyse
		 */
		public Lexer(String grammar, String textToAnalyse)
		{
			this(grammar);startParsing(textToAnalyse);
		}
		
		/** Constructs a lexer without committing to text to analyse - very good for reusable parsers
		 * where sequences are built from text extracted from XML.
		 * 
		 * @param grammar
		 */
		public Lexer(String grammar)
		{
			pattern = Pattern.compile(grammar);text = null;lexer = null;lastMatch = null;lastMatchNumber =-1;
		}
		
		/** Starts the lexing process on the supplied the chunk of text.
		 *  
		 * @param data what to lex.
		 */
		public void startParsing(String data)
		{
			lastMatch = null;lastMatchNumber =-1;
			text = data;lexer = pattern.matcher(text);
		}
		
		public void throwException(String errMsg)
		{
			throw new IllegalArgumentException(errMsg+" starting from \""+text.substring(lexer.regionStart())+"\"");
		}
		
		public int getMatchType()
		{
			int result = -1;
			if(lastMatchNumber >=0)
			{// Switch lexing to the next part of the input text
				try
				{
					lexer.region(lexer.end(lastMatchNumber),lexer.regionEnd());
				}
				catch(IllegalStateException e)
				{
					throwException(e.getMessage()+" when lexing "+text);
				}
			}
			
			if (lexer.regionStart() < lexer.regionEnd())
			{// not run out of elements, ask lexer to do it.
				if (!lexer.lookingAt())
					throwException("failed to lex");
	
				// Now find what has matched
				int i=1;
				while(i<lexer.groupCount()+1 && lexer.group(i) == null) ++i;
				if (i == lexer.groupCount()+1)
					throwException("failed to lex (group number is out of boundary)");
		
				lastMatch = lexer.group(i);
				lastMatchNumber = i;result = i;
			}
			else
			{// record the fact we reached the end of the text to parse.
				lastMatchNumber = -1;
				lastMatch = null;
			}
			
			return result;
		}
		
		public String remaining()
		{
			return text.substring(lexer.regionStart());
		}
		
		public int getLastMatchType()
		{
			return lastMatchNumber;
		}
		
		public String getMatch()
		{
			return lastMatch;
		}
		
		public String group(int i)
		{
			return lexer.group(i);
		}
		
		public String getText()
		{
			return text;
		}
	}
	
	/** Maps names of vertices to the corresponding "real" vertices. */
	private Map<String,CmpVertex> verticesUsed = new HashMap<String,CmpVertex>(); 
	private int vertexCounter = 1;
	
	/** Adds a state to the graph and the transition matrix.
	 * If a state already exists, it is returned.
	 *  
	 * @param name for a state
	 * @return the vertex corresponding to it. 
	 */
	private CmpVertex addState(String name)
	{
		CmpVertex vert = verticesUsed.get(name);

		if (vert == null)
		{// add new vertex
			vert = AbstractLearnerGraph.generateNewCmpVertex(new VertexID(VertKind.NEUTRAL,vertexCounter++), config);
			vert.setAccept(name.startsWith("accept_"));vert.setColour(LearnerGraphND.ltlColour);
			matrixFromLTL.transitionMatrix.put(vert,matrixFromLTL.createNewRow());
			verticesUsed.put(name, vert);
		}
		return vert;
	}
	
	/** Looks through all the states for the one matching the supplied name and returns the corresponding vertex.
	 * 
	 * @param nameToSearchFor name of the initial state.
	 * @param map of names to vertices map to search through.
	 * @return the found vertex. 
	 * @throws IllegalArgumentException if a vertex was not found.
	 */
	public static CmpVertex findInitialState(String nameToSearchFor,Map<String,CmpVertex> map)
	{
		CmpVertex vertexFound = null;
		for(Entry<String,CmpVertex> entry:map.entrySet())
			if (entry.getKey().contains(nameToSearchFor))
			{
				vertexFound = entry.getValue();break;
			}
		if (vertexFound == null)
			throw new IllegalArgumentException("missing state");
		
		return vertexFound;
	}

	/** The name of the initial state given by ltl2ba. */
	public static final String initStateName = "init";
	
	/** Parses the output of LTL2BA, with the aim to extract a buchi automaton.
	 * Throws an {@link IllegalArgumentException} if something goes wrong.
	 * Non-deterministic automata extracted from LTL is stored in the <em>matrix</em> array.
	 *  
	 * @param output
	 */
	public void parse(String whatToParse)
	{
		Lexer lexer = new Lexer(
			"(\\s*::\\s*([^\\-]+)\\s+\\->\\s*goto\\s+(\\w+)\\s+)|"+// if conditional
				"(\\s*(\\w+)\\s*:\\s)"+"|"+// state name
				"(\\s*if\\s*)"+"|"+ // if opening statement, 6
				"(\\s*fi\\s*;\\s*)"+"|"+ // if closing statement, 7
				"("+baStart+"\\s*\\{\\s*)"+"|"+ // start, 8
				"(\\s*false\\s*;\\s+)"+"|"+ // false statement, 9
				"("+baSimpleComment+")"+"|"+ // comment, 10
				"(\\s*\\}\\s*)"+"|"+ // end of the claim, 11
				"(\\s*skip\\s*)" // skip statement, 12
				,whatToParse+"\n");
		int currentMatch = lexer.getMatchType();
		if (currentMatch == lexSTATE && baError.equals(lexer.group(lexSTATELABEL)))
			lexer.throwException("syntax error reported by ltl2ba");
		if (currentMatch != lexSTART)
			 lexer.throwException("failed to find the start of automaton");
		currentMatch = lexer.getMatchType();
		int state = lexSTART;
		CmpVertex currentState = null;
		while(currentMatch>=0)
		{
			if (currentMatch != lexCOMMENT)
			switch(state)
			{
			case lexSTART:
				switch(currentMatch)
				{
				case lexSTATE:
					state = lexIF;
					currentState=addState(lexer.group(lexSTATELABEL));break;
				default:
					lexer.throwException("unexpected token type "+currentMatch);
				}
				break;
			case lexEND:
				if (currentMatch != lexEND)
					lexer.throwException("expected end "+currentMatch);
				state = -1;// we should not get any more tokens
				break;
			case lexSTATE:
				switch(currentMatch)
				{
				case lexSTATE:
					state = lexIF;
					currentState=addState(lexer.group(lexSTATELABEL));break;
				case lexEND:
					state = -1;break;// we should not get any more tokens
				default:
					lexer.throwException("unexpected lexSTATE token type "+currentMatch);
				}
				break;
			case lexIF:
				switch(currentMatch)
				{
				case lexFALSE:
					state = lexSTATE;// expect next if
					break;
				case lexSKIP:
					addTransitionsBetweenStates(currentState,"1",currentState);
					state = lexSTATE;
					break;
				case lexIF:
					state = lexTRANSITION;break;
				default:
					lexer.throwException("expected if, skip or false"+currentMatch);
				}
				break;
			case lexTRANSITION:
				switch(currentMatch)
				{
				case lexTRANSITION:
					addTransitionsBetweenStates(currentState,lexer.group(lexTRANSITIONLABEL),addState(lexer.group(lexTRANSITIONTARGET)));
					break;
				case lexFI:
						state = lexSTATE;break;
				default:
					lexer.throwException("unexpected lexTRANSITION token type "+currentMatch);
				}
				break;
			default:
				lexer.throwException("unexpected state "+state);
			}
				
			currentMatch = lexer.getMatchType();
		}
		
		
		matrixFromLTL.setInit(findInitialState(initStateName, verticesUsed));
		//Visualiser.updateFrame(matrixFromLTL, null);
	}
	
	/** Takes a SPIN transition label and a target state and adds transitions from the given state to the target state,
	 * interpreting the expression on the label.
	 * 
	 * @param currentState the current state
	 * @param transitionLabel the SPIN label
	 * @param targetState target state
	 */
	protected void addTransitionsBetweenStates(CmpVertex currentState, String transitionLabel, CmpVertex targetState)
	{
		Map<Label,List<CmpVertex>> row = matrixFromLTL.transitionMatrix.get(currentState);
		for(Label currLabel:interpretString(transitionLabel))
		{
			List<CmpVertex> targetList = row.get(currLabel);
			if (targetList == null)
			{
				targetList = new LinkedList<CmpVertex>();row.put(currLabel, targetList);
			}
			targetList.add(targetState);
		}
	}
	
	/** Alphabet of a graph we'd like to augment with LTL. Described as a map in order to intern labels. */
	protected Map<Label,Label> alphabet = null;
	
	void setAlphabet(Collection<Label> alph)
	{
		alphabet = new TreeMap<Label,Label>();for(Label lbl:alph) alphabet.put(lbl, lbl);
	}
		
	enum OPERATION { AND,OR,NEG,ASSIGN }
	
	public static final int exprOpen = 1;
	public static final int exprClose =2;
	public static final int exprAND = 3;
	public static final int exprOR = 4;
	public static final int exprNEG = 5;
	public static final int exprWord = 6;
	public static final int exprWordText = 7;
	
	/** Lexical analyser for embedded context-free expressions. */
	protected Lexer lexExpr = null;

	protected void buildExprLexer(String data)
	{
		lexExpr = new Lexer("(\\s*\\(\\s*)|(\\s*\\)\\s*)|(\\s*&&\\s*)|(\\s*\\|\\|\\s*)|(\\s*!\\s*)|(\\s*(\\w+)\\s*)",data);
	}
	
	/** Given a composite transition label, this method converts it into a corresponding set of labels. */
	Set<Label> interpretString(String data)
	{
		buildExprLexer(data+")");
		Set<Label> result = interpretExpression();
		if (lexExpr.getMatchType() >=0)
			throw new IllegalArgumentException("extra tokens at the end of expression");
		return result;
	}
	
	/** Performs a lexical analysis and extracts the alphabet. */
	Set<Label> computeAlphabet(String data)
	{
		buildExprLexer(data+")");
		Set<Label> result = computeAlphabet();
		if (lexExpr.getMatchType() >=0)
			throw new IllegalArgumentException("extra tokens at the end of expression");

		return result;
	}
	
	private Set<Label> computeAlphabet()
	{
		int currentMatch = lexExpr.getMatchType();
		if (currentMatch < 0 || currentMatch == exprClose)
			throw new IllegalArgumentException("unexpected end of expression");
		
		Set<Label> currentValue = new TreeSet<Label>();// the alphabet constructed so far.

		while(currentMatch >= 0 && currentMatch != exprClose)
		{
			switch(currentMatch)
			{
			case exprOpen: // embedded expression
				currentValue.addAll(computeAlphabet());
				break;
			case exprAND:
				break;
			case exprOR:
				break;
			case exprNEG:
				break;
			case exprWord:
				currentValue.add(AbstractLearnerGraph.generateNewLabel(lexExpr.group(exprWordText),config,converter));
				break;
			default:
				throw new IllegalArgumentException("invalid token "+currentMatch+", looking at "+lexExpr.getMatch());
			}
			currentMatch = lexExpr.getMatchType();
		}

		return currentValue;
	}
	
	/** Given an expression with brackets, && and ||, this one interprets it as a set of labels. */
	protected Set<Label> interpretExpression()
	{
		int currentMatch = lexExpr.getMatchType();
		if (currentMatch < 0 || currentMatch == exprClose)
			throw new IllegalArgumentException("unexpected end of expression");
		
		boolean expectWord = true;// this means that we are waiting for a word
		
		Set<Label> currentValue = new TreeSet<Label>();// the outcome of the left-hand side.

		OPERATION currentOperation = OPERATION.ASSIGN;
		while(currentMatch >= 0 && currentMatch != exprClose)
		{
			switch(currentMatch)
			{
			case exprOpen: // embedded expression
				if (!expectWord)
					throw new IllegalArgumentException("expected binary operation instead of "+lexExpr.getMatch());
				performOperation(currentValue, currentOperation, interpretExpression());
				expectWord = false;
				break;
			case exprAND:
				if (expectWord)
					throw new IllegalArgumentException("expected word instead of "+lexExpr.getMatch());
				currentOperation = OPERATION.AND;expectWord = true;
				break;
			case exprOR:
				if (expectWord)
					throw new IllegalArgumentException("expected word instead of "+lexExpr.getMatch());
				currentOperation = OPERATION.OR;expectWord = true;
				break;
			case exprNEG:
				if (!expectWord)
					throw new IllegalArgumentException("expected binary operation instead of "+lexExpr.getMatch());
				Set<Label> tmp = new TreeSet<Label>();performOperation(tmp,OPERATION.NEG,interpretUnary());
				performOperation(currentValue, currentOperation, tmp);
				currentOperation = OPERATION.ASSIGN;expectWord = false;
				break;
			case exprWord:
				if (!expectWord)
					throw new IllegalArgumentException("expected binary operation instead of "+lexExpr.getMatch());
				performOperation(currentValue, currentOperation, interpretInputLabel(lexExpr.group(exprWordText)));
				currentOperation = OPERATION.ASSIGN;expectWord = false;
				break;
			default:
				throw new IllegalArgumentException("invalid token "+currentMatch+", looking at "+lexExpr.getMatch());
			}
			currentMatch = lexExpr.getMatchType();
		}
		return currentValue;
	}
	
	protected Set<Label> interpretUnary()
	{
		int currentMatch = lexExpr.getMatchType();
		Set<Label> currentValue = new TreeSet<Label>();// the outcome of the left-hand side.
		switch(currentMatch)
		{
		case exprOpen: // expression in braces
			performOperation(currentValue, OPERATION.ASSIGN, interpretExpression());
			return currentValue;
		case exprNEG:
			performOperation(currentValue, OPERATION.NEG, interpretUnary());
			break;
		case exprWord:
			performOperation(currentValue, OPERATION.ASSIGN, interpretInputLabel(lexExpr.group(exprWordText)));
			break;
		default:
			throw new IllegalArgumentException("invalid token "+currentMatch+", looking at "+lexExpr.getMatch());
		}
		return currentValue;
	}
	
	/** The notation for each label in a BA is one of the following:
	 * "label", "1".
	 * 
	 * @param left the left-hand side and the receiver of the outcome of the operation.
	 * @param oper the operation to perform between the supplied sets,
	 * <em>null</em> means assignment of the right-hand side to the left-hand. 
	 * @param right the right-hand side
	 */
	protected void performOperation(Set<Label> left, OPERATION oper, Set<Label> right)
	{
		switch(oper)
		{
		case ASSIGN:
			left.clear();left.addAll(right);
			break;
		case NEG:
			left.clear();left.addAll(alphabet.keySet());left.removeAll(right);
			break;
		case AND:
			left.retainAll(right);break;
		case OR:
			left.addAll(right);break;
		}
	}
	
	
	public static class UnrecognisedLabelException extends IllegalArgumentException
	{
		/**
		 * ID for serialization
		 */
		private static final long serialVersionUID = -7687604869719653857L;
		protected String unrecognisedLabel;
		
		public UnrecognisedLabelException(String stringLabel)
		{
			super("unrecognised label "+stringLabel);unrecognisedLabel = stringLabel;
		}
		
		String getLabel()
		{
			return unrecognisedLabel;
		}
	}
	
	/** The notation for each label in a BA is one of the following:
	 * "label", "1".
	 * 
	 * @param stringLabel label to interpret, using an alphabet.
	 * @return result of interpretation.
	 * 
	 */
	protected Set<Label> interpretInputLabel(String stringLabel)
	{
		if (stringLabel == null)
			throw new IllegalArgumentException("empty label");
		
		Set<Label> result = new TreeSet<Label>();
		
		if (stringLabel.equals("1")) 
			result.addAll(alphabet.keySet());
		else
		{
			Label label=AbstractLearnerGraph.generateNewLabel(stringLabel,config,converter);
			if (!alphabet.containsKey(label))
				throw new UnrecognisedLabelException(stringLabel);
			result.add(alphabet.get(label));
		}
		return result;
	}
	
	/** Runs a supplied ltl formula through ltl2ba.
	 * The graph returned by ltl2ba is stored in the internal matrix. 
	 *
	 * @param ltl formula to run
	 * @param pathTo_ltl2ba path to executable lbl2ba, if null uses "ltl2ba".
	 */
	protected void runLTL2BA(String ltl, String pathTo_ltl2ba)
	{
		final StringBuffer converterOutput = new StringBuffer();
		try 
		{
			final Process ltlconverter = Runtime.getRuntime().exec(new String[]{pathTo_ltl2ba==null?"ltl2ba":pathTo_ltl2ba, "-f",ltl});// run LTL2BA
			ExperimentRunner.dumpStreams(ltlconverter,timeBetweenHearbeats,new HandleProcessIO() {

			@Override
			public void OnHeartBeat() {// no prodding is done for a short-running converter.
			}

			@Override
			public void StdErr(StringBuffer b) {
				System.err.print(b.toString());
			}

			@Override
			public void StdOut(StringBuffer b) {
				converterOutput.append(b);
			}});
			ltlconverter.waitFor();
		} catch (IOException e1) {
			statechum.Helper.throwUnchecked("failed to run ltl2ba", e1);
		} catch (InterruptedException e) {
			statechum.Helper.throwUnchecked("wait for ltl2ba to terminate aborted", e);
		}
		
		parse(converterOutput.toString());
	}

	/** Takes a collection of LTL formulae and builds the corresponding FSM,
	 * assuming the properties are all safety ones.
	 * 
	 * @param ltl formulas to run
	 * @param graph in order to correctly interpret symbols used by ltl2ba 
	 * such as "1", we need to be aware of the alphabet of an FSM being built. 
	 * @param invert if the ltl expression is to be inverted before passing it to ltl2ba.
	 * @param pathTo_ltl2ba path to ltl2ba executable, if null the default path will be used.
	 * @return false if there is no LTL to extract.
	 * @throws IncompatibleStatesException 
	 */
	public boolean ltlToBA(Collection<String> ltl, Collection<Label> alphabetToUse, boolean invert, String pathTo_ltl2ba)
	{
		if (alphabetToUse != null)
			setAlphabet(alphabetToUse);
		String ltlString = concatenateLTL(ltl).toString();
		if (ltlString.length() == 0)
			return false;
		
		runLTL2BA( (invert?"!":"")+"("+ltlString+")",pathTo_ltl2ba);
		for(CmpVertex v:matrixFromLTL.transitionMatrix.keySet())
			if (!v.isAccept())
				throw new IllegalArgumentException("not all states are accept-states");
		
		return true;
	}
	
}

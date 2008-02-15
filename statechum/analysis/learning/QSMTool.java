package statechum.analysis.learning;

/**
 * Takes a text file, structured as follows:
 * 
 * first line: either "active" or "passive" followed by \n
 * following lines:
 * strings that belong to the target machine:
 * + function1, function2...
 * + function1, function3...
 * and optionally strings that do NOT belong to the target machine:
 * -function1, function4
 * @author nw
 *
 */

import java.io.*;
import java.util.*;

import statechum.analysis.learning.oracles.*;

public class QSMTool {
	
	
	public static void main(String[] args){
		Set<List<String>> sPlus = new HashSet<List<String>>();
		Set<List<String>> sMinus = new HashSet<List<String>>();
		Set<String> ltl = new HashSet<String>();
		boolean active = true;
		try{
			BufferedReader in = new BufferedReader(new FileReader(args[0]));
			String fileString;
			String activePassive = in.readLine();
			if(activePassive.equalsIgnoreCase("passive"))
				active = false;
	        while ((fileString = in.readLine()) != null) {
	            process(fileString, sPlus, sMinus, ltl);
	        }
	        in.close();
		} 	catch (IOException e) {e.printStackTrace();}
		//new PickNegativesVisualiser(new SootCallGraphOracle()).construct(sPlus, sMinus,null, active);
		if(ltl.isEmpty())
			new PickNegativesVisualiser().construct(sPlus, sMinus,null, active);
		else
			new PickNegativesVisualiser().construct(sPlus, sMinus,ltl, null, active);
	}
	
	private static void process(String fileString, Set<List<String>>sPlus, Set<List<String>> sMinus, Set<String> ltl){
		if(fileString.trim().equalsIgnoreCase(""))
			return;
		StringTokenizer tokenizer = new StringTokenizer(fileString.substring(1));
		ArrayList<String> sequence = new ArrayList<String>();
		while(tokenizer.hasMoreTokens())
			sequence.add(tokenizer.nextToken());
		if(fileString.startsWith("+"))
			sPlus.add(sequence);
		else if(fileString.startsWith("-"))
			sMinus.add(sequence);
		else if(fileString.startsWith("ltl"))
			ltl.add(getLtlString(sequence));
			
	}
	
	private static String getLtlString(List<String> sequence){
		String expression = new String();
		for(int i=1; i<sequence.size();i++){
			expression = expression.concat(sequence.get(i));
		}
		return expression;
	}

}
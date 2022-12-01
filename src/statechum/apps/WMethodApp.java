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

package statechum.apps;

/*
 * Prints out the test set for a state machine as computed by Chow's W-Method (see Chow TSE article 1978).
 *
 * First argument - path to state machine file (as graphml - see examples in resources/TestGraphs)
 * Second argument (optional) - number of extra states you want to test for - default is zero.
 */

import statechum.Configuration;
import statechum.Helper;
import statechum.Label;
import statechum.analysis.learning.AbstractOracle;
import statechum.analysis.learning.rpnicore.FsmParserDot;
import statechum.analysis.learning.rpnicore.LearnerGraph;
import statechum.analysis.learning.rpnicore.Transform;

import java.io.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WMethodApp {
    public static final Configuration config = Configuration.getDefaultConfiguration().copy();
    /**
     * Label converter to use.
     */
    public static final Transform.ConvertALabel converter = new Transform.InternStringLabel();

    public static void main(String[] args) {

        try {
            String specDot = Helper.loadFile(new File(args[0]));
            LearnerGraph reference = FsmParserDot.buildLearnerGraph(specDot, "reference", config, converter, true);
            int k = 0;
            if (args.length > 1) {
                k = Integer.parseInt(args[1]);
            }
            Collection<List<Label>> testSet = reference.wmethod.getFullTestSet(k);
//            printTests(testSet);
//            FileWriter fileWriter = new FileWriter("tests.txt", false);
//            BufferedWriter testWriter = new BufferedWriter(fileWriter);
//            testWriter.write("Size of test suite: " + test_string.size());
//            testWriter.newLine();
            List<String> test_string = stringTests(testSet).collect(Collectors.toList());
            PrintWriter testWriter
                    = new PrintWriter(new BufferedWriter(new FileWriter("tests.txt", false)));
            for (String test : test_string) {
                System.out.println(test);
                testWriter.println(test);
            }
            testWriter.close();
        } catch (IOException ex) {
            Helper.throwUnchecked("failed to load graph", ex);
        }
    }

    private static void printTests(Collection<List<Label>> testSet) {
        stringTests(testSet).forEach(System.out::println);
    }

    private static Stream<String> stringTests(Collection<List<Label>> testSet) {
        return testSet.stream().map(WMethodApp::toStringLabel);
    }

    private static String toStringLabel(List<Label> test) {
        StringBuilder ret = new StringBuilder();
        for (Label io : test) {
            int x = io.toString().indexOf('/');
            String in = io.toString().substring(0, x);
            ret.append(in);
            ret.append(" ");
        }
        return ret.toString();
    }

    private static void displayTests(Collection<List<Label>> tests, LearnerGraph g) {
        Iterator<List<Label>> testIt = tests.iterator();
        int count = 0;
        while (testIt.hasNext()) {
            count++;
            List<Label> test = testIt.next();
            int accept = g.paths.tracePathPrefixClosed(test);
            System.out.print("#" + count + " ");
            if (accept == AbstractOracle.USER_ACCEPTED)
                System.out.println("ACCEPTED: " + test);
            else
                formatRejectString(test, accept);
        }
    }

    private static void formatRejectString(List<Label> test, int rejectPoint) {
        System.out.println("ACCEPT: " + test.subList(0, rejectPoint));
        System.out.println("\tREJECT: " + test.get(rejectPoint));
    }

}

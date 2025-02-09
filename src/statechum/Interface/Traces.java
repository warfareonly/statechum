/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * Traces.java
 *
 * Created on Apr 14, 2011, 3:01:10 PM
 */
package statechum.Interface;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import statechum.analysis.Erlang.ErlangLabel;
import statechum.analysis.learning.ErlangOracleLearner;
import statechum.analysis.learning.ErlangOracleVisualiser;
import statechum.analysis.learning.rpnicore.LTL_to_ba.Lexer;
import statechum.apps.ErlangQSMOracle;
import statechum.apps.QSMTool;

/**
 * 
 * @author ramsay
 */
public class Traces extends javax.swing.JFrame {

	/**
	 * ID for serialization
	 */
	private static final long serialVersionUID = 7083317420588568865L;
	
	protected static ArrayList<String> alphabet;
	protected static String filename;
	protected static int poscount;
	protected static int negcount;

	protected static String module;

	public static void setModule(String mod) {
		module = mod;
	}

	/** Creates new form Traces */
	public Traces() {
		initComponents();
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		filenameLabel.setText(filename);
		tracecountLabel.setText("" + (poscount + negcount) + " (" + poscount
				+ " pos, " + negcount + " neg)");

		JLabel ta = new JLabel("<html>");
		for (String a : alphabet) {
			if (!ta.getText().equals("<html>")) {
				ta.setText(ta.getText() + "<br />");
			}
			ta.setText(ta.getText() + a);
		}
		ta.setText(ta.getText() + "</html>");
		alphabetPane.getViewport().removeAll();
		alphabetPane.getViewport().add(ta, BorderLayout.CENTER);

		this.setTitle(filename);

	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed"
	// <editor-fold defaultstate="collapsed"
	// desc="Generated Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		jLabel1 = new javax.swing.JLabel();
		filenameLabel = new javax.swing.JLabel();
		jLabel2 = new javax.swing.JLabel();
		tracecountLabel = new javax.swing.JLabel();
		jLabel3 = new javax.swing.JLabel();
		alphabetPane = new javax.swing.JScrollPane();
		jSeparator1 = new javax.swing.JSeparator();
		jButton1 = new javax.swing.JButton();
		jButton2 = new javax.swing.JButton();
		jSeparator2 = new javax.swing.JSeparator();
		jLabel4 = new javax.swing.JLabel();
		moduleLabel = new javax.swing.JLabel();
		suspendLearner = new JCheckBox("Suspend", false);
		terminateLearner = new javax.swing.JButton("Stop");
		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

		jLabel1.setText("Trace file name:");

		filenameLabel.setText("filename");

		jLabel2.setText("Traces found:");

		tracecountLabel.setText("traces");

		jLabel3.setText("Alphabet:");

		jButton1.setText("Run QSM in manual mode");
		jButton1.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton1ActionPerformed(evt);
			}
		});

		jButton2.setText("Run QSM");
		jButton2.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton2ActionPerformed(evt);
			}
		});
		terminateLearner.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				terminateActionPerformed(evt);
			}
		});
		suspendLearner.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				suspendActionPerformed(evt);
			}
		});
		terminateLearner.setEnabled(false);suspendLearner.setEnabled(false);
		jLabel4.setText("Module file:");

		moduleLabel.setText(module);

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(
				getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(
						layout.createSequentialGroup()
								.addContainerGap()
								.addGroup(
										layout.createParallelGroup(
												javax.swing.GroupLayout.Alignment.LEADING)
												.addComponent(
														alphabetPane,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														572, Short.MAX_VALUE)
												.addGroup(
														layout.createSequentialGroup()
																.addComponent(
																		jLabel4)
																.addPreferredGap(
																		javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
																.addComponent(
																		moduleLabel))
												.addGroup(
														layout.createSequentialGroup()
																.addComponent(
																		jLabel1)
																.addPreferredGap(
																		javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
																.addComponent(
																		filenameLabel))
												.addGroup(
														layout.createSequentialGroup()
																.addComponent(
																		jLabel2)
																.addGap(28, 28,
																		28)
																.addComponent(
																		tracecountLabel)))
								.addContainerGap())
				.addComponent(jSeparator1,
						javax.swing.GroupLayout.DEFAULT_SIZE, 612,
						Short.MAX_VALUE)
				.addComponent(jSeparator2,
						javax.swing.GroupLayout.DEFAULT_SIZE, 612,
						Short.MAX_VALUE)
				.addGroup(
						layout.createSequentialGroup().addContainerGap()
								.addComponent(jButton1)
								.addContainerGap(390, Short.MAX_VALUE))
				.addGroup(
						layout.createSequentialGroup().addContainerGap()
						.addComponent(jButton2)
						.addComponent(suspendLearner).addComponent(terminateLearner)
								.addContainerGap(255, Short.MAX_VALUE))/*
				.addGroup(
						layout.createSequentialGroup().addContainerGap()
								.addComponent(suspendLearner)
								.addContainerGap(30, Short.MAX_VALUE))
				.addGroup(
						layout.createSequentialGroup().addContainerGap()
								.addComponent(terminateLearner)
								.addContainerGap(30, Short.MAX_VALUE))*/
				.addGroup(
						layout.createSequentialGroup().addContainerGap()
								.addComponent(jLabel3)
								.addContainerGap(532, Short.MAX_VALUE)));
		layout.setVerticalGroup(layout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(
						layout.createSequentialGroup()
								.addGap(9, 9, 9)
								.addGroup(
										layout.createParallelGroup(
												javax.swing.GroupLayout.Alignment.BASELINE)
												.addComponent(jLabel1)
												.addComponent(filenameLabel))
								.addPreferredGap(
										javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addGroup(
										layout.createParallelGroup(
												javax.swing.GroupLayout.Alignment.BASELINE)
												.addComponent(jLabel4)
												.addComponent(moduleLabel))
								.addPreferredGap(
										javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addGroup(
										layout.createParallelGroup(
												javax.swing.GroupLayout.Alignment.BASELINE)
												.addComponent(jLabel2)
												.addComponent(tracecountLabel))
								.addPreferredGap(
										javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(jLabel3)
								.addPreferredGap(
										javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(alphabetPane,
										javax.swing.GroupLayout.PREFERRED_SIZE,
										186,
										javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(
										javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(jSeparator1,
										javax.swing.GroupLayout.PREFERRED_SIZE,
										10,
										javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(
										javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(jButton1)
								.addGap(18, 18, 18)
								.addComponent(jSeparator2,
										javax.swing.GroupLayout.PREFERRED_SIZE,
										10,
										javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(
										javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(jButton2)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(suspendLearner)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(terminateLearner)
										//
								/*
								.addContainerGap(
												javax.swing.GroupLayout.DEFAULT_SIZE,
												Short.MAX_VALUE)
								.addComponent(terminateLearner)
								.addContainerGap(
														javax.swing.GroupLayout.DEFAULT_SIZE,
														Short.MAX_VALUE)*/
														
						));

		pack();
	}// </editor-fold>//GEN-END:initComponents

	protected Thread learnerThread = null;
	protected ErlangOracleLearner innerLearner = null;
	
	protected void jButton2ActionPerformed(@SuppressWarnings("unused") ActionEvent evt) {
		if (learnerThread != null)
			return;
		ErlangOracleVisualiser viz = new ErlangOracleVisualiser();
		innerLearner = ErlangQSMOracle.createLearner(viz,filename);
		innerLearner.addObserver(viz);
		//innerLearner.setGraphName(new File(filename).getName());
		innerLearner.getTentativeAutomaton().getLayoutOptions().showNegatives = false;
		learnerThread = new Thread(new Runnable() {

			@Override
			public void run() {
				innerLearner.learnMachine();
				learnerThread = null;innerLearner = null;
				
				SwingUtilities.invokeLater(new Runnable(){

					@Override
					public void run() {
						jButton2.setEnabled(true);suspendLearner.setEnabled(false);terminateLearner.setEnabled(false);
					}
					
				});
				
			}
			
		});
		jButton2.setEnabled(false);suspendLearner.setEnabled(true);terminateLearner.setEnabled(true);
		learnerThread.start();
	}

	protected void notifyLearner()
	{
		if (innerLearner != null)
			synchronized(innerLearner.suspendInference)
			{
				innerLearner.suspendInference.notify();
			}
	}
	
	protected void terminateActionPerformed(@SuppressWarnings("unused") ActionEvent evt) {
		if (learnerThread == null)
			return;
		innerLearner.stopInference.set(true);
		innerLearner.suspendInference.set(false);
		notifyLearner();
		try {
			learnerThread.join();
		} catch (InterruptedException e) {
			// assume we were asked to stop waiting
		}
	}

	protected void suspendActionPerformed(@SuppressWarnings("unused") ActionEvent evt) {
		if (learnerThread == null || innerLearner == null)
			return;
		innerLearner.suspendInference.set(suspendLearner.isSelected());notifyLearner();
	}

	void jButton1ActionPerformed(@SuppressWarnings("unused") java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton1ActionPerformed
		QSMTool.main(new String[] { filename });
	}// GEN-LAST:event_jButton1ActionPerformed

	protected static void addAlphabetElements(String line) {
		String[] elems = line.split(" ");
		for (String e : elems) {
			if (!alphabet.contains(e)) {
				alphabet.add(e);
			}
		}
	}

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String args[]) {

		// Load and check the Traces file
		if (args.length < 1) {
			throw new RuntimeException("No traces file supplied!");
		}
		filename = args[0];
		poscount = 0;
		negcount = 0;
		alphabet = new ArrayList<String>();

		try {
			BufferedReader input = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = input.readLine()) != null) {
				boolean skip = false;
				line = line.trim();
				if (line.startsWith("+")) {
					poscount++;
					line = line.substring(1);
				} else if (line.startsWith("-")) {
					negcount++;
					line = line.substring(1);
				} else {
					if (line.startsWith("#ErlangModule ")) {
						System.out.println("GOT AN ERLANG MODULE: "
								+ line.substring("#ErlangModule ".length()));
						setModule(line.substring("#ErlangModule ".length()));
					}
					skip = true;
				}
				if (!skip) {
					// line = line.substring(1);
					// Match lists of traces
					line = line.trim();
					if (line.startsWith("[")) {
						// strip the start and end brackets
						line = line.substring(1, line.length() - 1).trim();
						Lexer lexer = ErlangLabel.buildLexer(line);
						int match = lexer.getMatchType();

						try {
							while (match > 0) {
								String s = ErlangLabel.parseFirstTermInText(
										lexer).toString();
								addAlphabetElements(s);
								match = lexer.getLastMatchType();
							}
						} catch (IllegalArgumentException e) {
							// we are done...
						}
					}
				}
			}
			input.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		java.awt.EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				new Traces().setVisible(true);
			}
		});
	}

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JScrollPane alphabetPane;
	private javax.swing.JLabel filenameLabel;
	private javax.swing.JButton jButton1;
	javax.swing.JButton jButton2;
	private javax.swing.JLabel jLabel1;
	private javax.swing.JLabel jLabel2;
	private javax.swing.JLabel jLabel3;
	private javax.swing.JLabel jLabel4;
	private javax.swing.JSeparator jSeparator1;
	private javax.swing.JSeparator jSeparator2;
	private javax.swing.JLabel moduleLabel;
	private javax.swing.JLabel tracecountLabel;
	javax.swing.JCheckBox suspendLearner;
	javax.swing.JButton terminateLearner;
	
	// End of variables declaration//GEN-END:variables
}


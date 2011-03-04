/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ErlangApplicationLoader.java
 *
 * Created on Feb 16, 2011, 10:16:50 AM
 */
package statechum.apps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import statechum.analysis.Erlang.ErlangApp;
import statechum.analysis.Erlang.ErlangAppReader;
import statechum.analysis.Erlang.ErlangModule;
import statechum.analysis.learning.experiments.ExperimentRunner;
import statechum.analysis.learning.experiments.ExperimentRunner.HandleProcessIO;
import statechum.analysis.learning.rpnicore.LTL_to_ba;

/**
 *
 * @author ramsay
 */
public class ErlangApplicationLoader extends javax.swing.JFrame {

    protected ErlangApp app;
    protected File folder;

    /** Creates new form ErlangApplicationLoader */
    public ErlangApplicationLoader() {
        super("StateChum Erlang Application Loader");
        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        appFile = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        beginButton = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        startModule = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        startModuleArgs = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        modules = new javax.swing.JList();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setText("App file:");

        jButton1.setText("...");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        beginButton.setText("Begin");
        beginButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                beginButtonActionPerformed(evt);
            }
        });

        jLabel2.setText("Start Module:");

        jLabel3.setText("Start Args:");

        jLabel4.setText("Modules:");

        jScrollPane1.setViewportView(modules);

        jButton2.setText("View");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setText("Reload");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(appFile, javax.swing.GroupLayout.DEFAULT_SIZE, 720, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(startModuleArgs, javax.swing.GroupLayout.PREFERRED_SIZE, 211, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(startModule, javax.swing.GroupLayout.PREFERRED_SIZE, 211, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 863, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jButton2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(beginButton)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(appFile, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1)
                    .addComponent(jButton3))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(startModule, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(startModuleArgs, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 434, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(beginButton)
                    .addComponent(jButton2))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    protected File selectedFile;

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        JFileChooser chooser = new JFileChooser();
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(new ErlangApplicationLoader.appFilter());
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int returnValue = chooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            selectedFile = chooser.getSelectedFile();
            appFile.setText(selectedFile.getName());
            loadData();
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    protected void loadData() {
        if (selectedFile.getName().endsWith(".app")) {
            folder = selectedFile.getParentFile();
            app = ErlangAppReader.readAppFile(selectedFile.getName(), folder);
        } else {
            folder = selectedFile;
            app = ErlangAppReader.readFolder(selectedFile);
        }
        startModule.setText(app.startModule);
        startModuleArgs.setText(app.startModuleArgs);
        DefaultListModel model = new DefaultListModel();
        for (ErlangModule m : app.modules) {
            model.addElement(m);
        }
        modules.setModel(model);

    }

    public static void dumpProcessOutput(Process p) {
        ExperimentRunner.dumpStreams(p, LTL_to_ba.timeBetweenHearbeats, new HandleProcessIO() {

            @Override
            public void OnHeartBeat() {// no prodding is done for a short-running converter.
            }

            @Override
            public void StdErr(@SuppressWarnings("unused") StringBuffer b) {
                System.err.print(b.toString());
            }

            @Override
            public void StdOut(@SuppressWarnings("unused") StringBuffer b) {
                System.out.print(b.toString());
            }
        });
        try {
            p.waitFor();
        } catch (InterruptedException e) {
            ;
        }
    }

    private void fileCopy(File from, File folder) {
        System.out.print("Copying " + from.toString() + " to " + folder.getAbsolutePath() + "/" + from.getName() + "...");

        try {
            FileReader in = new FileReader(from);
            FileWriter out = new FileWriter(new File(folder.getAbsolutePath() + "/" + from.getName()));
            int c;
            int count = 0;
            while ((c = in.read()) != -1) {
                out.write(c);
                count += 1;
            }
            System.out.println(count + " bytes");
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void beginButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_beginButtonActionPerformed
        // Copy in our generic stubs...
        try {
            for (File f : folder.listFiles()) {
                int dot = f.getName().lastIndexOf(".");
                if (dot >= 0) {
                    if (f.getName().substring(dot).equals(".erl")) {
                        dumpProcessOutput(Runtime.getRuntime().exec("erlc +debug_info " + f.getName(), null, folder));
                        //dumpProcessOutput(Runtime.getRuntime().exec("dialyzer --build_plt " + f.getName().replace(".erl", ".beam"), null, folder));
                        //dumpProcessOutput(Runtime.getRuntime().exec("typer " + f.getName(), null, folder));
                    }
                }
            }
            File ErlangFolder = new File("ErlangOracle");
            for (File f : ErlangFolder.listFiles()) {
                int dot = f.getName().lastIndexOf(".");
                if (dot >= 0) {
                    if (f.getName().substring(dot).equals(".erl")) {
                        fileCopy(f, folder);
                        dumpProcessOutput(Runtime.getRuntime().exec("erlc +debug_info " + f.getName(), null, folder));
                    }
                }
            }
            //dumpProcessOutput(Runtime.getRuntime().exec("ls " + (new File("ErlangOracle")).getAbsolutePath() + "/*.beam " + folder.getCanonicalPath() + "/", null, null));

            for (Object s : modules.getSelectedValues()) {
                ErlangModule m = (ErlangModule) s;
                // Create an Erlang QSM jobby for the selected behaviours...
                String otherModules = "";
                for (ErlangModule mm : app.modules) {
                    if (!otherModules.equals("")) {
                        otherModules += ",";
                    }
                    otherModules += mm.name;
                }
                try {
                    ErlangOracleRunner runner = new ErlangOracleRunner(folder.getCanonicalPath(), m, otherModules);
                    Thread t = new Thread(runner);
                    t.run();
                    while (t.isAlive()) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            ;
                        }
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
            for (File f : ErlangFolder.listFiles()) {
                int dot = f.getName().lastIndexOf(".");
                if (dot >= 0) {
                    if (f.getName().substring(dot).equals(".erl")) {
                        //System.out.println("########### deleting " + folder.getCanonicalPath() + "/" + f.getName());
                        (new File(folder.getCanonicalPath() + "/" + f.getName())).delete();
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

}//GEN-LAST:event_beginButtonActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        for (Object s : modules.getSelectedValues()) {
            ErlangModuleViewer view = new ErlangModuleViewer((ErlangModule) s);
            view.pack();
            view.setVisible(true);

        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        loadData();
    }//GEN-LAST:event_jButton3ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new ErlangApplicationLoader().setVisible(true);






            }
        });






    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel appFile;
    private javax.swing.JButton beginButton;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JList modules;
    private javax.swing.JLabel startModule;
    private javax.swing.JLabel startModuleArgs;
    // End of variables declaration//GEN-END:variables

    class appFilter extends FileFilter {

        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }
            int i = f.getName().lastIndexOf(".");
            if (i >= 0) {
                String extension = f.getName().substring(i).toLowerCase();
                if (extension.equals(".app")) {
                    return true;
                }
            }
            return false;
        }

        //The description of this filter
        public String getDescription() {
            return "Just .app files";
        }
    }
}
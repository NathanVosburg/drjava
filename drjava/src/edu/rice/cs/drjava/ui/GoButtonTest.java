package edu.rice.cs.drjava.ui;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import junit.framework.TestCase;
import edu.rice.cs.drjava.model.DummyGlobalModelListener;
import edu.rice.cs.util.FileOpenSelector;

/** Integration coverage for the toolbar's compile-and-run action. */
public class GoButtonTest extends TestCase {
  private MainFrame frame;
  private File directory;

  protected void setUp() throws Exception {
    directory = Files.createTempDirectory("drjava-go-").toFile();
    SwingUtilities.invokeAndWait(new Runnable() {
      public void run() {
        edu.rice.cs.drjava.DrJava.getConfig().resetToDefaults();
        frame = new MainFrame(); frame.pack();
      }
    });
    awaitEnabled();
  }

  protected void tearDown() throws Exception {
    if (frame != null) SwingUtilities.invokeAndWait(new Runnable() {
      public void run() { frame.dispose(); }
    });
    for (File file : directory.listFiles()) file.delete();
    directory.delete();
  }

  private void awaitEnabled() throws Exception {
    long deadline = System.currentTimeMillis() + 30000;
    final boolean[] enabled = new boolean[1];
    while (System.currentTimeMillis() < deadline) {
      SwingUtilities.invokeAndWait(new Runnable() {
        public void run() { enabled[0] = frame.getGoButton().isEnabled(); }
      });
      if (enabled[0]) return;
      Thread.sleep(50);
    }
    fail("Go did not become enabled");
  }

  private void open(String source) throws Exception {
    final File file = new File(directory, "GoExample.java");
    Files.write(file.toPath(), source.getBytes(StandardCharsets.UTF_8));
    SwingUtilities.invokeAndWait(new Runnable() {
      public void run() {
        frame.open(new FileOpenSelector() {
          public File[] getFiles() { return new File[] { file }; }
        });
      }
    });
  }

  public void testGoCompilesAndRuns() throws Exception {
    open("public class GoExample { public static void main(String[] args) { System.out.println(\"GO_TEST_OK\"); } }");
    final CountDownLatch ended = new CountDownLatch(1);
    frame.getModel().addListener(new DummyGlobalModelListener() {
      public void interactionEnded() { ended.countDown(); }
    });
    SwingUtilities.invokeAndWait(new Runnable() {
      public void run() {
        assertEquals("Go", frame.getGoButton().getText());
        frame.getGoButton().doClick();
      }
    });
    boolean finished = ended.await(30, TimeUnit.SECONDS);
    assertTrue("Program did not finish: " + frame.getModel().getInteractionsDocument().getText() + "\nConsole: " + frame.getModel().getConsoleDocument().getText(), finished);
    assertTrue("Source was not compiled", new File(directory, "GoExample.class").exists());
    assertTrue("Program output missing: " + frame.getModel().getInteractionsDocument().getText() + "\nConsole: " + frame.getModel().getConsoleDocument().getText(), frame.getModel().getInteractionsDocument().getText().contains("GO_TEST_OK"));

    // Re-run edited source using the regular main command as well as Smart Run.
    awaitEnabled();
    final CountDownLatch rerun = new CountDownLatch(1);
    frame.getModel().addListener(new DummyGlobalModelListener() {
      public void interactionEnded() { rerun.countDown(); }
    });
    SwingUtilities.invokeAndWait(new Runnable() {
      public void run() {
        try {
          edu.rice.cs.drjava.DrJava.getConfig().setSetting(
              edu.rice.cs.drjava.config.OptionConstants.SMART_RUN_FOR_APPLETS_AND_PROGRAMS, false);
          edu.rice.cs.drjava.model.OpenDefinitionsDocument doc = frame.getModel().getActiveDocument();
          doc.getDocument().remove(0, doc.getDocument().getLength());
          doc.getDocument().insertString(0,
              "public class GoExample { public static void main(String[] args) { System.out.print(\"GO_SECOND_OK\"); } }", null);
          frame._saveAll();
          frame.getGoButton().doClick();
        }
        catch (Exception e) { throw new RuntimeException(e); }
      }
    });
    assertTrue("Edited program did not finish", rerun.await(30, TimeUnit.SECONDS));
    assertTrue("Edited output missing: " + frame.getModel().getInteractionsDocument().getText(),
               frame.getModel().getInteractionsDocument().getText().contains("GO_SECOND_OK"));

  }

  public void testCompileErrorDoesNotRun() throws Exception {
    open("public class GoExample { public static void main(String[] args) { invalid syntax; } }");
    final CountDownLatch compiled = new CountDownLatch(1);
    final CountDownLatch ran = new CountDownLatch(1);
    frame.getModel().addListener(new DummyGlobalModelListener() {
      public void compileEnded(File wd, List<? extends File> excluded) { compiled.countDown(); }
      public void interactionStarted() { ran.countDown(); }
    });
    SwingUtilities.invokeAndWait(new Runnable() {
      public void run() { frame.getGoButton().doClick(); }
    });
    assertTrue("Compilation did not finish", compiled.await(30, TimeUnit.SECONDS));
    SwingUtilities.invokeAndWait(new Runnable() { public void run() { } });
    assertTrue(frame.getModel().getCompilerModel().getCompilerErrorModel().getNumCompilerErrors() > 0);
    assertFalse("Ran despite compiler errors", ran.await(1, TimeUnit.SECONDS));
  }
  private void editSource(final String source) throws Exception {
    SwingUtilities.invokeAndWait(new Runnable() {
      public void run() {
        try {
          edu.rice.cs.drjava.model.OpenDefinitionsDocument doc = frame.getModel().getActiveDocument();
          doc.getDocument().remove(0, doc.getDocument().getLength());
          doc.getDocument().insertString(0, source, null);
        }
        catch (Exception e) { throw new RuntimeException(e); }
      }
    });
  }

  public void testGoAfterProgramExits() throws Exception {
    open("public class GoExample { public static void main(String[] args) { System.exit(0); } }");
    final CountDownLatch exited = new CountDownLatch(1);
    frame.getModel().addListener(new DummyGlobalModelListener() {
      public void interactionEnded() { exited.countDown(); }
    });
    SwingUtilities.invokeAndWait(new Runnable() {
      public void run() {
        edu.rice.cs.drjava.DrJava.getConfig().setSetting(
            edu.rice.cs.drjava.config.OptionConstants.ALWAYS_SAVE_BEFORE_COMPILE, true);
        frame.getGoButton().doClick();
      }
    });
    assertTrue("Exit interaction did not finish", exited.await(30, TimeUnit.SECONDS));
    awaitEnabled();

    editSource("public class GoExample { public static void main(String[] args) { System.out.println(1) } }");
    final CountDownLatch compiled = new CountDownLatch(1);
    final CountDownLatch ran = new CountDownLatch(1);
    frame.getModel().addListener(new DummyGlobalModelListener() {
      public void compileEnded(File wd, List<? extends File> excluded) { compiled.countDown(); }
      public void interactionStarted() { ran.countDown(); }
    });
    SwingUtilities.invokeAndWait(new Runnable() {
      public void run() { frame.getGoButton().doClick(); }
    });
    assertTrue("First Go after exit must reach the compiler", compiled.await(30, TimeUnit.SECONDS));
    assertTrue(frame.getModel().getCompilerModel().getCompilerErrorModel().getNumCompilerErrors() > 0);
    assertFalse("Invalid source must not run", ran.await(1, TimeUnit.SECONDS));

    // Repair the source and confirm a new execution process can run it.
    editSource("public class GoExample { public static void main(String[] args) { System.out.print(\"AFTER_EXIT_OK\"); } }");
    final CountDownLatch finished = new CountDownLatch(1);
    frame.getModel().addListener(new DummyGlobalModelListener() {
      public void interactionEnded() { finished.countDown(); }
    });
    SwingUtilities.invokeAndWait(new Runnable() {
      public void run() { frame.getGoButton().doClick(); }
    });
    assertTrue("Repaired program did not finish", finished.await(30, TimeUnit.SECONDS));
    assertTrue("Output after exit missing: " + frame.getModel().getInteractionsDocument().getText(),
               frame.getModel().getInteractionsDocument().getText().contains("AFTER_EXIT_OK"));
  }

}

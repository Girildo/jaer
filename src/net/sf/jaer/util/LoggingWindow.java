package net.sf.jaer.util;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import net.sf.jaer.graphics.AEViewerAboutDialog;

/** A frame with text area to show logging results in. */
public class LoggingWindow extends JFrame {

    final private JTextArea textArea = new JTextArea();
    public static final String DEVELOPER_EMAIL="tobidelbruck@sourceforge.net,rberner@sourceforge.net";

    public LoggingWindow(String title, final int width,
            final int height) {
        super(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // exit if the user clicks the close button on uncaught exception
        EventQueue.invokeLater(new Runnable() {

            public void run() {
                addVersionInfo();
                setSize(width, height);
                JScrollPane pane = new JScrollPane(textArea);
                textArea.setEditable(false);
                getContentPane().add(pane, BorderLayout.CENTER);
                JButton copyBut = new JButton("Copy to clipboard");
                copyBut.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                            clipboard.setContents(new StringSelection(textArea.getText()), null);
                        } catch (Exception ex) {
                            System.err.println("couldn't copy exception pane: " + ex.toString());
                        }
                    }
                });
                
                JButton mailBut=new JButton("Mail to developers");
                 mailBut.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            mailToDevelopers();
                        } catch (Exception ex) {
                            System.err.println("couldn't copy exception pane: " + ex.toString());
                        }
                    }
                });              
                JPanel butPan = new JPanel();
                butPan.setLayout(new BoxLayout(butPan, BoxLayout.X_AXIS));
                butPan.add(new Box(BoxLayout.X_AXIS));
                butPan.add(copyBut);
                butPan.add(mailBut);
                getContentPane().add(butPan, BorderLayout.SOUTH);
                setVisible(true);
            }
        });
    }

    public void addLogInfo(final String data) {
        EventQueue.invokeLater(new Runnable() {

            public void run() {
                textArea.append(data);
            }
        });
    }

    public void addVersionInfo() {
        Properties props = new Properties();
        // when running from webstart  we are not allowed to open a file on the local file system, but we can
        // get a the contents of a resource, which in this case is the echo'ed date stamp written by ant on the last build
        ClassLoader cl = this.getClass().getClassLoader(); // get this class'es class loader
        addLogInfo("\nLoading version info from resource " + AEViewerAboutDialog.VERSION_FILE);
        URL versionURL = cl.getResource(AEViewerAboutDialog.VERSION_FILE); // get a URL to the time stamp file
        addLogInfo("\nVersion URL=" + versionURL);
        if (versionURL != null) {
            try {
                Object urlContents = versionURL.getContent();
                BufferedReader in = null;
                if (urlContents instanceof InputStream) {
                    props.load((InputStream) urlContents);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
            PrintWriter ps = new PrintWriter(baos);
            props.list(ps);
            ps.flush();
            try {
                addLogInfo("\n"+baos.toString("UTF-8"));
            } catch (UnsupportedEncodingException ex) {
                System.err.println("cannot encode version information in LoggingWindow.addVersionInfo: "+ex.toString());
            }
        } else {
            props.setProperty("version", "missing file " + AEViewerAboutDialog.VERSION_FILE + " in jAER.jar");
        }

    }

    void mailToDevelopers() {
        Desktop desktop = null;
        // Before more Desktop API is used, first check 
        // whether the API is supported by this particular 
        // virtual machine (VM) on this particular host.
        if (Desktop.isDesktopSupported()) {
            desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.MAIL)) {

                String mailTo = DEVELOPER_EMAIL;
                URI uriMailTo = null;
                try {
                    if (mailTo.length() > 0) {
                        uriMailTo = new URI("mailto", mailTo+"?subject=jAER uncaught exception&body="+textArea.getText(), null);
                        desktop.mail(uriMailTo);
                    } else {
                        desktop.mail();
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                } catch (URISyntaxException use) {
                    use.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        Thread.UncaughtExceptionHandler handler = new LoggingThreadGroup("jAER UncaughtExceptionHandler");
        Thread.setDefaultUncaughtExceptionHandler(handler);
        throw new RuntimeException("test exception");
    }
}

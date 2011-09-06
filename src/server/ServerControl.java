package server;

/**
 * @(#) ServerControl.java 22.04.2005 Version 1.0
 * @author Jens Buettner
 */
//import server.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.net.*;
import java.io.*;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Oberflaeche fuer die Serversteuerung
 */
public class ServerControl implements ActionListener {

    static Logger logger = Logger.getLogger(ServerControl.class.getName());
    /**
     *
     */
    private final static int MSG_ERROR = 0;
    /**
     *
     */
    private final static int MSG_INFORMATION = 1;
    /**
     *
     */
    private final static int MSG_WARNING = 2;
    /**
     *
     */
    //private final static String LOGGING="error.log"; //Dateiname der Logdatei
    /**
     *
     */
    public final static String VERSION = "2.5.13";
    /**
     * Dateiname der ServerKonfigurationsdatei
     */
    private final static String SERVERCONFIGFILE = "ini" + Property.FILESEPARATOR + "server.xml";
     /**
     * Dateiname der crossdomain
     */
    public final static String DOMAINPOLICY = "ini" + Property.FILESEPARATOR + "crossdomain.xml";
    /**
     * Dateiname der crossdomain
     */
    public final static String HTTPDOMAINPOLICY = "ini" + Property.FILESEPARATOR + "crossdomainhttp.xml";
    /**
     * Dateiname der Logconfig
     */
    public final static String LOGCONFIGFILE = "ini" + Property.FILESEPARATOR + "factor.txt";
     /**
     *
     */
    private Server server;
    /**
     *
     */
    private static ServerControl sc = null;
    /**
     *
     */
    private static PopupMenu popupMenu1;
    /**
     *
     */
    private Frame frame1,  frame2,  frame3,  frame4,  frame5;
    /**
     *
     */
    private MenuItem menuItem1,  menuItem2,  menuItem3,  menuItem4,  menuItem5,  menuItem6;
    /**
     *
     */
    private MenuItem menuItem1a,  menuItem2b,  menuItem3c;
    /**
     *
     */
    private Menu menu;
    /**
     *
     */
    private Button ok1,  ok2,  ok3,  ok4,  ok5;
    /**
     *
     */
    private Button speichern1,  speichern2;
    /**
     *
     */
    private Button abbruch1,  abbruch2;
    /**
     *
     */
    private Button loeschen;
    /**
     *
     */
    private Button drucken;
    /**
     *
     */
    private TextField tfServerPort;
    private TextField tfServerIP;
    private TextField tfSocketTimeout;
    private TextField tfDatenbankTreiber;
    private TextField tfMaximalConnection;
    private TextField tfTableName;
    private TextField tfTmpDirectory;
     /**
     *
     */
    private TextField[] tfuser;
    /**
     *
     */
    private int serverPort;
    /**
     *
     */
    private TextArea text;
    /**
     *
     */
    private static FileOutputStream out;
    /**
     *
     */
    private java.awt.TrayIcon ServerControl = null;
    /**
     *
     */
    private Property prop_server;
    /**
     *
     */
    private Property prop_users;

    /**
     * Konstruktor
     */
    public ServerControl() {

        //Mechanismus zum pruefen ob der Server schon laueft
        File lock = new File("running.tmp");
        PropertyConfigurator.configure(LOGCONFIGFILE);
        try {
            if (lock.createNewFile()) {
                lock.deleteOnExit();
                out = new FileOutputStream(lock);
                out.write(1);
            } else {
                if (lock.delete()) {
                    lock.createNewFile();
                    lock.deleteOnExit();
                    out = new FileOutputStream(lock);
                    out.write(1);
                } else {
                    //showMessage(MSG_ERROR,"Das Programm laeuft schon!");
                    logger.info("Programm läuft schon!");
                    System.exit(0);
                }
            }
        } catch (IOException ex) {
            showMessage(MSG_ERROR, "Es ist ein Fehler bei dem erstellen der Datei running.tmp aufgetreten!");
            logger.error("Running tmp exists");
            logger.error(ex);
            System.exit(0);
        }

        //Property Datei laden
        prop_server = new Property();
        prop_users = new Property();
        try {
            prop_server.loadProperty(SERVERCONFIGFILE);
            this.serverPort = Integer.parseInt(prop_server.getProperty("serverPort"));
        } catch (IOException ex) {
            showMessage(MSG_ERROR, "Programm konnte nicht gestartet werden da die " +
                    "Konfigurationdatei: " + SERVERCONFIGFILE + " fehlerhaft" +
                    " oder nicht geöffnet werden konnte! Diese Datei " +
                    "muss sich im selben Verzeichnis wie die " +
                    "Programmdatei befinden!");
            //System.out.println ("Fehler bei dem oeffen der "+SERVERCONFIGFILE+" : "+ex);
            logger.error("Fehler bei dem oeffen der " + SERVERCONFIGFILE);
            logger.error(ex);
            System.exit(0);
        } catch (NumberFormatException ex) {
            logger.error("Der Serverport in der : " + SERVERCONFIGFILE +
                    " ist fehlerhaft!");
            logger.error(ex);
            showMessage(MSG_ERROR, "Der Serverport in der : " + SERVERCONFIGFILE +
                    " ist fehlerhaft!");
            //System.out.println ("Fehler bei dem oeffen der "+SERVERCONFIGFILE+" : "+ex);
            System.exit(0);
        }

        popupMenu1 = new PopupMenu();
        menuItem1 = new MenuItem("Programm Beenden");
        menuItem2 = new MenuItem("Einstellungen");
        menuItem3 = new MenuItem("Statistik");
        menuItem4 = new MenuItem("aktueller Server Status");
        menuItem5 = new MenuItem("Meldungen");
        menuItem1a = new MenuItem("Server starten");
        menuItem2b = new MenuItem("Server resetten");
        menuItem3c = new MenuItem("Server anhalten");
        menu = new Menu("Server Steuerung");

        menu.add(menuItem1a);
        menu.add(menuItem2b);
        menu.add(menuItem3c);
        popupMenu1.add(menu);
        popupMenu1.add(menuItem5);
        popupMenu1.add(menuItem4);
        popupMenu1.add(menuItem3);
        popupMenu1.add(menuItem2);
        popupMenu1.add(menuItem1);

        menuItem1.addActionListener(this);
        menuItem2.addActionListener(this);
        menuItem3.addActionListener(this);
        menuItem4.addActionListener(this);
        menuItem5.addActionListener(this);
        menuItem1a.addActionListener(this);
        menuItem2b.addActionListener(this);
        menuItem3c.addActionListener(this);
        menu.addActionListener(this);

       //ServerControl initialsiiseren
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().getImage("icon.png");
            ServerControl = new java.awt.TrayIcon(image, "DB Server v. " + VERSION, popupMenu1);
            //ServerControl.addMouseListener(this);
            try {
                tray.add(ServerControl);
            } catch (AWTException e) {
                logger.error("AWTException Fehler: " + e.getMessage());
                logger.error(e);
            }
        } else {
            logger.error("AWTException Fehler: Konnte das ServerControl nicht Initialisieren da" +
                    " das Betriebssystem dies nicht unterstützt!");
            //System.exit(0);
        }
    }

    /**
     * liefert einen Modaldialog entsprechend des Types zurueck
     *
     * Type werte können sein:
     *
     * MSG_ERROR:       Fehler
     * MSG_INFORMATION: Hinweise
     * MSG_WARNING:     Warnungen
     * @param type
     * @param msg
     */
    private void showMessage(int type, String msg) {
        switch (type) {
            case MSG_ERROR:
                JOptionPane.showMessageDialog(null, msg, "", JOptionPane.ERROR_MESSAGE);
                break;
            case MSG_INFORMATION:
                JOptionPane.showMessageDialog(null, msg, "", JOptionPane.INFORMATION_MESSAGE);
                break;
            case MSG_WARNING:
                JOptionPane.showMessageDialog(null, msg, "", JOptionPane.WARNING_MESSAGE);
                break;
            default:
                JOptionPane.showMessageDialog(null, "Fehlerhafter MessageTypemodus: " +
                        +type, "", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Aktionlistener der auf die Events reagiert
     *
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        if (ev.getSource() == menuItem1) {
            System.out.println("menuItem1: Beenden");
            if (server != null && server.isAlive()) {
                sc.serverBeenden();
            }
            try {
                out.close();
            } catch (IOException ex) {
                System.out.println("ServerControl: IOException Fehler " +
                        "bei dem schliesen der running.tmp" + ex);
            }
            System.exit(0);
        } else if (ev.getSource() == menuItem2) {
            System.out.println("menuItem2: Einstellungen");
            sc.serverConfig();
        } else if (ev.getSource() == menuItem3) {
            System.out.println("menuItem3: Statistik");
            sc.serverConnectionStatus();
        } else if (ev.getSource() == menuItem4) {
            System.out.println("menuItem4: aktueller Server Status");
            sc.serverStatus();
        } else if (ev.getSource() == menuItem5) {
            System.out.println("menuItem5: Meldungen");
            sc.serverShowLog();
        } else if (ev.getSource() == menuItem1a) {
            System.out.println("menuItem1a: Server starten");
            sc.serverStarten();
        } else if (ev.getSource() == menuItem2b) {
            System.out.println("menuItem2b: Server resetten");
            sc.serverResetten();
        } else if (ev.getSource() == menuItem3c) {
            System.out.println("menuItem3c: Server anhalten");
            sc.serverBeenden();
        } else if (ev.getSource() == ok1) {
//			System.out.println ("ok1");
            frame1.setVisible(false);
            frame1.dispose();
        } else if (ev.getSource() == ok2) {
//			System.out.println ("ok2");
            frame2.setVisible(false);
            frame2.dispose();
        } else if (ev.getSource() == ok3) {
//			System.out.println ("ok3");
            frame3.setVisible(false);
            frame3.dispose();
        } else if (ev.getSource() == ok4) {
//			System.out.println ("ok4");
            frame4.setVisible(false);
            frame4.dispose();
        } else if (ev.getSource() == ok5) {
//			System.out.println ("ok5");
            frame5.setVisible(false);
            frame5.dispose();
        } else if (ev.getSource() == speichern1) {
//			System.out.println ("speichern1");
            saveServerConfig();
        } else if (ev.getSource() == abbruch1) {
//            System.out.println("abbruch1");
            frame3.setVisible(false);
            frame3.dispose();
        } else if (ev.getSource() == abbruch2) {
//            System.out.println("abbruch1");
            frame5.setVisible(false);
            frame5.dispose();

        } else {
            System.out.println("Server actionPerformed() Unbekanntes Bedienelement: " + ev.getSource());
        }
    }

    /**
     * speichert die geanderte Konfiguration
     */
    private void saveServerConfig() {
        prop_server.createProperty("serverPort", tfServerPort.getText());
        prop_server.createProperty("serverIP4Address", tfServerIP.getText());
        prop_server.createProperty("socketTimeout", tfSocketTimeout.getText());
        prop_server.createProperty("maxConnection", tfMaximalConnection.getText());

        showMessage(MSG_INFORMATION, "Die neuen Einstellungen werden erst nach dem nöchsten Start des Servers aktiv!");
        try {
            prop_server.writeProperty(SERVERCONFIGFILE, "Version 1.0");
        } catch (IOException ex) {

            showMessage(MSG_ERROR, "Konnte die neue Konfiguration nicht in die " + SERVERCONFIGFILE + " schreiben! " +
                    "Möglicherweise wird die Datei von einem anderen Programm verwendet!");
        }
    }

    

    /**
     * berechnet die Koordinaten des Fensters
     * @param frameSize
     * @return
     */
    private Point centerWindow(Dimension frameSize) {
        // GrÃƒÂ¶ÃƒÅ¸e des Bildschirms ermitteln
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        // Position des JFrames errechnen
        return new Point(((screenSize.width - frameSize.width) / 2), ((screenSize.height - frameSize.height) / 2));
    }

    /**
     * startet den Server
     */
    public void serverStarten() {
        logger.info("ServerControl: Aufruf von serverStarten()");
        String error = "Der Server konnte nicht gestartet werden da ";
        if (server == null || !server.isAlive()) {
            try {
                    server = new Server(SERVERCONFIGFILE);
                    server.start();
            } catch (IOException ex) {
                showMessage(MSG_ERROR, error + "die Konfiguration nicht geladen werden konnte!");
                logger.error("ServerControl: Fehler in serverStarten() IOException: " + ex.getLocalizedMessage());
            } catch (NumberFormatException ex) {
                showMessage(MSG_ERROR, error + "die Einstellungen der Konfigurationsdatei fehlerhaft sind!");
                logger.error("ServerControl: Fehler in serverStarten() NumberFormatException: " + ex.getLocalizedMessage());
            }
        } else {
            //showMessage(MSG_INFORMATION, "Der Server läuft schon, damit er gestartet werden kann muss er vorher beendet werden!");
        }
    }

    /**
     * startet den Server neu, erst werden alle Verbindungen beendet und dann
     * wird der Server wieder neu gestartet
     */
    public void serverResetten() {
        logger.info("ServerControl: Aufruf von serverResetten()");
        if (server != null && server.isAlive()) {
            serverBeenden();
            serverStarten();
        } else {
            showMessage(MSG_INFORMATION, "Der Server läuft noch nicht, damit er neugestartet werden kann muss er vorher gestartet werden!");
        }
    }

    /**
     * beendet des Server
     */
    public void serverBeenden() {
        logger.info("ServerControl: Aufruf von serverBeenden()");
        if (server != null && server.isAlive()) {
            try {
                server.serverStop();
                Socket sock = new Socket("127.0.0.1", serverPort);
                server.join();//wartet bis Server beendet ist
                System.out.println("join ist vorbei");
            } catch (InterruptedException ex) {
                logger.error(ex);
            //       logfile.addStackTrace("ServerControl: Fehler in serverBeenden() InterruptedException: "+ex.toString(),ex.getStackTrace());
            } catch (UnknownHostException ex) {
                logger.error(ex);
            //      logfile.addStackTrace("ServerControl: Fehler in serverBeenden() UnknownHostException: "+ex.toString(),ex.getStackTrace());
            } catch (IOException ex) {
                logger.error(ex);
            //     logfile.addStackTrace("ServerControl: Fehler in serverBeenden() IOException: "+ex.toString(),ex.getStackTrace());
            }
            server = null;
        } else {
            logger.info("Der Server läuft noch nicht, damit er beendet werden kann muss er vorher noch gestartet werden!");
        }
    }

    /**
     * gibt den Status des Servers aus
     */
    public void serverStatus() {
        if (frame1 == null || !frame1.isVisible()) {
            Panel p1 = new Panel();
            Panel p4 = new Panel();
            // Container c = frame1.getContentPane();
            Box b = Box.createVerticalBox(); //mit Boxlayoumanager

            p1.setLayout(new GridLayout(1, 2, 6, 2));

            Label status = new Label("Der aktuelle Status des Servers ist:");
            Label statusResult;

            if (server == null) {
                statusResult = new Label(" beendet ");
            } else if (server.isAlive()) {
                statusResult = new Label(" läuft ");
            } else if (!server.isAlive()) {
                statusResult = new Label(" inaktiv ");
            } else {
                statusResult = new Label(" nicht definiert ");
            }
            ok1 = new Button("ok");
            ok1.addActionListener(this);
            p1.add(status);
            p1.add(statusResult);
            p4.add(ok1);
            b.add(p1);
            b.add(p4);
            frame1 = new Frame("Server Status");
            frame1.addWindowListener(new java.awt.event.WindowAdapter() {

                @Override
                public void windowClosing(java.awt.event.WindowEvent evt) {
                    frame1.dispose();
                }
            });
            frame1.setSize(520, 100);	//festlegen der Fenstergroesse
            frame1.setLocation(centerWindow(frame1.getSize()));     //position des Fensters auf dem  Bildschirm
            frame1.setResizable(false);	//verhindert das Fenstermaximieren
            frame1.add(b);
            frame1.setVisible(true); //sichbarmachen des Hauptfenster
        }
    }

    /**
     * gibt die Verbindungsstatistik aus
     */
    public void serverConnectionStatus() {
        if (frame2 == null || !frame2.isVisible()) {
            Panel p1 = new Panel();
            Panel p4 = new Panel();
            Box b = Box.createVerticalBox(); //mit Boxlayoumanager
            p1.setLayout(new GridLayout(2, 2, 6, 2));

            Label connctionActiveResult;
            Label connctionAllResult;
            if (server != null) {
                connctionActiveResult = new Label("" + server.getActiveConnections() + "");
                connctionAllResult = new Label("" + server.getAllConnections() + "");
            } else {
                connctionActiveResult = new Label("-");
                connctionAllResult = new Label("-");
            }
            p1.add(new Label("Momentan aktive Verbindungen zum Server:"));
            p1.add(connctionActiveResult);
            p1.add(new Label("Gesamtanzahl der schon geführten Verbindungen:"));
            p1.add(connctionAllResult);

            ok2 = new Button("ok");
            ok2.addActionListener(this);
            p4.add(ok2);

            b.add(p1);
            b.add(p4);
            frame2 = new Frame("Server Statistik");
            frame2.addWindowListener(new java.awt.event.WindowAdapter() {

                @Override
                public void windowClosing(java.awt.event.WindowEvent evt) {
                    frame2.dispose();
                }
            });
            frame2.add(b);
            frame2.setSize(520, 123);	//festlegen der Fenstergroesse
            frame2.setLocation(centerWindow(frame2.getSize()));	//position des Fensters auf dem  Bildschirm
            frame2.setResizable(false);	//verhindert das Fenstermaximieren
            frame2.setVisible(true);    //sichbarmachen des Hauptfenster
        }
    }

    /**
     * ermoeglicht die Konfiguration des Programms
     */
    public void serverConfig() {
        if (frame3 == null || !frame3.isVisible()) {

            try {
                prop_server.loadProperty(SERVERCONFIGFILE);
            } catch (Exception ex) {
                logger.error("Konnte die Konfiguration nicht laden, es wird eine neue erstellt!"+ex.getLocalizedMessage());
                showMessage(MSG_WARNING, "Konnte die Konfiguration nicht laden, es wird eine neue erstellt!");

            }

            /*Server*/
            tfServerPort = new TextField(prop_server.getProperty("serverPort"), 6);
            tfServerPort.setMaximumSize(tfServerPort.getPreferredSize());
            tfServerIP = new TextField(prop_server.getProperty("serverIP4Address"), 8);
            tfServerIP.setMaximumSize(tfServerPort.getPreferredSize());
            tfSocketTimeout = new TextField(prop_server.getProperty("socketTimeout"), 8);
            tfSocketTimeout.setMaximumSize(tfSocketTimeout.getPreferredSize());
            tfMaximalConnection = new TextField(prop_server.getProperty("maxConnection"), 4);
            tfMaximalConnection.setMaximumSize(tfMaximalConnection.getPreferredSize());
            Panel p1 = new Panel();
            p1.setLayout(new GridLayout(5, 2));
            p1.add(new Label("Server Port: (für Flash-Clients >1024)"));
            p1.add(tfServerPort);
            p1.add(new Label("Server IP: (Abhängig von Karten im Rechner)"));
            p1.add(tfServerIP);
            p1.add(new Label("Socket Timeout:"));
            p1.add(tfSocketTimeout);
            p1.add(new Label("Maximalverbindungen:"));
            p1.add(tfMaximalConnection);

            ok3 = new Button("ok");
            speichern1 = new Button("änderung speichern");
            abbruch1 = new Button("abbruch");

            ok3.addActionListener(this);
            speichern1.addActionListener(this);
            abbruch1.addActionListener(this);

            Panel p5 = new Panel();
            p5.add(ok3);
            p5.add(speichern1);
            p5.add(abbruch1);

            Box b = Box.createVerticalBox();
            b.add(p1);
            b.add(p5);

            frame3 = new Frame("Server Einstellung");
            frame3.addWindowListener(new java.awt.event.WindowAdapter() {

                @Override
                public void windowClosing(java.awt.event.WindowEvent evt) {
                    frame3.dispose();
                }
            });
            frame3.add(b);
            frame3.setSize(500, 300);   //festlegen der Fenstergroesse
            frame3.setLocation(centerWindow(frame3.getSize()));   //position des Fensters auf dem  Bildschirm
            frame3.setResizable(false);//verhindert das Fenstermaximieren

            frame3.setVisible(true);   //sichbarmachen des Hauptfenster
        }
    }

    /**
     * hier werden die Eintraege der Log Datei angezeigt
     */
    public void serverShowLog() {
        if (frame4 == null || !frame4.isVisible()) {
            String filename = "logs/error.log";
            StringBuffer strbf = new StringBuffer();
            try {
                File file = new File(filename);
                BufferedReader br = new BufferedReader(new FileReader(file));

                while (br.ready()) {
                    strbf.append(br.readLine() + "\n");
                }
                br.close();

            } catch (FileNotFoundException ex) {
                showMessage(MSG_ERROR, "Konnte " + filename + " nicht finden!");
                return;
            } catch (IOException ex) {
                showMessage(MSG_ERROR, "Es gab ein Fehler bei dem Zugriff auf die " + filename + " Datei!");
                return;
            }
            Box b = Box.createVerticalBox(); //mit Boxlayoumanager

            ok4 = new Button("ok");
            ok4.addActionListener(this);
            Panel p1 = new Panel();
            Panel p4 = new Panel();
            text = new TextArea(strbf.toString(), 26, 150, TextArea.SCROLLBARS_BOTH);
            b.add(text);
            p4.add(ok4);
            b.add(p4);

            frame4 = new Frame("Server Einstellung");
            frame4.addWindowListener(new java.awt.event.WindowAdapter() {

                @Override
                public void windowClosing(java.awt.event.WindowEvent evt) {
                    frame4.dispose();
                }
            });
            frame4.add(b);

            frame4.setResizable(false);	//verhindert das Fenstermaximieren
            frame4.setSize(640, 480);	//festlegen der Fenstergroesse
            frame4.setLocation(centerWindow(frame4.getSize()));
            frame4.setVisible(true);    //sichbarmachen des Hauptfenster
            text.setCaretPosition(strbf.length());
        }
    }

   

    /**
     * starten des Programms
     * @param args
     */
    public static void main(String[] args) {
        //Singelton
        if (sc == null) {
            sc = new ServerControl();
            sc.serverStarten();
        }
        try {
            while (true) {
                synchronized (sc) {
                    sc.wait(1);
                }
            }
        } catch (InterruptedException ex) {
            logger.error("ServerControl: InterruptedException Fehler in main() InterruptedException: " + ex.getLocalizedMessage());
//			System.out.println ("traiycon Fehler in main() InterruptedException: "+ex);
        } catch (OutOfMemoryError ex) {
            logger.error("ServerControl: OutOfMemoryError Fehler in main() OutOfMemoryError: " + ex.getLocalizedMessage());
//			System.out.println ("traiycon Fehler in main() InterruptedException: "+ex);
        }
        try {
            out.close();
        } catch (IOException ex) {
            logger.error("ServerControl: IOException Fehler bei dem schliesen der running.tmp: " + ex.getLocalizedMessage());
        }

        System.exit(0);
    }
}

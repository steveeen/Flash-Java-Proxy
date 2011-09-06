/**
 * @(#) SocketThread.java 22.04.2005 Version 1.0
 * @author Jens Buettner
 */
package server;

import java.net.*;
import java.nio.charset.Charset;
import java.io.*;
//import java.nio.ByteBuffer;
import org.apache.log4j.Logger;

/**
 * Ein Objekt dieser Klasse stellt eine Verbindung zum Client dar
 */
public class SocketThread extends Thread {

    static Logger logger = Logger.getLogger(SocketThread.class.getName());
    /**
     * Socket des Nutzers
     */
    private Socket socket;
    /**
     * InputStream
     */
    private InputStream in;
    /**
     * OutputStream
     */
    private PrintStream out;
    /**
     * Objekt des Controlklasse
     */
    private SocketControl con;
    /**
     * Socketimeout
     */
    private int socketTimeout = 100;
    /**
     *
     */
    private byte[] buffer;
    /**
     *
     */
    private final Charset UTF8 = Charset.forName("UTF-8");
    /**
     *
     */
    private final Charset ISO88591 = Charset.forName("ISO-8859-1");
    /**
     * erhaelt den wert der schon gelesenen Zeichen
     */
    private int readResult = 1000;
    /**
     * Zeichen welches das Ende einer Nachricht Signalisiert
     */
    private final String MSGEND = String.valueOf('\u0000');
    /**
     * Protokoll Nachricht pong
     */
    private final String PONG = new String("pong");
    /**
     * Protokoll Nachricht exit
     */
    private final String EXIT = new String("exit");
    /**
     * Protokoll Nachricht get
     */
    private final String GET = new String("GET");
    /**
     * Protokoll Nachricht post
     */
    private final String POST = new String("POST");
    /**
     * Protokoll Nachricht pongTime
     */
    private final String PONGTIME = new String("pongTime");

    /**
     * Konstruktor der Klasse SocketThread
     * @param control steuert den Programmablauf
     * @param socket Socket
     * @param socketTimeout Zeitraum der angibt nach dem die Verbindung
     * automatisch unterbrochen werden soll in dem keine Nachricht empfangen
     * wurde vom client
     * @throws java.io.IOException
     */
    public SocketThread(Socket socket, int socketTimeout, SocketControl control)
            throws IOException {
        this.socket = socket;
        this.socketTimeout = socketTimeout;
        socket.setSoTimeout(socketTimeout);
        con = control;
        in = socket.getInputStream();
        out = new PrintStream(socket.getOutputStream());
    }

    /**
     * Standard Methode zum senden von Nachrichten an den Client
     *
     * @param  msg     Nachricht die an den Client gesendet werden soll
     * @return boolean wenn gesendet=true
     */
    protected synchronized boolean sendMsg(String msg) {
        logger.debug("Sende Antwort!: " + msg);
        out.print(msg + MSGEND);
        out.flush();
        return true;
    }

    /**
     * schliest den socket des SocketThreads
     * kann vom Server und vom Thread selbst aufgerufen werden
     */
    protected synchronized void socketConnectionClose() {
        logger.debug("socketConnClose()");
        if (!socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException ex) {
                logger.error("sockConClose() " + ex.getLocalizedMessage());
            }
        }
    }

    /**
     * Verarbeiten des XML Protokolls,
     * die komplette datenverarbeitung bleibt hier bestehen
     *
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     * @throws java.lang.OutOfMemoryError
     */
    private void flashSocketConnection() throws IOException {
        logger.debug("flashSocketConnection(): neue FlashSocketverbindung");
        String msg_beg = "";//Speichert einen unvollstaendige Nachricht
        String[] msgQueue; //array mit den vollstaendigen Nachrichten
        int l = 0;          //Anzahl der gesplitteten Nachrichten
        int len = -1;       //Nachrichtenlaenge
        String msg = "";
        boolean chkMsgLength = true;
        boolean initXML = false;   //fuer das 1malige hinzuefugen des Nachrichtenanfangs
        do {
            //zuerst Nachricht bis zum Ende zusammenfassen
            do {
                //zuruecksetzten des Nachrichtenbeginns
                msg += msg_beg;
                if (in.available() == 0) {
                    buffer = new byte[1];
                } else {
                    buffer = new byte[in.available()];
                }
                readResult = in.read(buffer);
                logger.info(readResult + "||" + in.available());
                len = buffer.length;
                msg += new String(buffer, ISO88591);
                if (len < 1024 && (readResult != -1)) {
                    try {
                        SocketThread.sleep(50);
                    } catch (InterruptedException ex) {
                        logger.error("flashSocketConn() Error bei Thread sleep!" + ex.getLocalizedMessage());
                        return;
                    }
                }
            } while ((buffer[len - 1] != 0) && (readResult != -1));
            l = msg.length();
            if (l <= 0) { //messageLength ist 0 weiter in schleife
                msg = "";
                System.out.println("SocketThread: hat nichts relevantes empfangen!");
            } else {
                /*
                 * zersplitten der Nachrichten
                 */
                l = -1;
                boolean noMsgEnd = msg.endsWith(MSGEND);
                msgQueue = msg.split(MSGEND);//array
                msg = "";
                l = msgQueue.length;
                if (!noMsgEnd) {
                    //der String endet nicht mit einer 0: es gibt eine unf. Nachricht am ende
                    logger.error("SocketThread: falsches Ende! " + msgQueue[l - 1]);
                    msg_beg = msgQueue[l - 1];//unvollständige Nachricht zurück in den Buffer l--;
                    l--;   //laenge der Kompletten Nachrichten um 1 veringern
                //return;
                }
                /*
                 *verarbeiten der Socket Nachrichten, vom Flash Client
                 */
                for (int i = 0; i < l; i++) {
                    //laengenabhaengig fuer Kurzbefehle
                    if (msgQueue[i].length() == 4) {
                        if (msgQueue[i].startsWith(PONG)) {
                            logger.debug("pong empfangen!");
                        } else if (msgQueue[i].startsWith(EXIT)) {
                            logger.debug("exit empfangen!");
                            this.socketConnectionClose();
                            //return laesst den Thread auslaufen
                            return;//geht zu new ValidLengthMessage() und weiter zu run()
                        } else {
                            System.out.println("SocketThread: hat eine unbekannte Nachricht empfangen! " + msgQueue[i]);
                        }
                    } else {//Normale XMLNachrichten
                        //logfile.addMessage(i+" Nachricht: "+msg);
                        System.out.println("\nSocketThread: neue Nachricht empfangen:");
                        System.out.println("-----------------------------------------");
                        System.out.println(msgQueue[i]);
                        System.out.println("-----------------------------------------\n");
                        //ist die Default Anweisung, Nachricht ist XML und wird
                        //an die Controlklasse geben und das Ergebniss an den Client senden
                        try {
                            con.newXMLMessage(msgQueue[i]);
                        } catch (Exception e) {
                            logger.error(e);
                        }
                    }
                }
            }
        } while (readResult != -1 && socket.isConnected());
        System.out.println("SocketThread: readResult: " + readResult + " socket.isConnected(): " + socket.isConnected());
        System.out.println("SocketThread: normeles ende gehe zu run zurueck!");
    }

    /**
     * Steuerung des SocketThread, er wartet auf eingehende Nachrichten
     * und reagiert ensprechend darauf. Folgende Schluessel sind definiert:
     */
    @Override
    public void run() {
        logger.info("run() wird gestartet!");
        try {
            while (in.available() < 9) {
                //   logger.debug("available="+in.available()+"ist <9 Thread sleeping 50ms");
                SocketThread.sleep(50);
            }
            this.buffer = new byte[in.available()];
            readResult = in.read(buffer);
            if (buffer.length < 9) {//ist fuer das erkennen von XML/HTTP Verbindung noetig chkMsgLength&&len>=8
                logger.error("run(): --> weniger als 9Bytes vorhanden!");
            }
            this.newValidLengthMessage();
            in.close();
            out.flush();
            out.close();
            logger.debug("SocketThread: wird geschlossen!");
        } catch (IOException ex) {
            logger.error("run() " + ex.getLocalizedMessage());
        } catch (InterruptedException ex) {
            logger.error("run() Fehler bei sleep  " + ex.getLocalizedMessage());
        }
        this.socketConnectionClose();
        logger.debug("run():ENDE");
    }

    private void newValidLengthMessage() throws IOException {
        String tmp = new String(buffer, 0, 8);
        logger.info("newValidLengthMessage() msg beginnt mit: " + tmp);
        //festlegen des Empfangsmodus XML, GET, POST
        if (tmp.startsWith(GET)) {
            logger.info("GET Message:\n" + new String(buffer));
        } else if (tmp.startsWith(POST)) {
           logger.info("POST Message:\n" + new String(buffer));
        } else if (tmp.startsWith("<policy-")) {
            String xxx=new String(buffer).trim();
             logger.info("DomainPolicy Message:\n" + xxx);
            if (xxx.equals(new String("<policy-file-request/>"))) {
                //    System.out.println("Eine Policy Request");
                //logger.info("DomainPolicy Anfrage:"+xxx);
                File pol = new File(ServerControl.DOMAINPOLICY);
                if (pol.exists() && pol.isFile()) {
                    try {
                        InputStream fileinput = new FileInputStream(pol);
                        byte[] b = new byte[fileinput.available()];
                        fileinput.read(b);
                        fileinput.close();
                        logger.info("domainPolicy wird gesendet!"+new String(b));
                        this.sendMsg(new String(b));
                    } catch (FileNotFoundException ex) {
                        logger.error("Fehler beim Policy per XML Senden:" + ex.getLocalizedMessage());
                    } catch (IOException ex) {
                        logger.error("Fehler beim Policy per XML Senden:" + ex.getLocalizedMessage());
                    }
                } else {
                    logger.error("run(): Datei " + pol.getPath() + " existiert nicht oder ist keine Datei!");
                }
            } else {
                logger.error("Error: es war doch keine Policy Message!" + new String(buffer));
            }
        } else {
            logger.error("empfing):\n " + new String(buffer));
        }
    }
}

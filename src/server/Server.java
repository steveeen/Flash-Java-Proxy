/**
 * @(#) Server.java 22.04.2005 Version 1.0
 * @author Jens Buettner
 */
package server;

import java.net.*;
import java.io.*;
import java.util.*;
import java.sql.*;
import org.xml.sax.*;
import javax.xml.parsers.*;
import org.apache.log4j.Logger;

/**
 * Ein Objekt dieser Klasse stellt ein Server dar.
 */
public class Server extends Thread {

    static Logger logger = Logger.getLogger(Server.class.getName());
    /**
     * 
     */
    //private static final SimpleLogger log = new SimpleLogger(Server.class);
    /**
     * das ServerSocket Objekt
     */
    private ServerSocket listener;
    /**
     * Vector zum speichern der verbindungen
     */
    private LinkedList<SocketControl> connectionQueue;
    /**
     * eindeutige connectionID einer verbindung
     */
    protected int connectionID;
    /**
     * bedingungs wert der while schleife die den server steuert
     */
    private boolean isServerRunning;
    /**
     * legt den Timeout eines Sockets fest, wird er ueberschritten schliesst er sich
     */
    private int socketTimeout;
    /**
     * Port des Servers
     */
    private int serverPort;
    /**
     * IPAdresse des Servers
     */
    private InetAddress serverIP;
    /**
     * legt die Maximale Anzahl der Verbindungen zum Server fest
     */
    private int maxConnection;  
    /**
     * BS abhaengiges backslash
     */
    private final static String SYSTEM = (System.getProperties().getProperty("file.separator", "\n").toString());

    /**
     * Konstruktor, es wird der name der Configurationsdatei uebergeben
     *
     * @param serverconfigfile Konfigurationsdatie des Servers
     * @param userconfigfile Konfigurationsdatei der Benutzer
     * @throws java.lang.InstantiationException wenn der DB Treiber nicht instatiert werden konnte
     * @throws java.lang.IllegalAccessException DB Fehler bei nichterlaubten zugriff
     * @throws java.io.IOException Ein/Ausgabe Fehler
     * @throws java.lang.NumberFormatException fehler bei dem umwandeln von Zahlen 
     * @throws java.sql.SQLException DB Fehler bei DB Abfragen
     * @throws java.lang.ClassNotFoundException DB Fehler bei Treiberinitialisierung
     * @throws UnknownHostException
     */
    public Server(String serverconfigfile) throws IOException,
            NumberFormatException, UnknownHostException {

        Property prop = new Property();
        prop.loadProperty(serverconfigfile);
        connectionQueue = new LinkedList<SocketControl>();
        // this.logfile=logfile;
       


        logger.info("Starte Server");
        serverPort = Integer.parseInt(prop.getProperty("serverPort"));

        this.serverIP = InetAddress.getByName(prop.getProperty("serverIP4Address"));

        socketTimeout = Integer.parseInt(prop.getProperty("socketTimeout"));
        maxConnection = Integer.parseInt(prop.getProperty("maxConnection"));
    }

    /**
     * alle Verbindungen werden aus dem connectionQueue entfernt und die
     * jeweiligen Temporaeren Dateien der Verbindung geloescht
     *
     * @throws java.io.IOException behandelt Ein/Ausgabe Fehler
     * @throws java.lang.InterruptedException Tread Fehler bei unterbrechung
     */
    private void serverRemoveAllConnections() {
        for (int i = 0; i < connectionQueue.size(); i++) {
            if (this.connectionQueue.get(i).isThreadaktive()) {
                this.connectionQueue.get(i).serverWillThreadStop();//warten bis SocketThread beendet ist
                logger.info(connectionQueue.get(i).getConnectionID() + "Die Socketthreads wurden beendet");
                this.connectionQueue.remove(i);

            }
        }
        this.connectionQueue.clear();//alle geschlossenen Elemente aus dem Vector entfernen
    //System.out.println(" ende von serverRemoveConnection: Anzahl der Verbindungen: "+connectionQueue.size());
    }

    /**
     * Methode ueberprueft ob eine verbindung im connectionQueue  beendet wurde.
     * Wenn ja, wird diese aus dem Vector entfernt und geprueft ob im temp-
     * poraeren Verzeichnis noch Dateien von dieser Verbindung liegen, diese
     * werden ebenfalls geloescht.
     *
     * @return Anzahl der aktiven Verbindungen
     */
    private int socketThreadStatus() {
        for (int i = 0; i < connectionQueue.size(); i++) {
            if (!this.connectionQueue.get(i).isThreadaktive()) {
                this.connectionQueue.remove(i);
            //System.out.println("Server: Beendete Verbinungen aus dem Vector entfernt");
            }
        }
        logger.info("Server: Alle beendeten Verbindungen entfernt: aktuelle Anzahl der Verbindungen"+ connectionQueue.size());
        return connectionQueue.size();
    }

    /**
     * setzt die isServerRunning variable auf false so das die serverRun() Methode
     * verlassen werden kann
     */
    public void serverStop() {
        this.isServerRunning = false;
    }

    /**
     * gibt die Verbindungen die seit dem Serverstart erfolgten, aus, egal ob
     * sie noch aktiv sind oder noch exsistieren
     *
     * @return int : gesammte Anzahl der erfolgten Verbindungen
     */
    public int getAllConnections() {
        return connectionID;
    }

    /**
     * gibt die Anzahl der momentan aktiven Verbindungen aus
     *
     * @return Anzahl der aktiven Verbindungen
     */
    public int getActiveConnections() {
        return socketThreadStatus();
    }

    /**
     * steuert den Server. er wartet auf eine neue verbindung, schaut
     * ob die verbindungen im vector noch alle mit dem client verbunden sind,
     * ansonsten werden sie geschlossen und aus dem connectionQueue  entfernt. ist
     * die whileschleife beendet so werden alle verbindungen aus dem vector
     * beendet, entfernt und der listener socket geschlossen. Die Exceptions die
     * hier auftreten werden alle in die Datei error.log geschrieben.
     *
     * @throws java.io.IOException behandelt Ein/Ausgabe Fehler
     * @throws java.lang.InterruptedException Threadfehler bei unterbrechung
     */
    private void serverRun() throws IOException, InterruptedException {
        logger.info("ServerRun START");
        listener = new ServerSocket(serverPort, 1, serverIP);
        logger.info("ServerRun START1");
        do {
            logger.info("Server: Warte auf neue Verbindung: Verbindungsanzahl:" + connectionQueue.size());
            Socket socket = listener.accept();
            System.gc();
            if (isServerRunning && (socketThreadStatus() < maxConnection)) {
                connectionID++; 
                    connectionQueue.add( new SocketControl(connectionID,socket, socketTimeout)); //hinzufuegen eines Socketthreads
                    logger.info(connectionID + " Server: Verbindung:  starte socketthread!");
                    connectionQueue.getLast().starteSocketThread();
                if (connectionID == Integer.MAX_VALUE) {
                    connectionID = 0;
                }
            } else {
                if (isServerRunning) {
                    logger.info(connectionID + " Server: Verbindung:  Die maximale Anzahl der Verbindung von: " + maxConnection + " wurde erreicht!");
                    logger.info(connectionID + " Server: Verbindung:  Die Verbindung kann nicht Aufgebaut werden!");
                    PrintStream pStreamTmp = new PrintStream(socket.getOutputStream());
                    pStreamTmp.print("max Verbindung erreicht! spaeter erneut versuchen..." + '\u0000');
                    pStreamTmp.flush();
                    pStreamTmp.close();
                    pStreamTmp = null;
                    socket.close();
                }
            }
        } while (isServerRunning && !listener.isClosed());
        serverRemoveAllConnections();
        listener.close();
        logger.info(" serverRun() ENDE!");
    }

    /**
     * Setzt die while Variable isServerRunning auf true und ruft die
     * Serversteuerungs methode serverRun() auf. Alle Fehler die hier auftreten
     * werden in die error.log geschrieben.
     */
    @Override
    public void run() {
        isServerRunning = Thread.currentThread().isAlive();
        try {
            logger.info("start Thread");
            serverRun();
        } catch (IOException ex) {
//            System.out.println("Server: Fehler in run() IOException: "+ex);
            logger.error(ex);
            System.gc();
        } catch (InterruptedException ex) {
//            System.out.println("Server: Fehler run() InterruptedException: "+ex);
            logger.error(ex);
            System.gc();
        }
        logger.info("Server: run() wird verlassen...");
    }
}

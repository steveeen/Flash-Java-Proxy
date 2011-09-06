/**
 * @(#) ProgControl.java 22.04.2005 Version 1.0
 * @author Jens Buettner
 */
package server;

import java.io.*;
import java.net.Socket;
import javax.xml.parsers.*;
import java.sql.*;
import org.xml.sax.*;
import org.apache.log4j.Logger;

/**
 * Klasse fuer das Steuern der wuensche des Clints
 */
public class SocketControl {

     /**
     * Datenbank Objekt
     */
    static Logger logger = Logger.getLogger(SocketControl.class.getName());
     /**
     * fuer das erzeugen von XML Daten
     */
    public Socket socket = null;

    /**
     * connectionID gibt Position in den Arrays an
     */
    private final int connectionID;
     /**
     *
     */
    private SocketThread sockCon;

    /**
     * Konstruktor der Klasse SocketControl
     *
     * @param dburl          Datenbank URL
     * @param thePKs         Schluessel der Tabelle
     * @param socket         socket der Verbindung
     * @param socketTimeout  legt den Timeout eines Sockets fest, wird er ueberschritten schliesst er sich
     * @param dbusers        DB Userdaten
     * @param connectionID   Verbindungs ID
     * @param tmpDirectory   Verzeichnis mit den Temp Dateien
     *
     * @throws org.xml.sax.SAXException  Fehler bei XML initialisierung
     * @throws java.sql.SQLException    Fehler bei DB abfrage
     * @throws java.io.IOException   Fehler bei Ein/Ausgabe
     * @throws javax.xml.parsers.ParserConfigurationException Fehler bei XML inintialisierung
     */
    public SocketControl(int connectionID,Socket socket, int socketTimeout)
            throws IOException {
        this.socket = socket;
        this.connectionID = connectionID;
        sockCon = new SocketThread(socket, socketTimeout, this);
        logger.debug("neues SocketControlObjekt erstellt");
    }

    /*##########################################################
     *######## Methoden auf die der ServerThread zugreift ######
     *##########################################################
    /**
     * startet den SocketThread
     */
    /**
     *
     */
    public void starteSocketThread() {
        logger.info("starte SocketThread:" + this.connectionID);
        sockCon.start();
    }

    /**
     *
     *
     * @return
     */
    public boolean isThreadaktive() {
        return sockCon.isAlive();
    }

    /**
     * schliesst den socketThreads
     * wird ausschliesslich vom Server aufgerufen
     * 
     */
    protected void serverWillThreadStop() {
        logger.info("serverWillThreadStop(): Server beendet SocketThread" + this.connectionID);
        sockCon.socketConnectionClose();
        try {
            sockCon.join();
        } catch (InterruptedException ex) {
            logger.error("StopSocketCOntrolThread(): Fehler beim Thread JOIN " + ex.getLocalizedMessage());
        }
    }

    /**
     * gibt die connectionID wieder
     *
     * @return Verbindungs ID
     */
    protected int getConnectionID() {
        return connectionID;
    }

    /*##################################################
     *######## Server Methoden ende ####################
     *################################################*/
     /**
     * erzeugt eine Neue XML Nachricht
     *
     * @param msg Nachricht
     */
    public void newXMLMessage(String msg) {
        //Message Parse und Objekte erstellen
            logger.info("newXMLMessage() " + connectionID);
            logger.debug("newXMLMessage() " + connectionID + msg);
        return;
    }

    
    /**
     * Erzeugt eine
     *
     * @param msg
     */
    public void newHttpGETMessage(byte[] msg) {
        try {
            logger.debug(connectionID + "newGetMessage()");
        } catch (NullPointerException ex) {
            logger.error(connectionID + "newGetMessage():NullPoint" + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    } 
}

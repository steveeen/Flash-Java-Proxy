/**
 * @(#) Property.java 22.04.2005 Version 1.0
 * @author Jens Buettner
 */
package server;

import java.io.*;
import java.util.*;

/**
 * Klasse fuer die das laden von Einstellungen
 */
public class Property {

    /**
     * 
     */
    private Properties prop;
    /**
     * 
     */
    public final static String FILESEPARATOR = System.getProperties().getProperty("file.separator", "\n").toString();

    /**
     * Konstruktor
     */
    public Property() {
        prop = new Properties();
    }

    /**
     * laedt eine Property datei die im xml format ist
     * @param filename 
     * @throws java.io.IOException 
     */
    public void loadProperty(String filename) throws IOException {
        prop.loadFromXML(new FileInputStream(filename));
    }

    /**
     * erstellt einen eintrag fuer eine Property datei
     * @param key 
     * @param value 
     */
    public void createProperty(String key, String value) {
        prop.setProperty(key, value);
    }

    /**
     * gibt den wert des uebergeben keys der geladenen Property datei wieder
     * @param key 
     * @return 
     */
    public String getProperty(String key) {
        return prop.getProperty(key);
    }

    /**
     * schreibt die neu hinzugefuegten Propertys in eine neue datei im xml format
     * @param filename 
     * @param version 
     * @throws java.io.IOException 
     */
    public void writeProperty(String filename, String version) throws IOException {
        FileOutputStream output = new FileOutputStream(filename);
        prop.storeToXML(output, version);
        output.flush();
        output.close();
    }
}

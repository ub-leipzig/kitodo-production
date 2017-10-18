/*
 *
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */
package de.sub.goobi.metadaten;

import de.sub.goobi.config.ConfigCore;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Bean für die Sperrung der Metadaten.
 */
public class MetadatenSperrung implements Serializable {
    private static final long serialVersionUID = -8248209179063050307L;
    private static HashMap<Integer, HashMap<String, String>> sperrungen = new HashMap<Integer, HashMap<String, String>>();
    /*
     * Zeit, innerhalb der der Benutzer handeln muss, um seine Sperrung zu
     * behalten (30 min)
     */
    private static final long sperrzeit = ConfigCore.getLongParameter("MetsEditorLockingTime", 30 * 60 * 1000);

    /**
     * Metadaten eines bestimmten Prozesses wieder freigeben.
     */
    public void setFree(int prozessID) {
        if (sperrungen.containsKey(prozessID)) {
            sperrungen.remove(prozessID);
        }
    }

    /**
     * Metadaten eines bestimmten Prozesses für einen Benutzer sperren.
     */
    public void setLocked(int prozessID, String benutzerID) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("Benutzer", benutzerID);
        map.put("Lebenszeichen", String.valueOf(System.currentTimeMillis()));
        sperrungen.put(prozessID, map);
    }

    /**
     * prüfen, ob bestimmte Metadaten noch durch anderen Benutzer gesperrt sind.
     */
    public static boolean isLocked(int prozessID) {
        HashMap<String, String> temp = sperrungen.get(prozessID);
        /* wenn der Prozess nicht in der Hashpmap ist, ist er nicht gesperrt */
        if (temp == null) {
            return false;
        } else {
            /* wenn er in der Hashmap ist, muss die Zeit geprüft werden */
            long lebenszeichen = Long.parseLong(temp.get("Lebenszeichen"));
            return lebenszeichen >= System.currentTimeMillis() - sperrzeit;
        }
    }

    /**
     * Java doc.
     *
     * @param inBenutzerID
     *            Integer
     */
    public void alleBenutzerSperrungenAufheben(Integer inBenutzerID) {
        String inBenutzerString = String.valueOf(inBenutzerID.intValue());
        HashMap<Integer, HashMap<String, String>> temp = new HashMap<Integer, HashMap<String, String>>(sperrungen);
        for (Iterator<Integer> iter = temp.keySet().iterator(); iter.hasNext();) {
            Integer myKey = iter.next();
            HashMap<String, String> intern = sperrungen.get(myKey);
            if (intern.get("Benutzer").equals(inBenutzerString)) {
                sperrungen.remove(myKey);
            }
        }
    }

    /**
     * Benutzer zurückgeben, der Metadaten gesperrt hat.
     */
    public String getLockBenutzer(int prozessID) {
        String rueckgabe = "-1";
        HashMap<String, String> temp = sperrungen.get(prozessID);
        /*
         * wenn der Prozess nicht in der Hashpmap ist, gibt es keinen Benutzer
         */
        if (temp != null) {
            rueckgabe = temp.get("Benutzer");
        }
        return rueckgabe;
    }

    /**
     * Remove lock for process.
     *
     * @param prozessID
     *            Id of process to unlock
     */
    public static void unlockProcess(int prozessID) {
        HashMap<String, String> temp = sperrungen.get(prozessID);
        /* wenn der Prozess in der Hashpmap ist, dort rausnehmen */
        if (temp != null) {
            sperrungen.remove(prozessID);
        }
    }

    /**
     * Sekunden zurückgeben, seit der letzten Bearbeitung der Metadaten.
     */
    public long getLockSekunden(int prozessID) {
        HashMap<String, String> temp = sperrungen.get(prozessID);
        /* wenn der Prozess nicht in der Hashmap ist, gibt es keine Zeit */
        if (temp == null) {
            return 0;
        } else {
            return (System.currentTimeMillis() - Long.parseLong(temp.get("Lebenszeichen"))) / 1000;
        }
    }
}

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
import de.sub.goobi.helper.TreeNode;

import java.util.ArrayList;

import ugh.dl.DocStruct;

public class TreeNodeStruct3 extends TreeNode {

    private DocStruct struct;
    private String firstImage;
    private String lastImage;
    private String zblNummer;
    private String mainTitle;
    private String ppnDigital;
    private String identifier;
    private String zblSeiten;
    private boolean einfuegenErlaubt;

    /**
     * Konstruktoren.
     */
    public TreeNodeStruct3() {
    }

    public TreeNodeStruct3(boolean expanded, String label, String id) {
        this.expanded = expanded;
        this.label = label;
        this.id = id;
        // TODO: Use generics
        this.children = new ArrayList<TreeNode>();
    }

    public TreeNodeStruct3(String label, DocStruct struct) {
        this.label = label;
        this.struct = struct;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getMainTitle() {

        int maxSize = ConfigCore.getIntParameter("MetsEditorMaxTitleLength", 0);
        if (maxSize > 0 && this.mainTitle != null && this.mainTitle.length() > maxSize) {
            return this.mainTitle.substring(0, maxSize - 1);
        }

        return this.mainTitle;
    }

    public void setMainTitle(String mainTitle) {
        this.mainTitle = mainTitle;
    }

    public String getPpnDigital() {
        return this.ppnDigital;
    }

    public void setPpnDigital(String ppnDigital) {
        this.ppnDigital = ppnDigital;
    }

    public String getFirstImage() {
        return this.firstImage;
    }

    public void setFirstImage(String firstImage) {
        this.firstImage = firstImage;
    }

    public String getLastImage() {
        return this.lastImage;
    }

    public void setLastImage(String lastImage) {
        this.lastImage = lastImage;
    }

    public DocStruct getStruct() {
        return this.struct;
    }

    public void setStruct(DocStruct struct) {
        this.struct = struct;
    }

    public String getZblNummer() {
        return this.zblNummer;
    }

    public void setZblNummer(String zblNummer) {
        this.zblNummer = zblNummer;
    }

    public String getDescription() {
        return this.label;
    }

    public void setDescription(String description) {
        this.label = description;
    }

    public boolean isEinfuegenErlaubt() {
        return this.einfuegenErlaubt;
    }

    public void setEinfuegenErlaubt(boolean einfuegenErlaubt) {
        this.einfuegenErlaubt = einfuegenErlaubt;
    }

    public String getZblSeiten() {
        return this.zblSeiten;
    }

    public void setZblSeiten(String zblSeiten) {
        this.zblSeiten = zblSeiten;
    }

}

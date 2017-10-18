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
package org.goobi.production.enums;

public enum StepReturnValue {

    Finished(0, "Step finished"), InvalidData(1, "Invalid data"), NoData(2, "No data found"), DataAllreadyExists(3,
            "Data already exists"), WriteError(4, "Data could not be written");

    private int id;
    private String value;

    private StepReturnValue(int id, String title) {
        this.setId(id);
        this.setValue(title);
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public void setValue(String title) {
        this.value = title;
    }

    public String getValue() {
        return this.value;
    }

    public static StepReturnValue getByValue(String title) {
        for (StepReturnValue t : StepReturnValue.values()) {
            if (t.getValue().equals(title)) {
                return t;
            }
        }
        return null;
    }

    public static StepReturnValue getById(int id) {
        for (StepReturnValue t : StepReturnValue.values()) {
            if (t.getId() == id) {
                return t;
            }
        }
        return null;
    }
}

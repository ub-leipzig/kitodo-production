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
package de.sub.goobi.forms;

import de.sub.goobi.config.ConfigCore;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.Page;

import java.io.File;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.kitodo.data.database.beans.Docket;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.data.exceptions.DataException;
import org.kitodo.services.ServiceManager;
import org.kitodo.services.data.ProcessService;

@Named("DocketForm")
@SessionScoped
public class DocketForm extends BasisForm {
    private static final long serialVersionUID = -445707928042517243L;
    private Docket myDocket = new Docket();
    private transient ServiceManager serviceManager = new ServiceManager();
    private static final Logger logger = LogManager.getLogger(DocketForm.class);
    private int docketId;

    public String Neu() {
        this.myDocket = new Docket();
        this.docketId = 0;
        return "/newpages/DocketEdit?faces-redirect=true";
    }

    /**
     * Save docket.
     *
     * @return page or empty String
     */
    public String save() {
        try {
            if (hasValidRulesetFilePath(myDocket, ConfigCore.getParameter("xsltFolder"))) {
                this.serviceManager.getDocketService().save(myDocket);
                return "/newpages/DocketList?faces-redirect=true";
            } else {
                Helper.setFehlerMeldung("DocketNotFound");
                return null;
            }
        } catch (DataException e) {
            Helper.setFehlerMeldung("fehlerNichtSpeicherbar", e.getMessage());
            logger.error(e);
            return null;
        }
    }

    private boolean hasValidRulesetFilePath(Docket d, String pathToRulesets) {
        File rulesetFile = new File(pathToRulesets + d.getFile());
        return rulesetFile.exists();
    }

    /**
     * Delete docket.
     *
     * @return page or empty String
     */
    public String deleteDocket() {
        try {
            if (hasAssignedProcesses(myDocket)) {
                Helper.setFehlerMeldung("DocketInUse");
                return null;
            } else {
                this.serviceManager.getDocketService().remove(this.myDocket);
            }
        } catch (DataException e) {
            Helper.setFehlerMeldung("fehlerNichtLoeschbar", e.getMessage());
            return null;
        }
        return "/newpages/DocketList?faces-redirect=true";
    }

    private boolean hasAssignedProcesses(Docket d) throws DataException {
        ProcessService processService = serviceManager.getProcessService();
        Integer number = processService.findByDocket(d).size();
        return number > 0;
    }

    /**
     * No filter.
     *
     * @return page or empty String
     */
    public String filterKein() {
        try {
            // HibernateUtil.clearSession();
            Session session = Helper.getHibernateSession();
            // session.flush();
            session.clear();
            Criteria crit = session.createCriteria(Docket.class);
            crit.addOrder(Order.asc("title"));
            this.page = new Page(crit, 0);
        } catch (HibernateException he) {
            Helper.setFehlerMeldung("fehlerBeimEinlesen", he.getMessage());
            return null;
        }
        return "/newpages/DocketList";
    }

    /**
     * This method initializes the docket list without any filter whenever the bean is constructed.
     */
    @PostConstruct
    public void initializeDocketList() {
        filterKein();
    }

    public String filterKeinMitZurueck() {
        filterKein();
        return this.zurueck;
    }

    /**
     * Method being used as viewAction for docket edit form.
     * If 'docketId' is '0', the form for creating a new docket will be displayed.
     */
    public void loadDocket() {
        try {
            if (!Objects.equals(this.docketId, 0)) {
                setMyDocket(this.serviceManager.getDocketService().find(this.docketId));
            }
        } catch (DAOException e) {
            Helper.setFehlerMeldung("Error retrieving docket with ID '" + this.docketId + "'; ", e.getMessage());
        }
    }

    /*
     * Getter und Setter
     */

    public Docket getMyDocket() {
        return this.myDocket;
    }

    public void setMyDocket(Docket docket) {
        Helper.getHibernateSession().clear();
        this.myDocket = docket;
    }

    public int getDocketId() {
        return this.docketId;
    }

    public void setDocketId(int id) {
        this.docketId = id;
    }
}

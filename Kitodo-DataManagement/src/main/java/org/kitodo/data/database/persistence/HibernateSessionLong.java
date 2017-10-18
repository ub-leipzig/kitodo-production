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
package org.kitodo.data.database.persistence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import java.io.Serializable;

// TODO: Fix for Hibernate-Session-Management, old Version reactivated

@Named("HibernateSessionLong")
@SessionScoped
public class HibernateSessionLong implements Serializable {
    private static final Logger logger = LogManager.getLogger(HibernateSessionLong.class);

    protected static SessionFactory factory;
    private Session sess;

    /**
     * ONLY ever call this method from within the context of a servlet request
     * (specifically, one that has been associated with this filter). If you
     * want a Hibernate session at some other time, call getSessionFactory() and
     * open/close the session yourself.
     *
     * @return an appropriate Session object
     */
    @SuppressWarnings("deprecation")
    public Session getSession() throws HibernateException {

        if (this.sess == null) {
            if (factory == null) {
                logger.debug("getSession() - hibernate-Factory initialisation");
                factory = new Configuration().configure().buildSessionFactory();
            }
            logger.debug("getSession() - hibernate-Session initialisation");
            this.sess = factory.openSession();

        }
        if (!this.sess.isOpen()) {
            this.sess = factory.openSession();
        }

        return this.sess;
    }

    /**
     * Get Session Factory.
     *
     * @return the hibernate session factory
     */
    @SuppressWarnings("deprecation")
    public static SessionFactory getSessionFactory() {
        if (factory == null) {
            logger.debug("getSessionFactory() - hibernate-Factory initialisation");
            factory = new Configuration().configure().buildSessionFactory();
        }
        return factory;
    }

    /**
     * This is a simple method to reduce the amount of code that needs to be
     * written every time hibernate is used.
     */
    public static void rollback(Transaction tx) {
        if (tx != null) {
            try {
                tx.rollback();
            } catch (HibernateException ex) {
                // Probably don't need to do anything - this is likely being
                // called because of another exception, and we don't want to
                // mask it with yet another exception.
            }
        }
    }
}

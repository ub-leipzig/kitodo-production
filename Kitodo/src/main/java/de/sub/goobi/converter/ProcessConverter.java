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
package de.sub.goobi.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kitodo.data.database.beans.Process;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.services.ServiceManager;
import org.kitodo.services.data.ProcessService;

public class ProcessConverter implements Converter {
    private final ServiceManager serviceManager = new ServiceManager();
    public static final String CONVERTER_ID = "ProcessConverter";
    private static final Logger logger = LogManager.getLogger(ProcessConverter.class);

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
        if (value == null) {
            return null;
        } else {
            try {
                return serviceManager.getProcessService().find(Integer.valueOf(value));
            } catch (NumberFormatException e) {
                logger.error(e);
                return "0";
            } catch (DAOException e) {
                logger.error(e);
                return "0";
            }
        }
    }

    /**
     * Replace ProzessDAO with ProcessService.
     *
     * @return a new ProzessDAO
     */
    public ProcessService getProzessService() {
        return serviceManager.getProcessService();
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) throws ConverterException {
        if (value == null) {
            return null;
        } else if (value instanceof Process) {
            return String.valueOf(((Process) value).getId().intValue());
        } else if (value instanceof String) {
            return (String) value;
        } else {
            throw new ConverterException("Falscher Typ: " + value.getClass() + " muss 'Prozess' sein!");
        }
    }

}

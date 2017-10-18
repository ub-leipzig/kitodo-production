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
package org.goobi.production.plugin.interfaces;

import net.xeoh.plugins.base.Plugin;

import org.goobi.production.enums.PluginType;

public interface IPlugin extends Plugin {

    public PluginType getType();

    public String getTitle();

    public String getDescription();

}

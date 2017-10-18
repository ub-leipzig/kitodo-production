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
package org.goobi.production.flow.statistics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import de.intranda.commons.chart.results.DataTable;
import de.schlichtherle.io.File;
import de.sub.goobi.config.ConfigCore;

import java.net.URI;
import java.util.Locale;

import org.goobi.production.flow.statistics.enums.StatisticsMode;
import org.goobi.production.flow.statistics.hibernate.UserDefinedFilter;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Crashes")
public class StatisticsRenderingElementTest {

    Locale locale = new Locale("GERMAN");
    IDataSource testFilter = new UserDefinedFilter("stepdone:5");
    StatisticsManager testManager = new StatisticsManager(StatisticsMode.THROUGHPUT, testFilter, locale);

    private DataTable inDataTable = new DataTable("testTable");
    private IStatisticalQuestion inQuestion = testManager.getStatisticMode().getStatisticalQuestion();
    private StatisticsRenderingElement testElement = new StatisticsRenderingElement(inDataTable, inQuestion);
    private static URI tempPath;

    @BeforeClass
    public static void setUp() {
        File f = new File("pages/imagesTemp/");
        tempPath = f.toURI();
    }

    @Test
    public final void testStatisticsRenderingElement() {
        StatisticsRenderingElement testStatisticsRenderingElement = new StatisticsRenderingElement(inDataTable,
                inQuestion);
        assertNotNull(testStatisticsRenderingElement);
    }

    @Test
    public final void testCreateRenderer() {
        ConfigCore.setImagesPath(tempPath);
        testElement.createRenderer(true);

    }

    @Test
    public final void testGetDataTable() {
        assertEquals("testTable", testElement.getDataTable().getName());

    }

    @Test
    public final void testGetHtmlTableRenderer() {
        ConfigCore.setImagesPath(tempPath);
        testElement.getHtmlTableRenderer();
    }

    @Test
    public final void testGetTitle() {
        assertNotNull(testElement.getTitle());
    }

    @Test
    public final void testGetImageUrl() {
        ConfigCore.setImagesPath(tempPath);
        testElement.createRenderer(true);
        assertNotNull(testElement.getImageUrl());
    }

}

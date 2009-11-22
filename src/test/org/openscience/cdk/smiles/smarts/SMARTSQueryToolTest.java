/* $Revision$ $Author$ $Date$
 * 
 * Copyright (C) 2007  Rajarshi Guha <>
 *
 * Contact: cdk-devel@lists.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.openscience.cdk.smiles.smarts;

import static java.util.Collections.sort;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openscience.cdk.CDKTestCase;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.CDKHueckelAromaticityDetector;
import org.openscience.cdk.config.Elements;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.nonotify.NoNotificationChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.templates.MoleculeFactory;

/**
 * JUnit test routines for the SMARTS substructure search.
 *
 * @author Rajarshi Guha
 * @cdk.module test-smarts
 * @cdk.require ant1.6
 */
public class SMARTSQueryToolTest extends CDKTestCase {

    /**
     * @throws CDKException
     * @cdk.bug 2788357
     */
    @Test(expected = CDKException.class)
    public void testLexicalError() throws Exception {
        SMARTSQueryTool sqt = new SMARTSQueryTool("Epoxide");
    }

    @Test
    public void testQueryTool() throws Exception {
        SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        IAtomContainer atomContainer = sp.parseSmiles("CC(=O)OC(=O)C");
        SMARTSQueryTool querytool = new SMARTSQueryTool("O=CO");

        boolean status = querytool.matches(atomContainer);
        Assert.assertTrue(status);

        int nmatch = querytool.countMatches();
        Assert.assertEquals(2, nmatch);

        List<Integer> map1 = new ArrayList<Integer>();
        map1.add(1);
        map1.add(2);
        map1.add(3);

        List<Integer> map2 = new ArrayList<Integer>();
        map2.add(3);
        map2.add(4);
        map2.add(5);

        List<List<Integer>> mappings = querytool.getMatchingAtoms();
        List<Integer> ret1 = mappings.get(0);
        sort(ret1);
        for (int i = 0; i < 3; i++) {
            Assert.assertEquals(map1.get(i), ret1.get(i));
        }

        List<Integer> ret2 = mappings.get(1);
        sort(ret2);
        for (int i = 0; i < 3; i++) {
            Assert.assertEquals(map2.get(i), ret2.get(i));
        }
    }

    @Test
    public void testQueryToolSingleAtomCase() throws Exception {
        SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        IAtomContainer atomContainer = sp.parseSmiles("C1CCC12CCCC2");
        SMARTSQueryTool querytool = new SMARTSQueryTool("C");

        boolean status = querytool.matches(atomContainer);
        Assert.assertTrue(status);

        int nmatch = querytool.countMatches();
        Assert.assertEquals(8, nmatch);
    }

    @Test
    public void testQueryToolResetSmarts() throws Exception {
        SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        IAtomContainer atomContainer = sp.parseSmiles("C1CCC12CCCC2");
        SMARTSQueryTool querytool = new SMARTSQueryTool("C");

        boolean status = querytool.matches(atomContainer);
        Assert.assertTrue(status);

        int nmatch = querytool.countMatches();
        Assert.assertEquals(8, nmatch);

        querytool.setSmarts("CC");
        status = querytool.matches(atomContainer);
        Assert.assertTrue(status);

        nmatch = querytool.countMatches();
        Assert.assertEquals(18, nmatch);

        List<List<Integer>> umatch = querytool.getUniqueMatchingAtoms();
        Assert.assertEquals(9, umatch.size());
    }

    @Test
    public void testUniqueQueries() throws Exception {
        SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        IAtomContainer atomContainer = sp.parseSmiles("c1ccccc1CCCNCCCc1ccccc1");
        CDKHueckelAromaticityDetector.detectAromaticity(atomContainer);
        SMARTSQueryTool querytool = new SMARTSQueryTool("c1ccccc1");

        boolean status = querytool.matches(atomContainer);
        Assert.assertTrue(status);

        int nmatch = querytool.countMatches();
        Assert.assertEquals(24, nmatch);

        List<List<Integer>> umatch = querytool.getUniqueMatchingAtoms();
        Assert.assertEquals(2, umatch.size());
    }

    @Test
    public void testQuery() throws Exception {
        SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        IAtomContainer atomContainer = sp.parseSmiles("c12cc(CCN)ccc1c(COC)ccc2");
        CDKHueckelAromaticityDetector.detectAromaticity(atomContainer);
        SMARTSQueryTool querytool = new SMARTSQueryTool("c12ccccc1cccc2");

        boolean status = querytool.matches(atomContainer);
        Assert.assertTrue(status);

        int nmatch = querytool.countMatches();
        Assert.assertEquals(4, nmatch);

        List<List<Integer>> umatch = querytool.getUniqueMatchingAtoms();
        Assert.assertEquals(1, umatch.size());
    }

    /**
     * Note that we don't test the generated SMILES against the
     * molecule obtained from the factory since the factory derived
     * molecule does not have an explicit hydrogen, which it really should
     * have.
     *
     * @cdk.bug 1985811
     */
    @Test
    public void testIndoleAgainstItself() throws Exception {

        IMolecule indole = MoleculeFactory.makeIndole();

        SmilesGenerator generator = new SmilesGenerator();
        generator.setUseAromaticityFlag(true);
        String indoleSmiles = generator.createSMILES(indole);

        SmilesParser smilesParser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        indole = smilesParser.parseSmiles(indoleSmiles);

        SMARTSQueryTool querytool = new SMARTSQueryTool(indoleSmiles);
        Assert.assertTrue(querytool.matches(indole));
    }

    /**
     * @cdk.bug 2149621
     */
    @Test
    public void testMethane() throws Exception {
        IMolecule methane =
             NoNotificationChemObjectBuilder.getInstance().newInstance(IMolecule.class);
        IAtom carbon = methane.getBuilder().newInstance(IAtom.class,Elements.CARBON);
        methane.addAtom(carbon);

        SMARTSQueryTool sqt = new SMARTSQueryTool("CC");
        boolean matches = sqt.matches(methane);
        Assert.assertFalse(matches);

    }
}

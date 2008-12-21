/* $RCSfile$
 * $Author$    
 * $Date$    
 * $Revision$
 * 
 * Copyright (C) 1997-2007  The Chemistry Development Kit (CDK) project
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
 * 
 */
package org.openscience.cdk.nonotify;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openscience.cdk.interfaces.IAdductFormula;
import org.openscience.cdk.interfaces.IAdductFormulaTest;

/**
 * Checks the functionality of the {@link NNAdductFormula}.
 *
 * @cdk.module test-nonotify
 */
public class NNAdductFormulaTest extends IAdductFormulaTest {

    @BeforeClass public static void setUp() {
        setBuilder(NoNotificationChemObjectBuilder.getInstance());
    }

    @Test public void testNNAdductFormula() {
        IAdductFormula mfS = new NNAdductFormula();
        Assert.assertNotNull(mfS);
    }

    @Test public void testNNAdductFormula_IMolecularFormula() {
        IAdductFormula mfS = new NNAdductFormula(getBuilder().newMolecularFormula());
        Assert.assertEquals(1, mfS.size());
    }

}

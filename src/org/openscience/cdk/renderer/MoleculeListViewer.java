/*  
 * $RCSfile$    $Author$    $Date$    $Revision$
 * 
 * Copyright (C) 1997-2001  The Chemistry Development Kit (CDK) project
 * 
 * Contact: steinbeck@ice.mpg.de, gezelter@maul.chem.nd.edu, egonw@sci.kun.nl
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 * All we ask is that proper credit is given for our work, which includes
 * - but is not limited to - adding the above copyright notice to the beginning
 * of your source code files, and to any copyright notice that you may distribute
 * with programs based on this work.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *  
 */

package org.openscience.cdk.renderer;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.util.Vector;
import java.util.zip.*;
import java.io.*;

public class MoleculeListViewer extends JFrame
{
	protected StrucContainer panel;

	protected JScrollPane scrollPane;

	protected int noOfStructures = 0;

	/** 
	  * The dimension of a single MoleculeViewerPanel
	  */
	protected Dimension molViewDim = new Dimension(250,250);

	protected Vector moleculeViewerPanels = new Vector();

	public static boolean standAlone = false;

	public MoleculeListViewer()
	{
		super();
		getContentPane().setLayout(new BorderLayout());
		setTitle("Structure Display");
		panel = new StrucContainer();
		scrollPane = new JScrollPane(panel);
		scrollPane.setPreferredSize(new Dimension(800, 600));
		getContentPane().add("Center",scrollPane);
		setTitle("MoleculeListViewer");
		pack();
		setVisible(true);
	}

	public void addStructure(MoleculeViewer2D mv)
	{
		String title = "Structure no. " + (noOfStructures + 1);
		addStructure(mv,title);
	}

	public void addStructure(MoleculeViewer2D mv, String title)
	{
		noOfStructures++;
		mv.setPreferredSize(molViewDim);
		mv.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title));
		moleculeViewerPanels.addElement(mv);
		panel.add(mv);
		panel.revalidate();
	}
	
	public void paint(Graphics g)
	{
		super.paint(g);
		panel.revalidate();
	}

	class StrucContainer extends JPanel
	{
		public StrucContainer()
		{
			super();
			setLayout(new FlowLayout(FlowLayout.LEFT,0,0));
		}

		public Dimension getPreferredSize()
		{
			int width, height;
			width = getContentPane().getSize().width;
			if (width < molViewDim.width)
			{
				width = molViewDim.width;
			}
			height = ((noOfStructures / ((int)width / molViewDim.width)) + 1) * molViewDim.height;
			height = (int)(height * 1.2);
			return new Dimension(width,height);
		}

		public Rectangle getBounds()
		{
			return new Rectangle(new java.awt.Point(0,0),getPreferredSize());
		}

	}

	public static void main(String[] args)
	{
		MoleculeListViewer mlv = new MoleculeListViewer();
		mlv.standAlone = true;
		mlv.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}

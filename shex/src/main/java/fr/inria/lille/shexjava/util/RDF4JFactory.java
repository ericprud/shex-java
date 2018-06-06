/*******************************************************************************
 * Copyright (C) 2018 Université de Lille - Inria
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package fr.inria.lille.shexjava.util;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.impl.AbstractValueFactory;

/** Implementation of RDFFactory that to do not generate a label if one is provided for bnode.
 * @author Jérémie Dusart
 *
 */
public class RDF4JFactory extends AbstractValueFactory {

	@Override
	public BNode createBNode(String nodeID) {
		if (nodeID.startsWith("genid-")) {
			String originalSt = nodeID.substring(39);
			if (originalSt.length()>0)
				return super.createBNode(originalSt);				
		}
		return super.createBNode(nodeID);
	}

	public static String MyBnodePrefix = "GENERATEDLABELFORBNODE#^§%*$";
	
	@Override
	public synchronized BNode createBNode() {
		BNode result = super.createBNode();
		return createBNode(MyBnodePrefix+result.stringValue());
	}

	private static final RDF4JFactory sharedInstance = new RDF4JFactory();

	/**
	 * Provide a single shared instance of a SimpleValueFactory.
	 * 
	 * @return a singleton instance of SimpleValueFactory.
	 */
	public static RDF4JFactory getInstance() {
		return sharedInstance;
	}

	/**
	 * Hidden constructor to enforce singleton pattern.
	 */
	protected RDF4JFactory() {
	}

}

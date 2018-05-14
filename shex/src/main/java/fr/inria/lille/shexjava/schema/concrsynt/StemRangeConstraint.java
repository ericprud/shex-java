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
package fr.inria.lille.shexjava.schema.concrsynt;

import org.apache.commons.rdf.api.RDFTerm;

/**
 * @author Jérémie Dusart
 *
 */
public abstract class StemRangeConstraint implements Constraint {
	protected Constraint stem;
	protected ValueSetValueConstraint exclusions;
	
	public StemRangeConstraint(Constraint stem,ValueSetValueConstraint exclusions) {
		this.stem = stem;
		this.exclusions = exclusions;
	}

	@Override
	public boolean contains(RDFTerm node) {
		if (stem!=null)
			if (!stem.contains(node))
			 return false;
		
		if (exclusions.contains(node))
			return false;
		
		return true;
	}
	
	public Constraint getStem() {
		return stem;
	}

	public ValueSetValueConstraint getExclusions() {
		return exclusions;
	}
	
	
}

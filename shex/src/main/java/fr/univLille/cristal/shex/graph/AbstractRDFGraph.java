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
package fr.univLille.cristal.shex.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;

/** Helper implementation of {@link RDF}.
 * Implementing classes need to implement only the methods {@link RDFGraph#getAllNodes()}, {@link #itInNeighbours(RDFNode)} and {@link #itOutNeighbours(Resource)}.
 * 
 * @author Iovka Boneva
 * @author Antonin Durey
 * 
 */
abstract class AbstractRDFGraph implements RDFGraph {
	public List<NeighborTriple> listAllNeighbours (Value focusNode) {
		return listNeighbours(itAllNeighbours(focusNode,null));
	}
	
	public List<NeighborTriple> listAllNeighboursWithPredicate (Value focusNode,Set<IRI> allowedPredicates){
		List<NeighborTriple> result = new ArrayList<NeighborTriple>();
		for (IRI predicate:allowedPredicates) {
			result.addAll(listNeighbours(itAllNeighbours(focusNode,predicate)));
		}
		return result;
	}

	
	public List<NeighborTriple> listOutNeighbours(Value focusNode) {
		if (! (focusNode instanceof Resource))
			return Collections.emptyList();
		return listNeighbours(itOutNeighbours((Resource) focusNode,null));
	}
	

	public List<NeighborTriple> listOutNeighboursWithPredicate (Value focusNode,Set<IRI> allowedPredicates){
		List<NeighborTriple> result = new ArrayList<NeighborTriple>();
		for (IRI predicate:allowedPredicates) {
			result.addAll(listNeighbours(itOutNeighbours(focusNode,predicate)));
		}
		return result;
	}
	

	public List<NeighborTriple> listInNeighbours(Value focusNode) {
		return listNeighbours(itInNeighbours(focusNode,null));
	}
	
	
	public List<NeighborTriple> listInNeighboursWithPredicate (Value focusNode,Set<IRI> allowedPredicates){
		List<NeighborTriple> result = new ArrayList<NeighborTriple>();
		for (IRI predicate:allowedPredicates) {
			result.addAll(listNeighbours(itInNeighbours(focusNode,predicate)));
		}
		return result;
	}
	
	
	//---------------------------------------------------------------------------
	// Iterators
	//---------------------------------------------------------------------------
	
	protected abstract Iterator<NeighborTriple> itOutNeighbours (Value focusNode, IRI predicate);
	protected abstract Iterator<NeighborTriple> itInNeighbours (Value focusNode, IRI predicate);
	
	private Iterator<NeighborTriple> itAllNeighbours (final Value focusNode,IRI predicate) {
		return new Iterator<NeighborTriple>() {
			Iterator<NeighborTriple> itFw, itBw;
			{
				if (focusNode instanceof Resource)
					itFw = itOutNeighbours((Resource) focusNode,predicate);
				else 
					itFw = new EmptyIterator<>();
				
				itBw = itInNeighbours(focusNode,predicate);
			}
			
			@Override
			public boolean hasNext() {
				return itFw.hasNext() || itBw.hasNext();
			}

			@Override
			public NeighborTriple next() {
				if (itFw.hasNext()) 
					return itFw.next();
				else 
					return itBw.next();
			}
			
		};
	}
	
	private class EmptyIterator<T> implements Iterator<T> {

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public T next() {
			throw new NoSuchElementException();
		}
	}
	
	
	//---------------------------------------------------------------------------
	// Utils
	//---------------------------------------------------------------------------
	
	private List<NeighborTriple> listNeighbours (Iterator<NeighborTriple> it) {
		ArrayList<NeighborTriple> result = new ArrayList<>();
		while (it.hasNext()) {
			result.add(it.next());
		}
		return result;
	}


}

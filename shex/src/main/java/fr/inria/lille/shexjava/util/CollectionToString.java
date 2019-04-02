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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import fr.inria.lille.shexjava.schema.abstrsynt.TripleExpr;

public class CollectionToString {
	public static String collectionToString (Collection<?> list, String separator, String prefix, String suffix) {
		StringBuilder s = new StringBuilder();
		s.append(prefix);
		Iterator<?> it = list.iterator();
		if (it.hasNext()) s.append(it.next());
		while (it.hasNext()) {
			s.append(separator);
			s.append(it.next());
		}
		s.append(suffix);
		return s.toString();
	}
	
	public static String collectionToPrettyString (Collection<TripleExpr> list, String separator, String prefix, String suffix, Map<String,String> prefixes) {
		StringBuilder s = new StringBuilder();
		s.append(prefix);
		Iterator<TripleExpr> it = list.iterator();
		if (it.hasNext()) s.append(it.next().toPrettyString(prefixes));
		while (it.hasNext()) {
			s.append(separator);
			s.append(it.next().toPrettyString(prefixes));
		}
		s.append(suffix);
		return s.toString();
	}
	
}

/**
Copyright 2017 University of Lille

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/

package fr.univLille.cristal.shex.schema.analysis;

import fr.univLille.cristal.shex.schema.abstrsynt.EachOfTripleExpression;
import fr.univLille.cristal.shex.schema.abstrsynt.EmptyTripleExpression;
import fr.univLille.cristal.shex.schema.abstrsynt.RepeatedTripleExpression;
import fr.univLille.cristal.shex.schema.abstrsynt.SomeOfTripleExpression;
import fr.univLille.cristal.shex.schema.abstrsynt.TripleConstraint;
import fr.univLille.cristal.shex.schema.abstrsynt.TripleExpression;

/**
 * 
 * @author Iovka Boneva
 * 10 oct. 2017
 * @param <ResultType>
 */
public abstract class TripleExpressionVisitor<ResultType> {
	
	public abstract ResultType getResult ();

	public abstract void visitTripleConstraint (TripleConstraint tc, Object ... arguments);
	public abstract void visitEmpty(EmptyTripleExpression emptyTripleExpression, Object[] arguments);
	
	public void visitEachOf (EachOfTripleExpression expr, Object ... arguments) {
		for (TripleExpression subExpr : expr.getSubExpressions())
			subExpr.accept(this, arguments);
	}
	
	public void visitSomeOf (SomeOfTripleExpression expr, Object ... arguments) {
		for (TripleExpression subExpr : expr.getSubExpressions())
			subExpr.accept(this, arguments);
	}

	public void visitRepeated(RepeatedTripleExpression expr, Object[] arguments) {
		expr.getSubExpression().accept(this, arguments);
	}

}

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

import java.util.ArrayList;
import java.util.List;

import fr.univLille.cristal.shex.schema.abstrsynt.EachOfTripleExpression;
import fr.univLille.cristal.shex.schema.abstrsynt.EmptyTripleExpression;
import fr.univLille.cristal.shex.schema.abstrsynt.RepeatedTripleExpression;
import fr.univLille.cristal.shex.schema.abstrsynt.SomeOfTripleExpression;
import fr.univLille.cristal.shex.schema.abstrsynt.TripleConstraint;
import fr.univLille.cristal.shex.schema.abstrsynt.TripleExpression;
import fr.univLille.cristal.shex.util.Interval;

/**
 * 
 * @author Iovka Boneva
 * 10 oct. 2017
 */
public class ComputeUnfoldedArbitraryRepetitionsVisitor extends TripleExpressionVisitor<TripleExpression> {
	
	private TripleExpression unfoldedExpr;
	private boolean hasChanged;
	
	
	@Override
	public TripleExpression getResult() {
		return unfoldedExpr;
	}

	@Override
	public void visitTripleConstraint(TripleConstraint tc, Object... arguments) {
		unfoldedExpr = tc;
		hasChanged = false;
	}

	@Override
	public void visitEmpty(EmptyTripleExpression emptyTripleExpression, Object[] arguments) {
		unfoldedExpr = emptyTripleExpression;
		hasChanged = false;
	}

	@Override
	public void visitRepeated(RepeatedTripleExpression expr, Object[] arguments) {
		if (expr.getCardinality().equals(Interval.PLUS) 
				|| expr.getCardinality().equals(Interval.STAR)
				|| expr.getSubExpression() instanceof TripleConstraint) {
			expr.getSubExpression().accept(this);
			if (hasChanged)
				unfoldedExpr = new RepeatedTripleExpression(unfoldedExpr, expr.getCardinality());
			else
				unfoldedExpr = expr;
		}
		else {
			expr.getSubExpression().accept(this);
			List<TripleExpression> clones = new ArrayList<>();
			int nbRepetitions, nbOptRepetitions;
			if (expr.getCardinality().max == Interval.UNBOUND) {
				clones.add(new RepeatedTripleExpression(expr.getSubExpression(), Interval.PLUS));
				nbRepetitions = expr.getCardinality().min-1;
				nbOptRepetitions = 0;
			} else {
				nbRepetitions = expr.getCardinality().min;
				nbOptRepetitions = expr.getCardinality().max - expr.getCardinality().min;
			}
			
			for (int i = 0; i < nbRepetitions; i++) {
				clones.add(clone(unfoldedExpr));
			}
			for (int i = 0; i < nbOptRepetitions; i++) {
				clones.add(new RepeatedTripleExpression(clone(unfoldedExpr), Interval.OPT));
			}
			unfoldedExpr = new EachOfTripleExpression(clones);
			hasChanged = true;
		}
	}
	
	@Override
	public void visitEachOf(EachOfTripleExpression expr, Object... arguments) {
		boolean changed = false;
		List<TripleExpression> newSubExpr = new ArrayList<>();
		for (TripleExpression subExpr: expr.getSubExpressions()) {
			subExpr.accept(this);
			changed = changed || hasChanged;
			newSubExpr.add(unfoldedExpr);
		}
		if (! changed) {
			hasChanged = false;
			unfoldedExpr = expr;
		}
		else {
			hasChanged = true;
			unfoldedExpr = new EachOfTripleExpression(newSubExpr);
		}
	}
	
	@Override
	public void visitSomeOf(SomeOfTripleExpression expr, Object... arguments) {
		boolean changed = false;
		List<TripleExpression> newSubExpr = new ArrayList<>();
		for (TripleExpression subExpr: expr.getSubExpressions()) {
			subExpr.accept(this);
			changed = changed || hasChanged;
			newSubExpr.add(unfoldedExpr);
		}
		if (! changed) {
			hasChanged = false;
			unfoldedExpr = expr;
		}
		else {
			hasChanged = true;
			unfoldedExpr = new SomeOfTripleExpression(newSubExpr);
		}
	}
	
	private TripleExpression clone (TripleExpression expr) {
		CloneTripleExpressionVisitor visitor = new CloneTripleExpressionVisitor();
		expr.accept(visitor);
		return visitor.getResult();
	}
	
	class CloneTripleExpressionVisitor extends TripleExpressionVisitor<TripleExpression> {

		private TripleExpression result;
		
		@Override
		public TripleExpression getResult() {
			return result;
		}

		@Override
		public void visitTripleConstraint(TripleConstraint tc, Object... arguments) {
			result = tc.clone();
		}

		@Override
		public void visitEmpty(EmptyTripleExpression emptyTripleExpression, Object[] arguments) {
			result = new EmptyTripleExpression();
		}
		
		@Override
		public void visitEachOf(EachOfTripleExpression expr, Object... arguments) {
			List<TripleExpression> subExpressions = new ArrayList<>();
			for (TripleExpression subExpr: expr.getSubExpressions()) {
				subExpr.accept(this);
				subExpressions.add(result);
			}
			result = new EachOfTripleExpression(subExpressions);
		}
		
		@Override
		public void visitSomeOf(SomeOfTripleExpression expr, Object... arguments) {
			List<TripleExpression> subExpressions = new ArrayList<>();
			for (TripleExpression subExpr: expr.getSubExpressions()) {
				subExpr.accept(this);
				subExpressions.add(result);
			}
			result = new SomeOfTripleExpression(subExpressions);
		}
		
		@Override
		public void visitRepeated(RepeatedTripleExpression expr, Object[] arguments) {
			expr.getSubExpression().accept(this);
			result = new RepeatedTripleExpression(result, expr.getCardinality());
		}
		
	}
	
	
}

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
package fr.inria.lille.shexjava.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.inria.lille.shexjava.schema.BNodeLabel;
import fr.inria.lille.shexjava.schema.IRILabel;
import org.apache.commons.rdf.api.RDF;

import fr.inria.lille.shexjava.schema.Label;
import fr.inria.lille.shexjava.schema.abstrsynt.EachOf;
import fr.inria.lille.shexjava.schema.abstrsynt.EmptyTripleExpression;
import fr.inria.lille.shexjava.schema.abstrsynt.OneOf;
import fr.inria.lille.shexjava.schema.abstrsynt.RepeatedTripleExpression;
import fr.inria.lille.shexjava.schema.abstrsynt.Shape;
import fr.inria.lille.shexjava.schema.abstrsynt.TripleConstraint;
import fr.inria.lille.shexjava.schema.abstrsynt.TripleExpr;
import fr.inria.lille.shexjava.schema.abstrsynt.TripleExprRef;
import fr.inria.lille.shexjava.schema.analysis.TripleExpressionVisitor;
import fr.inria.lille.shexjava.util.Interval;

/** Allows to compute a SORBE version of a triple expression. 
 * The computation results are memorized and won't be recomputed by further calls.
 * The SORBE version does not contains any triple expression reference, cardinality other than *, ? or + and an empty triple expression with the + cardinality.
 *
 * @author Iovka Boneva
 * @author Jérémie Dusart
 */
public class SORBEGenerator {
	private RDF rdfFactory;
	private int tripleLabelNb = 0;
	public static final String SORBE_TRIPLE_LABEL_SUFFIXE = "_SORBE_";
	
	private Map<Label,TripleExpr> sorbeMap;
	
	public SORBEGenerator(RDF rdfFactory) {
		this.rdfFactory=rdfFactory;
		this.sorbeMap=new HashMap<>();
	}
	
	
	
	
	/** Construct an equivalent triple expression that satisfies the SORBE requirement. 
	 * @param texpr
	 * @return
	 */
	public TripleExpr getSORBETripleExpr(TripleExpr texpr) {
		if (this.sorbeMap.containsKey(texpr.getId()))
			return this.sorbeMap.get(texpr.getId());
		texpr.accept(generatorTE);
		this.sorbeMap.put(texpr.getId(), generatorTE.getResult());
		return generatorTE.getResult();
	}

	/** Returns the label of the original expression for which the expression with the given label is the sorbe version.
	 * 
	 * @param label
	 * @return
	 */
	// TODO this is only used for triple constraints. Make it work with triple constraints only ? 
	// In any case, base this on a manipulation of the labels is weird
	// The labels are used as unique identifiers, and this is "secure" only if all expressions are generated by a factory
	public Label getOriginalNonsorbeVersion(Label label) {
		if (label instanceof BNodeLabel) {
			return new BNodeLabel(rdfFactory.createBlankNode(label.stringValue().split(SORBE_TRIPLE_LABEL_SUFFIXE)[0].substring(2)),
													label.isGenerated());
		}
		return new IRILabel(rdfFactory.createIRI(label.stringValue().split(SORBE_TRIPLE_LABEL_SUFFIXE)[0]),
											  label.isGenerated());
	}
	
	private GeneratorOfTripleExpr generatorTE = new GeneratorOfTripleExpr();
	
	private class GeneratorOfTripleExpr extends TripleExpressionVisitor<TripleExpr> {

		@Override
		public void visitTripleConstraint(TripleConstraint tc, Object... arguments) {
			setResult(tc.clone());
			setTripleLabel(getResult(),tc);
		}

		@Override
		public void visitTripleExprReference(TripleExprRef expr, Object... arguments) {
			expr.getTripleExp().accept(this);			
		}
		
		@Override
		public void visitEachOf (EachOf expr, Object ... arguments) {
			List<TripleExpr> newSubExprs = new ArrayList<TripleExpr>();
			for (TripleExpr subExpr : expr.getSubExpressions()) {
				subExpr.accept(this, arguments);
				newSubExprs.add(getResult());
			}
			setResult(new EachOf(newSubExprs));
			setTripleLabel(getResult(),expr);
		}
		
		@Override
		public void visitOneOf (OneOf expr, Object ... arguments) {
			List<TripleExpr> newSubExprs = new ArrayList<TripleExpr>();
			for (TripleExpr subExpr : expr.getSubExpressions()) {
				subExpr.accept(this, arguments);
				newSubExprs.add(getResult());
			}
			setResult(new OneOf(newSubExprs));
			setTripleLabel(getResult(),expr);
		}
		

		@Override
		public void visitEmpty(EmptyTripleExpression expr, Object[] arguments) {
			setResult(new EmptyTripleExpression());
			setTripleLabel(getResult(),expr);
		}
		
		@Override
		public void visitRepeated(RepeatedTripleExpression expr, Object[] arguments) {
			CheckIfContainsEmpty visitor = new CheckIfContainsEmpty();
			expr.accept(visitor);
			expr.getSubExpression().accept(this);
			if (expr.getCardinality().equals(Interval.PLUS) & visitor.getResult()) {
				setResult(new RepeatedTripleExpression(getResult(),Interval.STAR));
				setTripleLabel(getResult(),expr);
			} else if(expr.getCardinality().equals(Interval.PLUS)
					  || expr.getCardinality().equals(Interval.STAR)
					  || expr.getCardinality().equals(Interval.OPT)
					  || expr.getCardinality().equals(Interval.ZERO)){
				setResult(new RepeatedTripleExpression(getResult(),expr.getCardinality()));
				setTripleLabel(getResult(),expr);
			} else {
				Interval card = expr.getCardinality();
				int nbClones = 0;
				int	nbOptClones = 0;
				List<TripleExpr> clones = new ArrayList<>();

				if (card.max == Interval.UNBOUND) {
					nbClones = card.min -1;
					TripleExpr tmp = new RepeatedTripleExpression(getResult(), Interval.PLUS);
					setTripleLabel(tmp,expr);
					clones.add(tmp);
				}else {
					nbClones = card.min;
					nbOptClones = card.max - card.min;
				}

				for (int i=0; i<nbClones;i++) {
					expr.getSubExpression().accept(this);
					clones.add(result);	
				}
				for (int i=0; i<nbOptClones;i++) {
					expr.getSubExpression().accept(this);
					TripleExpr tmp = new RepeatedTripleExpression(getResult(), Interval.OPT);
					setTripleLabel(tmp,expr);
					clones.add(tmp);
				}
				if (clones.size()==1)
					setResult(clones.get(0));
				else {
					setResult(new EachOf(clones));
					setTripleLabel(getResult(),expr);
				}
			}
		}
		
	}
	
	
	private void setTripleLabel(TripleExpr newTriple,TripleExpr oldTriple) {
		if (oldTriple.getId() instanceof BNodeLabel) {
			String old = oldTriple.getId().stringValue().substring(2);
			newTriple.setId(new BNodeLabel(rdfFactory.createBlankNode(old+SORBE_TRIPLE_LABEL_SUFFIXE+tripleLabelNb),
									  oldTriple.getId().isGenerated()));
		}
		if (oldTriple.getId() instanceof IRILabel)
			newTriple.setId(new IRILabel(rdfFactory.createIRI(oldTriple.getId().stringValue()+SORBE_TRIPLE_LABEL_SUFFIXE+tripleLabelNb),
									  oldTriple.getId().isGenerated()));
		tripleLabelNb++;
	}
	
	
	class CheckIfContainsEmpty extends TripleExpressionVisitor<Boolean>{

		@Override
		public void visitTripleConstraint(TripleConstraint tc, Object... arguments) {
			setResult(false);
		}

		@Override
		public void visitEmpty(EmptyTripleExpression expr, Object[] arguments) {
			setResult(false);
		}

		@Override
		public void visitEachOf(EachOf expr, Object... arguments) {
			for (TripleExpr subExpr : expr.getSubExpressions()) {
				subExpr.accept(this, arguments);
				if (!getResult())
					return;
			}
		}

		@Override
		public void visitOneOf(OneOf expr, Object... arguments) {
			for (TripleExpr subExpr : expr.getSubExpressions()) {
				subExpr.accept(this, arguments);
				if (getResult())
					return;
			}
		}

		@Override
		public void visitRepeated(RepeatedTripleExpression expr, Object[] arguments) {
			if (expr.getCardinality().min == 0) {
				setResult(true);
			} else {
				expr.getSubExpression().accept(this, arguments);
			}
		}

		@Override
		public void visitTripleExprReference(TripleExprRef expr, Object... arguments) {
			expr.getTripleExp().accept(this, arguments);
		}
	}
}

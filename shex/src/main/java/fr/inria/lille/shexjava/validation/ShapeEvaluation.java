/* ******************************************************************************
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

import fr.inria.lille.shexjava.schema.abstrsynt.*;
import fr.inria.lille.shexjava.schema.analysis.ShapeExpressionVisitor;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.api.Triple;

import java.util.*;
import java.util.function.BiPredicate;

/** Allows to evaluate a shape against a node with w.r.t. a typing.
 * @author Iovka Boneva
 */
public class ShapeEvaluation {

    private Graph graph;
    private RDFTerm focusNode;
    private Shape topShape;
    private Typing neighboursTyping;

    private DynamicCollectorOfTripleConstraints collectorTC;
   	protected SORBEGenerator sorbeGenerator;

   	private PreMatching preMatching;

    public ShapeEvaluation(Graph graph, RDFTerm focusNode, Shape shape,
                           Typing neighboursTyping,
                           DynamicCollectorOfTripleConstraints collectorTC,
                           SORBEGenerator sorbeGenerator) {
        this.graph = graph;
        this.focusNode = focusNode;
        this.topShape = shape;
        this.neighboursTyping = neighboursTyping;

        this.collectorTC = collectorTC;
        this.sorbeGenerator = sorbeGenerator;
    }

    /** Evaluates the node given at construction time against the shape given at construction time. */
    public boolean evaluate () {
        List<TripleConstraint> allTripleConstraints = collectorTC.getTCs(topShape);
        List<Triple> focusNodeNeighbourhood = ValidationUtils.getMatchableNeighbourhood(graph, focusNode, allTripleConstraints, topShape.isClosed());
        preMatching = ValidationUtils.computePreMatching(focusNode, focusNodeNeighbourhood, allTripleConstraints,
                topShape.getExtraProperties(), ValidationUtils.getPredicateAndValueMatcher(), valueMatcherWithTyping);

        if (! preMatching.getUnmatched().isEmpty())
            return false;

        focusNodeNeighbourhood.removeAll(preMatching.getMatchedToExtra());
        return evaluateShape(focusNodeNeighbourhood, topShape);
    }

    /** Evaluates part of the neighbourhood against a shape. */
    private boolean evaluateShape (List<Triple> triples, Shape shape) {
        if (shape.getExtended().isEmpty())
            return evaluateShapeWithoutExtended(triples, shape);
        else
            return evaluateShapeWithExtended(triples, shape);
    }

    private boolean evaluateShapeWithoutExtended(List<Triple> triples, Shape shape) {
        return matches(triples, shape.getTripleExpression());
    }

    private boolean evaluateShapeWithExtended (List<Triple> triples, Shape shape) {
        Map<Triple, List<Object>> matchingSubExpressionsOfShape =
                contractPreMatchingToShapeSubExpressions(triples, shape);
        MatchingsIterator<Object> it = new MatchingsIterator<>(matchingSubExpressionsOfShape, triples);

        boolean valid = false;
        while (! valid && it.hasNext()) {
            Matching<Object> m = it.next();
            Map<Object, List<Triple>> partition = ValidationUtils.invertMatching(m);
            valid = evaluatePartition(partition);
        }
        return valid;
    }

    /** Evaluates a partition of (part of) the neighbourhood against expressions.
     * The expressions that are keys of the partition are sub-expressions of a Shape, that is, either one of {@link Shape#getExtended()} or {@link Shape#getTripleExpression()}.
     */
    private boolean evaluatePartition (Map<Object, List<Triple>> partition) {
        boolean failed = false;
        Iterator<Map.Entry<Object, List<Triple>>> partsIterator = partition.entrySet().iterator();
        while (!failed && partsIterator.hasNext()) {
            Map.Entry<Object, List<Triple>> entry = partsIterator.next();
            Object expr = entry.getKey();
            List<Triple> triples = entry.getValue();
            if (expr instanceof TripleExpr) {
                failed = ! matches(triples, (TripleExpr) expr);
            } else if (expr instanceof ShapeExprRef) {
                EvaluateShapeExprOnNeighbourhoodVisitor shexprEval = new EvaluateShapeExprOnNeighbourhoodVisitor(triples);
                ((ShapeExprRef) expr).accept(shexprEval, focusNode);
                failed = ! shexprEval.getResult();
            } else {
                throw new IllegalStateException("should not happen");
            }
        }
        return !failed;
    }


    /** With every triple from {@param triples} associates the set of sub-expressions of {@param shape} to which the triple can be matched.*/
    private Map<Triple, List<Object>> contractPreMatchingToShapeSubExpressions(
            List<Triple> triples,
            Shape shape) {

        Map<Triple, List<Object>> tripleToSubExpr = new HashMap<>(preMatching.getPreMatching().size());
        for (Triple triple: triples)
            tripleToSubExpr.put(triple, new ArrayList<>());
        for (Triple triple: triples) {
            for (TripleConstraint tc : preMatching.getPreMatching().get(triple)) {
                List<Object> list = tripleToSubExpr.get(triple);
                Object parent = collectorTC.getParentInShape(tc, shape);
                if (parent != null) list.add(parent);
            }
        }
        // Remove the repeated expressions
        for (Map.Entry<Triple, List<Object>> e : tripleToSubExpr.entrySet()) {
            e.setValue(new ArrayList<>(new HashSet<>(e.getValue())));
        }
        return tripleToSubExpr;
    }

    /** Checks whether the set of triples satisfies a triple expression.*/
    private boolean matches (List<Triple> triples, TripleExpr tripleExpr) {

        // Enumerate all possible matchings from the triples to the triple constraints and look for a valid matching among them
        TripleExpr sorbeTripleExpr = this.sorbeGenerator.getSORBETripleExpr(tripleExpr);
        List<TripleConstraint> tripleConstraints = collectorTC.getTCs(sorbeTripleExpr);
        PreMatching sorbePreMatching = ValidationUtils.computePreMatching(
                focusNode, triples, tripleConstraints,  Collections.emptySet(),
                               ValidationUtils.getPredicateAndValueMatcher(), valueMatcherWithTyping);
        if (!sorbePreMatching.getUnmatched().isEmpty() || !sorbePreMatching.getMatchedToExtra().isEmpty())
            return false;
        MatchingsIterator<TripleConstraint> mit = new MatchingsIterator<>(sorbePreMatching.getPreMatching());
        Matching<TripleConstraint> result = null;
        while (result == null && mit.hasNext()) {
            Matching<TripleConstraint> matching = mit.next();
            if (isLocallyValid(matching, sorbeTripleExpr, tripleConstraints)) {
                result = matching;
            }
        }
        // TODO we might want to use the matching, in this case it should be translated to a matching on the non sorbe triple expression
        return result != null;
    }

	/** Tests whether the matching satisfies the triple expression locally.
     * I.e. no checking whether the opposite node satisfies the value of the triple constraint. */
	private boolean isLocallyValid (Matching<TripleConstraint> matching, TripleExpr sorbeTripleExpr, List<TripleConstraint> tripleConstraints) {
		IntervalComputation intervalComputation = new IntervalComputation(collectorTC);
		sorbeTripleExpr.accept(intervalComputation, Bag.fromMatching(matching, tripleConstraints));
		return intervalComputation.getResult().contains(1);
	}

	private BiPredicate<RDFTerm,ShapeExpr> valueMatcherWithTyping = new BiPredicate<RDFTerm, ShapeExpr>() {
        @Override
        public boolean test(RDFTerm node, ShapeExpr shapeExpr) {
            shapeExpr.accept(evaluateValueConstraintWithTyping, node);
            return evaluateValueConstraintWithTyping.getResult();
        }
    };


	/** Allows to evaluate a node w.r.t. a shape expression, while using the {@link #neighboursTyping} to determine whether a shape is satisfied (no recursive evaluation of Shapes). */
	private ShapeExpressionVisitor<Boolean> evaluateValueConstraintWithTyping = new AbstractShapeExprEvaluator() {
        @Override
        public void visitShape(Shape expr, Object... arguments) {
            RDFTerm value = (RDFTerm) arguments[0];
            setResult(neighboursTyping.isConformant(value, expr.getId()));
        }
    };

	/** Allows to evaluate a (part of) a neigbourhood against a shape expression.
     * Recursively evaluates the Shapes using {@link #evaluateShape(List, Shape)}, thus using the {@link #neighboursTyping} as the typing for the neighbours.  */
    class EvaluateShapeExprOnNeighbourhoodVisitor extends AbstractShapeExprEvaluator {

        private List<Triple> neighbourhood;

        public EvaluateShapeExprOnNeighbourhoodVisitor(List<Triple> neighbourhood) {
            this.neighbourhood = neighbourhood;
        }

        @Override
        public void visitShape(Shape expr, Object... arguments) {
            // The closed and extra qualifiers are ignored here
            setResult(evaluateShape(neighbourhood, expr));
        }
    }

    /** Allows to evaluate a node against a shape expression, leaving the way Shapes are evaluated to the implementers. */
    public abstract static class AbstractShapeExprEvaluator extends ShapeExpressionVisitor<Boolean> {
        @Override
        public void visitNodeConstraint(NodeConstraint expr, Object... arguments) {
            RDFTerm value = (RDFTerm) arguments[0];
            setResult(expr.contains(value));
        }

        @Override
        public void visitShapeExprRef(ShapeExprRef shapeRef, Object... arguments) {
            shapeRef.getShapeDefinition().accept(this, arguments);
        }

        @Override
        public void visitShapeAnd(ShapeAnd expr, Object... arguments) {
            for (ShapeExpr e : expr.getSubExpressions()) {
                e.accept(this, arguments);
                if (!getResult()) break;
            }
        }

        @Override
        public void visitShapeOr(ShapeOr expr, Object... arguments) {
            for (ShapeExpr e : expr.getSubExpressions()) {
                e.accept(this, arguments);
                if (getResult()) break;
            }
        }

        @Override
        public void visitShapeNot(ShapeNot expr, Object... arguments) {
            expr.getSubExpression().accept(this, arguments);
            setResult(!getResult());
        }
    }
}


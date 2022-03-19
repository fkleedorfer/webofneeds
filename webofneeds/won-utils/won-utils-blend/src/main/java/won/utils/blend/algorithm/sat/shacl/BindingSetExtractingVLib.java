/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.utils.blend.algorithm.sat.shacl;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.other.G;
import org.apache.jena.shacl.engine.ShaclPaths;
import org.apache.jena.shacl.engine.Target;
import org.apache.jena.shacl.parser.NodeShape;
import org.apache.jena.shacl.parser.PropertyShape;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.sparql.path.Path;
import won.utils.blend.algorithm.sat.shacl.constraintvisitor.NodeShapeConstraintVisitor;
import won.utils.blend.algorithm.sat.shacl.constraintvisitor.PropertyShapeConstraintVisitor;
import won.utils.blend.algorithm.sat.shacl.constraintvisitor.ResultProducingConstraintVisitor;
import won.utils.blend.support.bindings.VariableBinding;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static won.utils.blend.algorithm.sat.shacl.BindingUtils.applyToAllBindingOptions;
import static won.utils.blend.algorithm.sat.shacl.ProcessingStepMessages.*;

public class BindingSetExtractingVLib {
    public static BindingExtractionState processNodeShape(BindingExtractionState state, NodeShape shape,
                    ExtractionContext context) {
        if (shape.deactivated()) {
            return state;
        }
        Node focusNode = state.node;
        context.indentedWriter.println(processingNodeShapeMsg(shape, focusNode));
        state = state.setNodeAndResetValueNodes(state.node);
        BindingExtractionState result = applyToAllBindingOptions(state, focusNode,
                        s -> {
                            // if the variable is targeted directly by the shape, no binding matches here!
                            // if (!BindingSetExtractingVLib.isFocusNode(shape, s.node,
                            // s.getBlendedData())){
                            // return s;
                            // }
                            BindingExtractionState ret = processNodeShapeConstraints(s, shape, context);
                            ret = ret.addCheckedBindings(shape.getPropertyShapes()
                                            .stream()
                                            .map(p -> processPropertyShape(s, p, context))
                                            .flatMap(st -> st.checkedBindings.stream())
                                            .collect(Collectors.toSet()));
                            return ret;
                        }, context);
        return result;
    }

    private static BindingExtractionState processNodeShapeConstraints(BindingExtractionState state, NodeShape shape,
                    ExtractionContext context) {
        BindingExtractionState result = state.addCheckedBindings(shape.getConstraints()
                        .stream()
                        .map(c -> {
                            ResultProducingConstraintVisitor<BindingExtractionState> visitor = NodeShapeConstraintVisitor
                                            .indenting(state
                                                            .setNodeAndResetValueNodes(state.node)
                                                            .setValueNodes(state.valueNodes), context);
                            c.visit(visitor);
                            BindingExtractionState ret = visitor.getResult();
                            context.indentedWriter.println(
                                            processNodeShapeConstraintMsg(shape, state.node, c.getClass(), ret));
                            return ret;
                        })
                        .collect(Collectors.toSet()));
        if (shouldCheckNewestBinding(result)) {
            context.indentedWriter.println("no invalid bindings. we should check shapes for the new binding!");
            BindingExtractionState bindingCheck = BindingSetExtractingVLib.processNewestBinding(result, context);
            result.addCheckedBindings(bindingCheck);
            context.indentedWriter.println("Full check for latest binding yields:");
            context.indentedWriter.incIndent();
            result.checkedBindings.stream().forEach(cb -> context.indentedWriter.println(cb));
            context.indentedWriter.decIndent();
        }
        return result;
    }

    public static BindingExtractionState processPropertyShape(
                    BindingExtractionState state,
                    PropertyShape propertyShape,
                    ExtractionContext context) {
        if (propertyShape.deactivated()) {
            return state;
        }
        context.indentedWriter.println(processingPropertyShapeMsg(propertyShape, state.node));
        context.indentedWriter.incIndent();
        state = state.setNodeAndResetValueNodes(state.node);
        Path path = propertyShape.getPath();
        System.err.println("Reminder: we may lose solutions when shacl paths go over a variable");
        Set<Node> vNodes = ShaclPaths.valueNodes(state.getBlendedData(), state.node, path);
        context.indentedWriter.println(String.format("considering the following value nodes of property path %s:",
                        propertyShape.getPath()));
        context.indentedWriter.incIndent();
        vNodes.stream().forEach(n -> context.indentedWriter.println(n));
        context.indentedWriter.decIndent();
        state = state.setValueNodes(vNodes);
        BindingExtractionState result = state.addCheckedBindings(
                        applyToAllBindingOptions(
                                        state,
                                        vNodes,
                                        s -> processPropertyShapeConstraints(s, propertyShape, context), context));
        context.indentedWriter.decIndent();
        return result;
    }

    private static BindingExtractionState processPropertyShapeConstraints(BindingExtractionState state,
                    PropertyShape propertyShape, ExtractionContext context) {
        BindingExtractionState result = state.addCheckedBindings(propertyShape.getConstraints()
                        .stream()
                        .map(c -> {
                            ResultProducingConstraintVisitor<BindingExtractionState> visitor = PropertyShapeConstraintVisitor
                                            .indenting(
                                                            state.setNodeAndResetValueNodes(
                                                                            state.node).setValueNodes(state.valueNodes),
                                                            context);
                            c.visit(visitor);
                            BindingExtractionState ret = visitor.getResult();
                            context.indentedWriter.println(processPropertyShapeConstraintMsg(c.getClass(), ret));
                            return ret;
                        })
                        .collect(Collectors.toSet()));
        if (shouldCheckNewestBinding(result)) {
            context.indentedWriter.println(
                            "Bindings not rejected by property shape constraints, applying shapes to newly bound node.");
            BindingExtractionState bindingCheck = BindingSetExtractingVLib.processNewestBinding(result, context);
            result.addCheckedBindings(bindingCheck);
            context.indentedWriter.println("Full check for latest binding yields:");
            context.indentedWriter.incIndent();
            result.checkedBindings.stream().forEach(cb -> context.indentedWriter.println(cb));
            context.indentedWriter.decIndent();
        }
        return result;
    }

    private static boolean shouldCheckNewestBinding(BindingExtractionState state) {
        if (state.containsInvalidBindings()) {
            return false;
        }
        if (state.inheritedBindings.isEmpty()) {
            return false;
        }
        VariableBinding newestBinding = state.inheritedBindings.get(state.inheritedBindings.size() - 1);
        if (state.isUnboundOrUnsetVariable(newestBinding.getVariable())) {
            return false;
        }
        if (state.isVariable(newestBinding.getBoundNode())) {
            return false;
        }
        if (state.checkedBindings.stream().anyMatch(cb -> cb.bindings.contains(newestBinding))) {
            return false;
        }
        return true;
    }

    private static BindingExtractionState processNewestBinding(BindingExtractionState state,
                    ExtractionContext context) {
        VariableBinding newestBinding = state.inheritedBindings.get(state.inheritedBindings.size() - 1);
        context.indentedWriter.println("checking newest binding: " + newestBinding);
        context.indentedWriter.incIndent();
        Node toCheck = newestBinding.getBoundNode();
        Set<BindingExtractionState> childResults = context.shapes.getTargetShapes().stream().map(shape -> {
            if (isFocusNode(shape, toCheck, state.getBlendedData())) {
                context.indentedWriter.println(String
                                .format("bound node %s of newest binding is focus node of shape %s", toCheck, shape));
                if (shape.isPropertyShape()) {
                    return processPropertyShape(state.setNodeAndResetValueNodes(toCheck), (PropertyShape) shape,
                                    context);
                } else {
                    return processNodeShape(state.setNodeAndResetValueNodes(toCheck), (NodeShape) shape, context);
                }
            }
            return null;
        }).filter(Objects::nonNull)
                        .map(s -> {
                            if (s.checkedBindings.stream().anyMatch(cb -> cb.valid.isFalse())) {
                                context.forbiddenBindings.add(newestBinding);
                            }
                            return s;
                        })
                        .collect(Collectors.toSet());
        context.indentedWriter.decIndent();
        return state.addCheckedBindings(childResults);
    }

    // ValidationProc
    public static Collection<Node> focusNodes(Graph data, Shape shape) {
        Collection<Node> acc = new HashSet<>();
        shape.getTargets().forEach(target -> acc.addAll(focusNodes(data, target)));
        return acc;
    }

    // ValidationProc
    public static Collection<Node> focusNodes(Graph data, Target target) {
        return target.getFocusNodes(data);
    }

    // From ValidationProc.
    public static boolean isFocusNode(Shape shape, Node node, Graph data) {
        return shape.getTargets()
                        .stream()
                        .anyMatch(target -> isFocusNode(target, node, data));
    }

    public static boolean isFocusNode(Target target, Node node, Graph data) {
        Node targetObject = target.getObject();
        switch (target.getTargetType()) {
            case targetClass:
            case implicitClass:
                return G.isOfType(data, node, targetObject);
            case targetNode:
                return targetObject.equals(node);
            case targetObjectsOf:
                return data.contains(null, targetObject, node);
            case targetSubjectsOf:
                return data.contains(node, targetObject, null);
            case targetExtension:
                // Ouch
                focusNodes(data, target).contains(node);
            default:
                return false;
        }
    }
}

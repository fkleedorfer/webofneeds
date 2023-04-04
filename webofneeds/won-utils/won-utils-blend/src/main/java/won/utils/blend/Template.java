package won.utils.blend;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.other.G;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import won.utils.blend.support.bindings.ImmutableVariableBindings;
import won.utils.blend.support.bindings.VariableBinding;
import won.utils.blend.support.bindings.VariableBindings;
import won.utils.blend.support.graph.TemplateGraphs;
import won.utils.blend.support.shacl.ShaclShapeVariableExtractor;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Template {
    private final TemplateGraphs template;
    private final Set<Node> variables;
    private final Set<Node> constants;
    private final Set<Node> blankNodes;
    private final VariableBindings fixedBindings;
    private final Map<Node, Graph> shapeGraphs;
    private final Map<Node, Set<Node>> variablesToShapesMap;
    private final Map<Node, Set<Node>> shapesToVariablesMap;

    public Template(TemplateGraphs templateGraphs) {
        this.template = templateGraphs;
        this.blankNodes = Collections.unmodifiableSet(findBlankNodes(templateGraphs));
        this.variables = Collections.unmodifiableSet(findVariables(templateGraphs, blankNodes));
        this.constants = Collections.unmodifiableSet(findConstants(templateGraphs, this.variables));
        this.fixedBindings = new VariableBindings(this.variables, findFixedBindings(templateGraphs, this.variables));
        ShaclShapeVariableExtractor extractor = new ShaclShapeVariableExtractor(templateGraphs.getShapesGraph(),
                        variables);
        this.shapeGraphs = Collections.unmodifiableMap(extractor.getShapeGraphs());
        this.variablesToShapesMap = extractor.getVariablesToShapesMap();
        this.shapesToVariablesMap = extractor.getShapesToVariablesMap();
    }

    public TemplateGraphs getTemplateGraphs() {
        return template;
    }

    public Set<Node> getConstants() {
        return constants;
    }

    public Set<Node> getVariables() {
        return variables;
    }

    public Set<Node> getBlankNodes() {
        return blankNodes;
    }

    public Map<Node, Graph> getShapeGraphs() {
        return shapeGraphs;
    }

    public boolean isVariable(Node nodeToCheck) {
        return getVariables().contains(nodeToCheck);
    }

    public boolean isBlankNode(Node nodeToCheck) {
        return getBlankNodes().contains(nodeToCheck);
    }

    public boolean isConstant(Node nodeToCheck) {
        return getConstants().contains(nodeToCheck);
    }

    public Set<Node> getVariablesUsedInShape(Node shapeNode) {
        return Collections.unmodifiableSet(shapesToVariablesMap.getOrDefault(shapeNode, Collections.emptySet()));
    }

    public VariableBindings getFixedBindings() {
        return new ImmutableVariableBindings(this.fixedBindings);
    }

    private Set<Node> findBlankNodes(TemplateGraphs templateGraphs) {
        return templateGraphs.getDataGraph().stream()
                        .flatMap(t -> Stream.of(t.getSubject(), t.getObject()))
                        .filter(Node::isBlank)
                        .collect(Collectors.toSet());
    }

    private static Set<Node> findVariables(TemplateGraphs templateGraphs, Set<Node> blankNodes) {
        if (!templateGraphs.hasBlendingConfig()) {
            return Collections.emptySet();
        }
        Set<Node> variables = findByType(templateGraphs.getBlendingConfigGraph(), BLEND.Variable);
        variables.addAll(blankNodes);
        return variables;
    }

    private static Set<VariableBinding> findFixedBindings(TemplateGraphs templateGraphs,
                    Set<Node> variables) {
        if (!templateGraphs.hasBindings()) {
            return Collections.emptySet();
        }
        Iterator<Triple> it = G.find(templateGraphs.getBindingsGraph(), null, BLEND.boundTo, null);
        Set<VariableBinding> result = new HashSet<>();
        while (it.hasNext()) {
            Triple t = it.next();
            result.add(new VariableBinding(t.getSubject(), t.getObject(), variables.contains(t.getObject())));
        }
        return result;
    }

    private static Set<Node> findConstants(TemplateGraphs templateGraphs, Set<Node> variables) {
        Set<Node> result = allNodes(templateGraphs.getDataGraph());
        Set<Node> allVarProperties = new HashSet<>();
        result.removeAll(variables);
        if (templateGraphs.hasBlendingConfig()) {
            result.removeAll(findByType(templateGraphs.getBlendingConfigGraph(), BLEND.Unblendable));
        }
        result = result.stream().filter(n -> !n.isURI() || !hasBlendingPrefix(n)).collect(Collectors.toSet());
        return result;
    }

    private static boolean hasBlendingPrefix(Node n) {
        return n.getURI().startsWith(BLEND.baseUri);
    }

    private static Set<Node> findByType(Graph data, Node type) {
        return G.allPO(data, RDF.type.asNode(), type);
    }

    private static Set<Node> allNodes(Graph data) {
        Set<Node> nodes = new HashSet<>();
        ExtendedIterator<Triple> it = data.find();
        while (it.hasNext()) {
            Triple t = it.next();
            nodes.add(t.getSubject());
            nodes.add(t.getObject());
        }
        return nodes;
    }
}
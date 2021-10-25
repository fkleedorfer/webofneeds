package won.shacl2java.runtime.model;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.util.iterator.ExtendedIterator;

import java.net.URI;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class GraphEntity implements Cloneable {
    private Node node;
    private Graph graph;
    private Set<EntityTriple> entityTriples = new HashSet<>();
    private Set<InverseEntityTriple> inverseEntityTriples = new HashSet<>();

    /**
     * Create entity identified by the specified URI.
     *
     * @param uri
     */
    public GraphEntity(String uri) {
        this(uri, null);
    }

    /**
     * Create entity identified by the specified URI.
     *
     * @param uri
     */
    public GraphEntity(URI uri) {
        this(uri.toString(), null);
    }

    /**
     * Create entity identified by the specified node.
     *
     * @param node
     */
    public GraphEntity(Node node) {
        this(node, null);
    }

    /**
     * Create entity identified by the specified URI attached to the specified
     * graph.
     *
     * @param uri
     */
    public GraphEntity(String uri, Graph graph) {
        this(NodeFactory.createURI(uri), graph);
    }

    /**
     * Create entity identified by the specified URI.
     *
     * @param uri
     */
    public GraphEntity(URI uri, Graph graph) {
        this(uri.toString(), graph);
    }

    /**
     * Create entity identified by the specified node attached to the specified
     * graph.
     * 
     * @param node the node (optional) - must be URI or blank node
     * @param graph the graph (optional)
     */
    public GraphEntity(Node node, Graph graph) {
        this.node = node;
        this.graph = graph;
        requireEntityNodeKind(node);
    }

    public GraphEntity() {
    }

    public Object clone() {
        GraphEntity clone = null;
        try {
            clone = (GraphEntity) super.clone();
        } catch (CloneNotSupportedException e) {
            // swallow
        }
        clone.node = node;
        clone.graph = graph;
        return clone;
    }

    public void requireEntityNodeKind(Node node) {
        if (node != null) {
            if (!(node.isBlank() || node.isURI())) {
                throw new IllegalArgumentException(
                                "A GraphEntity's node must be an URI or a blank node, but received: " + node);
            }
        }
    }

    public void detach() {
        this.graph = null;
    }

    public void accept(GraphEntityVisitor visitor) {
        visitor.visit(this);
    }

    public Node getNode() {
        return this.node;
    }

    public void setNode(Node node) {
        requireEntityNodeKind(node);
        this.node = node;
    }

    public void setNode(String nodeUri) {
        setNode(NodeFactory.createURI(nodeUri));
    }

    public void setNode(URI nodeUri) {
        setNode(NodeFactory.createURI(nodeUri.toString()));
    }

    public Node getNodeCreateIfNecessary() {
        createNodeIfNecessary();
        return this.node;
    }

    public Graph getGraph() {
        return this.graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    /**
     * Passes all triples in the entity's graph to the consumer which have its
     * <code>node</code> as their subject.
     * 
     * @param consumer sink for triples
     */
    public void toRdf(Consumer<Triple> consumer) {
        if (this.graph == null) {
            throw new IllegalStateException("Cannot export RDF from a detached GraphEntity");
        }
        if (this.node == null) {
            throw new IllegalStateException(
                            "Cannot export RDF from GraphEntity without a node. Call createNode() first.");
        }
        graph.find(this.node, null, null).forEach(consumer);
        additionalTriplesToRdf(consumer);
    }

    public abstract void toRdf(Consumer<Triple> consumer, boolean includeIndividuals);

    /**
     * Provides the triples to the consumer that have been added via
     * <code>addEntityRelation</code> or <code>addEntityTriple</code>.
     *
     * @param consumer
     */
    protected void additionalTriplesToRdf(Consumer<Triple> consumer) {
        getEntityTriples().forEach(t -> consumer.accept(t));
        getInverseEntityTriples().forEach(t -> consumer.accept(t));
    }

    public void createNodeIfNecessary() {
        if (this.node == null) {
            createNode();
        }
    }

    public void createNode() {
        if (this.node != null) {
            throw new IllegalStateException("GraphEntity already has a node");
        }
        this.node = NodeFactory.createBlankNode();
    }

    /**
     * If the entity is attached to a graph, this method returns the set of objects
     * of all triples in which this entity's <code>node</code> is the subject and
     * </code><code>property</code> is the predicate. If the entity is detached
     * (i.e., <code>graph == null</code>), the empty set is returned.
     * 
     * @param property
     * @return
     */
    public Set<Node> getPropertiesFromGraph(Node property) {
        if (this.graph == null) {
            return Collections.emptySet();
        }
        ExtendedIterator<Triple> it = this.graph.find(this.node, property, null);
        Set<Node> properties = new HashSet<>();
        while (it.hasNext()) {
            properties.add(it.next().getObject());
        }
        return properties;
    }

    public Set<Node> getPropertiesFromGraph(URI property) {
        return getPropertiesFromGraph(property.toString());
    }

    public Set<Node> getPropertiesFromGraph(String propertyUri) {
        return getPropertiesFromGraph(NodeFactory.createURI(propertyUri));
    }

    /**
     * If the entity is attached to a graph, this method returns the object of the
     * triples in which this entity's <code>node</code> is the subject and
     * </code><code>property</code> is the predicate. If the entity is detached
     * (i.e., <code>graph == null</code>), the empty set is returned. If there is
     * more than one such triple, a {@link NotASingletonPropertyException} is
     * thrown.
     *
     * @param property
     * @throws NotASingletonPropertyException
     * @return
     */
    public Optional<Node> getPropertyFromGraph(Node property) {
        Set<Node> properties = getPropertiesFromGraph(property);
        if (properties.size() > 1) {
            throw new NotASingletonPropertyException("Expected to find at most 1 " + property
                            + " property value on graph entity " + toString() + ", but found " + properties.size());
        }
        return properties.stream().findFirst();
    }

    public Optional<Node> getPropertyFromGraph(URI property) {
        return getPropertyFromGraph(property.toString());
    }

    public Optional<Node> getPropertyFromGraph(String propertyUri) {
        return getPropertyFromGraph(NodeFactory.createURI(propertyUri));
    }

    public void addEntityTriple(Node predicate, Node object) {
        this.entityTriples.add(new EntityTriple(predicate, object));
    }

    public void addEntityTriple(String predicate, Node object) {
        this.entityTriples.add(new EntityTriple(predicate, object));
    }

    public void addEntityTriple(URI predicate, Node object) {
        this.entityTriples.add(new EntityTriple(predicate, object));
    }

    public Set<Triple> getEntityTriples() {
        createNodeIfNecessary();
        return entityTriples.stream().map(e -> new Triple(this.node, e.getPredicate(), e.getObject()))
                        .collect(Collectors.toSet());
    }

    public void addInverseEntityTriple(Node subject, Node predicate) {
        this.inverseEntityTriples.add(new InverseEntityTriple(subject, predicate));
    }

    public void addInverseEntityTriple(String subject, String predicate) {
        this.inverseEntityTriples.add(new InverseEntityTriple(subject, predicate));
    }

    public void addInverseEntityTriple(URI subject, URI predicate) {
        this.inverseEntityTriples.add(new InverseEntityTriple(subject, predicate));
    }

    public Set<Triple> getInverseEntityTriples() {
        createNodeIfNecessary();
        return inverseEntityTriples.stream().map(e -> new Triple(e.getSubject(), e.getPredicate(), this.node))
                        .collect(Collectors.toSet());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GraphEntity that = (GraphEntity) o;
        if (this.node == null && that.node == null) {
            return false;
        }
        return Objects.equals(node, that.node);
    }

    public abstract boolean deepEquals(Object other);

    public abstract boolean deepEquals(Object other, ArrayDeque visited);

    @Override
    public int hashCode() {
        return Objects.hash(node);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                        "attached=" + (this.graph != null) +
                        ", node=" + node +
                        '}';
    }
}

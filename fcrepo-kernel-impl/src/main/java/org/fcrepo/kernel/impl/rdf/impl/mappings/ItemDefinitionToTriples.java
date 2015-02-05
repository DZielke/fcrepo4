/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.impl.rdf.impl.mappings;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.utils.iterators.RdfStream;

import org.modeshape.jcr.api.Namespaced;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeType;

import java.util.function.Function;

import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.vocabulary.RDF.Property;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static com.hp.hpl.jena.vocabulary.RDFS.domain;
import static com.hp.hpl.jena.vocabulary.RDFS.label;
import static org.fcrepo.kernel.impl.rdf.JcrRdfTools.getRDFNamespaceForJcrNamespace;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Utility for moving generic Item Definitions into RDFS triples
 * @author cbeer
 * @author ajs6f
 *
 * @since Oct 2013
 *
 * @param <T> the property of T
 */
public class ItemDefinitionToTriples<T extends ItemDefinition> implements Function<T, RdfStream> {

    private static final Logger LOGGER = getLogger(ItemDefinitionToTriples.class);

    private final Node context;

    /**
     * Translate ItemDefinitions into triples. The definitions will hang off
     * the provided RDF Node
     * @param context the context
     */
    public ItemDefinitionToTriples(final Node context) {
        this.context = context;
    }

    @Override
    public RdfStream apply(final T input) {

        try {
            final Node propertyDefinitionNode = getResource(input).asNode();

            LOGGER.trace("Adding triples for nodeType: {} with child nodes: {}", context.getURI(),
                    propertyDefinitionNode.getURI());

            return new RdfStream(
                    create(propertyDefinitionNode, type.asNode(), Property.asNode()),
                    create(propertyDefinitionNode, domain.asNode(), context),
                    create(propertyDefinitionNode, label.asNode(), createLiteral(input.getName())));
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Get a RDF {@link Resource} for a {@link Namespaced} JCR object.
     * {@link Namespaced} is a Modeshape API type which is implemented by types
     * that fulfill the JCR interfaces that represent definitions.
     *
     * @param namespacedObject the namespace object
     * @return a resource for the given Namespaced JCR object
     * @throws javax.jcr.RepositoryException if repository exception occurred
     */
    public static Resource getResource(final Namespaced namespacedObject) {
        // TODO find a better way to create an explicitly-namespaced resource
        // if Jena offers one, since this isn't actually a Property
        try {
            final String namespaceURI = namespacedObject.getNamespaceURI();
            final String localName = namespacedObject.getLocalName();
            LOGGER.trace("Creating RDF resource for {}:{}", namespaceURI, localName);
            return createProperty(getRDFNamespaceForJcrNamespace(namespaceURI), localName).asResource();
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Get a RDF {@link Resource} for a {@link NodeType} JCR object.
     * {@link Namespaced} is a Modeshape API type which is implemented by types
     * that fulfill the JCR interfaces that represent definitions.
     *
     * @param nodeType the node type
     * @return a Resource for the given NodeType
     */
    public static Resource getResource(final NodeType nodeType) {
        return getResource((Namespaced) nodeType);
    }

    /**
     * Get a RDF {@link Resource} for a {@link ItemDefinition} JCR object.
     * {@link Namespaced} is a Modeshape API type which is implemented by types
     * that fulfill the JCR interfaces that represent definitions.
     *
     * @param itemDefinition the given item definition
     * @return a resource for the given ItemDefinition
     * @throws javax.jcr.RepositoryException if repository exception occurred
     */
    public static Resource getResource(final ItemDefinition itemDefinition) throws RepositoryException {
        return getResource((Namespaced) itemDefinition);
    }
}

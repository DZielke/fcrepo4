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
package org.fcrepo.kernel.impl.rdf.impl;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.identifiers.IdentifierConverter;

import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static java.util.function.Function.identity;
import static org.fcrepo.kernel.impl.rdf.JcrRdfTools.getRDFNamespaceForJcrNamespace;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Construct rdf:type triples for primary type, mixins, and their supertypes.
 *
 * @author cabeer
 * @author ajs6f
 * @since 10/1/14
 */
public class TypeRdfContext extends NodeRdfContext {
    private static final Logger LOGGER = getLogger(TypeRdfContext.class);

    /**
     * Default constructor.
     *
     * @param resource the resource
     * @param idTranslator the id translator
     * @throws RepositoryException if repository exception occurred
     */
    public TypeRdfContext(final FedoraResource resource,
                          final IdentifierConverter<Resource, FedoraResource> idTranslator)
            throws RepositoryException {
        super(resource, idTranslator);

        final Node node = resource().getNode();
        final NodeType primaryNodeType = node.getPrimaryNodeType();
        final NodeType[] mixins = node.getMixinNodeTypes();
        final Stream<NodeType> allTypes =
                Stream.of(Stream.of(primaryNodeType), Stream.of(primaryNodeType.getSupertypes()), Stream.of(mixins),
                        Stream.of(mixins).flatMap(t -> Arrays.stream(t.getSupertypes()))).flatMap(identity());
        concat(allTypes.map(nodetype2triple));
    }

    private final Function<NodeType, Triple> nodetype2triple =
            nodeType -> {
                final String fullTypeName = nodeType.getName();
                LOGGER.trace("Translating JCR mixin name: {}", fullTypeName);
                final String prefix = fullTypeName.split(":")[0];
                final String typeName = fullTypeName.split(":")[1];
                final String namespace = getJcrUri(prefix);
                LOGGER.trace("with JCR namespace: {}", namespace);
                final com.hp.hpl.jena.graph.Node rdfType =
                        createURI(getRDFNamespaceForJcrNamespace(namespace) + typeName);
                LOGGER.trace("into RDF resource: {}", rdfType);
                return create(subject(), type.asNode(), rdfType);
            };

    private String getJcrUri(final String prefix) {
        try {
            return resource().getNode().getSession().getWorkspace().getNamespaceRegistry().getURI(prefix);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }
}

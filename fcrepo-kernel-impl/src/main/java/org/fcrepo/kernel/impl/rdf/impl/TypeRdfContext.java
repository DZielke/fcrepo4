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
import java.util.stream.Stream;

import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.impl.rdf.JcrRdfTools.getRDFNamespaceForJcrNamespace;
import static org.fcrepo.kernel.utils.Streams.flatten;
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
     * @param resource
     * @param idTranslator
     */
    public TypeRdfContext(final FedoraResource resource,
                          final IdentifierConverter<Resource, FedoraResource> idTranslator) {
        super(resource, idTranslator);
    }

    @Override
    public Stream<Triple> applyThrows(final Node node) throws RepositoryException {
        final NodeType primaryNodeType = node.getPrimaryNodeType();
        final NodeType[] mixins = node.getMixinNodeTypes();
        final Stream<NodeType> mixinSupertypes = Stream.of(mixins).map(NodeType::getSupertypes).flatMap(Arrays::stream);
        final Stream<NodeType> primarySupertypes = Stream.of(primaryNodeType.getSupertypes());
        final Stream<NodeType> allTypes =
                flatten(Stream.of(primaryNodeType), primarySupertypes, Stream.of(mixins), mixinSupertypes);
        return allTypes.map(nodeType -> {
            final String fullTypeName = nodeType.getName();
            LOGGER.trace("Translating mixin name: {}", fullTypeName);
            final String prefix = fullTypeName.split(":")[0];
            final String typeName = fullTypeName.split(":")[1];
            final String namespace = getJcrUri(prefix);
            LOGGER.trace("with namespace: {}", namespace);
            final com.hp.hpl.jena.graph.Node rdfType =
                    createURI(getRDFNamespaceForJcrNamespace(namespace) + typeName);
            LOGGER.trace("into RDF resource: {}", rdfType);
            return create(topic(), type.asNode(), rdfType);
        });
    }

    private String getJcrUri(final String prefix) {
        try {
            return session().getWorkspace().getNamespaceRegistry().getURI(prefix);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }
}

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

import org.fcrepo.kernel.models.NonRdfSourceDescription;
import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.utils.UncheckedPredicate;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.rdf.converters.ValueConverter;
import org.fcrepo.kernel.impl.rdf.impl.mappings.PropertyValueStream;

import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static java.util.stream.Stream.empty;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_BASIC_CONTAINER;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_DIRECT_CONTAINER;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_HAS_MEMBER_RELATION;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_INDIRECT_CONTAINER;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_INSERTED_CONTENT_RELATION;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_MEMBER_RESOURCE;
import static org.fcrepo.kernel.RdfLexicon.LDP_MEMBER;
import static org.fcrepo.kernel.RdfLexicon.MEMBER_SUBJECT;
import static org.fcrepo.kernel.impl.identifiers.NodeResourceConverter.nodeConverter;
import static org.fcrepo.kernel.impl.rdf.converters.PropertyConverter.getPropertyNameFromPredicate;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.getReferencePropertyName;
import static org.fcrepo.kernel.utils.Streams.fromIterator;
import static org.fcrepo.kernel.utils.UncheckedFunction.uncheck;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @author ajs6f
 * @since 9/25/14
 */
public class LdpContainerRdfContext extends NodeRdfContext {

    private static final Logger log = getLogger(LdpContainerRdfContext.class);

    /**
     * Default constructor.
     *
     * @param resource
     * @param idTranslator
     */
    public LdpContainerRdfContext(final FedoraResource resource,
                                  final IdentifierConverter<Resource, FedoraResource> idTranslator) {
        super(resource, idTranslator);

    }

    @Override
    public Stream<Triple> applyThrows(final Node node) throws RepositoryException {
        final Iterator<Property> memberReferences = node.getReferences(LDP_MEMBER_RESOURCE);
        final Stream<Property> properties = fromIterator(memberReferences).filter(isContainer);
        return properties.flatMap(uncheck(p -> memberRelations(nodeConverter.convert(p.getParent()))));
    }

    private static final Predicate<Property> isContainer = UncheckedPredicate.uncheck(property -> {
            final Node container = property.getParent();
            return container.isNodeType(LDP_DIRECT_CONTAINER) || container.isNodeType(LDP_INDIRECT_CONTAINER);
    });

    /**
     * Get the member relations assert on the subject by the given node
     * @param container
     * @return
     * @throws RepositoryException
     */
    private Stream<Triple> memberRelations(final FedoraResource container) throws RepositoryException {
        final com.hp.hpl.jena.graph.Node memberRelation;

        if (container.hasProperty(LDP_HAS_MEMBER_RELATION)) {
            final Property property = container.getProperty(LDP_HAS_MEMBER_RELATION);
            memberRelation = createURI(property.getString());
        } else if (container.hasType(LDP_BASIC_CONTAINER)) {
            memberRelation = LDP_MEMBER.asNode();
        } else {
            return empty();
        }

        final String insertedContainerProperty;

        if (container.hasType(LDP_INDIRECT_CONTAINER)) {
            if (container.hasProperty(LDP_INSERTED_CONTENT_RELATION)) {
                insertedContainerProperty = container.getProperty(LDP_INSERTED_CONTENT_RELATION).getString();
            } else {
                return empty();
            }
        } else {
            insertedContainerProperty = MEMBER_SUBJECT.getURI();
        }
        log.debug("Using inserted container property: {}", insertedContainerProperty);
        return container.getChildren().flatMap(uncheck(child -> {

            final FedoraResource childResource =
                    child instanceof NonRdfSourceDescription ? ((NonRdfSourceDescription) child)
                            .getDescribedResource() : child;
            final com.hp.hpl.jena.graph.Node childSubject = translator().reverse().convert(childResource).asNode();
            log.debug("Operating over child resource: {}", childSubject);
            if (insertedContainerProperty.equals(MEMBER_SUBJECT.getURI())) {
                log.debug("Inserted container property equals {}", MEMBER_SUBJECT);
                return Stream.of(create(topic(), memberRelation, childSubject));
            }
            String insertedContentProperty = getPropertyNameFromPredicate(resource().getNode(),
                    createResource(insertedContainerProperty), namespaces());
            log.debug("Looking for inserted content property: {}", insertedContentProperty);
            if (child.hasProperty(insertedContentProperty)) {
                log.debug("Using property {} directly.", insertedContentProperty);
            } else if (child.hasProperty(getReferencePropertyName(insertedContentProperty))) {
                // The insertedContentProperty is a pseudo reference property
                insertedContentProperty = getReferencePropertyName(insertedContentProperty);
                log.debug("Using property {} via reference.", insertedContentProperty);
            } else {
                log.debug("No property {} found directly or via reference.", insertedContentProperty);
                return empty();
            }
            log.debug("Using values from inserted content property: {}", insertedContentProperty);
            final Stream<Value> values =
                    child.hasProperty(insertedContentProperty) ? new PropertyValueStream(child
                            .getProperty(insertedContentProperty)) : empty();
            return values.map(v -> new ValueConverter(session(), translator()).convert(v).asNode()).map(
                    o -> create(topic(), memberRelation, o));
        }));
    }
}

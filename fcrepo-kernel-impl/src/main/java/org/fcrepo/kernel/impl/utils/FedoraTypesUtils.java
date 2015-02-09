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
package org.fcrepo.kernel.impl.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;



import java.util.stream.Stream;

import org.fcrepo.kernel.FedoraJcrTypes;
import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.services.functions.AnyTypesPredicate;
import org.fcrepo.kernel.services.functions.JcrPropertyFunctions;
import org.fcrepo.kernel.utils.UncheckedPredicate;




import org.slf4j.Logger;




import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;




import static java.util.Arrays.asList;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.UNDEFINED;
import static javax.jcr.PropertyType.WEAKREFERENCE;
import static org.fcrepo.kernel.services.functions.JcrPropertyFunctions.isBinaryContentProperty;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Convenience class with static methods for manipulating Fedora types in the
 * JCR.
 *
 * @author ajs6f
 * @since Feb 14, 2013
 */
@SuppressWarnings("unused")
public abstract class FedoraTypesUtils implements FedoraJcrTypes {

    public static final String REFERENCE_PROPERTY_SUFFIX = "_ref";

    private static final Logger LOGGER = getLogger(FedoraTypesUtils.class);

    private static List<Integer> REFERENCE_TYPES = asList(REFERENCE, WEAKREFERENCE);

    /**
     * Predicate for determining whether this {@link Node} is a {@link org.fcrepo.kernel.models.Container}.
     */
    public static Predicate<Node> isContainer =
            new AnyTypesPredicate(FEDORA_CONTAINER);

    /**
     * Predicate for determining whether this {@link Node} is a {@link org.fcrepo.kernel.models.NonRdfSourceDescription}
     */
    public static Predicate<Node> isNonRdfSourceDescription =
            new AnyTypesPredicate(FEDORA_NON_RDF_SOURCE_DESCRIPTION);


    /**
     * Predicate for determining whether this {@link Node} is a Fedora
     * binary.
     */
    public static Predicate<Node> isFedoraBinary =
            new AnyTypesPredicate(FEDORA_BINARY);

    /**
     * Predicate for determining whether this {@link FedoraResource} has a frozen node
     */
    public static Predicate<FedoraResource> isFrozenNode = f -> f.hasType(FROZEN_NODE) ||
            f.getPath().contains(JCR_FROZEN_NODE);

    /**
     * Predicate for determining whether this {@link Node} is a Fedora
     * binary.
     */
    public static java.util.function.Predicate<? super Node> isBlankNode =
            new AnyTypesPredicate(FEDORA_BLANKNODE);

    /**
     * Check if a property is a reference property.
     */
    public static Predicate<Property> isInternalReferenceProperty = UncheckedPredicate
            .uncheck(p -> (p.getType() == REFERENCE || p.getType() == WEAKREFERENCE)
                    && p.getName().endsWith(REFERENCE_PROPERTY_SUFFIX));



    /**
     * Check whether a property is protected (ie, cannot be modified directly) but is not one we've explicitly chosen
     * to include.
     */

    public static Predicate<Property> isProtectedAndShouldBeHidden = UncheckedPredicate.uncheck(p -> {
        if (!p.getDefinition().isProtected()) {
            return false;
        }
        if (p.getParent().isNodeType(FROZEN_NODE)) {
            // everything on a frozen node is protected
            // but we wish to display it anyway and there's
            // another mechanism in place to make clear that
            // things cannot be edited.
            return false;
        }
        final String name = p.getName();
        return !EXPOSED_PROTECTED_JCR_TYPES.contains(name);
    });

    /**
     * Check whether a property is an internal property that should be suppressed from external output.
     */
    public static Predicate<Property> isInternalProperty = p -> isBinaryContentProperty.test(p) ||
            isProtectedAndShouldBeHidden.test(p);

    /**
     * Check if a node is "internal" and should not be exposed e.g. via the REST API
     */
    public static Predicate<Node> isInternalNode = UncheckedPredicate.uncheck(n -> n.isNodeType("mode:system"));

    /**
     * Get the JCR property type ID for a given property name. If unsure, mark
     * it as UNDEFINED.
     *
     * @param node the JCR node to add the property on
     * @param propertyName the property name
     * @return a PropertyType value
     */
    public static int getPropertyType(final Node node, final String propertyName) {
        LOGGER.debug("Getting type of property: {} from node: {}",
                propertyName, node);
        final Optional<PropertyDefinition> def = getDefinitionForPropertyName(node, propertyName);
        return def.map(PropertyDefinition::getRequiredType).orElse(UNDEFINED);
    }

    /**
     * Determine if a given JCR property name is single- or multi- valued.
     * If unsure, choose the least restrictive
     * option (multivalued)
     *
     * @param node the JCR node to check
     * @param propertyName the property name
     *   (which may or may not already exist)
     * @return true if the property is (or could be) multivalued
     * @throws RepositoryException if repository exception occurred
     */
    public static boolean isMultivaluedProperty(final Node node,
                                                final String propertyName)
            throws RepositoryException {
        final Optional<PropertyDefinition> def = getDefinitionForPropertyName(node, propertyName);
        return def.map(PropertyDefinition::isMultiple).orElse(true);
    }

    /**
     * Get the property definition information (containing type and multi-value
     * information)
     *
     * @param node the node to use for inferring the property definition
     * @param propertyName the property name to retrieve a definition for
     * @return a JCR PropertyDefinition, if available, or null
     */
    public static Optional<PropertyDefinition> getDefinitionForPropertyName(final Node node,
            final String propertyName) {
        try {
            final PropertyDefinition[] propertyDefinitions = node.getPrimaryNodeType().getPropertyDefinitions();
            final Stream<NodeType> types =
                    Stream.concat(Stream.of(node.getPrimaryNodeType()), Stream.of(node.getMixinNodeTypes()));
            LOGGER.debug("Looking for property name: {}", propertyName);
            return types.map(NodeType::getPropertyDefinitions).flatMap(Arrays::stream).filter(
                    p -> propertyName.equals(p.getName())).findAny();
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * When we add certain URI properties, we also want to leave a reference node
     * @param propertyName the property name
     * @return property name as a reference
     */
    public static String getReferencePropertyName(final String propertyName) {
        return propertyName + REFERENCE_PROPERTY_SUFFIX;
    }

    /**
     * Given an internal reference node property, get the original name
     * @param refPropertyName the reference node property name
     * @return original property name of the reference property
     */
    public static String getReferencePropertyOriginalName(final String refPropertyName) {
        return refPropertyName.endsWith(REFERENCE_PROPERTY_SUFFIX) ? refPropertyName.substring(0, refPropertyName
                .lastIndexOf(REFERENCE_PROPERTY_SUFFIX)) : refPropertyName;
    }

    /**
     * Check if a property definition is a reference property
     * @param node the given node
     * @param propertyName the property name
     * @return whether a property definition is a reference property
     * @throws RepositoryException if repository exception occurred
     */
    public static boolean isReferenceProperty(final Node node, final String propertyName) throws RepositoryException {
        final Optional<PropertyDefinition> propertyDefinition = getDefinitionForPropertyName(node, propertyName);
        return propertyDefinition.map(p -> REFERENCE_TYPES.contains(p.getRequiredType())).orElse(false);
    }


    /**
     * Get the closest ancestor that current exists
     *
     * @param session the given session
     * @param path the given path
     * @return the closest ancestor that current exists
     * @throws RepositoryException if repository exception occurred
     */
    public static Node getClosestExistingAncestor(final Session session,
                                                  final String path) throws RepositoryException {
        final String[] pathSegments = path.replaceAll("^/+", "").replaceAll("/+$", "").split("/");

        final StringBuilder existingAncestorPath = new StringBuilder(path.length());
        existingAncestorPath.append("/");

        final int len = pathSegments.length;
        for (int i = 0; i != len; ++i) {
            final String pathSegment = pathSegments[i];

            if (session.nodeExists(existingAncestorPath.toString() + pathSegment)) {
                // Add to existingAncestorPath  ...
                existingAncestorPath.append(pathSegment);
                if (i != (len - 1)) {
                existingAncestorPath.append("/");
                }
            } else {
                if (i != 0) {
                    existingAncestorPath.deleteCharAt(existingAncestorPath.length() - 1);
                }
                break;
            }

        }

        return session.getNode(existingAncestorPath.toString());
    }

}

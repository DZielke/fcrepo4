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

package org.fcrepo.kernel.utils;

import java.util.function.Predicate;

import javax.jcr.RepositoryException;

import org.fcrepo.kernel.exception.RepositoryRuntimeException;

public interface UncheckedPredicate<T> extends Predicate<T> {

    @Override
    default boolean test(final T elem) {
        try {
            return testThrows(elem);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    boolean testThrows(T elem) throws RepositoryException;

    static <T> UncheckedPredicate<T> uncheck(final ThrowingPredicate<T> p) {
        return new UncheckedPredicate<T>() {

            @Override
            public boolean testThrows(final T elem) throws RepositoryException {
                return p.test(elem);
            }
        };

    }

    @FunctionalInterface
    public static interface ThrowingPredicate<T> {

        boolean test(T element) throws RepositoryException;
    }
}

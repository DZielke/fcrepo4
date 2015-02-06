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

import java.util.function.Function;
import javax.jcr.RepositoryException;

import org.fcrepo.kernel.exception.RepositoryRuntimeException;

/**
 * We often need to use {@link Function}s that wrap methods that throw {@link RepositoryException}. This does that.
 *
 * @author ajs6f
 */
@FunctionalInterface
public interface UncheckedFunction<T, R> extends Function<T, R> {

    @Override
    default R apply(final T elem) {
        try {
            return applyThrows(elem);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    R applyThrows(T elem) throws RepositoryException;

    static <T, R> UncheckedFunction<T, R> uncheck(final ThrowingLambda<T, R> f) {
        return new UncheckedFunction<T, R>() {

            @Override
            public R applyThrows(final T elem) throws RepositoryException {
                return f.apply(elem);
            }
        };

    }

    @FunctionalInterface
    public static interface ThrowingLambda<T, R> {

        R apply(T element) throws RepositoryException;
    }
}

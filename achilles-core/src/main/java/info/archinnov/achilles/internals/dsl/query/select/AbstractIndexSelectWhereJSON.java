/*
 * Copyright (C) 2012-2016 DuyHai DOAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.archinnov.achilles.internals.dsl.query.select;

import static java.lang.String.format;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ExecutionInfo;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.querybuilder.Select;

import info.archinnov.achilles.internals.dsl.StatementProvider;
import info.archinnov.achilles.internals.dsl.action.SelectJSONAction;
import info.archinnov.achilles.internals.dsl.options.AbstractOptionsForSelect;
import info.archinnov.achilles.internals.metamodel.AbstractEntityProperty;
import info.archinnov.achilles.internals.options.Options;
import info.archinnov.achilles.internals.runtime.RuntimeEngine;
import info.archinnov.achilles.internals.statements.BoundStatementWrapper;
import info.archinnov.achilles.internals.statements.OperationType;
import info.archinnov.achilles.internals.statements.StatementWrapper;
import info.archinnov.achilles.internals.types.JSONIteratorWrapper;
import info.archinnov.achilles.type.tuples.Tuple2;

public abstract class AbstractIndexSelectWhereJSON<T extends AbstractIndexSelectWhereJSON<T, ENTITY>, ENTITY>
        extends AbstractSelectWhereJSON<T, ENTITY> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractIndexSelectWhereJSON.class);

    protected AbstractIndexSelectWhereJSON(Select.Where where, Options cassandraOptions) {
        super(where, cassandraOptions);
    }

    @Override
    protected StatementWrapper getInternalBoundStatementWrapper() {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(format("Get bound statement wrapper"));
        }

        final RuntimeEngine rte = getRte();
        final AbstractEntityProperty<ENTITY> meta = getMetaInternal();
        final Options options = getOptions();

        final String queryString;
        if (options.hasRawSolrQuery()) {
            getBoundValuesInternal().add(0, options.generateRawSolrQuery());
            getEncodedValuesInternal().add(0, options.generateRawSolrQuery());
            queryString = where.getQueryString();
        } else if (options.hasSolrQuery()) {
            getBoundValuesInternal().add(0, options.generateSolrQuery());
            getEncodedValuesInternal().add(0, options.generateSolrQuery());
            queryString = where.getQueryString();
        } else {
            queryString = where.getQueryString().trim().replaceFirst(";$", " ALLOW FILTERING;");
        }

        final PreparedStatement ps = rte.prepareDynamicQuery(queryString);

        final StatementWrapper statementWrapper = new BoundStatementWrapper(OperationType.SELECT,
                meta, ps,
                getBoundValuesInternal().toArray(),
                getEncodedValuesInternal().toArray());

        statementWrapper.applyOptions(options);
        return statementWrapper;
    }
}

/**
 * Copyright 2014 Flipkart Internet Pvt. Ltd.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.flipkart.foxtrot.core.querystore.impl;

import com.flipkart.foxtrot.common.ActionRequest;
import com.flipkart.foxtrot.common.Table;
import com.flipkart.foxtrot.core.common.PeriodSelector;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * User: Santanu Sinha (santanu.sinha@flipkart.com)
 * Date: 24/03/14
 * Time: 3:46 PM
 */
public class ElasticsearchUtils {
    public static final String DOCUMENT_TYPE_NAME = "document";
    public static final String DOCUMENT_META_TYPE_NAME = "metadata";
    public static final String DOCUMENT_META_FIELD_NAME = "__FOXTROT_METADATA__";
    public static final String DOCUMENT_TIME_FIELD_NAME = "date";
    public static final String DOCUMENT_META_ID_FIELD_NAME = String.format("%s.id", DOCUMENT_META_FIELD_NAME);
    public static final String DOCUMENT_META_TIMESTAMP_FIELD_NAME = String.format("%s.time", DOCUMENT_META_FIELD_NAME);
    public static final String TABLENAME_POSTFIX = "table";
    public static final String TIME_FIELD = "time";
    public static final int DEFAULT_SUB_LIST_SIZE = 50;
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("dd-M-yyyy");
    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchUtils.class.getSimpleName());
    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("dd-M-yyyy");
    public static String TABLENAME_PREFIX = "foxtrot";

    public static void setTableNamePrefix(ElasticsearchConfig config) {
        ElasticsearchUtils.TABLENAME_PREFIX = config.getTableNamePrefix();
    }

    public static String getIndexPrefix(final String table) {
        return String.format("%s-%s-%s-", ElasticsearchUtils.TABLENAME_PREFIX, table, ElasticsearchUtils.TABLENAME_POSTFIX);
    }

    public static String getIndices(final String table) {
        /*long currentTime = new Date().getTime();
        String names[] = new String[30]; //TODO::USE TABLE METADATA
        for(int i = 0 ; i < 30; i++) {
            String postfix = new SimpleDateFormat("dd-M-yyyy").format(new Date(currentTime));
            names[i] = String.format("%s-%s-%s", TABLENAME_PREFIX, table, postfix);
        }*/
        return String.format("%s-%s-%s-*", ElasticsearchUtils.TABLENAME_PREFIX, table, ElasticsearchUtils.TABLENAME_POSTFIX);
    }

    public static String[] getIndices(final String table, final ActionRequest request) throws Exception {
        return getIndices(table, request, new PeriodSelector(request.getFilters()).analyze());
    }

    @VisibleForTesting
    public static String[] getIndices(final String table, final ActionRequest request, final Interval interval) {
        DateTime start = interval.getStart()
                .toLocalDate()
                .toDateTimeAtStartOfDay();
        if(start.getYear() <= 1970) {
            logger.warn("Request of type {} running on all indices", request.getClass()
                    .getSimpleName());
            return new String[]{getIndices(table)};
        }
        List<String> indices = Lists.newArrayList();
        final DateTime end = interval.getEnd()
                .plusDays(1)
                .toLocalDate()
                .toDateTimeAtStartOfDay();
        while (start.getMillis() < end.getMillis()) {
            final String index = getCurrentIndex(table, start.getMillis());
            indices.add(index);
            start = start.plusDays(1);
        }
        logger.info("Request of type {} on indices: {}", request.getClass()
                .getSimpleName(), indices);
        return indices.toArray(new String[indices.size()]);
    }

    public static String getCurrentIndex(final String table, long timestamp) {
        //TODO::THROW IF TIMESTAMP IS BEYOND TABLE META.TTL
        String datePostfix = FORMATTER.print(timestamp);
        return String.format("%s-%s-%s-%s", ElasticsearchUtils.TABLENAME_PREFIX, table, ElasticsearchUtils.TABLENAME_POSTFIX, datePostfix);
    }

    public static PutIndexTemplateRequest getClusterTemplateMapping() {
        try {
            return new PutIndexTemplateRequest().name("template_foxtrot_mappings")
                    .patterns(Lists.newArrayList(String.format("%s-*", ElasticsearchUtils.TABLENAME_PREFIX)))
                    .mapping(DOCUMENT_TYPE_NAME, getDocumentMapping());
        } catch (IOException ex) {
            logger.error("TEMPLATE_CREATION_FAILED", ex);
            return null;
        }
    }

    public static XContentBuilder getDocumentMapping() throws IOException {
        return XContentFactory.jsonBuilder()
                .startObject()
                .field(DOCUMENT_TYPE_NAME)
                .startObject()
                .field("_source")
                .startObject()
                .field("enabled", false)
                .endObject()
                .field("_all")
                .startObject()
                .field("enabled", false)
                .endObject()
                .field("dynamic_templates")
                .startArray()

                .startObject()
                .field("template_metadata_timestamp")
                .startObject()
                .field("path_match", ElasticsearchUtils.DOCUMENT_META_TIMESTAMP_FIELD_NAME)
                .field("mapping")
                .startObject()
                .field("store", true)
                .field("index", true)
                .field("type", "date")
                .endObject()
                .endObject()
                .endObject()

                .startObject()
                .field("template_metadata_string")
                .startObject()
                .field("path_match", ElasticsearchUtils.DOCUMENT_META_FIELD_NAME + ".*")
                .field("match_mapping_type", "string")
                .field("mapping")
                .startObject()
                .field("store", true)
                .field("index", true)
                .field("type", "keyword")
                .endObject()
                .endObject()
                .endObject()

                .startObject()
                .field("template_metadata_others")
                .startObject()
                .field("path_match", ElasticsearchUtils.DOCUMENT_META_FIELD_NAME + ".*")
                .field("mapping")
                .startObject()
                .field("store", true)
                .field("index", true)
                .endObject()
                .endObject()
                .endObject()

                .startObject()
                .field("template_no_store_analyzed")
                .startObject()
                .field("match", "*")
                .field("match_mapping_type", "string")
                .field("mapping")
                .startObject()
                .field("store", true)
                .field("index", true)
                .field("type", "keyword")
                .field("fields")
                .startObject()
                .field("analyzed")
                .startObject()
                .field("store", false)
                .field("index", true)
                .field("type", "text")
                .endObject()
                .endObject()
                .endObject()
                .endObject()
                .endObject()

                .startObject()
                .field("template_no_store")
                .startObject()
                .field("match_mapping_type", "*")
                .field("match_pattern", "regex")
                .field("path_match", ".*")
                .field("mapping")
                .startObject()
                .field("store", false)
                .field("index", true)
                .endObject()
                .endObject()
                .endObject()

                .endArray()
                .field("properties")
                .startObject()
                .field("time")
                .startObject()
                .field("type", "long")
                .field("fields")
                .startObject()
                .field("date")
                .startObject()
                .field("index", "true")
                .field("store", true)
                .field("type", "date")
                .field("format", "epoch_millis")
                .endObject()
                .endObject()
                .endObject()
                .endObject()
                .endObject()
                .endObject();
    }

    public static void initializeMappings(Client client) {
        PutIndexTemplateRequest templateRequest = getClusterTemplateMapping();
        client.admin()
                .indices()
                .putTemplate(templateRequest)
                .actionGet();
    }

    public static String getValidTableName(String table) {
        if(table == null)
            return null;
        return table.trim()
                .toLowerCase();
    }

    public static boolean isIndexValidForTable(String index, String table) {
        String indexPrefix = getIndexPrefix(table);
        return index.startsWith(indexPrefix);
    }

    public static boolean isIndexEligibleForDeletion(String index, Table table) {
        if(index == null || table == null || !isIndexValidForTable(index, table.getName())) {
            return false;
        }

        DateTime creationDate = parseIndexDate(index, table.getName());
        DateTime startTime = new DateTime(0L);
        DateTime endTime = new DateTime().minusDays(table.getTtl())
                .toDateMidnight()
                .toDateTime();
        return creationDate.isAfter(startTime) && creationDate.isBefore(endTime);
    }

    public static DateTime parseIndexDate(String index, String table) {
        String indexPrefix = getIndexPrefix(table);
        String creationDateString = index.substring(index.indexOf(indexPrefix) + indexPrefix.length());
        return DATE_TIME_FORMATTER.parseDateTime(creationDateString);
    }

    public static String getTableNameFromIndex(String currentIndex) {
        if(currentIndex.contains(TABLENAME_PREFIX) && currentIndex.contains(TABLENAME_POSTFIX)) {
            String tempIndex = currentIndex.substring(currentIndex.indexOf(TABLENAME_PREFIX) + TABLENAME_PREFIX.length() + 1);
            int position = tempIndex.lastIndexOf(String.format("-%s", TABLENAME_POSTFIX));
            return tempIndex.substring(0, position);
        } else {
            return null;
        }
    }

    public static String getAllIndicesPattern() {
        return String.format("%s-*-%s-*", ElasticsearchUtils.TABLENAME_PREFIX, ElasticsearchUtils.TABLENAME_POSTFIX);
    }
}

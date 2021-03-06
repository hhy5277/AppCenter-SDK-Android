/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.storage.exception.StorageException;
import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.storage.models.ReadOptions;
import com.microsoft.appcenter.storage.models.WriteOptions;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.storage.DatabaseManager;
import com.microsoft.appcenter.utils.storage.SQLiteUtils;

import java.util.Calendar;

import static com.microsoft.appcenter.AppCenter.LOG_TAG;

class LocalDocumentStorage {

    /**
     * Database name.
     */
    @VisibleForTesting
    static final String DATABASE = "com.microsoft.appcenter.documents";

    /**
     * Table name.
     */
    private static final String TABLE = "cache";

    /**
     * Partition column.
     */
    private static final String PARTITION_COLUMN_NAME = "partition";

    /**
     * Document Id column.
     */
    private static final String DOCUMENT_ID_COLUMN_NAME = "document_id";

    /**
     * Document column.
     */
    private static final String DOCUMENT_COLUMN_NAME = "document";

    /**
     * Etag column.
     */
    private static final String ETAG_COLUMN_NAME = "etag";

    /**
     * Expiration time column.
     */
    private static final String EXPIRATION_TIME_COLUMN_NAME = "expiration_time";

    /**
     * Download time column.
     */
    private static final String DOWNLOAD_TIME_COLUMN_NAME = "download_time";

    /**
     * Operation time column.
     */
    private static final String OPERATION_TIME_COLUMN_NAME = "operation_time";

    /**
     * Pending operation column.
     */
    private static final String PENDING_OPERATION_COLUMN_NAME = "pending_operation";

    /**
     * Pending operation CREATE value.
     */
    private static final String PENDING_OPERATION_CREATE_VALUE = "CREATE";

    /**
     * Pending operation REPLACE value.
     */
    private static final String PENDING_OPERATION_REPLACE_VALUE = "REPLACE";

    /**
     * Pending operation DELETE value.
     */
    private static final String PENDING_OPERATION_DELETE_VALUE = "DELETE";

    /**
     * Current version of the schema.
     */
    private static final int VERSION = 1;

    /**
     * Current schema.
     */
    private static final ContentValues SCHEMA =
            getContentValues("", "", new Document<>(), "", 0, 0, 0, "");

    private final DatabaseManager mDatabaseManager;

    private LocalDocumentStorage(DatabaseManager databaseManager) {
        mDatabaseManager = databaseManager;
    }

    LocalDocumentStorage(Context context) {
        this(new DatabaseManager(context, DATABASE, TABLE, VERSION, SCHEMA, new DatabaseManager.DefaultListener()));
    }

    <T> void write(Document<T> document, WriteOptions writeOptions) {
        AppCenterLog.debug(LOG_TAG, String.format("Trying to replace %s:%s document to cache", document.getPartition(), document.getId()));
        Calendar now = Calendar.getInstance();
        Calendar expiresAt = now;
        expiresAt.add(Calendar.SECOND, writeOptions.getDeviceTimeToLive());
        ContentValues values = getContentValues(
                document.getPartition(), document.getId(),
                document,
                document.getEtag(),
                expiresAt.getTimeInMillis(),
                now.getTimeInMillis(),
                now.getTimeInMillis(),
                PENDING_OPERATION_CREATE_VALUE);
        mDatabaseManager.replace(values);
    }

    <T> Document<T> read(String partition, String documentId, Class<T> documentType, ReadOptions readOptions) {
        AppCenterLog.debug(LOG_TAG, String.format("Trying to read %s:%s document from cache", partition, documentId));
        Cursor cursor;
        ContentValues values;
        try {
            cursor = mDatabaseManager.getCursor(
                    getPartitionAndDocumentIdQueryBuilder(),
                    null,
                    new String[]{partition, documentId},
                    EXPIRATION_TIME_COLUMN_NAME + " DESC");
        } catch (RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "Failed to read from cache: ", e);
            return new Document<>("Failed to read from cache.", e);
        }

        /* We only expect one value as we do upserts in the `write` method */
        values = mDatabaseManager.nextValues(cursor);
        if (values != null) {
            if (readOptions.isExpired(values.getAsLong(EXPIRATION_TIME_COLUMN_NAME))) {
                mDatabaseManager.delete(cursor.getLong(0));
                AppCenterLog.info(LOG_TAG, "Document was found in the cache, but it was expired. The cached document has been invalidated.");
                return new Document<>(new StorageException("Document was found in the cache, but it was expired. The cached document has been invalidated."));
            }
            Document<T> document = Utils.parseDocument(values.getAsString(DOCUMENT_COLUMN_NAME), documentType);
            write(document, new WriteOptions(readOptions.getDeviceTimeToLive()));
            document.setIsFromCache(true);
            return document;
        }
        AppCenterLog.info(LOG_TAG, "Document was found in the cache, but it was expired. The cached document has been invalidated.");
        return new Document<>(new StorageException("Document was not found in the cache."));
    }

    void delete(String partition, String documentId) {
        AppCenterLog.debug(LOG_TAG, String.format("Trying to delete %s:%s document from cache", partition, documentId));
        try {
            mDatabaseManager.delete(
                    String.format("%s = ? AND %s = ?", PARTITION_COLUMN_NAME, DOCUMENT_ID_COLUMN_NAME),
                    new String[]{partition, documentId});
        } catch (RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "Failed to delete from cache: ", e);
        }
    }

    private static SQLiteQueryBuilder getPartitionAndDocumentIdQueryBuilder() {
        SQLiteQueryBuilder builder = SQLiteUtils.newSQLiteQueryBuilder();
        builder.appendWhere(PARTITION_COLUMN_NAME + " = ?");
        builder.appendWhere(" AND ");
        builder.appendWhere(DOCUMENT_ID_COLUMN_NAME + " = ?");
        return builder;
    }

    private static <T> ContentValues getContentValues(
            String partition,
            String documentId,
            Document<T> document,
            String etag,
            long expirationTime,
            long downloadTime,
            long operationTime,
            String pendingOperation) {
        ContentValues values = new ContentValues();
        values.put(PARTITION_COLUMN_NAME, partition);
        values.put(DOCUMENT_ID_COLUMN_NAME, documentId);
        values.put(DOCUMENT_COLUMN_NAME, Utils.getGson().toJson(document));
        values.put(ETAG_COLUMN_NAME, etag);
        values.put(EXPIRATION_TIME_COLUMN_NAME, expirationTime);
        values.put(DOWNLOAD_TIME_COLUMN_NAME, downloadTime);
        values.put(OPERATION_TIME_COLUMN_NAME, operationTime);
        values.put(PENDING_OPERATION_COLUMN_NAME, pendingOperation);
        return values;
    }
}

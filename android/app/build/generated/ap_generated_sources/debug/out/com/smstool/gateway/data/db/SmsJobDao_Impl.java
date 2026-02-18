package com.smstool.gateway.data.db;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SmsJobDao_Impl implements SmsJobDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SmsJobEntity> __insertionAdapterOfSmsJobEntity;

  private final EntityDeletionOrUpdateAdapter<SmsJobEntity> __deletionAdapterOfSmsJobEntity;

  private final EntityDeletionOrUpdateAdapter<SmsJobEntity> __updateAdapterOfSmsJobEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteJobsOlderThan;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public SmsJobDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSmsJobEntity = new EntityInsertionAdapter<SmsJobEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `sms_jobs` (`jobId`,`toNumber`,`body`,`status`,`attempts`,`maxRetries`,`createdAt`,`updatedAt`,`sentAt`,`deliveredAt`,`errorCode`,`errorMessage`,`nextRetryAt`,`pendingReport`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final SmsJobEntity entity) {
        if (entity.jobId == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.jobId);
        }
        if (entity.toNumber == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.toNumber);
        }
        if (entity.body == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.body);
        }
        if (entity.status == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.status);
        }
        statement.bindLong(5, entity.attempts);
        statement.bindLong(6, entity.maxRetries);
        statement.bindLong(7, entity.createdAt);
        statement.bindLong(8, entity.updatedAt);
        if (entity.sentAt == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.sentAt);
        }
        if (entity.deliveredAt == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.deliveredAt);
        }
        if (entity.errorCode == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, entity.errorCode);
        }
        if (entity.errorMessage == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.errorMessage);
        }
        if (entity.nextRetryAt == null) {
          statement.bindNull(13);
        } else {
          statement.bindLong(13, entity.nextRetryAt);
        }
        final int _tmp = entity.pendingReport ? 1 : 0;
        statement.bindLong(14, _tmp);
      }
    };
    this.__deletionAdapterOfSmsJobEntity = new EntityDeletionOrUpdateAdapter<SmsJobEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `sms_jobs` WHERE `jobId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final SmsJobEntity entity) {
        if (entity.jobId == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.jobId);
        }
      }
    };
    this.__updateAdapterOfSmsJobEntity = new EntityDeletionOrUpdateAdapter<SmsJobEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `sms_jobs` SET `jobId` = ?,`toNumber` = ?,`body` = ?,`status` = ?,`attempts` = ?,`maxRetries` = ?,`createdAt` = ?,`updatedAt` = ?,`sentAt` = ?,`deliveredAt` = ?,`errorCode` = ?,`errorMessage` = ?,`nextRetryAt` = ?,`pendingReport` = ? WHERE `jobId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final SmsJobEntity entity) {
        if (entity.jobId == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.jobId);
        }
        if (entity.toNumber == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.toNumber);
        }
        if (entity.body == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.body);
        }
        if (entity.status == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.status);
        }
        statement.bindLong(5, entity.attempts);
        statement.bindLong(6, entity.maxRetries);
        statement.bindLong(7, entity.createdAt);
        statement.bindLong(8, entity.updatedAt);
        if (entity.sentAt == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.sentAt);
        }
        if (entity.deliveredAt == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.deliveredAt);
        }
        if (entity.errorCode == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, entity.errorCode);
        }
        if (entity.errorMessage == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.errorMessage);
        }
        if (entity.nextRetryAt == null) {
          statement.bindNull(13);
        } else {
          statement.bindLong(13, entity.nextRetryAt);
        }
        final int _tmp = entity.pendingReport ? 1 : 0;
        statement.bindLong(14, _tmp);
        if (entity.jobId == null) {
          statement.bindNull(15);
        } else {
          statement.bindString(15, entity.jobId);
        }
      }
    };
    this.__preparedStmtOfDeleteJobsOlderThan = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM sms_jobs WHERE createdAt < ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM sms_jobs";
        return _query;
      }
    };
  }

  @Override
  public long insertJob(final SmsJobEntity job) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      final long _result = __insertionAdapterOfSmsJobEntity.insertAndReturnId(job);
      __db.setTransactionSuccessful();
      return _result;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteJob(final SmsJobEntity job) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfSmsJobEntity.handle(job);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void updateJob(final SmsJobEntity job) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfSmsJobEntity.handle(job);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public int deleteJobsOlderThan(final long olderThanMillis) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteJobsOlderThan.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, olderThanMillis);
    try {
      __db.beginTransaction();
      try {
        final int _result = _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
        return _result;
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfDeleteJobsOlderThan.release(_stmt);
    }
  }

  @Override
  public void deleteAll() {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfDeleteAll.release(_stmt);
    }
  }

  @Override
  public SmsJobEntity getJobById(final String jobId) {
    final String _sql = "SELECT * FROM sms_jobs WHERE jobId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (jobId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, jobId);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfJobId = CursorUtil.getColumnIndexOrThrow(_cursor, "jobId");
      final int _cursorIndexOfToNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "toNumber");
      final int _cursorIndexOfBody = CursorUtil.getColumnIndexOrThrow(_cursor, "body");
      final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
      final int _cursorIndexOfAttempts = CursorUtil.getColumnIndexOrThrow(_cursor, "attempts");
      final int _cursorIndexOfMaxRetries = CursorUtil.getColumnIndexOrThrow(_cursor, "maxRetries");
      final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
      final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
      final int _cursorIndexOfSentAt = CursorUtil.getColumnIndexOrThrow(_cursor, "sentAt");
      final int _cursorIndexOfDeliveredAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deliveredAt");
      final int _cursorIndexOfErrorCode = CursorUtil.getColumnIndexOrThrow(_cursor, "errorCode");
      final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "errorMessage");
      final int _cursorIndexOfNextRetryAt = CursorUtil.getColumnIndexOrThrow(_cursor, "nextRetryAt");
      final int _cursorIndexOfPendingReport = CursorUtil.getColumnIndexOrThrow(_cursor, "pendingReport");
      final SmsJobEntity _result;
      if (_cursor.moveToFirst()) {
        final String _tmpJobId;
        if (_cursor.isNull(_cursorIndexOfJobId)) {
          _tmpJobId = null;
        } else {
          _tmpJobId = _cursor.getString(_cursorIndexOfJobId);
        }
        final String _tmpToNumber;
        if (_cursor.isNull(_cursorIndexOfToNumber)) {
          _tmpToNumber = null;
        } else {
          _tmpToNumber = _cursor.getString(_cursorIndexOfToNumber);
        }
        final String _tmpBody;
        if (_cursor.isNull(_cursorIndexOfBody)) {
          _tmpBody = null;
        } else {
          _tmpBody = _cursor.getString(_cursorIndexOfBody);
        }
        final String _tmpStatus;
        if (_cursor.isNull(_cursorIndexOfStatus)) {
          _tmpStatus = null;
        } else {
          _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
        }
        final int _tmpAttempts;
        _tmpAttempts = _cursor.getInt(_cursorIndexOfAttempts);
        final int _tmpMaxRetries;
        _tmpMaxRetries = _cursor.getInt(_cursorIndexOfMaxRetries);
        final long _tmpCreatedAt;
        _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
        final long _tmpUpdatedAt;
        _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
        _result = new SmsJobEntity(_tmpJobId,_tmpToNumber,_tmpBody,_tmpStatus,_tmpAttempts,_tmpMaxRetries,_tmpCreatedAt,_tmpUpdatedAt);
        if (_cursor.isNull(_cursorIndexOfSentAt)) {
          _result.sentAt = null;
        } else {
          _result.sentAt = _cursor.getLong(_cursorIndexOfSentAt);
        }
        if (_cursor.isNull(_cursorIndexOfDeliveredAt)) {
          _result.deliveredAt = null;
        } else {
          _result.deliveredAt = _cursor.getLong(_cursorIndexOfDeliveredAt);
        }
        if (_cursor.isNull(_cursorIndexOfErrorCode)) {
          _result.errorCode = null;
        } else {
          _result.errorCode = _cursor.getInt(_cursorIndexOfErrorCode);
        }
        if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
          _result.errorMessage = null;
        } else {
          _result.errorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
        }
        if (_cursor.isNull(_cursorIndexOfNextRetryAt)) {
          _result.nextRetryAt = null;
        } else {
          _result.nextRetryAt = _cursor.getLong(_cursorIndexOfNextRetryAt);
        }
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfPendingReport);
        _result.pendingReport = _tmp != 0;
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<SmsJobEntity> getJobsByStatus(final String status) {
    final String _sql = "SELECT * FROM sms_jobs WHERE status = ? ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (status == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, status);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfJobId = CursorUtil.getColumnIndexOrThrow(_cursor, "jobId");
      final int _cursorIndexOfToNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "toNumber");
      final int _cursorIndexOfBody = CursorUtil.getColumnIndexOrThrow(_cursor, "body");
      final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
      final int _cursorIndexOfAttempts = CursorUtil.getColumnIndexOrThrow(_cursor, "attempts");
      final int _cursorIndexOfMaxRetries = CursorUtil.getColumnIndexOrThrow(_cursor, "maxRetries");
      final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
      final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
      final int _cursorIndexOfSentAt = CursorUtil.getColumnIndexOrThrow(_cursor, "sentAt");
      final int _cursorIndexOfDeliveredAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deliveredAt");
      final int _cursorIndexOfErrorCode = CursorUtil.getColumnIndexOrThrow(_cursor, "errorCode");
      final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "errorMessage");
      final int _cursorIndexOfNextRetryAt = CursorUtil.getColumnIndexOrThrow(_cursor, "nextRetryAt");
      final int _cursorIndexOfPendingReport = CursorUtil.getColumnIndexOrThrow(_cursor, "pendingReport");
      final List<SmsJobEntity> _result = new ArrayList<SmsJobEntity>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final SmsJobEntity _item;
        final String _tmpJobId;
        if (_cursor.isNull(_cursorIndexOfJobId)) {
          _tmpJobId = null;
        } else {
          _tmpJobId = _cursor.getString(_cursorIndexOfJobId);
        }
        final String _tmpToNumber;
        if (_cursor.isNull(_cursorIndexOfToNumber)) {
          _tmpToNumber = null;
        } else {
          _tmpToNumber = _cursor.getString(_cursorIndexOfToNumber);
        }
        final String _tmpBody;
        if (_cursor.isNull(_cursorIndexOfBody)) {
          _tmpBody = null;
        } else {
          _tmpBody = _cursor.getString(_cursorIndexOfBody);
        }
        final String _tmpStatus;
        if (_cursor.isNull(_cursorIndexOfStatus)) {
          _tmpStatus = null;
        } else {
          _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
        }
        final int _tmpAttempts;
        _tmpAttempts = _cursor.getInt(_cursorIndexOfAttempts);
        final int _tmpMaxRetries;
        _tmpMaxRetries = _cursor.getInt(_cursorIndexOfMaxRetries);
        final long _tmpCreatedAt;
        _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
        final long _tmpUpdatedAt;
        _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
        _item = new SmsJobEntity(_tmpJobId,_tmpToNumber,_tmpBody,_tmpStatus,_tmpAttempts,_tmpMaxRetries,_tmpCreatedAt,_tmpUpdatedAt);
        if (_cursor.isNull(_cursorIndexOfSentAt)) {
          _item.sentAt = null;
        } else {
          _item.sentAt = _cursor.getLong(_cursorIndexOfSentAt);
        }
        if (_cursor.isNull(_cursorIndexOfDeliveredAt)) {
          _item.deliveredAt = null;
        } else {
          _item.deliveredAt = _cursor.getLong(_cursorIndexOfDeliveredAt);
        }
        if (_cursor.isNull(_cursorIndexOfErrorCode)) {
          _item.errorCode = null;
        } else {
          _item.errorCode = _cursor.getInt(_cursorIndexOfErrorCode);
        }
        if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
          _item.errorMessage = null;
        } else {
          _item.errorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
        }
        if (_cursor.isNull(_cursorIndexOfNextRetryAt)) {
          _item.nextRetryAt = null;
        } else {
          _item.nextRetryAt = _cursor.getLong(_cursorIndexOfNextRetryAt);
        }
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfPendingReport);
        _item.pendingReport = _tmp != 0;
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<SmsJobEntity> getJobsReadyForRetry(final long nowMillis) {
    final String _sql = "SELECT * FROM sms_jobs WHERE status = 'failed_retrying' AND nextRetryAt <= ? ORDER BY nextRetryAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, nowMillis);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfJobId = CursorUtil.getColumnIndexOrThrow(_cursor, "jobId");
      final int _cursorIndexOfToNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "toNumber");
      final int _cursorIndexOfBody = CursorUtil.getColumnIndexOrThrow(_cursor, "body");
      final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
      final int _cursorIndexOfAttempts = CursorUtil.getColumnIndexOrThrow(_cursor, "attempts");
      final int _cursorIndexOfMaxRetries = CursorUtil.getColumnIndexOrThrow(_cursor, "maxRetries");
      final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
      final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
      final int _cursorIndexOfSentAt = CursorUtil.getColumnIndexOrThrow(_cursor, "sentAt");
      final int _cursorIndexOfDeliveredAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deliveredAt");
      final int _cursorIndexOfErrorCode = CursorUtil.getColumnIndexOrThrow(_cursor, "errorCode");
      final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "errorMessage");
      final int _cursorIndexOfNextRetryAt = CursorUtil.getColumnIndexOrThrow(_cursor, "nextRetryAt");
      final int _cursorIndexOfPendingReport = CursorUtil.getColumnIndexOrThrow(_cursor, "pendingReport");
      final List<SmsJobEntity> _result = new ArrayList<SmsJobEntity>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final SmsJobEntity _item;
        final String _tmpJobId;
        if (_cursor.isNull(_cursorIndexOfJobId)) {
          _tmpJobId = null;
        } else {
          _tmpJobId = _cursor.getString(_cursorIndexOfJobId);
        }
        final String _tmpToNumber;
        if (_cursor.isNull(_cursorIndexOfToNumber)) {
          _tmpToNumber = null;
        } else {
          _tmpToNumber = _cursor.getString(_cursorIndexOfToNumber);
        }
        final String _tmpBody;
        if (_cursor.isNull(_cursorIndexOfBody)) {
          _tmpBody = null;
        } else {
          _tmpBody = _cursor.getString(_cursorIndexOfBody);
        }
        final String _tmpStatus;
        if (_cursor.isNull(_cursorIndexOfStatus)) {
          _tmpStatus = null;
        } else {
          _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
        }
        final int _tmpAttempts;
        _tmpAttempts = _cursor.getInt(_cursorIndexOfAttempts);
        final int _tmpMaxRetries;
        _tmpMaxRetries = _cursor.getInt(_cursorIndexOfMaxRetries);
        final long _tmpCreatedAt;
        _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
        final long _tmpUpdatedAt;
        _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
        _item = new SmsJobEntity(_tmpJobId,_tmpToNumber,_tmpBody,_tmpStatus,_tmpAttempts,_tmpMaxRetries,_tmpCreatedAt,_tmpUpdatedAt);
        if (_cursor.isNull(_cursorIndexOfSentAt)) {
          _item.sentAt = null;
        } else {
          _item.sentAt = _cursor.getLong(_cursorIndexOfSentAt);
        }
        if (_cursor.isNull(_cursorIndexOfDeliveredAt)) {
          _item.deliveredAt = null;
        } else {
          _item.deliveredAt = _cursor.getLong(_cursorIndexOfDeliveredAt);
        }
        if (_cursor.isNull(_cursorIndexOfErrorCode)) {
          _item.errorCode = null;
        } else {
          _item.errorCode = _cursor.getInt(_cursorIndexOfErrorCode);
        }
        if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
          _item.errorMessage = null;
        } else {
          _item.errorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
        }
        if (_cursor.isNull(_cursorIndexOfNextRetryAt)) {
          _item.nextRetryAt = null;
        } else {
          _item.nextRetryAt = _cursor.getLong(_cursorIndexOfNextRetryAt);
        }
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfPendingReport);
        _item.pendingReport = _tmp != 0;
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<SmsJobEntity> getJobsWithPendingReports() {
    final String _sql = "SELECT * FROM sms_jobs WHERE pendingReport = 1 ORDER BY updatedAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfJobId = CursorUtil.getColumnIndexOrThrow(_cursor, "jobId");
      final int _cursorIndexOfToNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "toNumber");
      final int _cursorIndexOfBody = CursorUtil.getColumnIndexOrThrow(_cursor, "body");
      final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
      final int _cursorIndexOfAttempts = CursorUtil.getColumnIndexOrThrow(_cursor, "attempts");
      final int _cursorIndexOfMaxRetries = CursorUtil.getColumnIndexOrThrow(_cursor, "maxRetries");
      final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
      final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
      final int _cursorIndexOfSentAt = CursorUtil.getColumnIndexOrThrow(_cursor, "sentAt");
      final int _cursorIndexOfDeliveredAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deliveredAt");
      final int _cursorIndexOfErrorCode = CursorUtil.getColumnIndexOrThrow(_cursor, "errorCode");
      final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "errorMessage");
      final int _cursorIndexOfNextRetryAt = CursorUtil.getColumnIndexOrThrow(_cursor, "nextRetryAt");
      final int _cursorIndexOfPendingReport = CursorUtil.getColumnIndexOrThrow(_cursor, "pendingReport");
      final List<SmsJobEntity> _result = new ArrayList<SmsJobEntity>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final SmsJobEntity _item;
        final String _tmpJobId;
        if (_cursor.isNull(_cursorIndexOfJobId)) {
          _tmpJobId = null;
        } else {
          _tmpJobId = _cursor.getString(_cursorIndexOfJobId);
        }
        final String _tmpToNumber;
        if (_cursor.isNull(_cursorIndexOfToNumber)) {
          _tmpToNumber = null;
        } else {
          _tmpToNumber = _cursor.getString(_cursorIndexOfToNumber);
        }
        final String _tmpBody;
        if (_cursor.isNull(_cursorIndexOfBody)) {
          _tmpBody = null;
        } else {
          _tmpBody = _cursor.getString(_cursorIndexOfBody);
        }
        final String _tmpStatus;
        if (_cursor.isNull(_cursorIndexOfStatus)) {
          _tmpStatus = null;
        } else {
          _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
        }
        final int _tmpAttempts;
        _tmpAttempts = _cursor.getInt(_cursorIndexOfAttempts);
        final int _tmpMaxRetries;
        _tmpMaxRetries = _cursor.getInt(_cursorIndexOfMaxRetries);
        final long _tmpCreatedAt;
        _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
        final long _tmpUpdatedAt;
        _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
        _item = new SmsJobEntity(_tmpJobId,_tmpToNumber,_tmpBody,_tmpStatus,_tmpAttempts,_tmpMaxRetries,_tmpCreatedAt,_tmpUpdatedAt);
        if (_cursor.isNull(_cursorIndexOfSentAt)) {
          _item.sentAt = null;
        } else {
          _item.sentAt = _cursor.getLong(_cursorIndexOfSentAt);
        }
        if (_cursor.isNull(_cursorIndexOfDeliveredAt)) {
          _item.deliveredAt = null;
        } else {
          _item.deliveredAt = _cursor.getLong(_cursorIndexOfDeliveredAt);
        }
        if (_cursor.isNull(_cursorIndexOfErrorCode)) {
          _item.errorCode = null;
        } else {
          _item.errorCode = _cursor.getInt(_cursorIndexOfErrorCode);
        }
        if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
          _item.errorMessage = null;
        } else {
          _item.errorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
        }
        if (_cursor.isNull(_cursorIndexOfNextRetryAt)) {
          _item.nextRetryAt = null;
        } else {
          _item.nextRetryAt = _cursor.getLong(_cursorIndexOfNextRetryAt);
        }
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfPendingReport);
        _item.pendingReport = _tmp != 0;
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public LiveData<List<SmsJobEntity>> getAllJobsLive() {
    final String _sql = "SELECT * FROM sms_jobs ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"sms_jobs"}, false, new Callable<List<SmsJobEntity>>() {
      @Override
      @Nullable
      public List<SmsJobEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfJobId = CursorUtil.getColumnIndexOrThrow(_cursor, "jobId");
          final int _cursorIndexOfToNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "toNumber");
          final int _cursorIndexOfBody = CursorUtil.getColumnIndexOrThrow(_cursor, "body");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfAttempts = CursorUtil.getColumnIndexOrThrow(_cursor, "attempts");
          final int _cursorIndexOfMaxRetries = CursorUtil.getColumnIndexOrThrow(_cursor, "maxRetries");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfSentAt = CursorUtil.getColumnIndexOrThrow(_cursor, "sentAt");
          final int _cursorIndexOfDeliveredAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deliveredAt");
          final int _cursorIndexOfErrorCode = CursorUtil.getColumnIndexOrThrow(_cursor, "errorCode");
          final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "errorMessage");
          final int _cursorIndexOfNextRetryAt = CursorUtil.getColumnIndexOrThrow(_cursor, "nextRetryAt");
          final int _cursorIndexOfPendingReport = CursorUtil.getColumnIndexOrThrow(_cursor, "pendingReport");
          final List<SmsJobEntity> _result = new ArrayList<SmsJobEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SmsJobEntity _item;
            final String _tmpJobId;
            if (_cursor.isNull(_cursorIndexOfJobId)) {
              _tmpJobId = null;
            } else {
              _tmpJobId = _cursor.getString(_cursorIndexOfJobId);
            }
            final String _tmpToNumber;
            if (_cursor.isNull(_cursorIndexOfToNumber)) {
              _tmpToNumber = null;
            } else {
              _tmpToNumber = _cursor.getString(_cursorIndexOfToNumber);
            }
            final String _tmpBody;
            if (_cursor.isNull(_cursorIndexOfBody)) {
              _tmpBody = null;
            } else {
              _tmpBody = _cursor.getString(_cursorIndexOfBody);
            }
            final String _tmpStatus;
            if (_cursor.isNull(_cursorIndexOfStatus)) {
              _tmpStatus = null;
            } else {
              _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            }
            final int _tmpAttempts;
            _tmpAttempts = _cursor.getInt(_cursorIndexOfAttempts);
            final int _tmpMaxRetries;
            _tmpMaxRetries = _cursor.getInt(_cursorIndexOfMaxRetries);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new SmsJobEntity(_tmpJobId,_tmpToNumber,_tmpBody,_tmpStatus,_tmpAttempts,_tmpMaxRetries,_tmpCreatedAt,_tmpUpdatedAt);
            if (_cursor.isNull(_cursorIndexOfSentAt)) {
              _item.sentAt = null;
            } else {
              _item.sentAt = _cursor.getLong(_cursorIndexOfSentAt);
            }
            if (_cursor.isNull(_cursorIndexOfDeliveredAt)) {
              _item.deliveredAt = null;
            } else {
              _item.deliveredAt = _cursor.getLong(_cursorIndexOfDeliveredAt);
            }
            if (_cursor.isNull(_cursorIndexOfErrorCode)) {
              _item.errorCode = null;
            } else {
              _item.errorCode = _cursor.getInt(_cursorIndexOfErrorCode);
            }
            if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
              _item.errorMessage = null;
            } else {
              _item.errorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
            }
            if (_cursor.isNull(_cursorIndexOfNextRetryAt)) {
              _item.nextRetryAt = null;
            } else {
              _item.nextRetryAt = _cursor.getLong(_cursorIndexOfNextRetryAt);
            }
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfPendingReport);
            _item.pendingReport = _tmp != 0;
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public int countJobsByStatus(final String status) {
    final String _sql = "SELECT COUNT(*) FROM sms_jobs WHERE status = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (status == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, status);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}

package com.sshfp.ssh;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.sshfp.model.Host;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class HostDao_Impl implements HostDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Host> __insertionAdapterOfHost;

  private final EntityDeletionOrUpdateAdapter<Host> __deletionAdapterOfHost;

  private final EntityDeletionOrUpdateAdapter<Host> __updateAdapterOfHost;

  private final SharedSQLiteStatement __preparedStmtOfDeleteHostById;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllHosts;

  private final SharedSQLiteStatement __preparedStmtOfUpdateLastConnected;

  private final SharedSQLiteStatement __preparedStmtOfUpdateSortOrder;

  public HostDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfHost = new EntityInsertionAdapter<Host>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `hosts` (`id`,`name`,`address`,`port`,`username`,`authMethod`,`encryptedPassword`,`privateKeyPath`,`encryptedPassphrase`,`initialDirectory`,`createdAt`,`lastConnectedAt`,`sortOrder`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Host entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getAddress());
        statement.bindLong(4, entity.getPort());
        statement.bindString(5, entity.getUsername());
        statement.bindString(6, __AuthMethod_enumToString(entity.getAuthMethod()));
        statement.bindString(7, entity.getEncryptedPassword());
        statement.bindString(8, entity.getPrivateKeyPath());
        statement.bindString(9, entity.getEncryptedPassphrase());
        statement.bindString(10, entity.getInitialDirectory());
        statement.bindLong(11, entity.getCreatedAt());
        statement.bindLong(12, entity.getLastConnectedAt());
        statement.bindLong(13, entity.getSortOrder());
      }
    };
    this.__deletionAdapterOfHost = new EntityDeletionOrUpdateAdapter<Host>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `hosts` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Host entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfHost = new EntityDeletionOrUpdateAdapter<Host>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `hosts` SET `id` = ?,`name` = ?,`address` = ?,`port` = ?,`username` = ?,`authMethod` = ?,`encryptedPassword` = ?,`privateKeyPath` = ?,`encryptedPassphrase` = ?,`initialDirectory` = ?,`createdAt` = ?,`lastConnectedAt` = ?,`sortOrder` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Host entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getAddress());
        statement.bindLong(4, entity.getPort());
        statement.bindString(5, entity.getUsername());
        statement.bindString(6, __AuthMethod_enumToString(entity.getAuthMethod()));
        statement.bindString(7, entity.getEncryptedPassword());
        statement.bindString(8, entity.getPrivateKeyPath());
        statement.bindString(9, entity.getEncryptedPassphrase());
        statement.bindString(10, entity.getInitialDirectory());
        statement.bindLong(11, entity.getCreatedAt());
        statement.bindLong(12, entity.getLastConnectedAt());
        statement.bindLong(13, entity.getSortOrder());
        statement.bindLong(14, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteHostById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM hosts WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllHosts = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM hosts";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateLastConnected = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE hosts SET lastConnectedAt = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateSortOrder = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE hosts SET sortOrder = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertHost(final Host host, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfHost.insertAndReturnId(host);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertHosts(final List<Host> hosts, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfHost.insert(hosts);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteHost(final Host host, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfHost.handle(host);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateHost(final Host host, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfHost.handle(host);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteHostById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteHostById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteHostById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllHosts(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllHosts.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAllHosts.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateLastConnected(final long hostId, final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateLastConnected.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, hostId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateLastConnected.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateSortOrder(final long hostId, final int order,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateSortOrder.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, order);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, hostId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateSortOrder.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Host>> getAllHosts() {
    final String _sql = "SELECT * FROM hosts ORDER BY sortOrder, name";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"hosts"}, new Callable<List<Host>>() {
      @Override
      @NonNull
      public List<Host> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfPort = CursorUtil.getColumnIndexOrThrow(_cursor, "port");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfAuthMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "authMethod");
          final int _cursorIndexOfEncryptedPassword = CursorUtil.getColumnIndexOrThrow(_cursor, "encryptedPassword");
          final int _cursorIndexOfPrivateKeyPath = CursorUtil.getColumnIndexOrThrow(_cursor, "privateKeyPath");
          final int _cursorIndexOfEncryptedPassphrase = CursorUtil.getColumnIndexOrThrow(_cursor, "encryptedPassphrase");
          final int _cursorIndexOfInitialDirectory = CursorUtil.getColumnIndexOrThrow(_cursor, "initialDirectory");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastConnectedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastConnectedAt");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final List<Host> _result = new ArrayList<Host>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Host _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpAddress;
            _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            final int _tmpPort;
            _tmpPort = _cursor.getInt(_cursorIndexOfPort);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final Host.AuthMethod _tmpAuthMethod;
            _tmpAuthMethod = __AuthMethod_stringToEnum(_cursor.getString(_cursorIndexOfAuthMethod));
            final String _tmpEncryptedPassword;
            _tmpEncryptedPassword = _cursor.getString(_cursorIndexOfEncryptedPassword);
            final String _tmpPrivateKeyPath;
            _tmpPrivateKeyPath = _cursor.getString(_cursorIndexOfPrivateKeyPath);
            final String _tmpEncryptedPassphrase;
            _tmpEncryptedPassphrase = _cursor.getString(_cursorIndexOfEncryptedPassphrase);
            final String _tmpInitialDirectory;
            _tmpInitialDirectory = _cursor.getString(_cursorIndexOfInitialDirectory);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpLastConnectedAt;
            _tmpLastConnectedAt = _cursor.getLong(_cursorIndexOfLastConnectedAt);
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            _item = new Host(_tmpId,_tmpName,_tmpAddress,_tmpPort,_tmpUsername,_tmpAuthMethod,_tmpEncryptedPassword,_tmpPrivateKeyPath,_tmpEncryptedPassphrase,_tmpInitialDirectory,_tmpCreatedAt,_tmpLastConnectedAt,_tmpSortOrder);
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
  public Object getAllHostsList(final Continuation<? super List<Host>> $completion) {
    final String _sql = "SELECT * FROM hosts ORDER BY sortOrder, name";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Host>>() {
      @Override
      @NonNull
      public List<Host> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfPort = CursorUtil.getColumnIndexOrThrow(_cursor, "port");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfAuthMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "authMethod");
          final int _cursorIndexOfEncryptedPassword = CursorUtil.getColumnIndexOrThrow(_cursor, "encryptedPassword");
          final int _cursorIndexOfPrivateKeyPath = CursorUtil.getColumnIndexOrThrow(_cursor, "privateKeyPath");
          final int _cursorIndexOfEncryptedPassphrase = CursorUtil.getColumnIndexOrThrow(_cursor, "encryptedPassphrase");
          final int _cursorIndexOfInitialDirectory = CursorUtil.getColumnIndexOrThrow(_cursor, "initialDirectory");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastConnectedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastConnectedAt");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final List<Host> _result = new ArrayList<Host>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Host _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpAddress;
            _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            final int _tmpPort;
            _tmpPort = _cursor.getInt(_cursorIndexOfPort);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final Host.AuthMethod _tmpAuthMethod;
            _tmpAuthMethod = __AuthMethod_stringToEnum(_cursor.getString(_cursorIndexOfAuthMethod));
            final String _tmpEncryptedPassword;
            _tmpEncryptedPassword = _cursor.getString(_cursorIndexOfEncryptedPassword);
            final String _tmpPrivateKeyPath;
            _tmpPrivateKeyPath = _cursor.getString(_cursorIndexOfPrivateKeyPath);
            final String _tmpEncryptedPassphrase;
            _tmpEncryptedPassphrase = _cursor.getString(_cursorIndexOfEncryptedPassphrase);
            final String _tmpInitialDirectory;
            _tmpInitialDirectory = _cursor.getString(_cursorIndexOfInitialDirectory);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpLastConnectedAt;
            _tmpLastConnectedAt = _cursor.getLong(_cursorIndexOfLastConnectedAt);
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            _item = new Host(_tmpId,_tmpName,_tmpAddress,_tmpPort,_tmpUsername,_tmpAuthMethod,_tmpEncryptedPassword,_tmpPrivateKeyPath,_tmpEncryptedPassphrase,_tmpInitialDirectory,_tmpCreatedAt,_tmpLastConnectedAt,_tmpSortOrder);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getHostById(final long id, final Continuation<? super Host> $completion) {
    final String _sql = "SELECT * FROM hosts WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Host>() {
      @Override
      @Nullable
      public Host call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfPort = CursorUtil.getColumnIndexOrThrow(_cursor, "port");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfAuthMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "authMethod");
          final int _cursorIndexOfEncryptedPassword = CursorUtil.getColumnIndexOrThrow(_cursor, "encryptedPassword");
          final int _cursorIndexOfPrivateKeyPath = CursorUtil.getColumnIndexOrThrow(_cursor, "privateKeyPath");
          final int _cursorIndexOfEncryptedPassphrase = CursorUtil.getColumnIndexOrThrow(_cursor, "encryptedPassphrase");
          final int _cursorIndexOfInitialDirectory = CursorUtil.getColumnIndexOrThrow(_cursor, "initialDirectory");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastConnectedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastConnectedAt");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final Host _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpAddress;
            _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            final int _tmpPort;
            _tmpPort = _cursor.getInt(_cursorIndexOfPort);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final Host.AuthMethod _tmpAuthMethod;
            _tmpAuthMethod = __AuthMethod_stringToEnum(_cursor.getString(_cursorIndexOfAuthMethod));
            final String _tmpEncryptedPassword;
            _tmpEncryptedPassword = _cursor.getString(_cursorIndexOfEncryptedPassword);
            final String _tmpPrivateKeyPath;
            _tmpPrivateKeyPath = _cursor.getString(_cursorIndexOfPrivateKeyPath);
            final String _tmpEncryptedPassphrase;
            _tmpEncryptedPassphrase = _cursor.getString(_cursorIndexOfEncryptedPassphrase);
            final String _tmpInitialDirectory;
            _tmpInitialDirectory = _cursor.getString(_cursorIndexOfInitialDirectory);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpLastConnectedAt;
            _tmpLastConnectedAt = _cursor.getLong(_cursorIndexOfLastConnectedAt);
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            _result = new Host(_tmpId,_tmpName,_tmpAddress,_tmpPort,_tmpUsername,_tmpAuthMethod,_tmpEncryptedPassword,_tmpPrivateKeyPath,_tmpEncryptedPassphrase,_tmpInitialDirectory,_tmpCreatedAt,_tmpLastConnectedAt,_tmpSortOrder);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private String __AuthMethod_enumToString(@NonNull final Host.AuthMethod _value) {
    switch (_value) {
      case PASSWORD: return "PASSWORD";
      case KEY: return "KEY";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private Host.AuthMethod __AuthMethod_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "PASSWORD": return Host.AuthMethod.PASSWORD;
      case "KEY": return Host.AuthMethod.KEY;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }
}

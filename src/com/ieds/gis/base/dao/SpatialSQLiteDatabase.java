package com.ieds.gis.base.dao;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import jsqlite.Callback;
import android.database.SQLException;
import android.util.Config;
import android.util.Log;

public class SpatialSQLiteDatabase {
	private static final String TAG = "Database";
	private jsqlite.Database spatialDB;
	/**
	 * Maximum Length Of A LIKE Or GLOB Pattern The pattern matching algorithm
	 * used in the default LIKE and GLOB implementation of SQLite can exhibit
	 * O(N^2) performance (where N is the number of characters in the pattern)
	 * for certain pathological cases. To avoid denial-of-service attacks the
	 * length of the LIKE or GLOB pattern is limited to
	 * SQLITE_MAX_LIKE_PATTERN_LENGTH bytes. The default value of this limit is
	 * 50000. A modern workstation can evaluate even a pathological LIKE or GLOB
	 * pattern of 50000 bytes relatively quickly. The denial of service problem
	 * only comes into play when the pattern length gets into millions of bytes.
	 * Nevertheless, since most useful LIKE or GLOB patterns are at most a few
	 * dozen bytes in length, paranoid application developers may want to reduce
	 * this parameter to something in the range of a few hundred if they know
	 * that external users are able to generate arbitrary patterns.
	 */
	public static final int SQLITE_MAX_LIKE_PATTERN_LENGTH = 50000;

	/**
	 * Flag for {@link #openDatabase} to open the database for reading and
	 * writing. If the disk is full, this may fail even before you actually
	 * write anything.
	 * 
	 * {@more} Note that the value of this flag is 0, so it is the
	 * default.
	 */
	public static final int OPEN_READWRITE = 0x00000000; // update native code
															// if changing

	/**
	 * Flag for {@link #openDatabase} to open the database for reading only.
	 * This is the only reliable way to open a database if the disk may be full.
	 */
	public static final int OPEN_READONLY = 0x00000001; // update native code if
														// changing

	private static final int OPEN_READ_MASK = 0x00000001; // update native code
															// if changing

	/**
	 * Flag for {@link #openDatabase} to open the database without support for
	 * localized collators.
	 * 
	 * {@more} This causes the collator <code>LOCALIZED</code> not to be
	 * created. You must be consistent when using this flag to use the setting
	 * the database was created with. If this is set, {@link #setLocale} will do
	 * nothing.
	 */
	public static final int NO_LOCALIZED_COLLATORS = 0x00000010; // update
																	// native
																	// code if
																	// changing

	/**
	 * Flag for {@link #openDatabase} to create the database file if it does not
	 * already exist.
	 */
	public static final int CREATE_IF_NECESSARY = 0x10000000; // update native
																// code if
																// changing

	/**
	 * Indicates whether the most-recently started transaction has been marked
	 * as successful.
	 */
	private boolean mInnerTransactionIsSuccessful;

	/**
	 * Valid during the life of a transaction, and indicates whether the entire
	 * transaction (the outer one and all of the inner ones) so far has been
	 * successful.
	 */
	private boolean mTransactionIsSuccessful;

	/** Synchronize on this when accessing the database */
	private final ReentrantLock mLock = new ReentrantLock(true);
	/**
	 * If set then the SQLiteDatabase is made thread-safe by using locks around
	 * critical sections
	 */
	private boolean mLockingEnabled = true;
	private String mPath;

	public SpatialSQLiteDatabase(String path) throws Exception {
		super();
		mPath = path;
		Class.forName("jsqlite.JDBCDriver").newInstance();
		spatialDB = new jsqlite.Database();
		spatialDB.open(path, jsqlite.Constants.SQLITE_OPEN_READWRITE);
	}

	public static SpatialSQLiteDatabase openDatabase(String path)
			throws Exception {
		// Open the database.
		return new SpatialSQLiteDatabase(path);
	}

	/**
	 * Control whether or not the SQLiteDatabase is made thread-safe by using
	 * locks around critical sections. This is pretty expensive, so if you know
	 * that your DB will only be used by a single thread then you should set
	 * this to false. The default is true.
	 * 
	 * @param lockingEnabled
	 *            set to true to enable locks, false otherwise
	 */
	public void setLockingEnabled(boolean lockingEnabled) {
		mLockingEnabled = lockingEnabled;
	}

	/* package */void onCorruption() throws jsqlite.Exception {
		try {
			// Close the database (if we can), which will cause subsequent
			// operations to fail.
			close();
		} finally {
			Log.e(TAG, "Removing corrupt database: " + mPath);
			// Delete the corrupt file. Don't re-create it now -- that would
			// just confuse people
			// -- but the next time someone tries to open it, they can set it up
			// from scratch.
			new File(mPath).delete();
		}
	}

	/**
	 * Close the database.
	 */
	public void close() throws jsqlite.Exception {
		lock();
		try {
			spatialDB.close();
		} finally {
			unlock();
		}
	}

	/**
	 * Locks the database for exclusive access. The database lock must be held
	 * when touch the native sqlite3* object since it is single threaded and
	 * uses a polling lock contention algorithm. The lock is recursive, and may
	 * be acquired multiple times by the same thread. This is a no-op if
	 * mLockingEnabled is false.
	 * 
	 * @see #unlock()
	 */
	/* package */void lock() {
		if (!mLockingEnabled)
			return;
		mLock.lock();
	}

	/**
	 * Locks the database for exclusive access. The database lock must be held
	 * when touch the native sqlite3* object since it is single threaded and
	 * uses a polling lock contention algorithm. The lock is recursive, and may
	 * be acquired multiple times by the same thread.
	 * 
	 * @see #unlockForced()
	 */
	private void lockForced() {
		mLock.lock();
	}

	/**
	 * Releases the database lock. This is a no-op if mLockingEnabled is false.
	 * 
	 * @see #unlock()
	 */
	/* package */void unlock() {
		if (!mLockingEnabled)
			return;
		mLock.unlock();
	}

	/**
	 * Releases the database lock.
	 * 
	 * @see #unlockForced()
	 */
	private void unlockForced() {
		mLock.unlock();
	}

	/**
	 * Begins a transaction. Transactions can be nested. When the outer
	 * transaction is ended all of the work done in that transaction and all of
	 * the nested transactions will be committed or rolled back. The changes
	 * will be rolled back if any transaction is ended without being marked as
	 * clean (by calling setTransactionSuccessful). Otherwise they will be
	 * committed.
	 * 
	 * <p>
	 * Here is the standard idiom for transactions:
	 * 
	 * <pre>
	 *   db.beginTransaction();
	 *   try {
	 *     ...
	 *     db.setTransactionSuccessful();
	 *   } finally {
	 *     db.endTransaction();
	 *   }
	 * </pre>
	 */
	public void beginTransaction() throws jsqlite.Exception {
		lockForced();
		boolean ok = false;
		try {
			// If this thread already had the lock then get out
			if (mLock.getHoldCount() > 1) {
				if (mInnerTransactionIsSuccessful) {
					String msg = "Cannot call beginTransaction between "
							+ "calling setTransactionSuccessful and endTransaction";
					IllegalStateException e = new IllegalStateException(msg);
					Log.e(TAG, "beginTransaction() failed", e);
					throw e;
				}
				ok = true;
				return;
			}

			// This thread didn't already have the lock, so begin a database
			// transaction now.
			spatialDB.exec("BEGIN EXCLUSIVE;", null);
			mTransactionIsSuccessful = true;
			mInnerTransactionIsSuccessful = false;
			ok = true;
		} finally {
			if (!ok) {
				// beginTransaction is called before the try block so we must
				// release the lock in
				// the case of failure.
				unlockForced();
			}
		}
	}

	/**
	 * End a transaction. See beginTransaction for notes about how to use this
	 * and when transactions are committed and rolled back.
	 */
	public void endTransaction() throws jsqlite.Exception {
		if (!mLock.isHeldByCurrentThread()) {
			throw new IllegalStateException("no transaction pending");
		}
		try {
			if (mInnerTransactionIsSuccessful) {
				mInnerTransactionIsSuccessful = false;
			} else {
				mTransactionIsSuccessful = false;
			}
			if (mLock.getHoldCount() != 1) {
				return;
			}
			if (mTransactionIsSuccessful) {
				spatialDB.exec("COMMIT;", null);
			} else {
				try {
					spatialDB.exec("ROLLBACK;", null);
				} catch (SQLException e) {
					if (Config.LOGD) {
						Log.d(TAG,
								"exception during rollback, maybe the DB previously "
										+ "performed an auto-rollback");
					}
				}
			}
		} finally {
			unlockForced();
			if (Config.LOGV) {
				Log.v(TAG, "unlocked " + Thread.currentThread()
						+ ", holdCount is " + mLock.getHoldCount());
			}
		}
	}

	/**
	 * Execute a single SQL statement that is not a query. For example, CREATE
	 * TABLE, DELETE, INSERT, etc. Multiple statements separated by ;s are not
	 * supported. it takes a write lock
	 * 
	 * @throws SQLException
	 *             If the SQL string is invalid for some reason
	 */
	public void execSQL(String sql, Callback cb) throws jsqlite.Exception {
		lock();
		try {
			spatialDB.exec(sql, cb);
		} finally {
			unlock();
		}
	}

	/**
	 * Marks the current transaction as successful. Do not do any more database
	 * work between calling this and calling endTransaction. Do as little
	 * non-database work as possible in that situation too. If any errors are
	 * encountered between this and endTransaction the transaction will still be
	 * committed.
	 * 
	 * @throws IllegalStateException
	 *             if the current thread is not in a transaction or the
	 *             transaction is already marked as successful.
	 */
	public void setTransactionSuccessful() {
		if (!mLock.isHeldByCurrentThread()) {
			throw new IllegalStateException("no transaction pending");
		}
		if (mInnerTransactionIsSuccessful) {
			throw new IllegalStateException(
					"setTransactionSuccessful may only be called once per call to beginTransaction");
		}
		mInnerTransactionIsSuccessful = true;
	}

	/**
	 * return true if there is a transaction pending
	 */
	public boolean inTransaction() {
		return mLock.getHoldCount() > 0;
	}

	/**
	 * Checks if the database lock is held by this thread.
	 * 
	 * @return true, if this thread is holding the database lock.
	 */
	public boolean isDbLockedByCurrentThread() {
		return mLock.isHeldByCurrentThread();
	}

	/**
	 * Checks if the database is locked by another thread. This is just an
	 * estimate, since this status can change at any time, including after the
	 * call is made but before the result has been acted upon.
	 * 
	 * @return true, if the database is locked by another thread
	 */
	public boolean isDbLockedByOtherThreads() {
		return !mLock.isHeldByCurrentThread() && mLock.isLocked();
	}

	/**
	 * @param m
	 * @return
	 */
	public Callback getCall(final MyCursor m) {
		return new Callback() {

			@Override
			public void types(String[] arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public boolean newrow(String[] arg0) {
				List<String> rowList = new ArrayList<String>();
				for (String a : arg0) {
					rowList.add(a);
				}
				m.getValueList().add(rowList);
				return false;
			}

			@Override
			public void columns(String[] arg0) {
				// TODO Auto-generated method stub
				m.getColumnList().clear();
				m.getValueList().clear();
				for (String a : arg0) {
					m.getColumnList().add(a);
				}
			}
		};
	}

	public MyCursor rawQuery(String sql) throws jsqlite.Exception {
		final MyCursor m = new MyCursor();
		execSQL(sql, getCall(m));
		return m;
	}

}

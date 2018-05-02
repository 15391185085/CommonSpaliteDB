/*
 * Copyright (c) 2013. wyouflf (wyouflf@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ieds.gis.base.dao;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.sqlite.SQLiteException;

import com.lidroid.xutils.db.sqlite.SqlInfo;
import com.lidroid.xutils.db.sqlite.WhereBuilder;
import com.lidroid.xutils.db.table.KeyValue;
import com.lidroid.xutils.db.table.MyId;
import com.lidroid.xutils.db.table.MyTable;
import com.lidroid.xutils.exception.DbException;
import com.lidroid.xutils.util.LogUtils;
import com.lidroid.xutils.util.StringUtil;

/**
 * po类必须有构造函数 带外键的表不能用复合主键
 * 
 * 注意: 复合主键需要用saveOrUpdateDoubleKey来做“增，改”的业务 复合主键需要用deleteDoubleKey来做“删除”的业务
 * 
 * @update 2014-11-12 上午11:08:31<br>
 * @author <a href="mailto:lihaoxiang@ieds.com.cn">李昊翔</a>
 * 
 */
public abstract class SpatialiteDbUtils implements IDbUtils {
	/**
	 * 创建数据库时调用 版本号从0开始进行更新 检查到高于当前版本时进行更新 更新后自动保存版本号到最新给定的值
	 */
	private static final int DATABASE_INIT = 0;
	public static final String NOT_WHERE = "参数没有定义";
	private SpatialSQLiteDatabase database;
	private boolean debug = false;
	private boolean allowTransaction = false;

	public SpatialiteDbUtils(File dbFile, int mNewVersion) throws Exception {
		if (mNewVersion < 1)
			throw new IllegalArgumentException("Version must be >= 1, was "
					+ mNewVersion);
		// 允许交易
		this.configAllowTransaction(true);
		// 允许打印日志
		this.configDebug(true);
		this.database = getSQLiteDatabase(dbFile, mNewVersion);
	}

	/**
	 * Gets the database version.
	 * 
	 * @return the database version
	 * @throws DbException
	 */
	public int getVersion(SpatialSQLiteDatabase db) throws Exception {
		String m = "0";
		MyCursor c = db.rawQuery("PRAGMA user_version;");
		while (c.moveToNext()) {
			m = c.getValue(0);
		}
		return Integer.valueOf(m);
	}

	/**
	 * Sets the database version.
	 * 
	 * @param version
	 *            the new database version
	 * @throws DbException
	 */
	public void setVersion(int version, SpatialSQLiteDatabase db)
			throws Exception {
		db.execSQL("PRAGMA user_version = " + version, null);

	}

	public SpatialSQLiteDatabase getSQLiteDatabase(File dbFile, int mNewVersion)
			throws Exception {
		SpatialSQLiteDatabase db = SpatialSQLiteDatabase.openDatabase(dbFile
				.getPath());

		final int version = getVersion(db);
		if (version != mNewVersion) {
			db.beginTransaction();
			try {
				if (version == DATABASE_INIT) {
					onCreate(db);
				} else {
					if (version > mNewVersion) {
						onDowngrade(db, version, mNewVersion);
					} else {
						onUpgrade(db, version, mNewVersion);
					}
				}
				setVersion(mNewVersion, db);
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
		}

		return db;
	}

	public abstract void onCreate(SpatialSQLiteDatabase db);

	public abstract void onUpgrade(SpatialSQLiteDatabase db, int oldVersion,
			int newVersion);

	public void onDowngrade(SpatialSQLiteDatabase db, int oldVersion,
			int newVersion) {
		throw new SQLiteException("Can't downgrade database from version "
				+ oldVersion + " to " + newVersion);
	}

	public SpatialSQLiteDatabase getDatabase() {
		return database;
	}

	public SpatialiteDbUtils configDebug(boolean debug) {
		this.debug = debug;
		return this;
	}

	public SpatialiteDbUtils configAllowTransaction(boolean allowTransaction) {
		this.allowTransaction = allowTransaction;
		return this;
	}

	public void ignore(Object entity) throws DbException {
		try {
			beginTransaction();

			ignoreWithoutTransaction(entity);

			setTransactionSuccessful();
		} finally {
			endTransaction();

		}
	}

	public <T> void ignore(List<T> entities) throws DbException {
		try {
			beginTransaction();

			for (Object entity : entities) {
				if (entity != null) {
					ignoreWithoutTransaction(entity);
				}
			}

			setTransactionSuccessful();
		} finally {
			endTransaction();

		}
	}

	/**
	 * @param entity
	 * @return
	 * @throws DbException
	 */
	public <T> T findFirstByIdEnableNull(T entity) throws DbException {
		Selector selector = getSelectorById(entity);
		T t = findFirstEnableNull(selector);
		return t;
	}

	/**
	 * @param entity
	 * @return
	 * @throws DbException
	 */
	public <T> T findFirstById(T entity) throws DbException {
		Selector selector = getSelectorById(entity);
		T t = findFirstEnableNull(selector);
		return getFindCheck(selector, t);
	}

	public void replace(Object entity) throws DbException {
		try {
			beginTransaction();

			replaceWithoutTransaction(entity);

			setTransactionSuccessful();
		} finally {
			endTransaction();

		}
	}

	public <T> void replace(List<T> entities) throws DbException {
		try {
			beginTransaction();

			for (Object entity : entities) {
				if (entity != null) {
					replaceWithoutTransaction(entity);
				}
			}

			setTransactionSuccessful();
		} finally {
			endTransaction();

		}
	}

	public void save(Object entity) throws DbException {
		try {
			beginTransaction();

			saveWithoutTransaction(entity);

			setTransactionSuccessful();
		} finally {
			endTransaction();

		}
	}

	public <T> void save(List<T> entities) throws DbException {
		try {
			beginTransaction();

			for (Object entity : entities) {
				if (entity != null) {
					saveWithoutTransaction(entity);
				}
			}

			setTransactionSuccessful();
		} finally {
			endTransaction();

		}
	}

	public void delete(Object entity) throws DbException {
		try {
			beginTransaction();

			deleteWithoutTransaction(entity);

			setTransactionSuccessful();
		} finally {
			endTransaction();

		}
	}

	public void deleteById(Object entity) throws DbException {
		try {
			beginTransaction();

			deleteWithoutTransactionById(entity);

			setTransactionSuccessful();
		} finally {
			endTransaction();

		}
	}

	public <T> void deleteById(List<T> entities) throws DbException {
		if (entities == null || entities.size() < 1)
			return;
		try {
			beginTransaction();

			for (Object entity : entities) {
				if (entity != null) {
					deleteWithoutTransactionById(entity);
				}
			}

			setTransactionSuccessful();
		} finally {
			endTransaction();

		}
	}

	public <T> void delete(List<T> entities) throws DbException {
		if (entities == null || entities.size() < 1)
			return;
		try {
			beginTransaction();

			for (Object entity : entities) {
				if (entity != null) {
					deleteWithoutTransaction(entity);
				}
			}

			setTransactionSuccessful();
		} finally {
			endTransaction();
		}
	}

	public void delete(Class<?> entityType, WhereBuilder whereBuilder)
			throws DbException {
		try {
			beginTransaction();

			SqlInfo sql = SpatialSqlInfoBuilder.buildDeleteSqlInfo(entityType,
					whereBuilder);
			execNonQuery(sql);

			setTransactionSuccessful();
		} finally {
			endTransaction();
		}
	}

	/**
	 * 根据id更新对象数据
	 * 
	 * @param entity
	 * @throws DbException
	 */
	public void updateById(Object entity) throws DbException {
		try {
			beginTransaction();

			updateWithoutTransaction(entity);

			setTransactionSuccessful();
		} finally {
			endTransaction();

		}
	}

	/**
	 * 根据id更新所有对象数据
	 * 
	 * @param entity
	 * @throws DbException
	 */
	public <T> void updateById(List<T> entities) throws DbException {
		if (entities == null || entities.size() < 1)
			return;
		try {
			beginTransaction();

			for (Object entity : entities) {
				if (entity != null) {
					updateWithoutTransaction(entity);
				}
			}

			setTransactionSuccessful();
		} finally {
			endTransaction();

		}
	}

	public void updateByWhere(Object entity, WhereBuilder whereBuilder)
			throws DbException {
		try {
			beginTransaction();

			execNonQuery(SpatialSqlInfoBuilder.buildUpdateSqlInfo(entity,
					whereBuilder));

			setTransactionSuccessful();
		} finally {
			endTransaction();

		}
	}

	/**
	 * 查询结果不能为空，为空时抛出异常
	 * 
	 * @param entity
	 * @return
	 * @throws DbException
	 */
	public <T> T findFirstEnableNull(Object entity) throws DbException {
		Selector selector = getSelector(entity);
		return findFirstEnableNull(selector);
	}

	public <T> T findFirstEnableNull(ISelector selector) throws DbException {
		if (selector.getWhereBuilder() == null) {
			throw new DbException(getSqlError(NOT_WHERE, selector.limit(1)
					.getSelectSql()));
		}
		String sql = selector.limit(1).getSelectSql();
		MyCursor cursor = execQuery(sql);
		if (cursor.moveToNext()) {
			T entity = (T) MyCursorUtils.getEntity(cursor,
					selector.getEntityType());
			return entity;
		}
		return null;
	}

	/**
	 * 查询结果不能为空，为空时抛出异常
	 * 
	 * @param selector
	 * @return
	 * @throws DbException
	 */
	public <T> T findFirst(ISelector selector) throws DbException {
		T t = findFirstEnableNull(selector);
		return getFindCheck(selector, t);

	}

	/**
	 * @param selector
	 * @param t
	 * @return
	 * @throws DbException
	 */
	public static <T> T getFindCheck(ISelector selector, T t)
			throws DbException {
		if (t == null) {
			throw new DbException("查询不到有效数据,sql=("
					+ selector.limit(1).getSelectSql() + ")");
		} else {
			return t;
		}
	}

	/**
	 * 查询结果不能为空，为空时抛出异常
	 * 
	 * @param entity
	 * @return
	 * @throws DbException
	 */
	public <T> T findFirst(Object entity) throws DbException {
		Selector selector = getSelector(entity);
		return findFirst(selector);
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> findAll(ISelector selector) throws DbException {
		String sql = selector.getSelectSql();
		MyCursor cursor = execQuery(sql);
		List<T> result = new ArrayList<T>();
		while (cursor.moveToNext()) {
			T entity = (T) MyCursorUtils.getEntity(cursor,
					selector.getEntityType());
			result.add(entity);
		}
		return result;
	}

	public <T> List<T> findAll(Object entity) throws DbException {
		Selector selector = getSelector(entity);
		return findAll(selector);
	}

	/**
	 * 根据对象的属性查询该对象的完整属性
	 * 
	 * @param entity
	 * @return
	 * @throws DbException
	 */
	public Selector getSelector(Object entity) throws DbException {
		Selector selector = new Selector(entity.getClass());
		List<KeyValue> entityKvList = SpatialSqlInfoBuilder
				.entityKeyAndValueList(entity);
		if (entityKvList != null && !entityKvList.isEmpty()) {
			WhereBuilder wb = WhereBuilder.b();
			for (KeyValue keyValue : entityKvList) {
				wb.append(keyValue.getKey(), "=", keyValue.getValue());
			}
			selector.where(wb);
		}
		return selector;
	}

	/**
	 * 根据对象的id属性查询该对象的完整属性
	 * 
	 * @param entity
	 * @return
	 */
	public Selector getSelectorById(Object entity) throws DbException {
		Selector selector = new Selector(entity.getClass());
		MyTable table = MyTable.get(entity.getClass());
		List<MyId> idList = table.getId();
		if (idList != null && !idList.isEmpty()) {
			WhereBuilder wb = WhereBuilder.b();
			for (int i = 0; i < idList.size(); i++) {
				MyId id = (MyId) idList.get(i);
				Object idValue = id.getColumnValue(entity);

				if (idValue == null) {
					throw new DbException("对象[" + entity.getClass()
							+ "]的id不能是null");
				}
				wb.append(id.getColumnName(), "=", idValue);
			}
			selector.where(wb);
		}
		return selector;
	}

	public interface DbUpgradeListener {
		public void onUpgrade(SpatialSQLiteDatabase db, int oldVersion,
				int newVersion);
	}

	private void replaceWithoutTransaction(Object entity) throws DbException {
		execNonQuery(SpatialSqlInfoBuilder.buildReplaceSqlInfo(entity));
	}

	private void saveWithoutTransaction(Object entity) throws DbException {
		execNonQuery(SpatialSqlInfoBuilder.buildInsertSqlInfo(entity));
	}

	private void ignoreWithoutTransaction(Object entity) throws DbException {
		execNonQuery(SpatialSqlInfoBuilder.buildIgnoreSqlInfo(entity));
	}

	private void deleteWithoutTransaction(Object entity) throws DbException {
		SqlInfo result = new SqlInfo();
		List<KeyValue> entityKvList = SpatialSqlInfoBuilder
				.entityKeyAndValueList(entity);
		WhereBuilder wb = null;
		if (entityKvList != null && !entityKvList.isEmpty()) {
			wb = WhereBuilder.b();
			for (KeyValue keyValue : entityKvList) {
				wb.append(keyValue.getKey(), "=", keyValue.getValue());
			}
		}
		SqlInfo sql = SpatialSqlInfoBuilder.buildDeleteSqlInfo(
				entity.getClass(), wb);
		result.setSql(sql.getSql());
		execNonQuery(result);
	}

	private void deleteWithoutTransactionById(Object entity) throws DbException {
		SqlInfo result = new SqlInfo();
		Selector selector = getSelectorById(entity);
		if (selector.getWhereBuilder() == null) {
			throw new DbException(getSqlError(NOT_WHERE, selector.limit(1)
					.getSelectSql()));
		}
		SqlInfo sql = SpatialSqlInfoBuilder.buildDeleteSqlInfo(
				entity.getClass(), selector.getWhereBuilder());
		result.setSql(sql.getSql());
		execNonQuery(result);
	}

	private void updateWithoutTransaction(Object entity) throws DbException {
		execNonQuery(SpatialSqlInfoBuilder.buildUpdateSqlInfo(entity));
	}

	// ************************************************ tools
	// ***********************************

	private static void fillContentValues(ContentValues contentValues,
			List<KeyValue> list) {
		if (list != null && contentValues != null) {
			for (KeyValue kv : list) {
				contentValues.put(kv.getKey(), kv.getValue().toString());
			}
		} else {
			LogUtils.w("List<KeyValue> is empty or ContentValues is empty!");
		}
	}

	public void dropDb() throws DbException {
		MyCursor cursor = execQuery("SELECT name FROM sqlite_master WHERE type ='table'");
		if (cursor != null) {
			while (cursor.moveToNext()) {
				try {
					execNonQuery("DROP TABLE " + cursor.getValue(0));
				} catch (Exception e) {
					LogUtils.e(e.getMessage(), e);
				}
			}
		}
	}

	public void dropTable(Class<?> entityType) throws DbException {
		MyTable table = MyTable.get(entityType);
		execNonQuery("DROP TABLE " + table.getTableName());
	}

	// /////////////////////////////////// exec sql
	// /////////////////////////////////////////////////////
	private void debugSql(String sql) {
		if (debug) {
			LogUtils.d(sql);
		}
	}

	public void beginTransaction() throws DbException {
		try {
			if (allowTransaction) {
				database.beginTransaction();
			}
		} catch (Exception e) {
			throw new DbException(e.getMessage());
		}
	}

	public void setTransactionSuccessful() {
		if (allowTransaction) {
			database.setTransactionSuccessful();
		}
	}

	public void endTransaction() throws DbException {
		try {
			if (allowTransaction) {
				database.endTransaction();
			}
		} catch (Exception e) {
			throw new DbException(e.getMessage());
		}
	}

	public void execNonQuery(SqlInfo sqlInfo) throws DbException {
		debugSql(sqlInfo.getSql());
		try {
			database.execSQL(sqlInfo.getSql(), null);
		} catch (Exception e) {
			e.printStackTrace();
			throw new DbException(getSqlError(e.getMessage(), sqlInfo.getSql(),
					sqlInfo.getBindArgsAsArray()));
		}
	}

	public void execNonQuery(String sql) throws DbException {
		debugSql(sql);
		try {
			database.execSQL(sql, null);
		} catch (Exception e) {
			e.printStackTrace();
			throw new DbException(getSqlError(e.getMessage(), sql));
		}
	}

	public MyCursor execQuery(SqlInfo sqlInfo) throws DbException {
		debugSql(sqlInfo.getSql());
		try {
			return database.rawQuery(sqlInfo.getSql());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DbException(getSqlError(e.getMessage(), sqlInfo.getSql(),
					sqlInfo.getBindArgsAsStrArray()));
		}
	}

	public MyCursor execQuery(String sql) throws DbException {
		debugSql(sql);
		try {
			return database.rawQuery(sql);
		} catch (Exception e) {
			e.printStackTrace();
			throw new DbException(getSqlError(e.getMessage(), sql));
		}
	}

	public static String getSqlError(String message, String sql) {
		return getSqlError(message, sql, null);
	}

	/**
	 * @param sql
	 * @return
	 */
	public static String getSqlError(String message, String sql, Object[] array) {
		String a = "无";
		String s = "无";
		if (sql != null) {
			s = sql;
		}

		if (array != null) {
			StringBuilder sb = new StringBuilder();
			for (Object b : array) {
				sb.append(b.toString() + ",");
			}
			a = StringUtil.deleteLastCharacter(sb);
		}
		return "异常原因：" + message + "\n异常语句：sql=(" + s + "),参数=(" + a + ")";

	}

	@Override
	public ISelector from(Class<?> entityType) throws DbException {
		// TODO Auto-generated method stub
		return new Selector(entityType);
	}

	@Override
	public List<List<String>> execListQuery(SqlInfo sqlInfo) throws DbException {
		MyCursor my = execQuery(sqlInfo);
		return my.getValueList();
	}

	@Override
	public List<List<String>> execListQuery(String sql) throws DbException {
		MyCursor my = execQuery(sql);
		return my.getValueList();
	}
}

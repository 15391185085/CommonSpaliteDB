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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.lidroid.xutils.db.sqlite.SqlInfo;
import com.lidroid.xutils.db.sqlite.WhereBuilder;
import com.lidroid.xutils.db.table.KeyValue;
import com.lidroid.xutils.db.table.MyColumn;
import com.lidroid.xutils.db.table.MyId;
import com.lidroid.xutils.db.table.MyTable;
import com.lidroid.xutils.exception.DbException;

/**
 * Build "insert", "replace",，"update", "delete" and "create" sql.
 */
public class SpatialSqlInfoBuilder {

	private SpatialSqlInfoBuilder() {
	}

	// *********************************************** delete sql
	// ***********************************************

	private static String buildDeleteSqlByTableName(String tableName) {
		return "DELETE FROM " + tableName;
	}

	public static SqlInfo buildDeleteSqlInfo(Class<?> entityType,
			WhereBuilder whereBuilder) throws DbException {
		MyTable table = MyTable.get(entityType);
		StringBuilder sb = new StringBuilder(
				buildDeleteSqlByTableName(table.getTableName()));

		if (whereBuilder != null) {
			sb.append(" WHERE ").append(whereBuilder.toString());
		}

		return new SqlInfo(sb.toString());
	}

	// *********************************************** insert sql
	// ***********************************************

	public static SqlInfo buildInsertSqlInfo(Object entity) throws DbException {

		List<KeyValue> keyValueList = entityKeyAndValueList(entity);
		if (keyValueList.size() == 0)
			return null;

		SqlInfo result = new SqlInfo();
		StringBuffer sqlBuffer = new StringBuffer();

		sqlBuffer.append("INSERT INTO ");
		sqlBuffer.append(MyTable.get(entity.getClass()).getTableName());
		sqlBuffer.append(" (");
		StringBuffer valueBuffer = new StringBuffer();

		for (KeyValue kv : keyValueList) {
			sqlBuffer.append(kv.getKey()).append(",");
			String value = kv.getValue().toString();
			if (value.indexOf(Selector.GEOM_FROM_TEXT) != -1) {
				valueBuffer.append(value + ",");
			} else {
				value = getSqlString(value);
				valueBuffer.append("'" + value + "',");
			}
		}
		valueBuffer.deleteCharAt(valueBuffer.length() - 1);
		sqlBuffer.deleteCharAt(sqlBuffer.length() - 1);
		sqlBuffer.append(") VALUES (");
		sqlBuffer.append(valueBuffer.toString());
		sqlBuffer.append(")");
		result.setSql(sqlBuffer.toString());

		return result;
	}

	// *********************************************** replace sql
	// ***********************************************

	public static SqlInfo buildReplaceSqlInfo(Object entity) throws DbException {

		List<KeyValue> keyValueList = entityKeyAndValueList(entity);
		if (keyValueList.size() == 0)
			return null;

		SqlInfo result = new SqlInfo();
		StringBuffer sqlBuffer = new StringBuffer();

		sqlBuffer.append("INSERT OR REPLACE INTO ");
		sqlBuffer.append(MyTable.get(entity.getClass()).getTableName());
		sqlBuffer.append(" (");
		StringBuffer valueBuffer = new StringBuffer();
		for (KeyValue kv : keyValueList) {
			sqlBuffer.append(kv.getKey()).append(",");
			String value = kv.getValue().toString();
			if (value.indexOf(Selector.GEOM_FROM_TEXT) != -1) {
				valueBuffer.append(value + ",");
			} else {
				value = getSqlString(value);
				valueBuffer.append("'" + value + "',");
			}
		}
		valueBuffer.deleteCharAt(valueBuffer.length() - 1);
		sqlBuffer.deleteCharAt(sqlBuffer.length() - 1);
		sqlBuffer.append(") VALUES (");
		sqlBuffer.append(valueBuffer.toString());
		sqlBuffer.append(")");

		result.setSql(sqlBuffer.toString());

		return result;
	}

	public static SqlInfo buildIgnoreSqlInfo(Object entity) throws DbException {

		List<KeyValue> keyValueList = entityKeyAndValueList(entity);
		if (keyValueList.size() == 0)
			return null;

		SqlInfo result = new SqlInfo();
		StringBuffer sqlBuffer = new StringBuffer();

		sqlBuffer.append("INSERT OR IGNORE INTO ");
		sqlBuffer.append(MyTable.get(entity.getClass()).getTableName());
		sqlBuffer.append(" (");
		StringBuffer valueBuffer = new StringBuffer();
		for (KeyValue kv : keyValueList) {
			sqlBuffer.append(kv.getKey()).append(",");
			String value = kv.getValue().toString();
			if (value.indexOf(Selector.GEOM_FROM_TEXT) != -1) {
				valueBuffer.append(value + ",");
			} else {
				value = getSqlString(value);
				valueBuffer.append("'" + value + "',");
			}
		}
		valueBuffer.deleteCharAt(valueBuffer.length() - 1);
		sqlBuffer.deleteCharAt(sqlBuffer.length() - 1);
		sqlBuffer.append(") VALUES (");
		sqlBuffer.append(valueBuffer.toString());
		sqlBuffer.append(")");

		result.setSql(sqlBuffer.toString());

		return result;
	}

	public static String getSqlString(String getSqlValue) throws DbException {
		return getSqlValue.replace("'", "''");
	}

	/**
	 * 处理更新用，可以将null更新回数据库
	 * 
	 * @param entity
	 * @param column
	 * @return
	 */
	public static SqlInfo buildUpdateSqlInfo(Object entity) throws DbException {

		List<KeyValue> keyValueList = entityKeyAndValueList(entity);
		if (keyValueList.size() == 0)
			return null;

		MyTable table = MyTable.get(entity.getClass());

		SqlInfo result = new SqlInfo();
		StringBuffer sqlBuffer = new StringBuffer("UPDATE ");
		sqlBuffer.append(table.getTableName());
		sqlBuffer.append(" SET ");
		for (KeyValue kv : keyValueList) {
			String value = kv.getValue().toString();
			if (value.indexOf(Selector.GEOM_FROM_TEXT) != -1) {
				sqlBuffer.append(kv.getKey()).append("=" + value + ",");
			} else {
				value = getSqlString(value);
				sqlBuffer.append(kv.getKey()).append("='" + value + "',");
			}
		}
		sqlBuffer.deleteCharAt(sqlBuffer.length() - 1);

		List<MyId> idList = table.getId();
		sqlBuffer.append(" WHERE ");
		for (int i = 0; i < idList.size(); i++) {
			MyId id = (MyId) idList.get(i);
			Object idValue = id.getColumnValue(entity);
			if (idValue == null) {
				throw new DbException("对象[" + entity.getClass() + "]的id不能是null");
			}
			sqlBuffer.append(WhereBuilder.b(id.getColumnName(), "=", idValue));
			if (i < (idList.size() - 1)) {
				sqlBuffer.append(" and ");

			}
		}

		result.setSql(sqlBuffer.toString());
		return result;
	}

	public static SqlInfo buildUpdateSqlInfo(Object entity,
			WhereBuilder whereBuilder) throws DbException {

		List<KeyValue> keyValueList = entityKeyAndValueList(entity);
		if (keyValueList.size() == 0)
			return null;

		MyTable table = MyTable.get(entity.getClass());

		SqlInfo result = new SqlInfo();
		StringBuffer sqlBuffer = new StringBuffer("UPDATE ");
		sqlBuffer.append(table.getTableName());
		sqlBuffer.append(" SET ");
		for (KeyValue kv : keyValueList) {
			String value = kv.getValue().toString();
			if (value.indexOf(Selector.GEOM_FROM_TEXT) != -1) {
				sqlBuffer.append(kv.getKey()).append("=" + value + ",");
			} else {
				value = getSqlString(value);
				sqlBuffer.append(kv.getKey()).append("='" + value + "',");
			}
		}
		sqlBuffer.deleteCharAt(sqlBuffer.length() - 1);
		if (whereBuilder != null) {
			sqlBuffer.append(" WHERE ").append(whereBuilder.toString());
		}

		result.setSql(sqlBuffer.toString());
		return result;
	}

	private static KeyValue columnKeyValue(Object entity, MyColumn column)
			throws DbException {
		KeyValue kv = null;
		String key = column.getColumnName();
		Object value = column.getColumnValue(entity);
		value = value == null ? column.getDefaultValue() : value;
		if (key != null && value != null) {
			kv = new KeyValue(key, value);
		}
		return kv;
	}

	/**
	 * 获取key和value的组合
	 * 
	 * @param db
	 * @param entity
	 * @return
	 * @throws DbException
	 */
	public static List<KeyValue> entityKeyAndValueList(Object entity)
			throws DbException {

		List<KeyValue> keyValueList = new ArrayList<KeyValue>();

		MyTable table = MyTable.get(entity.getClass());

		List<MyId> idList = table.getId();
		for (Iterator iterator = idList.iterator(); iterator.hasNext();) {
			MyId id = (MyId) iterator.next();
			Object idValue = id.getColumnValue(entity);
			if (idValue != null) {
				KeyValue kv = new KeyValue(id.getColumnName(), idValue);
				keyValueList.add(kv);
			}
		}

		Collection<MyColumn> columns = table.columnMap.values();
		for (MyColumn column : columns) {
			KeyValue kv = columnKeyValue(entity, column);
			if (kv != null) {
				keyValueList.add(kv);
			}
		}

		return keyValueList;
	}
}

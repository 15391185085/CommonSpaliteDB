package com.ieds.gis.base.dao;

import java.util.Iterator;
import java.util.List;

import com.lidroid.xutils.db.table.MyColumn;
import com.lidroid.xutils.db.table.MyId;
import com.lidroid.xutils.db.table.MyTable;
import com.lidroid.xutils.util.LogUtils;

public class MyCursorUtils {


	@SuppressWarnings("unchecked")
	public static <T> T getEntity(MyCursor cursor, Class<T> entityType) {
		if (cursor == null) {
			return null;
		}

		try {
			MyTable table = MyTable.get(entityType);
			T entity = entityType.newInstance();
			int columnCount = cursor.getColumnCount();
			for (int i = 0; i < columnCount; i++) {
				String columnName = cursor.getColumnName(i);
				if (columnName.equals(Selector.getSelectGeometry())) {
					columnName = Selector.GEOMETRY_FIELD;
				}
				MyColumn column = table.columnMap.get(columnName);
				if (column != null) {
					column.setValue2Entity(entity, cursor.getValue(i));
				} else {
					List<MyId> idList = table.getId();
					for (Iterator iterator = idList.iterator(); iterator
							.hasNext();) {
						MyId id = (MyId) iterator.next();
						if (columnName.equals(id.getColumnName())) {
							id.setValue2Entity(entity, cursor.getValue(i));
						}
					}

				}
			}
			return entity;
		} catch (Exception e) {
			LogUtils.e(e.getMessage(), e);
		}

		return null;
	}

}

package com.ieds.gis.base.dao;

import java.util.ArrayList;
import java.util.List;

class MyCursor {
	private static final int INIT_MOVE = -1;
	private List<String> columnList = new ArrayList<String>();
	private List<List<String>> valueList = new ArrayList<List<String>>();
	private int currentMove = INIT_MOVE;
	private List<String> currentValueList = new ArrayList<String>();

	public boolean moveToNext() {
		if (valueList.size() <= (currentMove + 1)) {
			return false; // 移动到头了，没有新的数据
		} else {
			currentMove = currentMove + 1;
			currentValueList = valueList.get(currentMove);
			return true;
		}
	}

	public String getValue(int count) {
		return currentValueList.get(count);
	}

	public List<String> getColumnList() {
		return columnList;
	}

	public List<List<String>> getValueList() {
		return valueList;
	}

	public String getColumnName(int number) {
		return columnList.get(number);
	}

	public int getColumnCount() {
		return columnList.size();
	}

}

package com.ieds.gis.base.test.dao;

import java.io.File;
import java.util.List;

import com.ieds.gis.base.dao.ISelector;
import com.ieds.gis.base.dao.SpatialSQLiteDatabase;
import com.ieds.gis.base.dao.SpatialiteDbUtils;
import com.lidroid.xutils.db.sqlite.SqlInfo;
import com.lidroid.xutils.exception.DbException;
import com.lidroid.xutils.util.FileUtil;

public class SqliteDAO extends SpatialiteDbUtils {

	private static final int DATABASE_VERSION = 1;

	public static final String CLOSE_ERROR = "关闭数据库连接失败！";
	public static final String OPEN_ERROR = "连接数据库失败！";

	@Override
	public void onCreate(SpatialSQLiteDatabase db) {
		// TODO Auto-generated method stub
		
	}

	public static synchronized void close() throws DbException {
		try {
			if (instance != null) {
				instance.getDatabase().close();
				instance = null;
			}
		} catch (Exception e) {
			// TODO: handle exception
			throw new DbException(CLOSE_ERROR);
		}
	}

	@Override
	public void onUpgrade(SpatialSQLiteDatabase  db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		for (int i = oldVersion; i < newVersion; i++) {
			switch (i) {
			case 2:
				System.out.println(i);
				break;
			case 3:
				System.out.println(i);
				break;
			default:
				break;
			}
		}
		System.out.println(oldVersion + " " + newVersion);

	}

	private static SqliteDAO instance = null;

	public static synchronized SqliteDAO getInstance() throws DbException {
		try {
			if (instance == null) {
				instance = new SqliteDAO(new File(FileUtil.getSdcardPath()
						+ "/demo.sqlite"), DATABASE_VERSION);
			}
			return instance;
		} catch (Exception e) {
			// TODO: handle exception
			throw new DbException(OPEN_ERROR);
		}
	}

	public SqliteDAO(File dbFile, int mNewVersion) throws Exception {
		super(dbFile, mNewVersion);
		// TODO Auto-generated constructor stub
	}

}

package com.urun.camera_test.Data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;


public class DatabaseHelper extends SQLiteAssetHelper{

	private static final String DB_NAME = "pictureDB";
	private static final int DB_VERSION = 1;
	public static final String TABLE_BYTE = "byteArray";
	
	public DatabaseHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}


	public void insertPictureData(int id, byte[] rawData){
		SQLiteDatabase db = this.getWritableDatabase();
		db.rawQuery("INSERT INTO "+TABLE_BYTE+"(id,pictureData) values("+id+","+rawData+")",null);
	}
//	public Cursor getUser(String id){
//		SQLiteDatabase db = this.getReadableDatabase();
//		Cursor res = db.rawQuery("select * from "+TABLE_CUSTOMER+" where customerID like '"+id+"'", null);
//		return res;
//	}
//	public Cursor getCartData(){
//		SQLiteDatabase db = this.getWritableDatabase();
//		Cursor res = db.rawQuery("select * from "+TABLE_CART,null);
//		return res;
//	}
//	public Cursor getCustumerData(){
//		SQLiteDatabase db = this.getWritableDatabase();
//		Cursor res = db.rawQuery("select * from "+TABLE_CUSTOMER,null);
//		return res;
//	}
}

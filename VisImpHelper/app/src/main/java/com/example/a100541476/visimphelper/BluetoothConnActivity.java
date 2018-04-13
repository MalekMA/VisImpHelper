package com.example.a100541476.visimphelper;


import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;

/**
 * Created by 100541476 on 2/9/2018.
 */

public class BluetoothConnActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{
    String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
    String NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        //textView = (TextView) findViewById(R.id.out);
        getSupportLoaderManager().initLoader(1, null, this);
    }
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        Uri CONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        return new CursorLoader(this, CONTENT_URI, null,null, null, null);
    }
    public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
        StringBuilder sb = new StringBuilder();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Log.d("Name", cursor.getString(cursor.getColumnIndex(DISPLAY_NAME)));
            Log.d("Number", cursor.getString(cursor.getColumnIndex(NUMBER)));

            sb.append("\n" + cursor.getString(cursor.getColumnIndex(DISPLAY_NAME)));
            sb.append(":" + cursor.getString(cursor.getColumnIndex(NUMBER)));
            cursor.moveToNext();
        }
        //textView.setText(sb);
    }
    public void onLoaderReset(Loader<Cursor> loader) {
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

}



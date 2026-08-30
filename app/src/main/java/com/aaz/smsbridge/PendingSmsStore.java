package com.aaz.smsbridge;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class PendingSmsStore extends SQLiteOpenHelper {
  private static final String DB="aaz_sms_queue.db";
  private static final String KEY_ALIAS="aaz_sms_queue_key_v1";
  private static final String TABLE="pending_sms";

  static final class PendingSms {
    final String id;
    final String sender;
    final String body;
    final long receivedAt;
    PendingSms(String id,String sender,String body,long receivedAt){ this.id=id; this.sender=sender; this.body=body; this.receivedAt=receivedAt; }
  }

  PendingSmsStore(Context context){ super(context.getApplicationContext(),DB,null,1); }
  @Override public void onCreate(SQLiteDatabase db){
    db.execSQL("CREATE TABLE "+TABLE+" (id TEXT PRIMARY KEY, sender TEXT NOT NULL, received_at INTEGER NOT NULL, ciphertext BLOB NOT NULL, iv BLOB NOT NULL, created_at INTEGER NOT NULL)");
  }
  @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){}

  boolean put(String id,String sender,String body,long receivedAt) throws Exception {
    Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE,key());
    ContentValues values=new ContentValues();
    values.put("id",id);
    values.put("sender",sender);
    values.put("received_at",receivedAt);
    values.put("ciphertext",cipher.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    values.put("iv",cipher.getIV());
    values.put("created_at",System.currentTimeMillis());
    return getWritableDatabase().insertWithOnConflict(TABLE,null,values,SQLiteDatabase.CONFLICT_IGNORE)!=-1;
  }

  PendingSms get(String id) throws Exception {
    try(Cursor cursor=getReadableDatabase().query(TABLE,new String[]{"sender","received_at","ciphertext","iv"},"id=?",new String[]{id},null,null,null)){
      if(!cursor.moveToFirst()) return null;
      Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,cursor.getBlob(3)));
      String body=new String(cipher.doFinal(cursor.getBlob(2)),StandardCharsets.UTF_8);
      return new PendingSms(id,cursor.getString(0),body,cursor.getLong(1));
    }
  }

  void delete(String id){ getWritableDatabase().delete(TABLE,"id=?",new String[]{id}); }

  private static synchronized SecretKey key() throws Exception {
    KeyStore store=KeyStore.getInstance("AndroidKeyStore");
    store.load(null);
    if(store.containsAlias(KEY_ALIAS)) return (SecretKey)store.getKey(KEY_ALIAS,null);
    KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
    generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
    return generator.generateKey();
  }
}

package com.aaz.smsbridge;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
  private static final int RECEIVE_SMS_REQUEST=100;
  private static final String[] ACTION_LABELS={"Block entire message","Remove keyword only","Remove sentence","Remove line","Remove range"};
  private static final String[] ACTION_VALUES={"block","remove_keyword","remove_sentence","remove_line","remove_range"};
  EditText baseUrl,secret,deviceId,blockKeywords,removeKeywords,removeSentenceKeywords,removeLineKeywords;
  CheckBox enabled,smartFilter;
  TextView status,senderRuleSummary,keywordForwardSummary;
  String senderRulesRaw,keywordForwardRaw;
  AlertDialog ruleManagerDialog;

  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    setContentView(R.layout.activity_main);
    baseUrl=findViewById(R.id.endpoint);
    secret=findViewById(R.id.secret);
    deviceId=findViewById(R.id.deviceId);
    blockKeywords=findViewById(R.id.blockKeywords);
    removeKeywords=findViewById(R.id.removeKeywords);
    removeSentenceKeywords=findViewById(R.id.removeSentenceKeywords);
    removeLineKeywords=findViewById(R.id.removeLineKeywords);
    enabled=findViewById(R.id.enabled);
    smartFilter=findViewById(R.id.smartFilter);
    status=findViewById(R.id.status);
    senderRuleSummary=findViewById(R.id.senderRuleSummary);
    keywordForwardSummary=findViewById(R.id.keywordForwardSummary);
    baseUrl.setText(Prefs.baseApiUrl(this));
    secret.setText(Prefs.secret(this));
    deviceId.setText(Prefs.deviceId(this));
    blockKeywords.setText(Prefs.blockMessageKeywords(this));
    removeKeywords.setText(Prefs.removeKeywords(this));
    removeSentenceKeywords.setText(Prefs.removeSentenceKeywords(this));
    removeLineKeywords.setText(Prefs.removeLineKeywords(this));
    senderRulesRaw=Prefs.senderRules(this);
    keywordForwardRaw=Prefs.keywordForwardRules(this);
    enabled.setChecked(Prefs.enabled(this));
    smartFilter.setChecked(Prefs.smartFilterEnabled(this));
    updateRuleSummary();
    updateKeywordForwardSummary();
    findViewById(R.id.manageSenderRules).setOnClickListener(v -> showRuleManager());
    findViewById(R.id.manageKeywordForward).setOnClickListener(v -> showKeywordForwardManager());
    findViewById(R.id.viewDeliveryLog).setOnClickListener(v -> showDeliveryLog());
    findViewById(R.id.save).setOnClickListener(v -> save());
    findViewById(R.id.test).setOnClickListener(v -> runV15Test());
    requestReceivePermission();
  }

  void save(){
    Prefs.save(this,baseUrl.getText().toString(),secret.getText().toString(),deviceId.getText().toString(),enabled.isChecked(),smartFilter.isChecked(),blockKeywords.getText().toString(),removeKeywords.getText().toString(),removeSentenceKeywords.getText().toString(),removeLineKeywords.getText().toString(),senderRulesRaw,keywordForwardRaw);
    baseUrl.setText(Prefs.baseApiUrl(this));
    status.setText(enabled.isChecked()?"Forwarding enabled":"Forwarding disabled");
  }

  private void updateRuleSummary(){
    int count=SenderRules.parse(senderRulesRaw).size();
    senderRuleSummary.setText(count==0?"No sender-specific rules.":count+" sender-specific rule(s). Tap Manage to edit.");
  }

  private void updateKeywordForwardSummary(){
    int count=KeywordForwardRules.parse(keywordForwardRaw).size();
    keywordForwardSummary.setText(count==0?"No forwarding keywords.":count+" forwarding keyword(s). Tap to edit.");
  }

  private void showKeywordForwardManager(){
    EditText input=new EditText(this);
    input.setHint("Keywords separated by comma or new line");
    input.setText(keywordForwardRaw);
    input.setMinLines(4);
    input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    int padding=dp(20);
    LinearLayout holder=new LinearLayout(this);
    holder.setPadding(padding,dp(4),padding,0);
    holder.addView(input,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
    AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Keyword Based Forward")
        .setMessage("If any keyword matches the SMS body, it can forward regardless of sender. Original sender is preserved.")
        .setView(holder).setPositiveButton("SAVE",null).setNegativeButton("CANCEL",null).create();
    dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
      keywordForwardRaw=input.getText().toString().trim();
      updateKeywordForwardSummary();
      dialog.dismiss();
      Toast.makeText(this,"Keywords updated. Tap SAVE SETTINGS to apply.",Toast.LENGTH_SHORT).show();
    }));
    dialog.show();
  }

  private void showDeliveryLog(){
    List<String> entries=DeliveryLog.entries(this);
    String message=entries.isEmpty()?"No delivery events yet.":android.text.TextUtils.join("\n\n",entries);
    new AlertDialog.Builder(this).setTitle("Delivery log — latest 20").setMessage(message)
        .setPositiveButton("CLOSE",null).setNegativeButton("CLEAR",(dialog,which) -> DeliveryLog.clear(this)).show();
  }

  private void showRuleManager(){
    if(ruleManagerDialog!=null&&ruleManagerDialog.isShowing()) ruleManagerDialog.dismiss();
    List<SenderRules.Rule> rules=SenderRules.parse(senderRulesRaw);
    LinearLayout list=new LinearLayout(this);
    list.setOrientation(LinearLayout.VERTICAL);
    list.setPadding(dp(16),dp(8),dp(16),dp(8));
    if(rules.isEmpty()){
      TextView empty=new TextView(this);
      empty.setText("No rules yet. Add one for a discovered or manually entered sender.");
      empty.setPadding(0,dp(12),0,dp(12));
      list.addView(empty);
    }
    for(int i=0;i<rules.size();i++){
      final int index=i;
      SenderRules.Rule rule=rules.get(i);
      LinearLayout row=new LinearLayout(this);
      row.setOrientation(LinearLayout.HORIZONTAL);
      TextView text=new TextView(this);
      text.setText(rule.display());
      text.setPadding(0,dp(12),dp(8),dp(12));
      text.setOnClickListener(v -> showRuleEditor(index));
      row.addView(text,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));
      Button delete=new Button(this);
      delete.setText("DELETE");
      delete.setOnClickListener(v -> {
        List<SenderRules.Rule> updated=SenderRules.parse(senderRulesRaw);
        if(index<updated.size()) updated.remove(index);
        senderRulesRaw=SenderRules.serialize(updated);
        updateRuleSummary();
        showRuleManager();
      });
      row.addView(delete);
      list.addView(row);
    }
    ScrollView scroll=new ScrollView(this);
    scroll.addView(list);
    ruleManagerDialog=new AlertDialog.Builder(this).setTitle("Sender-specific rules").setView(scroll)
        .setPositiveButton("ADD RULE",(dialog,which) -> showRuleEditor(-1))
        .setNegativeButton("DONE",null).create();
    ruleManagerDialog.show();
  }

  private void showRuleEditor(int editIndex){
    List<SenderRules.Rule> existing=SenderRules.parse(senderRulesRaw);
    SenderRules.Rule editing=editIndex>=0&&editIndex<existing.size()?existing.get(editIndex):null;
    LinearLayout form=new LinearLayout(this);
    form.setOrientation(LinearLayout.VERTICAL);
    form.setPadding(dp(20),dp(8),dp(20),0);

    List<String> choices=new ArrayList<>();
    choices.add("Select discovered sender");
    choices.addAll(Prefs.knownSenders(this));
    Spinner senderSpinner=new Spinner(this);
    senderSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,choices));
    form.addView(senderSpinner);
    EditText senderInput=new EditText(this);
    senderInput.setHint("Sender name/number (or enter manually)");
    if(editing!=null) senderInput.setText(editing.sender);
    form.addView(senderInput);
    senderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
      public void onItemSelected(AdapterView<?> parent,View view,int position,long id){ if(position>0) senderInput.setText(choices.get(position)); }
      public void onNothingSelected(AdapterView<?> parent){}
    });

    Spinner actionSpinner=new Spinner(this);
    actionSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,ACTION_LABELS));
    if(editing!=null) actionSpinner.setSelection(actionIndex(editing.action));
    form.addView(actionSpinner);
    EditText valueInput=new EditText(this);
    valueInput.setHint("Keyword(s), comma separated");
    EditText endInput=new EditText(this);
    endInput.setHint("End marker, e.g. TxnId:");
    if(editing!=null&&"remove_range".equals(editing.action)){
      String[] range=editing.value.split("=>",2);
      valueInput.setText(range[0].trim());
      if(range.length>1) endInput.setText(range[1].trim());
    } else if(editing!=null) valueInput.setText(editing.value);
    form.addView(valueInput);
    form.addView(endInput);
    Runnable updateRangeUi=() -> {
      boolean range=actionSpinner.getSelectedItemPosition()==4;
      valueInput.setHint(range?"Start marker, e.g. Your A/C Balance:":"Keyword(s), comma separated");
      endInput.setVisibility(range?View.VISIBLE:View.GONE);
    };
    actionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
      public void onItemSelected(AdapterView<?> parent,View view,int position,long id){ updateRangeUi.run(); }
      public void onNothingSelected(AdapterView<?> parent){}
    });
    updateRangeUi.run();

    AlertDialog dialog=new AlertDialog.Builder(this).setTitle(editing==null?"Add sender rule":"Edit sender rule")
        .setView(form).setPositiveButton("SAVE",null).setNegativeButton("CANCEL",null).create();
    dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
      String sender=senderInput.getText().toString().trim();
      String start=valueInput.getText().toString().trim();
      boolean range=actionSpinner.getSelectedItemPosition()==4;
      String end=endInput.getText().toString().trim();
      if(sender.isEmpty()||start.isEmpty()||(range&&end.isEmpty())||sender.contains("|")){
        Toast.makeText(this,"Enter a sender and the required keyword/markers.",Toast.LENGTH_SHORT).show();
        return;
      }
      String value=range?start+" => "+end:start;
      SenderRules.Rule rule=new SenderRules.Rule(sender,ACTION_VALUES[actionSpinner.getSelectedItemPosition()],value);
      List<SenderRules.Rule> updated=SenderRules.parse(senderRulesRaw);
      if(editIndex>=0&&editIndex<updated.size()) updated.set(editIndex,rule); else updated.add(rule);
      senderRulesRaw=SenderRules.serialize(updated);
      updateRuleSummary();
      dialog.dismiss();
      showRuleManager();
    }));
    dialog.show();
  }

  private static int actionIndex(String action){
    for(int i=0;i<ACTION_VALUES.length;i++) if(ACTION_VALUES[i].equals(action)) return i;
    return 0;
  }
  private int dp(int value){ return Math.round(value*getResources().getDisplayMetrics().density); }

  private void runV15Test(){
    save();
    status.setText("Syncing test sender…");
    new Thread(() -> {
      try {
        BridgeClient.SenderSyncResult sync=BridgeClient.syncSender(this,"AAZ-TEST");
        if(!sync.routed){ runOnUiThread(() -> status.setText("Sender synced successfully but AAZ-TEST is not routed yet.")); return; }
        long receivedAt=System.currentTimeMillis();
        String body="AAZ SMS Bridge v1.5 test message";
        BridgeClient.sendInbox(this,"AAZ-TEST",body,receivedAt,BridgeClient.smsId("AAZ-TEST",body,receivedAt));
        runOnUiThread(() -> status.setText("Sender synced and test SMS forwarded successfully."));
      } catch(Exception e){ String message=statusFor(e); runOnUiThread(() -> status.setText(message)); }
    },"aaz-v15-test").start();
  }

  private static String statusFor(Exception error){
    if(error instanceof BridgeClient.HttpStatusException){
      int code=((BridgeClient.HttpStatusException)error).statusCode;
      if(code==401||code==403) return "Authentication failed.";
      if(code>=500) return "Server unavailable.";
    }
    if(error instanceof IOException&&"Invalid server response".equals(error.getMessage())) return "Invalid server response.";
    return "Connection failed. Check the server URL and network.";
  }
  private void requestReceivePermission(){
    if(checkSelfPermission(Manifest.permission.RECEIVE_SMS)!=PackageManager.PERMISSION_GRANTED)
      requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS},RECEIVE_SMS_REQUEST);
  }
  @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){
    super.onRequestPermissionsResult(requestCode,permissions,results);
    if(requestCode==RECEIVE_SMS_REQUEST&&(results.length==0||results[0]!=PackageManager.PERMISSION_GRANTED))
      status.setText("SMS receive permission denied. Forwarding cannot receive messages.");
  }
}

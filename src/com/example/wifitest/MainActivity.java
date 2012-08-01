package com.example.wifitest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.util.ArrayList;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.speech.RecognizerIntent;
import android.support.v4.app.NavUtils;

public class MainActivity extends Activity implements Runnable{
	
	Button speak,nextWindow,nextField,paste;
	TextView spokentext;
	
	public static final int TAB_COMMAND=0x81;
	
	public static final int NEXT_WINDOW_COMMAND=0x82;
	
	public static final int PORTNUMBER=28195;
	
	Socket client;
	
	MulticastSocket multisock;
	
	InetAddress serveraddress;
	
	WifiManager wifiman;

	Intent speechintent;
	
	public static final String echomessage="TeclaShield";
	
	public static final String password="android";
	
	boolean connectionstatus=false;
	
	BufferedInputStream in;
	BufferedOutputStream out;
	InputStreamReader isr;
	OutputStreamWriter osw;
	
	Object lock;
	
	String Dictation;
	
	public static final int SPEECH_REQUEST_INTENT=1000;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        lock=new Object();
        
        //Initialize components
        speak=(Button)findViewById(R.id.speakbutton);
        nextField=(Button)findViewById(R.id.nextbutton);
        nextWindow=(Button)findViewById(R.id.switchbutton);
        paste=(Button)findViewById(R.id.pastebutton);
        
        spokentext=(TextView)findViewById(R.id.SpokenText);
        
        wifiman=(WifiManager)this.getSystemService(Context.WIFI_SERVICE);
        
        Dictation="";
        
        if(wifiman.isWifiEnabled()){
        	search_server();
        }else{
        	spokentext.setText("Wifi is Disabled. Switch on Wifi first");
        }
        
        speak.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				callGoogleVoiceActions();
				
			}
		});
        paste.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				if(connectionstatus)
					send("dictate:"+Dictation);
				
			}
		});
        nextWindow.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				if(connectionstatus)
				send("command:"+NEXT_WINDOW_COMMAND);
				
			}
		});
        nextField.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				if(connectionstatus)
					send("command:"+TAB_COMMAND);
			}
		});
        
        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    public void connect(){
    	DatagramPacket packet;
    	byte[] buf=new byte[256];
    	try {
    		packet=new DatagramPacket(buf,buf.length);
    		
			multisock=new MulticastSocket(PORTNUMBER);
			
			multisock.joinGroup(InetAddress.getByName("225.0.0.0"));
			
			multisock.receive(packet);
			
			if(packet!=null)
				serveraddress=packet.getAddress();
			
			buf=echomessage.getBytes();
			
			packet=new DatagramPacket(buf,buf.length); 
			
			multisock.send(packet);
			
			multisock.setSoTimeout(60000);
			
			multisock.receive(packet);
			
			if(packet.equals("Yes")){
				
				multisock.close();
				
				client=new Socket(serveraddress,PORTNUMBER);
				
				client.setSoTimeout(60000);
				
				in=new BufferedInputStream(client.getInputStream());
				
				out=new BufferedOutputStream(client.getOutputStream());
				
				isr=new InputStreamReader(in,"UTF8");
				
				osw=new OutputStreamWriter(out,"UTF8");
				
				send(password);
				
				String result=receive();
				
				if(result!=null && result.equals("Success")){
					connectionstatus=true;
				}
				
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			disconnect();
		}
    }
    
    
    public void send(String data){
    	try{
    		
    		if(out != null &&osw != null )
    			osw.write(data);
    		
    	}catch (IOException e){
    		
    	}
    }
    
    public String receive(){
    	StringBuilder builder=new StringBuilder();
    	int val;
    	try{
    		if(isr !=null && in != null){
    			val=isr.read();
    			while(val != -1){
    				builder.append(val);
    				val=isr.read();
    			}
    		}
    	  		
    	}catch (IOException e){
    		
    	}
    	return builder.toString();
    }
    
    public boolean connectionstatus(){
    	if(client != null){
    		return client.isConnected();
    	}
    	return false;
    }
    
    public void disconnect(){
    		try {
    			if(in != null)
    				in.close();				
    			if(out != null)
    				out.close();
    			if(isr!= null)
    				isr.close();
    			if(osw!= null)
    				osw.close();
    			if(client!=null)
    				client.close();
    			connectionstatus=false;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	
    	
    }
    
    
    public void search_server(){
    	new Thread(this).start();
    }
    
	public void run() {
		// TODO Auto-generated method stub
		connect();
		
	}
	
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode==SPEECH_REQUEST_INTENT){
			
			if(resultCode ==RESULT_OK){
				
				ArrayList<String>list=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
				
				if(list.size()>0){
					
					createDialog(list);
					
				}
				
				
			}
			
		}
		
		
	}
	
	public void callGoogleVoiceActions(){
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 100);
        startActivityForResult(intent, SPEECH_REQUEST_INTENT);
	}
	
	public String createDialog(final ArrayList<String> list){
		String dictated=null;
		
		final Dialog chooserdialog=new Dialog(this);
		
		chooserdialog.setContentView(R.layout.dialog);
		
		ListView lv=(ListView) chooserdialog.findViewById(R.id.resultlist);
		
		ArrayAdapter<String> listsadapter=new ArrayAdapter<String>(chooserdialog.getContext(),
				android.R.layout.simple_list_item_1,list);
		lv.setAdapter(listsadapter);
		listsadapter.notifyDataSetChanged();
		
		lv.setOnItemClickListener(new OnItemClickListener(){

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				Dictation=list.get((int)arg3);
				spokentext.setText(Dictation);
				chooserdialog.hide();
			}
			
		});
		
		chooserdialog.show();
		return dictated;
	}
}

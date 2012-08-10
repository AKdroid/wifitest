package com.example.wifitest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.util.ArrayList;

import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
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
	TextView spokentext,statustext;
	
	public static final int TAB_COMMAND=0x81;
	
	public static final int NEXT_WINDOW_COMMAND=0x82;
	
	public static final int PORTNUMBER=28195;
	
	Socket client;
	
	MulticastSocket multisock;
	
	InetAddress serveraddress;
	
	WifiManager wifiman;

	Intent speechintent;
	
	boolean locker;
	
	public static final String echomessage="TeclaShield";
	
	public static final String password="android";
	
	boolean connectionstatus=false,flag=true;
	
	ObjectInputStream in;
	ObjectOutputStream out;
	
	DatagramPacket pack;
	
	byte[] buffer;
	
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
        statustext=(TextView)findViewById(R.id.statustext);
        
        wifiman=(WifiManager)this.getSystemService(Context.WIFI_SERVICE);
        
        Dictation="";
        
        if(wifiman.isWifiEnabled()){
        	new Connect().execute(0);
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
				else
					search_server();
				
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
    	Log.v("connection","Started");
    	locker=false;
    	Thread p=new Thread(this);
    	p.start();
    	try{
    		while(!locker);
    		
    		Log.v("connection","pack in connect="+pack);
    		
			if(pack!=null){
				Log.v("connection",new String(pack.getData()));
				serveraddress=pack.getAddress();
				Log.v("connection",""+serveraddress);
			}
			buf=echomessage.getBytes();
			
			//multisock.leaveGroup(InetAddress.getByName("225.0.0.0"));
			
			MulticastSocket multisocksend=new MulticastSocket(PORTNUMBER+1);
			
			packet=new DatagramPacket(buf,buf.length,InetAddress.getByName("226.0.0.0"),PORTNUMBER+1); 
			
			multisocksend.setSoTimeout(30000);
			
			for(int i=0;i<8;i++)
			{
			multisocksend.send(packet);
			}
			
			Log.v("connection","reached here");
			
			
			
			flag=false;
			
			
			
				client=new Socket();
							
				client.connect(new InetSocketAddress(serveraddress,PORTNUMBER+2),60000);
				
				Log.v("connection","reached here2");
				
				out=new ObjectOutputStream(client.getOutputStream());
				out.flush();
				
				
				send(password);
				
				
				in=new ObjectInputStream(client.getInputStream());
				
				Log.v("connection",""+out);
				
				String result=receive();
				
				Log.v("connection",""+result);
				if(result!=null && result.equals("Success")){
					Log.v("connection","accepted");
					connectionstatus=true;
				}
				
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			disconnect();
		}
    }
    
    
    public void send(String data){
    	try{
    		
    		if(out != null)
    		{
    			Log.v("connection","password="+data);
    			out.writeUTF(data);
    			out.flush();
    			
    		}
    	}catch (IOException e){
    		e.printStackTrace();
    	}
    }
    
    public String receive(){
    	
    	try{
    		String data=in.readUTF();
    		Log.v("conenction","receiving"+data);
    		return data;
    	  		
    	}catch (IOException e){
    		e.printStackTrace();
    		return null;
    	}
    	
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
			DatagramPacket packet;
			buffer=new byte[256];
			int count=0;
			pack =new DatagramPacket(buffer,buffer.length);
			flag=true;
			
		try {
			
			
			multisock=new MulticastSocket(PORTNUMBER);
			
			multisock.setSoTimeout(0);			
			
			multisock.joinGroup(InetAddress.getByName("225.0.0.0"));
			
			buffer=new byte[256];
			
			packet=new DatagramPacket(buffer,buffer.length);
			
			
			
			Log.v("connection","pack="+pack);
			
			multisock.receive(packet);
			
			pack=new DatagramPacket(packet.getData(),packet.getData().length);
			pack.setAddress(packet.getAddress());
			
			Log.v("connection","pack="+new String(pack.getData()));
			
			locker=true;
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		while(flag){ 
			
			try {
				buffer=new byte[256];
				
				packet=new DatagramPacket(buffer,buffer.length);
				
				multisock.receive(packet);
				
				pack=new DatagramPacket(packet.getData(),packet.getData().length);
				
				pack.setAddress(packet.getAddress());
				count++;
				
				Log.v("connection","count="+count);
				
			} catch (IOException e) { 
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
		
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
				send("dictate:"+Dictation);
				chooserdialog.hide();
			}
			
		});
		
		chooserdialog.show();
		return dictated;
	}
	
	class Connect extends AsyncTask{

		@Override
		protected Object doInBackground(Object... arg0) {
			// TODO Auto-generated method stub
			connect();
			return arg0[0];
		}
		
		protected void onProgressUpdate(String text) {
	         
	     }
		
		protected void onPostExecute(Object arg) {
			if(serveraddress!=null)
	         statustext.setText(serveraddress.toString());
	     }
		
		
	};
	
	
	
	
}

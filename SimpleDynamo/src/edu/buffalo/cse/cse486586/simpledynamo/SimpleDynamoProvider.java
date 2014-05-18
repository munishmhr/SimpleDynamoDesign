package edu.buffalo.cse.cse486586.simpledynamo;
/*
 * 	Submition copy
 * 
 */
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDynamoProvider extends ContentProvider {
	static String TAG = "SimpleDynamoProvider";
	private HashMap<String,String> keyValue;	
	private HashMap<String,String> versionkeyValue;	
	ArrayList<String> mNodeList;
	Context context;
	ContentResolver mContentResolver;
	String currentNode;
	static private String key_e="key";
	static private String value_e="value";
	static String resultRow[]={key_e,value_e};
	static MatrixCursor mt = new MatrixCursor(resultRow);
	MatrixCursor localCur;
	int mutex=0;
	int mutexInsert = 0;
	int mutexForwardInsert=0;
	static String mutexQuery = null;
	String Originator;
	boolean cursorIsEmpty = false;
	String pred = "";
	String succ = "";
	String sP[]= null;
	Message operation;
	Uri mUri;


	String[] predAvailNode(String currenrNode){
		int index = mNodeList.indexOf(currenrNode);
		String predId = null;
		String succId = null;
		String[] s = new String[2];
		if(index == 0) {
			succId = mNodeList.get(index+1);
			predId = mNodeList.get(mNodeList.size()-1);
		} else if(index == (mNodeList.size()-1) ) {
			succId = mNodeList.get(0);
			predId = mNodeList.get(index-1);
		} else {
			succId = mNodeList.get(index+1);
			predId = mNodeList.get(index-1);
		}
		s[0] = succId;
		s[1] = predId;
		return s;
	}

	void insertKeyValue(String key, String value2) throws IOException {
		FileOutputStream fos = context.openFileOutput(key,Context.MODE_PRIVATE);
		OutputStreamWriter osw = new OutputStreamWriter(fos);
		osw.write(value2);
		osw.flush();
		osw.close();
	}

	String getNodeForSingleKeyQuery(String key) throws NoSuchAlgorithmException{
		String node="";
		if(genHash(key).compareTo(genHash("5556"))<0 && genHash(key).compareTo(genHash("5562"))>0 ){
			node = "5556";
		}
		else if(genHash(key).compareTo(genHash("5554"))<0 && genHash(key).compareTo(genHash("5556"))>0 ){
			node = "5554";
		}
		else if(genHash(key).compareTo(genHash("5558"))<0 && genHash(key).compareTo(genHash("5554"))>0 ){
			node = "5558";
		}
		else if(genHash(key).compareTo(genHash("5560"))<0 && genHash(key).compareTo(genHash("5558"))>0 ){
			node = "5560";
		}
		else{
			node = "5562";
		}
		return node;
	}

	void replicateKeyValue(Message operation){
		operation.setType("replicate");
		String[] sP = predAvailNode(operation.getSucc());	
		String succ1 = sP[0];
		operation.setSucc(succ1);
//		Log.d(TAG,"Replicate() Calling client currentNode "+currentNode+" Succ is :"+operation.getSucc()+" key: "
//				+operation.getKey()+" values: " + operation.getValue());
		new Thread(new Client(operation)).start(); 
		sP = predAvailNode(operation.getSucc());	
		succ1 = sP[0];
		operation.setSucc(succ1);
//		Log.d(TAG,"Replicate() Calling client currentNode "+currentNode+" Succ is :"+operation.getSucc()+" key: "
//				+operation.getKey()+" values: " + operation.getValue());
		new Thread(new Client(operation)).start();
	}

	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
		init();
		currentNode = getPort();
		context = getContext();
		mContentResolver = context.getContentResolver();
		sP = predAvailNode(currentNode);
		mUri = buildUri("content","edu.buffalo.cse.cse486586.simpledynamo.provider");	
		pred = sP[1];
		succ = sP[0];
		Log.d(TAG, "onCreate() :: init() and predAvailNode(currentNode) called");
		Thread server = new Server();
		server.start();
		versionkeyValue = new HashMap<String,String>();
		delete(mUri, "@", null);
		recovery();
		return false;
	}

	void recoveryInsert(HashMap<String,String> localRecoveryUpdate) throws NoSuchAlgorithmException, IOException{
		String expectedNodes="";
		sP = predAvailNode(currentNode);
		String pred = sP[1];
		sP = predAvailNode(pred);
		String predPred = sP[1];
		Iterator<Entry<String, String>> it = localRecoveryUpdate.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, String> pairs = it.next();
			String key = (String)pairs.getKey() ;
			String value = (String) pairs.getValue() ;
			expectedNodes = getNodeForSingleKeyQuery(key);
			//Log.d(TAG,"Checking expectedNodes : "+expectedNodes+" pred : "+pred + " predPred : "+predPred);
			if(expectedNodes.equals(currentNode) || expectedNodes.equals(pred) || expectedNodes.equals(predPred)) {
				insertKeyValue(key,value);
				//Log.d(TAG,"Required predPred key value : "+key +" value :"+value);
			}
		}
	}

	void recovery(){
		// TODO Auto-generated method stub
		keyValue = new HashMap<String,String>();
		operation = new Message();
		operation.setType("@query");
		operation.setOriginator(currentNode);
		/*
		 *  Sending recovery msg to all
		 */
		operation.setSucc(succ);
		new Thread(new Client(operation)).start();
		Log.d(TAG,"recoery() Sending to succ : "+operation.getSucc());

		sP = predAvailNode(succ);
		String succ4 = sP[0];
		operation.setSucc(succ4);
		new Thread(new Client(operation)).start();
		Log.d(TAG,"recoery() Sending to succ's succ : "+operation.getSucc());

		operation.setSucc(pred);
		new Thread(new Client(operation)).start();
		Log.d(TAG,"recoery() Sending to pred : "+operation.getSucc());

		sP = predAvailNode(operation.getSucc());
		String pred2 = sP[1];
		operation.setSucc(pred2);
		new Thread(new Client(operation)).start();
		Log.d(TAG,"recoery() Sending to pred's pred : "+operation.getSucc());
	}

	void deleteLocal(){
		String[] filesToDel = context.fileList();
		for (int i = 0; i < filesToDel.length; i++){
			context.deleteFile(filesToDel[i]);		
		}
	}

	@Override
	synchronized public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		operation = new Message();
		operation.setType("delete");
		operation.setKey(selection);
		operation.setOriginator(currentNode);
		operation.setSucc(succ);
		String node = "";
		Log.d(TAG,"delete() CurrentNode : "+ currentNode );
		if(selection.equals("@")){
			Log.d(TAG,"Delete() : @ selection : "+selection);
			deleteLocal();	
		}
		if(selection.equals("*")){
			if(cursorIsEmpty == false){
				//Log.d(TAG,"Delete() : * cursorIsEmpty : "+cursorIsEmpty +" calling delete for : "+succ);
				deleteLocal();
				new Thread(new Client(operation)).start();
				cursorIsEmpty = true;
			}	
		}
		if(selection.length() > 1){
			try {
				node  = getNodeForSingleKeyQuery(selection);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(node.equals(currentNode)){
				context.deleteFile(selection);	
			}else{

				operation.setType("delete@");

				operation.setSucc(node);
				new Thread(new Client(operation)).start();
				Log.d(TAG,"delete() Sending to nodes : "+operation.getSucc());

				sP = predAvailNode(node);
				String succ7 = sP[0];
				operation.setSucc(succ7);
				
				new Thread(new Client(operation)).start();
				Log.d(TAG,"delete() Sending to nodes succ : "+operation.getSucc());
				sP = predAvailNode(succ7);
				String succ8 = sP[0];
				operation.setSucc(succ8);
				
				new Thread(new Client(operation)).start();
				Log.d(TAG,"delete() Sending to nodes succ's succ : "+operation.getSucc());


			}
		}
		return 0;
	}

	@Override
	synchronized public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		String key = (String) values.get("key");
		String value = (String) values.get("value");
		operation = new Message();
		operation.setKey(key);
		operation.setValue(value);
		operation.setOriginator(currentNode);
		operation.setType("insert");
		operation.setSucc(succ);

//		Log.d(TAG,"Insert() :: Values set in operation are Key : "+operation.getKey()+", value: "+operation.getValue()
//				+", CurrentNode/Originator : "+operation.getOriginator()+", Type : "+operation.getType()
//				+", Succ : " +operation.getSucc() );
		try{
			if(genHash(key).compareTo(genHash("5556"))<0 && genHash(key).compareTo(genHash("5562"))>0 ){
				operation.setSucc("5556");
			}
			else if(genHash(key).compareTo(genHash("5554"))<0 && genHash(key).compareTo(genHash("5556"))>0 ){
				operation.setSucc("5554");
			}
			else if(genHash(key).compareTo(genHash("5558"))<0 && genHash(key).compareTo(genHash("5554"))>0 ){
				operation.setSucc("5558");
			}
			else if(genHash(key).compareTo(genHash("5560"))<0 && genHash(key).compareTo(genHash("5558"))>0 ){
				operation.setSucc("5560");
			}
			else{
				operation.setSucc("5562");
			}
			if(operation.getSucc().equals(currentNode)){
				Log.d(TAG,"Insert() and replicating current port no : "+getPort()+" Key: "+key+"value: "+value);
				insertKeyValue(key,value);	
				replicateKeyValue(operation);
			}else{
				Log.d(TAG, "Insert() Client forward Insert node : "+operation.getSucc() +" from currentNode : "+currentNode
						+" Key : "+key+" Value :"+value);
				new Thread(new Client(operation)).start();
				Log.d(TAG,"Insert() Replicating to succ's check succ port's, Key: "+operation.getKey()+"value: "+operation.getValue());
				replicateKeyValue(operation);

			}
		}catch(Exception e){
			Log.d(TAG,"Exception in Insert() :: "+ e);
		}

		return null;
	}

	class Server extends Thread{
		ServerSocket serverSocket=null;
		Socket soc = null;
		ObjectInputStream ois = null;
		public void run(){
			try{
				serverSocket = new ServerSocket(10000);
				while(true){
					soc = serverSocket.accept();
					ois = new ObjectInputStream(soc.getInputStream()); 
					Message operation = (Message)ois.readObject(); 
//					Log.d(TAG,"Server :: Values in operation are Key : "+operation.getKey()+", value: "+operation.getValue()
//							+", CurrentNode/Originator : "+operation.getOriginator()+", Type : "+operation.getType()
//							+", Succ : " +operation.getSucc() );

					if(operation.getType().equals("insert")){
						//synchronized(this){
						Log.d(TAG,"Server Insert port no : "+currentNode+" Key: "+operation.getKey()+"value: "+operation.getValue());
						insertKeyValue(operation.getKey(),operation.getValue());	
						//}
					}
					
					
					if(operation.getType().equals("replicate")){
						//synchronized(this){
						insertKeyValue(operation.getKey(),operation.getValue());
						Log.d(TAG,"Server replicated values at currentNode "+currentNode+" key: "+operation.getKey()+" " +
								"values: " + operation.getValue());
						//}
					}
					if(operation.getType().equals("*query")){
						if(!operation.getOriginator().equals(currentNode)){
							Log.d(TAG,"Server Class *query, node : "+currentNode+" calling retunnGDump");
							sP = predAvailNode(currentNode);
							String succ2 = sP[0];
							operation.setSucc(succ2);
							returnGDump(operation);
						}else{
							Log.d(TAG,"Server Class *query, Originator Node : "+currentNode +" returning cursor" );
							keyValue = operation.getKeyValue();
							Iterator<Entry<String, String>> it = keyValue.entrySet().iterator();
							while (it.hasNext()) {
								Entry<String, String> pairs = it.next();
								String[] row = { (String)pairs.getKey(), (String) pairs.getValue() };
								mt.addRow(row);
							}
							localCur =mt;
							mutex = 1;
						}
					}
					if(operation.getType().equals("@query")){
						Log.d(TAG,"Server @query currentNode :"+currentNode);
						HashMap<String,String > localRecovery = new HashMap<String,String>();
						localRecovery = returnLDumpHash();
						operation.setRecoveryKeyValue(localRecovery);
						operation.setSucc(operation.getOriginator());
						operation.setType("updateRecovery");
						operation.setComingFrom(currentNode);
						new Thread(new Client(operation)).start();

					}
					if(operation.getType().equals("updateRecovery")){
						if(!operation.getComingFrom().equals(currentNode)){
							Log.d(TAG,"Server updateRecovery :: response coming from : "+operation.getComingFrom()
									+" size of map is : "+operation.getRecoveryKeyValue().size());
							recoveryInsert(operation.getRecoveryKeyValue());	
						}
					}
					if(operation.getType().equals("singleKeyQuery")){
						String value="";
						Log.d(TAG, "Server singleKeyQuery key is "+operation.getKey()+" currentNode is "+currentNode+" Originator is "+operation.getOriginator());
						value = returnValueForKeyQuery(operation.getKey());
						operation.setSucc(operation.getOriginator());
						operation.setType("singleKeyQueryValue");
						
						if(value == null){
							while(value == null){
								Thread.sleep(200);
								value = returnValueForKeyQuery(operation.getKey());
								Log.d(TAG,"Server singleKeyQuery while loop key is "+operation.getKey()+" value :"+value);
							}
						}
						if(value != null){
							operation.setValue(value);
							operation.setComingFrom(currentNode);
							Log.d(TAG,"Server singleKeyQuery sending to Originator :: Values set in operation are Key : "+operation.getKey()+
									", value: "+operation.getValue()	+", CurrentNode/Originator : "+operation.getOriginator()+
									", Type : "+operation.getType() +", Succ : " +operation.getSucc() );
							new Thread(new Client(operation)).start();	
						}
					}
					if(operation.getType().equals("singleKeyQueryValue")){
						if(operation.getComingFrom()!=currentNode){
							Log.d(TAG,"Server singleKeyQueryValue coming form " +operation.getComingFrom() +" Current Node is :"
									+currentNode+" Value returned is :"+operation.getValue()+" for key :"+operation.getKey());
							Log.d(TAG, "Operation.getOriginator() : "+operation.getOriginator());
							mutexQuery = operation.getValue();
						}
					}
					if(operation.getType().equals("delete")){
						Log.d(TAG,"Server delete *");
						Uri mUri = buildUri("content","edu.buffalo.cse.cse486586.simpledynamo.provider");						
						delete(mUri,"*", null) ;
					}
					if(operation.getType().equals("delete@")){
						Log.d(TAG,"Server delete @");
						context.deleteFile(operation.getKey());
						Log.d(TAG,"Server delete@ from cuurentNode : "+currentNode +" key : "+operation.getKey());

					}
				}
			}catch(Exception e ){
				Log.d(TAG, "Exception Server : "+e);
				e.printStackTrace();
			}
		}
	}

	class Client extends Thread{
		ObjectOutputStream oos;
		Socket clientSock;
		String remotePort;
		int port1;
		Message operation = new Message();
		Client(Message operation){ 
			this.remotePort = operation.getSucc();
			this.operation = operation;
		}
		public void run(){
			try{
				Log.d(TAG,"Client sending to Succ Server :: Values set in operation are Key : "+operation.getKey()+
						", value: "+operation.getValue()	+", CurrentNode/Originator : "+operation.getOriginator()+
						", Type : "+operation.getType() +", Succ : " +operation.getSucc() );
				port1 = Integer.parseInt(remotePort)*2;
				remotePort = port1 + "";
				Log.d(TAG, "Client Remote port is "+remotePort);
				clientSock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),Integer.parseInt(remotePort.trim()));
				clientSock.setSoTimeout(500);
				oos = new ObjectOutputStream(clientSock.getOutputStream()); 
				oos.writeObject(operation);
				oos.close();
				clientSock.close();
			}catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SocketTimeoutException e) {
				
				// TODO Auto-generated catch block
				Log.d(TAG, "Client  Socket Time out type : "+operation.getType()+" key : "+operation.getKey()
						+" value : "+operation.getValue());
				if(operation.getType().equals("*query")){
					sP = predAvailNode(operation.getSucc());
					operation.setSucc(sP[0]);
					Log.d(TAG,"Client Remote Port : "+remotePort +" is down sending to "+operation.getSucc());
					new Thread(new Client(operation)).start();
				}
				if(operation.getType().equals("singleKeyQuery")){
					sP = predAvailNode(operation.getSucc());	
					operation.setSucc(sP[0]);
					Log.d(TAG,"Client Remote Port : "+remotePort +" is down sending to "+operation.getSucc());
					new Thread(new Client(operation)).start();
				}
				e.printStackTrace();
			}
			catch (StreamCorruptedException e) {
				Log.d(TAG, "Client  Socket Time out type : "+operation.getType()+" key : "+operation.getKey()
						+" value : "+operation.getValue());
				if(operation.getType().equals("*query")){
					sP = predAvailNode(operation.getSucc());	
					operation.setSucc(sP[0]);
					Log.d(TAG,"Client Remote Port : "+remotePort +" is down sending to "+operation.getSucc());
					new Thread(new Client(operation)).start();

				}
				if(operation.getType().equals("singleKeyQuery")){
					sP = predAvailNode(operation.getSucc());	
					operation.setSucc(sP[0]);
					Log.d(TAG,"Client Remote Port : "+remotePort +" is down sending to "+operation.getSucc());
					new Thread(new Client(operation)).start();
				}
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}catch(Exception e){
				e.printStackTrace();
				Log.d(TAG,"Cleint class exception : "+e);
			}
		}
	}

	MatrixCursor returnLDump(){
		MatrixCursor localCur = new MatrixCursor(resultRow);
		String[] noOfFiles = context.fileList();
		Log.d(TAG,"returnLDump() ::  noOfFiles :"+noOfFiles);
		for (int i = 0; i < noOfFiles.length; i++) {
			try {
				FileInputStream fin = context.openFileInput(noOfFiles[i]);
				InputStreamReader inpReader = new InputStreamReader(fin);
				BufferedReader br = new BufferedReader(inpReader);
				String value = br.readLine();
				String resultRow[] = { noOfFiles[i], value };
				localCur.addRow(resultRow);
				//Log.d(TAG,"returnLDump() :: Key : "+noOfFiles[i] +" value : "+value);
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		return localCur;
	}

	HashMap<String, String> returnLDumpHash(){
		HashMap<String,String> recoveryLocal = new HashMap<String,String>();
		String[] noOfFiles = context.fileList();
		for (int i = 0; i < noOfFiles.length; i++) {
			try {
				FileInputStream fin = context.openFileInput(noOfFiles[i]);
				InputStreamReader inpReader = new InputStreamReader(fin);
				BufferedReader br = new BufferedReader(inpReader);
				String value = br.readLine();
				recoveryLocal.put(noOfFiles[i], value);
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		return recoveryLocal;
	}

	MatrixCursor returnGDump(Message operation){
		localCur = new MatrixCursor(resultRow);
		keyValue = new HashMap<String, String>();
		String[] noOfFiles = context.fileList();
		if(operation.getOriginator().equals(currentNode)){
			for (int i = 0; i < noOfFiles.length; i++) {
				try {
					FileInputStream fin = context.openFileInput(noOfFiles[i]);
					InputStreamReader inpReader = new InputStreamReader(fin);
					BufferedReader br = new BufferedReader(inpReader);
					String value = br.readLine();
					//Log.d(TAG,"returnGDump() Key "+ noOfFiles[i]+" Value : "+value);
					keyValue.put(noOfFiles[i], value);
				}catch (Exception e) {
					e.printStackTrace();
				}
			}

			operation.setKeyValue(keyValue);
			new Thread(new Client(operation)).start();
			while(mutex==0){}
			mutex=1;
		}else if(!operation.getOriginator().equals(currentNode)){
			keyValue = new HashMap<String, String>();
			keyValue = operation.getKeyValue();
			for (int i = 0; i < noOfFiles.length; i++) {
				try {
					FileInputStream fin = context.openFileInput(noOfFiles[i]);
					InputStreamReader inpReader = new InputStreamReader(fin);
					BufferedReader br = new BufferedReader(inpReader);
					String value = br.readLine();
					//Log.d(TAG,"returnGDump() Key "+ noOfFiles[i]+" Value : "+value);
					keyValue.put(noOfFiles[i], value);			
				}catch (Exception e) {
					e.printStackTrace();
				}
			}
			operation.setKeyValue(keyValue);
			new Thread(new Client(operation)).start();
		}
		return localCur;
	}

	String returnValueForKeyQuery(String key) throws IOException{
		Log.d(TAG,"returnValueForKeyQuery() checking in Node CurrentNode: "+currentNode +" Key is : "+key);
		String value="";
		FileInputStream fin;
		String[] filesToDel = context.fileList();
		for (int i = 0; i < filesToDel.length; i++){
			if(key.equals(filesToDel[i])){
				fin = context.openFileInput(key);
				InputStreamReader inpReader = new InputStreamReader(fin);
				BufferedReader br = new BufferedReader(inpReader);
				value = br.readLine();
				br.close();
				Log.d(TAG,"returnValueForKeyQuery() :: found key "+key +" value is : "+ value +" in Node : "+currentNode); 
				return value;
			}
		}
		Log.d(TAG,"returnValueForKeyQuery() Not found in Node CurrentNode: "+currentNode +" Key is : "+key);
		return null;
	}

	@Override
	synchronized public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		Log.d(TAG, "Query selection : "+selection);

		operation = new Message();
		operation.setKey(selection);
		operation.setOriginator(currentNode);
		operation.setSucc(succ);
		@SuppressWarnings("resource")
		MatrixCursor localCur = new MatrixCursor(resultRow);
		if(selection.equals("@")){
			Log.d(TAG,"query() : originator calling LDump");
			localCur = returnLDump();	
		}

		if(selection.equals("*")){
			Log.d(TAG,"query() : originator calling GDump");
			operation.setType("*query");
			localCur = returnGDump(operation);
		}

		if(selection.length()>1){
			operation.setType("singleKeyQuery");
			//boolean condition = true;
			try {
				
				String keyHouse = getNodeForSingleKeyQuery(selection);
				String value = returnValueForKeyQuery(selection);
				if(value!=null){
					String resultRow[] = { selection, value };
					localCur.addRow(resultRow);
					return localCur;
				}
				if(keyHouse.equals(currentNode)){
					value = returnValueForKeyQuery(selection);
					if(value == null){
						while(value == null){
							Thread.sleep(200);
							value = returnValueForKeyQuery(selection);
							Log.d(TAG,"Query() while loop kry is "+operation.getKey());
						}
					}
					if(value!=null){
						Log.d(TAG,"Query() :: found key "+selection +" in Node : "+currentNode +" mutexQuery : "+mutexQuery); 
						String resultRow[] = { selection, value };
						localCur.addRow(resultRow);
						return localCur;
					}
				}else if(!keyHouse.equals(currentNode)){
					mutexQuery=null;
					operation.setSucc(keyHouse);
					Log.d(TAG,"Query() :: Not found key "+selection +" in Node : "+currentNode+" sending key to "+operation.getSucc()); 
					new Thread(new Client(operation)).start();
					Log.d(TAG,"Query() holding for value of key :"+selection+" type : "+operation.getType()+"Current Value"
							+" for mutex is : "+mutexQuery);
					while(mutexQuery == null){};
					String row[] = {selection,mutexQuery};
					localCur.addRow(row);
					Log.d(TAG,"Query() after hold got value : "+ mutexQuery +" for Key : "+selection+
							" Responded by "+operation.getComingFrom());
					mutexQuery=null;
					//Log.d(TAG, "Query() Value for mutexQuery "+mutexQuery +" key : "+selection);
					return localCur;
				}
				
				
//				String value = returnValueForKeyQuery(selection);
//				if(value != null){
//					Log.d(TAG,"Query() :: found key "+selection +" in Node : "+currentNode +" mutexQuery : "+mutexQuery); 
//					String resultRow[] = { selection, value };
//					localCur.addRow(resultRow);
//					//condition=false;
//					return localCur;
//				}else if(value == null){
//					mutexQuery=null;
//					String keyHouse = getNodeForSingleKeyQuery(selection);
//					operation.setSucc(keyHouse);
//					Log.d(TAG,"Query() :: Not found key "+selection +" in Node : "+currentNode+" sending key to "+operation.getSucc()); 
//					new Thread(new Client(operation)).start();
//					Log.d(TAG,"Query() holding for value of key :"+selection+" type : "+operation.getType()+"Current Value"
//							+" for mutex is : "+mutexQuery);
//					while(mutexQuery == null){};
//					String row[] = {selection,mutexQuery};
//					localCur.addRow(row);
//					Log.d(TAG,"Query() after hold got value : "+ mutexQuery +" for Key : "+selection+
//							" Responded by "+operation.getComingFrom());
//					mutexQuery=null;
//					Log.d(TAG, "Query() Value for mutexQuery "+mutexQuery +" key : "+selection);
//					return localCur;
//				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return localCur;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	private String genHash(String input) throws NoSuchAlgorithmException {
		MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
		byte[] sha1Hash = sha1.digest(input.getBytes());
		Formatter formatter = new Formatter();
		for (byte b : sha1Hash) {
			formatter.format("%02x", b);
		}
		return formatter.toString();
	}

	void init() {
		mNodeList = new ArrayList<String>();
		mNodeList.add("5562");
		mNodeList.add("5556");
		mNodeList.add("5554");
		mNodeList.add("5558");
		mNodeList.add("5560");

	}
	public String getPort() {
		TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
		return tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
	}

	private Uri buildUri(String scheme, String authority) {
		Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.authority(authority);
		uriBuilder.scheme(scheme);
		return uriBuilder.build();
	}
}


package it.cambieri.sts;

import it.cambieri.sts.Store.StoreState;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

/**
 * @author Oscar Cambieri
 * @date 04/12/2008
 * 
 */
public class Main implements Observer {

	public static final short MAX_LOGS_NUMBER = 1000;
	private final int MAX_IC = 10000;
	private Store store = null;
	private StoreCommunication communication;
	//private List<String> logs = null;
	private Calendar lastLogCalendar = null;
	private URL settingsXmlFileUrl;
	private Document settingsXmlDocument;
	private String directory;
	private String domain;
	private String user;
	private String password;
	private int index;
	private int timeout;
	private StoreMessage rxMessage;
	private boolean ackReceived;
	private boolean nakReceived;
	private String requestFile;
	private String requestSyn;
	private String responseFile;
	private String responseSyn;
	private String positionFile;
	private String positionSyn;
	private String downFile;
	//private String logFile;
	private String sharedFolder;
	private String tomSharedFolder; 
	private int lastJustReceivedIndex = -1;

	/**
	 * 
	 */
	public Main() {
		try {
//			if (System.getSecurityManager() == null) {
//				System.setSecurityManager(new SecurityManager());
//			}
			//logs = new LinkedList<String>();
			settingsXmlFileUrl = getClass().getResource("/settings.xml");
			if (settingsXmlFileUrl != null) {
				init();
			} else {
				System.err.println("\n*** FATAL ERROR: Configuration file settings.xml not found!\n");
				System.exit(1);
			}
			deleteFile(getSocketFileName());
			writeLog("Main - INFO: Object created");
			writeLog(store.toStringExtended() + "\n\tDIRECTORY: " + directory);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Initialize the store using the configuration file settings.xml
	 * 
	 * @return a state message to check for errors.
	 */
	private String init() {
		boolean isNewStore = true;
		boolean isCommunicationChanged = false;
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			settingsXmlDocument = docBuilder.parse(settingsXmlFileUrl
					.openStream());
			settingsXmlDocument.getDocumentElement().normalize();
			NodeList listOfStores = settingsXmlDocument
					.getElementsByTagName("settings");
			Node myStore = listOfStores.item(0);
			int myStoreId;
			myStoreId = Integer.parseInt(myStore.getAttributes().getNamedItem("id").getNodeValue());
			String myStoreDescription = myStore.getAttributes().getNamedItem("description").getNodeValue();
			String myStorePlcIp = myStore.getAttributes().getNamedItem("plcip").getNodeValue();
			int myStorePlcPort = Integer.parseInt(myStore.getAttributes().getNamedItem("plcport").getNodeValue());
			int myStoreMyPort = Integer.parseInt(myStore.getAttributes().getNamedItem("myport").getNodeValue());
			timeout = Integer.parseInt(myStore.getAttributes().getNamedItem("timeout").getNodeValue());
			directory = (myStore.getAttributes().getNamedItem("directory").getNodeValue());
			domain = (myStore.getAttributes().getNamedItem("domain").getNodeValue());
			user = (myStore.getAttributes().getNamedItem("user").getNodeValue());
			password = (myStore.getAttributes().getNamedItem("password").getNodeValue());
			requestFile = (myStore.getAttributes().getNamedItem("requestfile").getNodeValue());
			requestSyn = (myStore.getAttributes().getNamedItem("requestsyn").getNodeValue());
			responseFile = (myStore.getAttributes().getNamedItem("responsefile").getNodeValue());
			responseSyn = (myStore.getAttributes().getNamedItem("responsesyn").getNodeValue());
			positionFile = (myStore.getAttributes().getNamedItem("positionfile").getNodeValue());
			positionSyn = (myStore.getAttributes().getNamedItem("positionsyn").getNodeValue());
			downFile = (myStore.getAttributes().getNamedItem("downfile").getNodeValue());
			//logFile = (myStore.getAttributes().getNamedItem("logfile").getNodeValue());
			sharedFolder = (myStore.getAttributes().getNamedItem("sharedfolder").getNodeValue());
			tomSharedFolder = (myStore.getAttributes().getNamedItem("tomsharedfolder").getNodeValue());
			if (store == null) {
				store = new Store(myStoreId, myStoreDescription, myStorePlcIp,
						myStorePlcPort, myStoreMyPort, timeout,
						Store.StoreState.UNDEFINED);
			} else {
				isNewStore = false;
				isCommunicationChanged = (!store.getIp().equals(myStorePlcIp)
						|| store.getPortNumber() != myStorePlcPort || store
						.getMyPortNumber() != myStoreMyPort);
				if (myStoreId != store.getId()) store.setId(myStoreId);
				if (!myStoreDescription.equals(store.getDescription())) store.setDescription(myStoreDescription);
				if (!myStorePlcIp.equals(store.getIp())) store.setIp(myStorePlcIp);
				if (myStorePlcPort != store.getPortNumber()) store.setPortNumber(myStorePlcPort);
				if (myStoreMyPort != store.getMyPortNumber()) store.setMyPortNumber(myStoreMyPort);
				if (timeout != store.getTimeout()) store.setTimeout(timeout);
				//if (!logFile.equals(store.getLogFile())) store.setLogFile(logFile);
				if (store.getState() != Store.StoreState.UNDEFINED) store.setState(Store.StoreState.UNDEFINED);				
			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Start the communication with the store PLC
		if (isNewStore || isCommunicationChanged) {
			communication = new StoreCommunication(store);
			communication.addObserver(this);
			index = 0;
			try {
				File mySocketFile = new File(getSocketFileName());
				mySocketFile.createNewFile();									
			} catch (Exception e) {									
			}
			while (isTomWorking()) {
				try {
					Thread.sleep(100);
				} catch (Exception e) {
				}
			}
			try {
				communication.start(index);
			} catch (SocketException e) {
				deleteFile(getSocketFileName());
				System.err.println("FATAL ERROR: Application already started");
				System.exit(1);
			}
			deleteFile(getSocketFileName());
		}
		return "Store Initialized...";
	}
	
	private StoreMessage getRxMessage() {
		if(rxMessage == null) {
			rxMessage = new StoreMessage(store);
		}
		return rxMessage;
	}
	
	private boolean isCifsFile(String filePath)
	{
		boolean ret = false;
		if (filePath != null) {
			ret = filePath.trim().toLowerCase().startsWith("smb://");
		}		
		return ret;
	}
	
	private NtlmPasswordAuthentication getSambaAuth()
	{
		NtlmPasswordAuthentication ret = null;
		if (user != null && !"".equals(user)) {
			String myUserInfo = (domain == null || "".equals(domain.trim())) ? "" : (domain.trim() + ";");
			myUserInfo += user + ":" + password;
			try {
				ret = new NtlmPasswordAuthentication(myUserInfo);				
			} catch (Exception e) {
				e.printStackTrace();
				ret = null;
			}
		}
		return ret;
	}
	
	private boolean isFileExisting(String filePath)
	{
		boolean ret = false;
		if (isCifsFile(filePath)) {
			SmbFile mySmbFile = getSmbFile(filePath);
			try {
				ret = mySmbFile != null &&  mySmbFile.exists() && mySmbFile.isFile();
			} catch (SmbException e) {
				//e.printStackTrace();
				ret = false;
			} catch (Exception e) {
				e.printStackTrace();
				ret = false;
			}
		} else {
			File myFile = new File(filePath);
			ret = myFile.exists() && myFile.isFile();			
		}		
		return ret;
	}
	
	private SmbFile getSmbFile(String filePath) {
		SmbFile ret = null;
		NtlmPasswordAuthentication myAuth = getSambaAuth();
		try {
			if (myAuth != null) {
				ret = new jcifs.smb.SmbFile(filePath, myAuth);
			} else {
				ret = new jcifs.smb.SmbFile(filePath);
			}
		} catch (Exception e) {
			e.printStackTrace();
			ret = null;
		}					
		return ret;
	}
	
	private InputStream getInputStream(String filePath) {
		InputStream ret = null;
		if(isCifsFile(filePath)) {
			try {
				SmbFile mySmbFile = getSmbFile(filePath);
				if(mySmbFile != null) {
					ret = new SmbFileInputStream(mySmbFile);
				}				
			} catch (Exception e) {
				e.printStackTrace();
				ret = null;
			}	
		} else {
			try {
				File myFile = new File(filePath);
				ret = new FileInputStream(myFile);
			} catch (Exception e) {
				e.printStackTrace();
				ret = null;
			}
		}		
		return ret;
	}
	
	private void deleteFile(String filePath) {
		if(isCifsFile(filePath)) {
			SmbFile mySmbFile = getSmbFile(filePath);
			try {
				if(mySmbFile != null && mySmbFile.exists() && mySmbFile.isFile()) {
					mySmbFile.delete();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		} else {
			try {
				File myFile = new File(filePath);
				if (myFile.exists() && myFile.isFile()) {
					myFile.delete();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void writeFile(String filePath, String fileContent) {
		if(isCifsFile(filePath)) {
			SmbFile mySmbFile = getSmbFile(filePath);
			try {
				if(mySmbFile != null) {
					SmbFileOutputStream mySmbOutStream = new SmbFileOutputStream(mySmbFile);
					mySmbOutStream.write(fileContent.getBytes());
					mySmbOutStream.flush();
					mySmbOutStream.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		} else {
			try {
				File myFile = new File(filePath);
				FileOutputStream myOutStream = new FileOutputStream(myFile);
				myOutStream.write(fileContent.getBytes());
				myOutStream.flush();
				myOutStream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private String getSocketFileName() {
		return sharedFolder + "/CMBSOCKET";
	}
	
	private String getTomSocketFileName() {
		return (tomSharedFolder != null && !"".equals(tomSharedFolder.trim())) ? tomSharedFolder + "/CMBSOCKET" : ""; 
	}
	
	private boolean isTomWorking()
	{
		boolean ret = false;
		String myTomFileName = getTomSocketFileName();
		if(!"".equals(myTomFileName)) {
			ret = isFileExisting(myTomFileName);
			if (ret) {
				File myFile = new File(myTomFileName);
				ret = (System.currentTimeMillis() - myFile.lastModified() < 120 * 1000);							
			}
		}
		return ret;
	}
	
	@SuppressWarnings("unused")
	private void writeXmlStore() {
		try {
			// first of all we request out DOM-implementation:
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			// then we have to create document-loader and document
			DocumentBuilder loader;
			loader = factory.newDocumentBuilder();
			Document doc = loader.newDocument();
			// create root element
			Element elRoot = doc.createElement("it.cambieri.sts");
			doc.appendChild(elRoot);
			// create store element
			Element elStore = doc.createElement("store");
			elStore.setAttribute("id", String.valueOf(store.getId()));
			elStore.setAttribute("description", store.getDescription());
			elStore.setAttribute("plcip", store.getIp());
			elStore.setAttribute("plcport", String.valueOf(store
					.getPortNumber()));
			elStore.setAttribute("myport", String.valueOf(store
					.getMyPortNumber()));
			elStore.setAttribute("timeout", String.valueOf(store.getTimeout()));
			elRoot.appendChild(elStore);
			// use specific Xerces class to write DOM-data to a file:
			XMLSerializer serializer = new XMLSerializer();
			serializer.setOutputCharStream(new java.io.FileWriter(
					settingsXmlFileUrl.getPath()));
			serializer.serialize(doc);
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeLog(String pLog) {
		String myLog = String.format("%1$tD - %1$tT -> %2$s", GregorianCalendar.getInstance(), pLog);
		System.out.println(myLog);
		lastLogCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
//		writeLogToFile(myLog);
//		if (store.getLogEnabled()) {
//			System.out.println(myLog);
//		}
//		if (logs.size() > MAX_LOGS_NUMBER) {
//			logs.remove(logs.size() - 1);
//		}
//		logs.add(0, myLog);
	}

	public void generaFileErrore(int tipo) {
		String st = "[main]" + (char) 13 + (char) 10;
		switch (tipo) {
		case 1: // syntax error
			writeLog("Creating ERROR file (syntax)");
			st += "code=WMS_ERROR" + (char) 13 + (char) 10;
			st += "state=" + store.getState().ordinal() + (char) 13 + (char) 10;
			st += "alarm_type=2" + (char) 13 + (char) 10;
			st += "alarm_code=" + (char) 13 + (char) 10;
			break;
		case 2: // nak error
			writeLog("Creating ERROR file (nack)");
			st += "code=WMS_ERROR" + (char) 13 + (char) 10;
			st += "state=" + store.getState().ordinal() + (char) 13 + (char) 10;
			int alcode = getRxMessage().getReceivedAlCode() < 2 ? 0 : 1;
			st += "alarm_type=" + alcode + (char) 13 + (char) 10;
			st += "alarm_code=" + (char) 13 + (char) 10;
			break;
		default:
			break;
		}
		writeResponse(responseFile, responseSyn, st);
	}

	public void generaFileConferma() {
		writeLog("Creating CONFIRMATION file");
		String st = "[main]" + (char) 13 + (char) 10;
		st += "code=WMS_C_PALLET_FOUND" + (char) 13 + (char) 10;
		writeResponse(responseFile, responseSyn, st);
	}

	public void generaFilePosizionamento() {
		writeLog("Creating POSITIONING file");
		String st = "[pack1]" + (char) 13 + (char) 10;
		int material = rxMessage.getReceivedMaterial();
		st += "name=" + material + (char) 13 + (char) 10;
		st += "format=" + material + (char) 13 + (char) 10;
		st += "quantity=1" + (char) 13 + (char) 10;
		st += "xdim=" + rxMessage.getReceivedX() + (char) 13 + (char) 10;
		st += "ydim=" + rxMessage.getReceivedY() + (char) 13 + (char) 10;
		int z = rxMessage.getReceivedZ();
		st += "zdim=" + (z / 10) + "." + (z % 10) + (char) 13 + (char) 10;
		st += "sw=1" + (char) 13 + (char) 10;
		st += "xpos=0" + (char) 13 + (char) 10;
		st += "ypos=0" + (char) 13 + (char) 10;
		st += "worked=0" + (char) 13 + (char) 10;
		writeResponse(positionFile, positionSyn, st);
	}

	private void writeResponse(String pFilename, String pSynfile, String pContent) {
		//FileWriter fw;	//SAMBA
		try {
			if (!pFilename.equals("")) {
				//fw = new FileWriter(directory + "/" + pFilename);
				//fw.write(pContent);
				//fw.close();
				writeFile(directory + "/" + pFilename, pContent);
			}
			if (!pSynfile.equals("")) {
				//fw = new FileWriter(directory + "/" + pSynfile);
				//fw.write("");
				//fw.close();
				writeFile(directory + "/" + pSynfile, "");
			}
		} catch (Exception e) {
			e.printStackTrace();
			writeLog("Exception while writing response file");
		}
	}
	
//	@SuppressWarnings("unused")
//	private void writeLogToFile(String pContent) {
//		try {
//			if (!logFile.equals("")) {
//				String myLog = pContent + (char) 13 + (char) 10;
//				FileWriter fw;
//				fw = new FileWriter(logFile, true);
//				fw.write(myLog);
//				fw.close();
//				lastLogCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

//	private void enableDisableLog() {
//		store.setLogEnabled(!store.getLogEnabled());
//	}

	public void work() {
		// main daemon
		new Thread() {
			@Override
			public void run() {
				Double temp;
				while (true) {
					try {
						// gestione automatica
						String pathName = directory + "/" + requestSyn;	//SAMBA
						//File f_syn = new File(pathName);
						boolean richiestaPendente = isFileExisting(pathName);
						if (richiestaPendente) {
							pathName = directory + "/" + requestFile;
							//File f_ini = new File(pathName);
							if (isFileExisting(pathName)) {
								try {
									File mySocketFile = new File(getSocketFileName());
									mySocketFile.createNewFile();									
								} catch (Exception e) {									
								}								
								//FileInputStream is = new FileInputStream(f_ini);
								InputStream is = getInputStream(pathName);
								Properties p = new Properties();
								p.load(is);
								try {
									String code = p.getProperty("code");
									if (!code.equalsIgnoreCase("C_WMS_PALLET_LOAD_REQ")) {
										throw new NumberFormatException();
									}
									temp = Double.parseDouble(p.getProperty("format"));
									Integer format = temp.intValue();
									temp = Double.parseDouble(p.getProperty("xdim"));
									Integer xdim = temp.intValue();
									temp = Double.parseDouble(p.getProperty("ydim"));
									Integer ydim = temp.intValue();
									temp = Double.parseDouble(p.getProperty("zdim")) * 10;
									Integer zdim = temp.intValue();
									String[] pStringsToSend = new String[Store.TX_PACKET_LENGTH / 2];
									pStringsToSend[0] = format.toString();
									pStringsToSend[1] = xdim.toString();
									pStringsToSend[2] = ydim.toString();
									pStringsToSend[3] = zdim.toString();
									ackReceived = false;
									nakReceived = false;
									index = (index < MAX_IC) ? index + 1 : 1;
									while(isTomWorking()) {
										sleep(100);
									}
									communication.sendMessage(index, pStringsToSend);
									writeLog("Main.index = " + index);
									sleep(timeout);
									if (!ackReceived && !nakReceived) {
										writeLog("*** Timeout! (NAK forced)\n");
										nakReceived = true;
										store.setState(StoreState.TIMEOUT);
										getRxMessage().setAlCode((byte) 0);
										generaFileErrore(2);
									}
								} catch (NumberFormatException e) {
									generaFileErrore(1);
								}
								is.close();
								//f_ini.delete();
								deleteFile(directory + "/" + requestFile);
								//f_syn.delete();
								deleteFile(directory + "/" + requestSyn);
								deleteFile(getSocketFileName());
							}
						} else if (!downFile.equals("")) {
							//String downPathName = directory + "/" + downFile;
							//File f_down = new File(downPathName);
							boolean richiestaDown = isFileExisting(directory + "/" + downFile);
							if (richiestaDown) {
								deleteFile(getSocketFileName());
								writeLog("\n\n***\nSTS stopped by user!\n***\n");
								System.exit(0);								
							}							
						} else if (isFileExisting(sharedFolder + "/STSCLOSE")) {
							deleteFile(getSocketFileName());
							writeLog("\n\n***\nSTS closed!\n***\n");
							System.exit(0);															
						}
						sleep(3000);
						long diff = (lastLogCalendar == null) ? 100000 : Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() - lastLogCalendar.getTimeInMillis();
						if (Math.abs(diff) > 60000) {
							writeLog("STS running...");
						}							
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
		// cli interface
		new Thread() {
			//@SuppressWarnings("resource")
			@Override
			public void run() {
				while (true) {					
					try {
						if (isFileExisting(sharedFolder + "/STSCLOSE")) {
							writeLog("\n\n***\nSTS closed!\n***\n");
							System.exit(0);															
						} else if (isFileExisting(sharedFolder + "/STSPING")) {
							ping();
							deleteFile(sharedFolder + "/STSPING");
						}
						sleep(100);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
/*				
				System.out.println("\nSalvagnini-SPR - Console Interface\n");
				System.out.println("Press <q> + <ENTER> to quit...\n");
				System.out
						.println("Press <?> + <ENTER> to see a list of the available commands\n\n");
				String st = "loop";
				// Waiting for the <q> key to quit
				while (!st.equalsIgnoreCase("q")) {
					try {
						st = new Scanner(System.in).nextLine();
						// System.out.print(st);
						String[] params = st.split(":");
						if (params[0].toLowerCase().equals("settings") || params[0].toLowerCase().equals("setting")) {
							String mySettings = store.toStringExtended() + "\n\tDIRECTORY: " + directory;
							System.out.println(mySettings);
							//writeLogToFile(mySettings);
						} else if (params[0].equals("a")) {
							//printLogs();
						} else if (params[0].equals("l")) {
							//printLastLog();
						} else if (params[0].equals("p")) {
							ping();
						} else if (params[0].equals("s")) {
							//enableDisableLog();
						} else if (!params[0].equals("q")) {
							System.out.println("\nAVAILABLE COMMANDS:");
							System.out
									.println("settings -> print the system settings");
							System.out.println("a -> print all logs");
							System.out.println("l -> print last log only");
							System.out.println("p -> ping the Stima store");
							System.out.println("s -> start/stop log");
							System.out.println("---");
							System.out.println("q -> quit the server");
						}
					} catch (Exception e) {
						e.printStackTrace();
						System.out
								.println("\nPress <?> + <ENTER> to see a list of the available commands\n");
					}
				}
				// Finalizing the test
				System.out
						.println("\n\n***\nProgram correctly stopped!\n***\n");
				System.exit(0);
			*/
			}
		}.start();
	}

	public void ping() throws RemoteException {
		try {
			File mySocketFile = new File(getSocketFileName());
			mySocketFile.createNewFile();									
		} catch (Exception e) {									
		}								
		index = (index < MAX_IC) ? index + 1 : 1;
		writeLog("Main - INFO: Command ping required with INDEX = " + index);
		while (isTomWorking()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}			
		}
		communication.ping(index);
		try {
			Thread.sleep(timeout);
		} catch (InterruptedException e) {
		}
		deleteFile(getSocketFileName());
	}

//	public void printLogs() {
//		for (String st : logs) {
//			System.out.println(st);
//		}
//	}
//
//	public void printLastLog() throws RemoteException {
//		if (logs != null && logs.size() > 0) {
//			//System.out.println(logs.get(logs.size() - 1));
//			System.out.println(logs.get(0));
//		}
//	}

	@Override
	public void update(Observable o, Object pStoreMessage) {
		StoreMessage justReceivedMessage = (StoreMessage) pStoreMessage;
		int justReceivedIndex = justReceivedMessage.getReceivedIndex();
		if (justReceivedIndex != lastJustReceivedIndex) {
			writeLog("*** Message with index " + justReceivedIndex
					+ " observed by Main");
			lastJustReceivedIndex = justReceivedIndex;
		}
		if (rxMessage == null) {
			rxMessage = justReceivedMessage;
		}
		if (justReceivedIndex == index && justReceivedIndex > 0) {
			rxMessage = justReceivedMessage;
			if ((!ackReceived) && (!nakReceived) && (rxMessage.isAck())) {
				ackReceived = true;
				generaFileConferma();
			} else if ((!nakReceived) && (!ackReceived) && (rxMessage.isNak())) {
				nakReceived = true;
				generaFileErrore(2);
			} else if ((ackReceived) && (rxMessage.isStrobe())) {
				generaFilePosizionamento();
			}
		} else if (justReceivedIndex <= 0 || index <= 0) {
			writeLog("Indexes not set; my: " + index + ", received: "
					+ justReceivedIndex);
			if (index <= 0) index = 1;
		} else {
			writeLog("Indexes don't match; expected: " + index + ", received: "
					+ justReceivedIndex);
		}
		//---VC communication.ping(index);
	}

	/**
	 * Entry point
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Main m = new Main();
		m.work();
	}
}

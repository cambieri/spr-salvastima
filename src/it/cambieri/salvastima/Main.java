package it.cambieri.salvastima;

import it.cambieri.salvastima.Store.StoreState;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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

	public static final short MAX_LOGS_NUMBER = 500;
	private Store store;
	private StoreCommunication communication;
	private List<String> logs;
	private URL settingsXmlFileUrl;
	private Document settingsXmlDocument;
	private String directory;
	private int index;
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

	/**
	 * 
	 */
	public Main() {
		try {
			logs = new LinkedList<String>();
			settingsXmlFileUrl = getClass().getResource("/settings.xml");
			if (settingsXmlFileUrl != null) {
				init();
			} else {
				System.err
						.println("\n*** FATAL ERROR: Configuration file settings.xml not found!\n");
				System.exit(1);
			}
			writeLog("Main - INFO: Object created");
			writeLog(store.toStringExtended() + "\n\tDIRECTORY:" + directory);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Initialize the store using the configuration file settings.xml
	 * 
	 * @return a state message to check for errors.
	 */
	private String init() {
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
			int myStoreId = Integer.parseInt(myStore.getAttributes()
					.getNamedItem("id").getNodeValue());
			String myStoreDescription = myStore.getAttributes().getNamedItem(
					"description").getNodeValue();
			String myStorePlcIp = myStore.getAttributes().getNamedItem("plcip")
					.getNodeValue();
			int myStorePlcPort = Integer.parseInt(myStore.getAttributes()
					.getNamedItem("plcport").getNodeValue());
			int myStoreMyPort = Integer.parseInt(myStore.getAttributes()
					.getNamedItem("myport").getNodeValue());
			int timeout = Integer.parseInt(myStore.getAttributes()
					.getNamedItem("timeout").getNodeValue());
			directory = (myStore.getAttributes().getNamedItem("directory")
					.getNodeValue());
			requestFile = (myStore.getAttributes().getNamedItem("requestfile")
					.getNodeValue());
			requestSyn = (myStore.getAttributes().getNamedItem("requestsyn")
					.getNodeValue());
			responseFile = (myStore.getAttributes().getNamedItem("responsefile")
					.getNodeValue());
			responseSyn = (myStore.getAttributes().getNamedItem("responsesyn")
					.getNodeValue());
			positionFile = (myStore.getAttributes().getNamedItem("positionfile")
					.getNodeValue());
			positionSyn = (myStore.getAttributes().getNamedItem("positionsyn")
					.getNodeValue());
			downFile = (myStore.getAttributes().getNamedItem("downfile")
					.getNodeValue());			
			store = new Store(myStoreId, myStoreDescription, myStorePlcIp,
					myStorePlcPort, myStoreMyPort, timeout,
					Store.StoreState.UNDEFINED, true);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Start the communication with the store PLC
		communication = new StoreCommunication(store);
		communication.addObserver(this);
		index = 0;
		try {
			communication.start(index);
		} catch (SocketException e) {
			System.err.println("FATAL ERROR: Application already started");
			System.exit(1);
		}
		return "Store Initialized...";
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
			Element elRoot = doc.createElement("it.cambieri.salvastima");
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
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void writeLog(String pLog) {
		String myLog = String.format("%1$tD - %1$tT -> %2$s", GregorianCalendar
				.getInstance(), pLog);
		if (store.getLogEnabled()) {
			System.out.println(myLog);
		}
		if (logs.size() > MAX_LOGS_NUMBER) {
			logs.remove(logs.size() - 1);
		}
		logs.add(0, myLog);
	}

	public void generaFileErrore(int tipo) {
		String st = "[main]" + (char) 13 + (char) 10;
		switch (tipo) {
		case 1: // syntax error
			writeLog("Generazione file di ERRORE (syntax)");
			st += "code=WMS_ERROR" + (char) 13 + (char) 10;
			st += "state=" + store.getState().ordinal() + (char) 13 + (char) 10;
			st += "alarm_type=2" + (char) 13 + (char) 10;
			st += "alarm_code=" + (char) 13 + (char) 10;
			break;
		case 2: // nak error
			writeLog("Generazione file di ERRORE (nack)");
			st += "code=WMS_ERROR" + (char) 13 + (char) 10;
			st += "state=" + store.getState().ordinal() + (char) 13 + (char) 10;
			int alcode = rxMessage.getReceivedAlCode() < 2 ? 0 : 1;
			st += "alarm_type=" + alcode + (char) 13 + (char) 10;
			st += "alarm_code=" + (char) 13 + (char) 10;
			break;
		default:
			break;
		}
		writeResponse(responseFile, responseSyn, st);
	}

	public void generaFileConferma() {
		writeLog("Generazione file di CONFERMA");
		String st = "[main]" + (char) 13 + (char) 10;
		st += "code=WMS_C_PALLET_FOUND" + (char) 13 + (char) 10;
		writeResponse(responseFile, responseSyn, st);
	}

	public void generaFilePosizionamento() {
		writeLog("Generazione file di POSIZIONAMENTO");
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
		FileWriter fw;
		try {
			if (pFilename != "") {
				fw = new FileWriter(directory + "/" + pFilename);
				fw.write(pContent);
				fw.close();
			}
			if (pSynfile != "") {
				fw = new FileWriter(directory + "/" + pSynfile);
				fw.write("");
				fw.close();
			}
		} catch (IOException e) {
			writeLog("IOException su generazione file responso");
			e.printStackTrace();
		}
	}
	
	private void enableDisableLog() {
		store.setLogEnabled(!store.getLogEnabled());
	}

	public void work() {
		// main daemon
		new Thread() {
			@Override
			public void run() {
				Double temp;
				while (true) {
					try {
						// gestione automatica
						String pathName = directory + "/" + requestSyn;
						File f_syn = new File(pathName);
						boolean richiestaPendente = f_syn.exists();
						if (richiestaPendente) {
							pathName = directory + "/" + requestFile;
							File f_ini = new File(pathName);
							if (f_ini.exists()) {
								FileInputStream is = new FileInputStream(f_ini);
								Properties p = new Properties();
								p.load(is);
								try {
									String code = p.getProperty("code");
									if (!code
											.equalsIgnoreCase("C_WMS_PALLET_LOAD_REQ")) {
										throw new NumberFormatException();
									}
									temp = Double.parseDouble(p
											.getProperty("format"));
									Integer format = temp.intValue();
//									Integer format = Integer.parseInt(p
//											.getProperty("format"));
									temp = Double.parseDouble(p
											.getProperty("xdim"));
									Integer xdim = temp.intValue();
//									Integer xdim = Integer.parseInt(p
//											.getProperty("xdim"));
									temp = Double.parseDouble(p
											.getProperty("ydim"));
									Integer ydim = temp.intValue();
//									Integer ydim = Integer.parseInt(p
//											.getProperty("ydim"));
									temp = Double.parseDouble(p
											.getProperty("zdim")) * 10;
									Integer zdim = temp.intValue();
									String[] pStringsToSend = new String[Store.TX_PACKET_LENGTH / 2];
									pStringsToSend[0] = format.toString();
									pStringsToSend[1] = xdim.toString();
									pStringsToSend[2] = ydim.toString();
									pStringsToSend[3] = zdim.toString();
									ackReceived = false;
									nakReceived = false;
									index = (index < 32000) ? index + 1 : 1;
									communication.sendMessage(index,
											pStringsToSend);
									writeLog("Main.index = " + index);
									sleep(2000);
									if (!ackReceived && !nakReceived) {
										writeLog("*** Timeout! (NAK forced)\n");
										nakReceived = true;
										store.setState(StoreState.TIMEOUT);
										rxMessage.setAlCode((byte) 0);
										generaFileErrore(2);
									}
								} catch (NumberFormatException e) {
									generaFileErrore(1);
								}
								is.close();
								f_ini.delete();
								f_syn.delete();
							}
						} else if (downFile != "") {
							String downPathName = directory + "/" + downFile;
							File f_down = new File(downPathName);
							boolean richiestaDown = f_down.exists();
							if (richiestaDown) {
								System.out
										.println("\n\n***\nProgram stopped by user!\n***\n");
								System.exit(0);								
							}							
						}
						sleep(3000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}.start();
		// cli interface
		new Thread() {
			@Override
			public void run() {
				System.out.println("\nSalvagnini-Stima - Console Interface\n");
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
						if (params[0].equals("settings")) {
							System.out.println(store.toStringExtended()
									+ "\n\tDIRECTORY:" + directory);
						} else if (params[0].equals("a")) {
							printLogs();
						} else if (params[0].equals("l")) {
							printLastLog();
						} else if (params[0].equals("p")) {
							ping();
						} else if (params[0].equals("s")) {
							enableDisableLog();
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
			}
		}.start();
	}

	public void ping() throws RemoteException {
		index = (index < 32000) ? index + 1 : 1;
		writeLog("Main - INFO: Command ping required with INDEX = " + index);
		communication.ping(index);
	}

	public void printLogs() {
		for (String st : logs) {
			System.out.println(st);
		}
	}

	public void printLastLog() throws RemoteException {
		System.out.println(logs.get(logs.size() - 1));
	}

	@Override
	public void update(Observable o, Object pStoreMessage) {
		StoreMessage justReceivedMessage = (StoreMessage) pStoreMessage;
		int justReceivedIndex = justReceivedMessage.getReceivedIndex();
		writeLog("*** Message with index " + justReceivedIndex
				+ " observed by Main");
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
		} else {
			writeLog("Indexes don't match; expected: " + index + ", received: "
					+ justReceivedIndex);
		}
		communication.ping(index);
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

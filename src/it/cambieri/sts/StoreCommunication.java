package it.cambieri.sts;

import java.net.BindException;
import java.net.SocketException;
import java.util.GregorianCalendar;
import java.util.Observable;
import java.util.Observer;

/**
 * This is the class that control all the communications tasks with the store.
 * 
 * @author Oscar Cambieri
 * @version 04/12/2008
 */
public class StoreCommunication extends Observable implements Observer {
	/**
	 * Private internal fields
	 */
	@SuppressWarnings("unused")
	private static final long serialVersionUID = 1L;
	private final Store store;
	private final StoreEvents storeEvents;
	private Thread storeEventsThread;
	private boolean connectionStarted;
	private Integer pingValue = 0;
	private int lastPingIndex = -1;

	/**
	 * Construct a StoreCommunication object for the given store.
	 * 
	 * @param pStore
	 *            the store to manage.
	 */
	public StoreCommunication(Store pStore) {
		super();
		store = pStore;
		storeEvents = new StoreEvents(store);
		storeEvents.addObserver(this);
		connectionStarted = false;
		writeLog("StoreCommunication - INFORMATION: object created ");
	}

	/**
	 * Start the events thread that listen for socket message from the store
	 * system and activate all this object functionalities.
	 * 
	 * @param index
	 *            the starting index.
	 * @throws BindException 
	 */
	public void start(int index) throws SocketException {
		if (!connectionStarted) {
			storeEventsThread = new Thread(storeEvents);
			storeEventsThread.start();
			connectionStarted = true;
			storeEvents.reset(index);
			writeLog("StoreCommunication - INFORMATION: connection started");
		} else {
			writeLog("StoreCommunication - WARNING: tried to start an already started connection.");
		}
	}

	/**
	 * Stop the events thread that listen for socket message from the store
	 * system and deactivate all this object functionalities.
	 */
	public void stop() {
		if (connectionStarted) {
			storeEventsThread = null;
			connectionStarted = false;
			writeLog("StoreCommunication - INFORMATION: connection stopped");
		} else {
			writeLog("StoreCommunication - WARNING: tried to stop a connection that is not started.");
		}
	}

	/**
	 * Try to reset the communication with the store system.
	 * 
	 * @param index
	 *            the starting index.
	 * @throws BindException 
	 */
	public void reset(int index) throws SocketException {
		if (connectionStarted) {
			storeEvents.reset(index);
			writeLog("StoreCommunication - INFORMATION: connection resetted");
		} else {
			writeLog("StoreCommunication - WARNING: tried to reset a connection not started yet.");
		}
	}

	/**
	 * Send a message to the managed store.
	 * 
	 * @param index
	 *            the index for the message to send.
	 * 
	 * @param pStringsToSend
	 *            the message to send.
	 */
	public void sendMessage(int index, String[] pStringsToSend) {
		if (connectionStarted) {
			if (Integer.parseInt(pStringsToSend[0]) == Store.PING_0
					|| Integer.parseInt(pStringsToSend[0]) == Store.PING_1) {
				if (index != lastPingIndex) {
					writeLog("StoreCommunication - INFORMATION: trying to send a PING, INDEX = "
							+ index);
					lastPingIndex = index;
				}
				
			} else {
				writeLog("StoreCommunication - INFORMATION: trying to send a command with MAT = "
						+ pStringsToSend[0] + ", INDEX = " + index);
			}
			storeEvents.sendMessage(index, pStringsToSend);
		} else {
			writeLog("StoreCommunication - WARNING: tried to send a message using a connection not started yet.");
		}
	}

	/**
	 * Try to send a ping command.
	 * 
	 * @param index
	 */
	public void ping(int index) {
		if (connectionStarted) {
			pingValue = (pingValue == Store.PING_0) ? Store.PING_1
					: Store.PING_0;
			String[] stringsToSend = new String[Store.TX_PACKET_LENGTH / 2];
			stringsToSend[0] = pingValue.toString();
			stringsToSend[1] = pingValue.toString();
			stringsToSend[2] = pingValue.toString();
			stringsToSend[3] = pingValue.toString();
			stringsToSend[4] = pingValue.toString();
			stringsToSend[5] = pingValue.toString();
			stringsToSend[6] = pingValue.toString();
			sendMessage(index, stringsToSend);
		} else {
			writeLog("PlcCommunication - WARNING: tried to send a ping() on a connection not started yet.");
		}
	}

	
//	public void enableDisableLog() {
//		store.setLogEnabled(!store.getLogEnabled());
//	}
	
	/**
	 * Write a log to the standard output and send a message to the observers.
	 * 
	 * @param pLog
	 *            the log to write and send to observers.
	 */
	public void writeLog(String pLog) {
		String myLog = String.format("%1$tD - %1$tT -> %2$s", GregorianCalendar
				.getInstance(), pLog);
		System.out.println(myLog);
//		writeLogToFile(myLog);
//		if (store.getLogEnabled()) {
//			System.out.println(myLog);
//		}
	}

//	private void writeLogToFile(String pContent) {
//		try {
//			if (!store.getLogFile().equals("")) {
//				String myLog = pContent + (char) 13 + (char) 10;
//				FileWriter fw;
//				fw = new FileWriter(store.getLogFile(), true);
//				fw.write(myLog);
//				fw.close();
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}


	public void update(Observable pStoreEvents, Object pStoreMessage) {
		setChanged();
		notifyObservers(pStoreMessage);
	}
}

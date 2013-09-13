package it.cambieri.salvastima;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.GregorianCalendar;
import java.util.Observable;

/**
 * @author Oscar Cambieri
 * @version 04/12/2008
 */
public class StoreEvents extends Observable implements Runnable {

	private final int SLEEP_MILLISEC = 100;

	/**
	 * Private internal fields
	 */
	private final String ip;
	private final int port;
	private final int myPort;
	InetAddress host;
	private DatagramSocket socket;
	private final StoreMessage txTowerMessage;
	private final StoreMessage rxStoreMessage;
	private final short index;
	private Store store;

	public StoreEvents(Store pStore) {
		ip = pStore.getIp();
		port = pStore.getPortNumber();
		myPort = pStore.getMyPortNumber();
		host = null;
		socket = null;
		txTowerMessage = new StoreMessage(pStore);
		rxStoreMessage = new StoreMessage(pStore);
		index = 0;
		store = pStore;
	}

	private DatagramSocket getSocket() throws SocketException {
		if (socket == null) {
			try {
				socket = new DatagramSocket(myPort);
				host = InetAddress.getByName(ip);
			} catch (UnknownHostException e) {
				String errorMessage = "StoreEvents - FATAL ERROR: unknown towers IP address";
				writeLog(errorMessage);
				e.printStackTrace();
			}
		}
		return socket;
	}

	private void writeLog(String pLog) {
		if (store.getLogEnabled()) {
			String myLog = String.format("%1$tD - %1$tT -> %2$s", GregorianCalendar
					.getInstance(), pLog);
			System.out.println(myLog);
		}
	}

	public void reset(int index) throws SocketException {
		socket = getSocket();
		if (socket != null) {
			String[] stringsToSend = new String[Store.TX_PACKET_LENGTH / 2];
			java.util.Arrays.fill(stringsToSend, "0");
			txTowerMessage.setTxBuffer(index, stringsToSend);
			sendMessage(txTowerMessage);
		}
	}

	public void sendMessage(int index, String[] pStringsToSend) {
		if (socket != null) {
			txTowerMessage.setTxBuffer(index, pStringsToSend);
			writeLog("StoreEvents - TX INFORMATION: message sending with index "
					+ index + ", waiting for response");
			sendMessage(txTowerMessage);
		} else {
			String errorMessage = "StoreEvents - TX WARNING: communication with towers closed; try to reset";
			writeLog(errorMessage);
		}
	}

	private void sendMessage(StoreMessage pMessageToSend) {
		try {
			pMessageToSend.sendUsingUdp(socket, host, port);
		} catch (IOException e) {
			writeLog("StoreEvents - ERROR: Problems sending message to Tower");
			e.printStackTrace();
		}
	}

	public StoreMessage getLastReceivedMessage() {
		return rxStoreMessage;
	}

	public void run() {
		boolean warningLogEnabled = true;
		while (true) {
			try {
				if (socket != null) {
					rxStoreMessage.receiveUsingUdp(socket);
					if ((index != 0)
							&& (rxStoreMessage.getReceivedIndex() != index)) {
						System.err.println("\n*** Index not aligned\n");
					}
					if (!warningLogEnabled) {
						String message = "StoreEvents - RX INFORMATION: communication with store correctly established";
						writeLog(message);
						warningLogEnabled = true;
					}
					String message = "StoreEvents - RX INFORMATION: message received: ";
					message += " (INDEX=" + rxStoreMessage.getReceivedIndex()
							+ ") ";
					message += rxStoreMessage
							.getRxBufferFormattedStringWithSeparator();
					writeLog(message);
					setChanged();
					notifyObservers(rxStoreMessage);
				} else {
					if (warningLogEnabled) {
						warningLogEnabled = false;
						String errorMessage = "StoreEvents - RX WARNING: communication with towers closed; try to reset";
						writeLog(errorMessage);
					}
				}
				Thread.sleep(SLEEP_MILLISEC);
			} catch (IOException e) {
				writeLog("StoreEvents - ERROR: Problems receveing message to Tower");
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}

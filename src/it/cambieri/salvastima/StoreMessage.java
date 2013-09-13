package it.cambieri.salvastima;

import it.cambieri.salvastima.Store.StoreState;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

/**
 * This class rapresent a tower message and contains all the methods to
 * manipulate, receive and send a message to tower.
 * 
 * @author Oscar Cambieri
 * @version 04/12/2008
 */
public class StoreMessage {
	/**
	 * This class rapresent a couple of byte.
	 * 
	 */
	private class ByteCouple {
		public byte high = 0;
		public byte low = 0;

		public ByteCouple() {
			this((byte) 0, (byte) 0);
		}

		public ByteCouple(byte pHigh, byte pLow) {
			high = pHigh;
			low = pLow;
		}
	}

	private byte[] txBuffer;
	private byte[] rxBuffer;
	private final Store store;

	/**
	 * Default constructor for an empty tower message.
	 * 
	 * @param pStore
	 */
	public StoreMessage(Store pStore) {
		super();
		store = pStore;
		txBuffer = new byte[Store.TX_PACKET_LENGTH];
		rxBuffer = new byte[Store.RX_PACKET_LENGTH];
	}

	/**
	 * Compact a ByteCouple value inside an integer value.
	 * 
	 * @param pBc
	 *            the ByteCouple to compact inside an integer.
	 * @return an integer rappresentation of the inner ByteCouple.
	 */
	private int intFromByteCouple(ByteCouple pBc) {
		return (intFromCoupleOfByte(pBc.high, pBc.low));
	}

	/**
	 * Compact a couple of byte inside an integer value.
	 * 
	 * @param high
	 *            the higher byte of the couple.
	 * @param low
	 *            the lower byte of the couple.
	 * @return an integer rappresentation of the inner couple of byte.
	 */
	private int intFromCoupleOfByte(byte high, byte low) {
		int i_high = (high & 0xff);
		int i_low = (low & 0xff);
		int retVal = (i_high << 8) + i_low;
		return retVal;
	}

	/**
	 * Extract a couple of byte from an integer.
	 * 
	 * @param num
	 *            the integer value to elaborate.
	 * @return a ByteCouple containing the split of the inner integer value.
	 */
	private ByteCouple byteFromInteger(int num) {
		ByteCouple retVal = new ByteCouple();
		retVal.low = (byte) (num & 255);
		retVal.high = (byte) (num >> 8);
		return retVal;
	}

	/**
	 * Helper function for getRxBufferFormattedString and
	 * getTxBufferFormattedString.
	 * 
	 * @param pBuffer
	 *            the buffer to transform in a long "human readable" string.
	 * @param pSeparator
	 *            an eventual separator between double words.
	 * @param numDbw
	 *            the number of DoubleWord to return in the final string; values
	 *            <=0 means all buffer returned.
	 * @return the received data into a formatted and justified string (5
	 *         characters for each DoubleWord).
	 */
	private String getBufferFormattedString(byte[] pBuffer, String pSeparator,
			int numDbw) {
		String retVal = "";
		int myLength = (numDbw > 0) ? numDbw * 2 : pBuffer.length;
		for (int i = 0; i < myLength; i += 2) {
			ByteCouple bc = new ByteCouple(pBuffer[i], pBuffer[i + 1]);
			int numToSend = intFromByteCouple(bc);
			String temp = "00000" + String.valueOf(numToSend);
			retVal += temp.substring(temp.length() - 5, temp.length())
					+ pSeparator;
		}
		return retVal.substring(0, retVal.length() - 2);
	}

	/**
	 * Get the receive buffer.
	 * 
	 * @return the rxBuffer
	 */
	public byte[] getRxBuffer() {
		return rxBuffer;
	}

	/**
	 * Get the received data formatted as long "human readable" string
	 * containing 5 characters for each DoubleWord and without spaces in
	 * between; usefull to transfer this kind of data to DB systems.
	 * 
	 * @return the received data into a formatted and justified string (5
	 *         characters for each DoubleWord).
	 */
	public String getRxBufferFormattedString() {
		return getBufferFormattedString(rxBuffer, "", -1);
	}

	/**
	 * Get the received data formatted as "human readable" string containing the
	 * first 5 DoubleWord (5 characters for each DoubleWord), with a separator
	 * in between; usefull for logs activities.
	 * 
	 * @return the first 5 DoubleWord of received data into a formatted and
	 *         justified string (5 characters for each DoubleWord + a
	 *         separator).
	 */
	public String getRxBufferFormattedStringWithSeparator() {
		return getBufferFormattedString(rxBuffer, ",", 0);
	}

	/**
	 * Get the received data formatted as an array of "human readable" strings.
	 * 
	 * @return the received data into an array of strings.
	 */
	public String[] getRxBufferArrayOfStrings() {
		String[] retVal = new String[Store.RX_PACKET_LENGTH / 2];
		for (int i = 0; i < retVal.length; i++) {
			int num = intFromCoupleOfByte(rxBuffer[i], rxBuffer[i + 1]);
			retVal[i] = String.valueOf(num);
		}
		return retVal;
	}

	/**
	 * Set the receive buffer.
	 * 
	 * @param pRxBuffer
	 *            the rxBuffer to set
	 */
	public void setRxBuffer(byte[] pRxBuffer) {
		rxBuffer = pRxBuffer;
	}

	/**
	 * Set the RX_ANSWER as NAK
	 */
	public void setNAK() {
		rxBuffer[Store.RX_ANSWER] = 0;
		rxBuffer[Store.RX_ANSWER + 1] = 2;
	}

	/**
	 * Set the RX_ALCODE with the given value
	 * 
	 * @param pAlCode
	 */
	public void setAlCode(byte pAlCode) {
		rxBuffer[Store.RX_ALCODE] = 0;
		rxBuffer[Store.RX_ALCODE + 1] = pAlCode;
	}

	/**
	 * Get the transmission buffer.
	 * 
	 * @return the txBuffer
	 */
	public byte[] getTxBuffer() {
		return txBuffer;
	}

	/**
	 * Get the sent data formatted as long "human readable" string containing 5
	 * characters for each DoubleWord and without spaces in between; usefull to
	 * transfer this kind of data to DB systems.
	 * 
	 * @return the sent data into a formatted and justified string (5 characters
	 *         for each DoubleWord).
	 */
	public String getTxBufferFormattedString() {
		return getBufferFormattedString(txBuffer, "", -1);
	}

	/**
	 * Set the transmission buffer.
	 * 
	 * @param pTxBuffer
	 *            the txBuffer to set
	 */
	public void setTxBuffer(byte[] pTxBuffer) {
		txBuffer = pTxBuffer;
	}

	/**
	 * Set the transmission buffer using the given values.
	 * 
	 * @param pProg
	 *            the progressive number of the message.
	 * @param pStringsToSend
	 *            the message to send contained into an array of strings.
	 */
	public void setTxBuffer(int pProg, String[] pStringsToSend) {
		// set packet
		for (int i = 0; i < pStringsToSend.length; i++) {
			int num;
			try {
				num = Integer.parseInt(pStringsToSend[i]);
			} catch (NumberFormatException e) {
				num = 0;
			}
			ByteCouple bc = byteFromInteger(num);
			txBuffer[i * 2] = bc.high;
			txBuffer[i * 2 + 1] = bc.low;
		}
		ByteCouple bcProg = byteFromInteger(pProg);
		// set Index
		txBuffer[Store.TX_INDEX] = bcProg.high;
		txBuffer[Store.TX_INDEX + 1] = bcProg.low;
	}

	/**
	 * Get the last received Index; usefull to check if an answer is given to a
	 * sent message.
	 * 
	 * @return the last received Index.
	 */
	public int getReceivedIndex() {
		byte high = rxBuffer[Store.RX_INDEX];
		byte low = rxBuffer[Store.RX_INDEX + 1];
		int retVal = intFromCoupleOfByte(high, low);
		return retVal;
	}

	public int getReceivedAlCode() {
		byte high = rxBuffer[Store.RX_ALCODE];
		byte low = rxBuffer[Store.RX_ALCODE + 1];
		int retVal = intFromCoupleOfByte(high, low);
		return retVal;
	}

	public int getReceivedMaterial() {
		byte high = rxBuffer[Store.RX_MAT];
		byte low = rxBuffer[Store.RX_MAT + 1];
		int retVal = intFromCoupleOfByte(high, low);
		return retVal;
	}

	public int getReceivedX() {
		byte high = rxBuffer[Store.RX_LUN];
		byte low = rxBuffer[Store.RX_LUN + 1];
		int retVal = intFromCoupleOfByte(high, low);
		return retVal;
	}

	public int getReceivedY() {
		byte high = rxBuffer[Store.RX_LAR];
		byte low = rxBuffer[Store.RX_LAR + 1];
		int retVal = intFromCoupleOfByte(high, low);
		return retVal;
	}

	public int getReceivedZ() {
		byte high = rxBuffer[Store.RX_SPE];
		byte low = rxBuffer[Store.RX_SPE + 1];
		int retVal = intFromCoupleOfByte(high, low);
		return retVal;
	}

	public boolean isAck() {
		return rxBuffer[Store.RX_ANSWER + 1] == 1;
	}

	public boolean isNak() {
		return rxBuffer[Store.RX_ANSWER + 1] == 2;
	}

	public boolean isStrobe() {
		return rxBuffer[Store.RX_STROBE + 1] == 1;
	}

	/**
	 * Send this message using UDP on an DatagramSocket.
	 * 
	 * @param pSocket
	 *            the DatagramSocket to use for the communication.
	 * @param pHost
	 *            the InetAddress of the receiver.
	 * @param pPort
	 *            the port used by the receiver to listen UDP messages.
	 * @throws IOException
	 */
	public void sendUsingUdp(DatagramSocket pSocket, InetAddress pHost,
			int pPort) throws IOException {
		DatagramPacket sendPacket = new DatagramPacket(txBuffer,
				Store.TX_PACKET_LENGTH, pHost, pPort);
		pSocket.send(sendPacket);
	}

	/**
	 * Receive data listen to the indicated socket and fill the internal fields.
	 * 
	 * @param pSocket
	 *            the socket to listen for messages.
	 * 
	 * @throws SocketTimeoutException
	 * @throws IOException
	 */
	public void receiveUsingUdp(DatagramSocket pSocket)
			throws SocketTimeoutException, IOException {
		byte[] receivedData = new byte[Store.RX_PACKET_LENGTH];
		DatagramPacket receivePacket = new DatagramPacket(receivedData,
				receivedData.length);
		pSocket.receive(receivePacket);
		setRxBuffer(receivePacket.getData());
		// STORE STATUS
		// byte high = _rxBuffer[Store.W_STATE ];
		byte low = rxBuffer[Store.RX_STATUS + 1];
		if (low == 1) {
			store.setState(StoreState.MANUAL);
		} else if (low == 2) {
			store.setState(StoreState.AUTOMATIC_READY);
		} else if (low == 3) {
			store.setState(StoreState.AUTOMATIC_ALARM);
		} else if (low == 4) {
			store.setState(StoreState.OFFLINE);
		} else {
			store.setState(StoreState.UNDEFINED);
		}
	}
}

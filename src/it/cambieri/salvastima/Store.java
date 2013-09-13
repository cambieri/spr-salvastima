package it.cambieri.salvastima;

/**
 * This class rapresent a Stima storehouse.
 * 
 * @author Oscar Cambieri.
 * @version 02/12/2008.
 */
public class Store {
	/**
	 * The possible store states.
	 * 
	 */
	public enum StoreState {
		UNDEFINED, MANUAL, AUTOMATIC_READY, AUTOMATIC_ALARM, OFFLINE, TIMEOUT
	}

	/**
	 * The protocol constants.
	 */
	public static final short TX_PACKET_LENGTH = 16; // in bytes
	public static final short TX_MAT = 0;
	public static final short TX_LUN = 2;
	public static final short TX_LAR = 4;
	public static final short TX_SPE = 6;
	public static final short TX_SPARE_1 = 8;
	public static final short TX_SPARE_2 = 10;
	public static final short TX_SPARE_3 = 12;
	public static final short TX_INDEX = 14;

	public static final short RX_PACKET_LENGTH = 34; // in bytes
	public static final short RX_ANSWER = 0;
	public static final short RX_STATUS = 2;
	public static final short RX_ALCODE = 4;
	public static final short RX_SPARE_1 = 6;
	public static final short RX_SPARE_2 = 8;
	public static final short RX_SPARE_3 = 10;
	public static final short RX_SPARE_4 = 12;
	public static final short RX_INDEX = 14;
	public static final short RX_SPARE_5 = 16;
	public static final short RX_SPARE_6 = 18;
	public static final short RX_MAT = 20;
	public static final short RX_LUN = 22;
	public static final short RX_LAR = 24;
	public static final short RX_SPE = 26;
	public static final short RX_SPARE_7 = 28;
	public static final short RX_SPARE_8 = 30;
	public static final short RX_STROBE = 32;
	
	public static final Integer PING_0 = 32010;
	public static final Integer PING_1 = 32011;	

	/**
	 * Internal private fields
	 */
	private int id;
	private String description;
	private String ip;
	private int portNumber;
	private int myPortNumber;
	private int timeout;
	private StoreState state;
	private boolean logEnabled;

	/**
	 * A default empty constructor.
	 */
	public Store() {
		this(0, "", "", 0, 0, 0, StoreState.UNDEFINED, true);
	}

	/**
	 * A complete constructor requiring all the available fields.
	 * 
	 * @param pId
	 * @param pDescription
	 * @param pIp
	 * @param pPortNumber
	 * @param pMyPortNumber
	 * @param pTimeout
	 * @param pState
	 * @param logEnabled
	 * 
	 */
	public Store(int pId, String pDescription, String pIp, int pPortNumber,
			int pMyPortNumber, int pTimeout, StoreState pState, boolean pLogEnabled) {
		super();
		id = pId;
		description = pDescription;
		ip = pIp;
		portNumber = pPortNumber;
		myPortNumber = pMyPortNumber;
		timeout = pTimeout;
		state = pState;
		logEnabled = pLogEnabled;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param pId
	 *            the id to set
	 */
	public void setId(int pId) {
		id = pId;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param pDescription
	 *            the description to set
	 */
	public void setDescription(String pDescription) {
		description = pDescription;
	}

	/**
	 * @return the ip
	 */
	public String getIp() {
		return ip;
	}

	/**
	 * @param pIp
	 *            the ip to set
	 */
	public void setIp(String pIp) {
		ip = pIp;
	}

	/**
	 * @return the portNumber
	 */
	public int getPortNumber() {
		return portNumber;
	}

	/**
	 * @param pPortNumber
	 *            the portNumber to set
	 */
	public void setPortNumber(int pPortNumber) {
		portNumber = pPortNumber;
	}

	/**
	 * @return the myPortNumber
	 */
	public int getMyPortNumber() {
		return myPortNumber;
	}

	/**
	 * @param pMyPortNumber
	 *            the myPortNumber to set
	 */
	public void setMyPortNumber(int pMyPortNumber) {
		myPortNumber = pMyPortNumber;
	}

	/**
	 * @return the socket receive-message timeout
	 */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * @param timeout
	 *            the socket receive-message timeout to set
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	/**
	 * @return the state
	 */
	public StoreState getState() {
		return state;
	}

	/**
	 * @param pState
	 *            the state to set
	 */
	public void setState(StoreState pState) {
		state = pState;
	}

	/**
	 * @return the log enabling
	 */
	public boolean getLogEnabled() {
		return logEnabled;
	}

	/**
	 * @param pLogEnabled
	 *            the log enabling set
	 */
	public void setLogEnabled(boolean pLogEnabled) {
		logEnabled = pLogEnabled;
	}

	/**
	 * A brief description of this store.
	 * 
	 * @return a string with a concise representation of this store.
	 */
	@Override
	public String toString() {
		return "Store\tID: " + id + " - " + description;
	}

	/**
	 * A complete description of this object and all the stations inside it.
	 * 
	 * @return a long and complete description of this object.
	 */
	public String toStringExtended() {
		String retVal = "\n" + toString();
		retVal += "\n\tIP: " + ip + " - PORT: " + portNumber + " - MY_PORT: "
				+ myPortNumber;
		retVal += "\n\tTIMEOUT: " + timeout + " - STATE: " + state;
		return retVal;
	}
}
package client;

import opencard.core.service.*;
import opencard.core.terminal.*;
import opencard.core.util.*;
import opencard.opt.util.*;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;

import java.io.IOException;
import sun.misc.BASE64Encoder;
import sun.misc.BASE64Decoder;


public class TheClient extends Thread {

    private PassThruCardService servClient = null;
    boolean DISPLAY = true;

	private Socket socket;

	private Scanner inConsole, inNetwork;
	private PrintWriter outConsole, outNetwork;

	// Client command
	private static final int MSG = 0; // msg
	private static final int SENDFILE = 1; // sendFile login filename
	private static final int LOGOUT = 2; // /logout or /exit
	private static final int LIST = 15;
	private static final int PRIVMSG = 16;

	// secureApp.Server command
	private static final int CONNECTED = 3;
	private static final int ALREADYCONNECTED = 4;
	private static final int REGISTERED = 5;
	private static final int ERR_REGISTERED = 6;
	private static final int DISCONNECTED = 7;
	private static final int SENDFILESTOP = 9;
	private static final int FILETRANSFERMODEON = 10;
	private static final int FILETRANSFERMODEOFF = 11;
	private static final int ISUSERCONNECTED = 12;
	private static final int RECEIVERISCONNECTED = 13;
	private static final int RECEIVERISNOTCONNECTED = 14;

	private boolean isClientConnected = false;

	private boolean fileTransferMode = false;
	private boolean isReceiverConnected = false;
	private boolean checkReceiverState = false;

	private FileOutputStream fout = null;

	private static final byte CLA = (byte) 0x90;
	private static final byte P1 = (byte) 0x00;
	private static final byte P2 = (byte) 0x00;
	private static final byte INS_RSA_ENCRYPT = (byte) 0xA0;
	private static final byte INS_RSA_DECRYPT = (byte) 0xA2;

	private static short DMS_DES = 248; // DATA MAX SIZE for DES
    private static final byte INS_DES_DECRYPT = (byte) 0xB0;
    private static final byte INS_DES_ENCRYPT = (byte) 0xB2;

    public TheClient(String host, int port) throws IOException {
		initStream(host, port);
		authentication();
		start();
		listenConsole();
    }

    private ResponseAPDU sendAPDU(CommandAPDU cmd) {
	    return sendAPDU(cmd, true);
    }

	private ResponseAPDU sendAPDU(CommandAPDU cmd, boolean display) {
		ResponseAPDU result = null;
		try {
			result = this.servClient.sendCommandAPDU(cmd);
			if (display)
				displayAPDU(cmd, result);
		} catch (Exception e) {
			System.out.println("Exception caught in sendAPDU: " + e.getMessage());
			java.lang.System.exit(-1);
		}
		return result;
	}


    /************************************************
     * *********** BEGINNING OF TOOLS ***************
     * **********************************************/


    private String apdu2string( APDU apdu ) {
	    return removeCR( HexString.hexify( apdu.getBytes() ) );
    }


    public void displayAPDU( APDU apdu ) {
	System.out.println( removeCR( HexString.hexify( apdu.getBytes() ) ) + "\n" );
    }


    public void displayAPDU( CommandAPDU termCmd, ResponseAPDU cardResp ) {
	System.out.println( "--> Term: " + removeCR( HexString.hexify( termCmd.getBytes() ) ) );
	System.out.println( "<-- Card: " + removeCR( HexString.hexify( cardResp.getBytes() ) ) );
    }


    private String removeCR( String string ) {
	    return string.replace( '\n', ' ' );
    }

	private static boolean getExceptionMessage(String msg, String respCode) {
		boolean isClear = false;
		if (respCode.equals("90 00")) {
			System.out.println(msg + ": [SW_NO_ERROR] No Error\n");
			isClear = true;
		} else if (respCode.equals("63 47")) {
			System.out.println(msg + ": [DEBUG] Debug\n");
		} else if (respCode.equals("69 99")) {
			System.out.println(msg + ": [SW_APPLET_SELECT_FAILED] Applet selection failed\n");
		} else if (respCode.equals("61 00")) {
			System.out.println(msg + ": [SW_BYTES_REMAINING_00] Response bytes remaining\n");
		} else if (respCode.equals("6E 00")) {
			System.out.println(msg + ": [SW_CLA_NOT_SUPPORTED] CLA value not supported\n");
		} else if (respCode.equals("68 84")) {
			System.out.println(msg + ": [SW_COMMAND_CHAINING_NOT_SUPPORTED] Command chaining not supported\n");
		} else if (respCode.equals("69 86")) {
			System.out.println(msg + ": [SW_COMMAND_NOT_ALLOWED] Command not allowed (no current EF)\n");
		} else if (respCode.equals("69 85")) {
			System.out.println(msg + ": [SW_CONDITIONS_NOT_SATISFIED] Conditions of use not satisfied\n");
		} else if (respCode.equals("6C 00")) {
			System.out.println(msg + ": [SW_CORRECT_LENGTH_00] Correct Expected Length (Le)\n");
		} else if (respCode.equals("69 84")) {
			System.out.println(msg + ": [SW_DATA_INVALID] Data invalid\n");
		} else if (respCode.equals("6A 84")) {
			System.out.println(msg + ": [SW_FILE_FULL] Not enough memory space in the file\n");
		} else if (respCode.equals("69 83")) {
			System.out.println(msg + ": [SW_FILE_INVALID] File invalid\n");
		} else if (respCode.equals("6A 82")) {
			System.out.println(msg + ": [SW_FILE_NOT_FOUND] File not found\n");
		} else if (respCode.equals("6A 81")) {
			System.out.println(msg + ": [SW_FUNC_NOT_SUPPORTED] Function not supported\n");
		} else if (respCode.equals("6A 86")) {
			System.out.println(msg + ": [SW_INCORRECT_P1P2] Incorrect parameters (P1,P2)\n");
		} else if (respCode.equals("6D 00")) {
			System.out.println(msg + ": [SW_INS_NOT_SUPPORTED] INS value not supported\n");
		} else if (respCode.equals("68 83")) {
			System.out.println(msg + ": [SW_LAST_COMMAND_EXPECTED] Last command in chain expected\n");
		} else if (respCode.equals("68 81")) {
			System.out.println(msg
					+ ": [SW_LOGICAL_CHANNEL_NOT_SUPPORTED] Card does not support the operation on the specified logical channel\n");
		} else if (respCode.equals("6A 83")) {
			System.out.println(msg + ": [SW_RECORD_NOT_FOUND] Record not found\n");
		} else if (respCode.equals("68 82")) {
			System.out.println(msg + ": [SW_SECURE_MESSAGING_NOT_SUPPORTED] Card does not support secure messaging\n");
		} else if (respCode.equals("69 82")) {
			System.out.println(msg + ": [SW_SECURITY_STATUS_NOT_SATISFIED] Security condition not satisfied\n");
		} else if (respCode.equals("6F 00")) {
			System.out.println(msg + ": [SW_UNKNOWN] No precise diagnosis\n");
		} else if (respCode.equals("62 00")) {
			System.out.println(msg + ": [SW_WARNING_STATE_UNCHANGED] Warning, card state unchanged\n");
		} else if (respCode.equals("6A 80")) {
			System.out.println(msg + ": [SW_WRONG_DATA] Wrong data\n");
		} else if (respCode.equals("67 00")) {
			System.out.println(msg + ": [SW_WRONG_LENGTH] Wrong length\n");
		} else if (respCode.equals("6B 00")) {
			System.out.println(msg + ": [SW_WRONG_P1P2] Incorrect parameters (P1,P2)\n");
		} else {
			System.out.println(msg + ": Undefined Error code\n");
		}
		return isClear;
	}

    /******************************************
     * *********** END OF TOOLS ***************
     * ****************************************/


    private boolean selectApplet() {
	 boolean cardOk = false;
	 try {
	    CommandAPDU cmd = new CommandAPDU( new byte[] {
                (byte)0x00, (byte)0xA4, (byte)0x04, (byte)0x00, (byte)0x0A,
		(byte)0xA0, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x62, 
		(byte)0x03, (byte)0x01, (byte)0x0C, (byte)0x06, (byte)0x01
            } );
            ResponseAPDU resp = this.sendAPDU( cmd );
	    if( this.apdu2string( resp ).equals( "90 00" ) )
		    cardOk = true;
	 } catch(Exception e) {
            System.out.println( "Exception caught in selectApplet: " + e.getMessage() );
            java.lang.System.exit( -1 );
        }
	return cardOk;
    }


	/* secureApp.Server init & close */
	public void initStream(String host, int port) throws IOException {
		this.inConsole = new Scanner(System.in);
		this.outConsole = new PrintWriter(System.out);

		try {
			this.socket = new Socket(host, port);
			this.inNetwork = new Scanner(socket.getInputStream());
			this.outNetwork = new PrintWriter(socket.getOutputStream(), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void closeNetwork() throws IOException {
		this.outNetwork.close();
		this.socket.close();
		System.exit(0);
	}

	private void closeConsole() throws IOException {
		this.outConsole.close();
	}
	/*********/

	/* Tools */
	private void displayConsole(String raw) {
		this.outConsole.println(raw);
		this.outConsole.flush();
	}

	private void sendServer(String raw) {
		this.outNetwork.println(raw);
		this.outNetwork.flush();
	}

	private static byte[] shortToByteArray(short s) {
		return new byte[] { (byte) ((s & (short) 0xff00) >> 8), (byte) (s & (short) 0x00ff) };
	}

	private String toLowerCases(String s) {
		String r = "";
		for(char c: s.toCharArray()) {
			r += Character.toLowerCase(c);
		}
		return r;
	}

	private static short byteToShort(byte b) {
		return (short) (b & 0xff);
	}

	private static short byteArrayToShort(byte[] ba, short offset) {
		return (short) (((ba[offset] << 8)) | ((ba[(short) (offset + 1)] & 0xff)));
	}

	private static byte[] addPadding(byte[] data, long fileLength) {
		short paddingSize = (short) (8 - (fileLength % 8));
		byte[] paddingData = new byte[(short) (data.length + paddingSize)];

		System.arraycopy(data, 0, paddingData, 0, (short) data.length);
		for (short i = (short) data.length; i < (data.length + paddingSize); ++i)
			paddingData[i] = shortToByteArray(paddingSize)[1];

		return paddingData;
	}

	private static byte[] removePadding(byte[] paddingData) {
		short paddingSize = byteToShort(paddingData[paddingData.length - 1]);
		if (paddingSize > 8)
			return paddingData;

		/* check if padding exists */
		for (short i = (short) (paddingData.length - paddingSize); i < paddingData.length; ++i)
			if (paddingData[i] != (byte) paddingSize)
				return paddingData;

		/* Remove padding */
		short dataLength = (short) (paddingData.length - paddingSize);
		byte[] data = new byte[dataLength];
		System.arraycopy(paddingData, 0, data, 0, (short) dataLength);

		return data;
	}

	/*********/

	private boolean isUserconnected(String raw){
		String[] splitRaw = raw.split(" ");
		if (splitRaw.length == 3) {
			sendServer("<SYSTEM> [SENDFILE]: " + "ISUSERCONNECTED" + " " + splitRaw[1].trim() + " " + splitRaw[2].trim());
			return true;
		}else {
			displayConsole("<SYSTEM> [SENDFILE]: Bad arguments");
			return false;
		}
	}

	/* Features */
	private void sendFile(String raw) throws IOException {
		System.out.println("sendFile(): " + raw);
		if(!checkReceiverState){
			this.checkReceiverState = isUserconnected(raw);
		} else {
			if (this.isReceiverConnected) {
				String[] splitRaw = raw.split(" ");
				if (splitRaw.length == 8) {
					File f = new File("../src/secureApp/client/Files/" + splitRaw[7].trim());
					if (!f.exists()) {
						displayConsole("<SYSTEM> [SENDFILE]: File doesn't exist");
					} else {
						sendServer("/sendfile " + splitRaw[5] + " " + splitRaw[7]);
						FileInputStream fin = new FileInputStream(f);
						int by = 0;
						while ((by = fin.read()) != -1) {
							sendServer(String.valueOf(by));
						}		
						sendServer("<SYSTEM> [SENDFILE]: " + "SENDFILESTOP");
					}

					this.checkReceiverState = false;
					this.isReceiverConnected = false;
				} else
					displayConsole("<SYSTEM> [SENDFILE]: Bad arguments");
			}
		}
	}

	private synchronized void retrieveFile(String raw) throws IOException {
		if(raw.startsWith("<SYSTEM> [SENDFILE]: SENDFILESTART")){
			String[] splitRaw = raw.split(" ");
			this.fout = new FileOutputStream("../src/secureApp/client/Files/retrieved_" + splitRaw[4]);
		} else if (raw.startsWith("<SYSTEM> [SENDFILE]: SENDFILESTOP")) {
			this.fileTransferMode = false;
			this.fout.close();
		} else {
			this.fout.write(Byte.parseByte(String.valueOf(shortToByteArray(Short.parseShort(raw))[1]), 10));
		}
	}

	private void logout() {
		sendServer("/logout");
	}

	/*private void authentication() throws IOException {
		while(this.inNetwork.hasNextLine()) {
			String raw = this.inNetwork.nextLine().trim();
			displayConsole(raw);
			
			if (raw.startsWith("<SYSTEM> Enter your username") || raw.startsWith("Enter password:") || raw.startsWith("Confirm password:")){
				sendServer(this.inConsole.nextLine().trim());
			} else if(raw.startsWith("<SYSTEM> Connected as:")){
				this.isClientConnected = true;
				return;
			} else if (raw.startsWith("<SYSTEM> User connected limit reached") || raw.startsWith("<SYSTEM> Registration Successful") || raw.startsWith("<SYSTEM> Username or password is incorrect") || raw.startsWith("<SYSTEM> User already connected")){
				closeConsole();
				closeNetwork();
				return;
			}
		}
	}*/

	private boolean initNewCard(SmartCard card, byte[] challengeBytes) {
		if( card != null )
			System.out.println("Smartcard inserted\n");
		else {
			System.out.println("Did not get a smartcard");
			return false;
		}
		System.out.println("ATR: " + HexString.hexify( card.getCardID().getATR() ) + "\n");
	
		try {
			this.servClient = (PassThruCardService)card.getCardService( PassThruCardService.class, true);
		} catch(Exception e) {
			System.out.println(e.getMessage());
			return false;
		}
	
		System.out.println("Applet selecting...");
		if(!this.selectApplet()) {
			System.out.println("Wrong card, no applet to select!\n");
			System.exit( 1 );
			return false;
		} else {
			System.out.println("Applet selected\n");
			return true;
		}
	}

	private byte[] sendAndRetrieveChallengeApplet(byte[] challengeBytes) {
		// Send encrypted
		CommandAPDU cmd;
		ResponseAPDU resp;

		/* APDU
		*	CLA	INS	P1	P2	LC	DATA
		*	0	1	2	3	4	5
		*/

		int LC = challengeBytes.length;
		byte[] DATA = challengeBytes;

		byte[] cmd_b = new byte[LC + 6];
		cmd_b[0] = CLA;
		cmd_b[1] = INS_RSA_DECRYPT;
		cmd_b[2] = (byte)0xFF;
		cmd_b[3] = P2;
		cmd_b[4] = (byte) LC;
		System.arraycopy(DATA, 0, cmd_b, 5, LC);
		cmd_b[cmd_b.length - 1] = (byte) LC;

		cmd = new CommandAPDU(cmd_b);
		resp = this.sendAPDU(cmd, DISPLAY);

		if (getExceptionMessage("RSA DECRYPT ", this.apdu2string(resp).trim().substring(this.apdu2string(resp).trim().length() - 5))) {
			byte[] bytes = resp.getBytes();
			byte[] data = new byte[bytes.length - 2];
			System.arraycopy(bytes, 0, data, 0, bytes.length - 2);
		
			//BASE64Encoder encoder = new BASE64Encoder();
			//System.out.println("Unciphered by card is:\n" + encoder.encode(data).trim().replaceAll("\n", "").replaceAll("\r", "") + "\n");
			return data;
		} else
			return new byte[] {(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff}; 
	}

	private void authentication() throws IOException {
		while(this.inNetwork.hasNextLine()) {
			String raw = this.inNetwork.nextLine().trim();
			
			if (!raw.startsWith("<SYSTEM> AUTHENTICATION NEW"))
				displayConsole(raw);
			
			if (raw.startsWith("<SYSTEM> Enter your username")) {
				sendServer(this.inConsole.nextLine().trim());
			} else if (raw.startsWith("<SYSTEM> AUTHENTICATION NEW")) {
				String challengeBytesB64 = raw.split(" ")[3];
				BASE64Decoder decoder = new BASE64Decoder();
				byte[] challengeBytes = decoder.decodeBuffer(challengeBytesB64);

				try {
					SmartCard.start();
					System.out.print("Smartcard inserted?... "); 
					CardRequest cr = new CardRequest (CardRequest.ANYCARD, null, null); 
					SmartCard sm = SmartCard.waitForCard (cr);
				    
					if (sm != null) {
						System.out.println ("Got a SmartCard object!\n");
					} else
						System.out.println("Did not get a SmartCard object!\n");
				   
					if(this.initNewCard(sm, challengeBytes)) {
						try {
							BASE64Encoder encoder = new BASE64Encoder();
							String challengeBytesUncipheredB64 = encoder.encode(sendAndRetrieveChallengeApplet(challengeBytes)).trim().replaceAll("\n", "").replaceAll("\r", "");
							sendServer("<SYSTEM> AUTHENTICATION SOLVED " + challengeBytesUncipheredB64);
						} catch( Exception e ) {
							System.out.println( "initNewCard: " + e );
						}
					}
					SmartCard.shutdown();
				} catch( Exception e ) {
					System.out.println("TheClient error: " + e.getMessage());
					closeConsole();
					closeNetwork();
				}

			} else if(raw.startsWith("<SYSTEM> Connected as:")) {
				this.isClientConnected = true;
				return;
			} else if (raw.startsWith("<SYSTEM> Authentication error") || raw.startsWith("<SYSTEM> User connected limit reached") || raw.startsWith("<SYSTEM> Username is not registered") || raw.startsWith("<SYSTEM> User already connected")){
				closeConsole();
				closeNetwork();
				return;
			}
		}
	}

	private synchronized String encryptMsgB64(String raw) {
		CommandAPDU cmd;
		ResponseAPDU resp;

		byte[] bytes = raw.getBytes();
		int remainingBytes = bytes.length;
		byte[] res = new byte[remainingBytes];

		short i = 0;
		while (remainingBytes > DMS_DES) {
			System.out.println("in while loop ");
			byte[] data = new byte[DMS_DES];
			System.arraycopy(bytes, i * DMS_DES, data, 0, DMS_DES);

			byte[] payload = new byte[DMS_DES + 6];
			payload[0] = CLA;
			payload[1] = INS_DES_ENCRYPT;
			payload[2] = P1;
			payload[3] = P2;
			payload[4] = (byte) DMS_DES;
			System.arraycopy(data, 0, payload, 5, DMS_DES);
			payload[payload.length - 1] = (byte) DMS_DES;
			
			cmd = new CommandAPDU(payload);
			resp = this.sendAPDU(cmd, DISPLAY);

			if (getExceptionMessage("DES ENCRYPT ", this.apdu2string(resp).trim().substring(this.apdu2string(resp).trim().length() - 5))) {
				byte[] b = resp.getBytes();
				System.arraycopy(b, 0, res, i * DMS_DES, b.length - 2);
				++i; remainingBytes -= DMS_DES;
			}
		}

		System.out.println("end while loop ");
		byte[] data = new byte[remainingBytes];
		System.arraycopy(bytes, i * DMS_DES, data, 0, remainingBytes);

		System.out.println("before add padding ");
		data = addPadding(data, raw.getBytes().length);
		System.out.println("after add padding ");

		byte[] payload = new byte[data.length + 6];
		payload[0] = CLA;
		payload[1] = INS_DES_ENCRYPT;
		payload[2] = P1;
		payload[3] = P2;
		payload[4] = (byte) data.length;
		System.arraycopy(data, 0, payload, 5, data.length);
		payload[payload.length - 1] = (byte) data.length;
		
		cmd = new CommandAPDU(payload);
		displayAPDU(cmd);

		System.out.println("before send apdu");
		resp = this.sendAPDU(cmd, DISPLAY);
		System.out.println("after send apdu"); // TODO: GIGA BUG

		if (getExceptionMessage("DES ENCRYPT ", this.apdu2string(resp).trim().substring(this.apdu2string(resp).trim().length() - 5))) {
			byte[] b = resp.getBytes();
			System.arraycopy(b, 0, res, i * DMS_DES, b.length - 2);
			BASE64Encoder encoder = new BASE64Encoder();
			return encoder.encode(res);
		}

		return "Error";
	}

	/*********/

	private int serverParser(String text){
		if(text.startsWith("<SYSTEM> Disconnecting..."))
			return DISCONNECTED;
		else if(text.startsWith("<SYSTEM> [SENDFILE]: SENDFILESTART"))
			return FILETRANSFERMODEON;
		else if(text.startsWith("<SYSTEM> [SENDFILE]: SENDFILESTOP"))
			return FILETRANSFERMODEOFF;
		else if(text.startsWith("<SYSTEM> [SENDFILE]: User is connected")) // for sendfile receiver
			return RECEIVERISCONNECTED;
		else if(text.startsWith("<SYSTEM> [SENDFILE]: User is not connected")) //for sendfile receiver
			return RECEIVERISNOTCONNECTED;
		else
			return MSG;
	}

	private void listenNetwork() throws IOException {
		while(this.inNetwork.hasNextLine()) {
			String raw = this.inNetwork.nextLine().trim();
			if (!this.fileTransferMode)
				displayConsole(raw);
			switch (serverParser(raw)) {
				case FILETRANSFERMODEON:
					this.fileTransferMode = true;
					break;
				case RECEIVERISCONNECTED:
					this.isReceiverConnected = true;
					sendFile(raw);
					break;
				case RECEIVERISNOTCONNECTED:
					this.isReceiverConnected = false;
					this.checkReceiverState = false;
					break;
				case DISCONNECTED:
					closeConsole();
					closeNetwork();
					break;
			}
			if(this.fileTransferMode){
				retrieveFile(raw);
			}
		}
	}

	private int commandParser(String text){
		String command = toLowerCases(text.split(" ")[0]);
		if (command.equals("/sendfile"))
			return isClientConnected ? SENDFILE : MSG;
		else if(command.equals("/exit") || command.equals("/logout"))
			return isClientConnected ? LOGOUT : MSG;
		else if(command.equals("/list"))
			return isClientConnected ? LIST : MSG;
		else if(command.equals("/msg"))
			return isClientConnected ? PRIVMSG : MSG;
		else
			return MSG;
	}

	private void listenConsole() throws IOException {
		while(this.inConsole.hasNextLine()){
			String raw = this.inConsole.nextLine().trim();
			switch (commandParser(raw)) {
				case SENDFILE: sendFile(raw); break;
				case MSG: sendServer(encryptMsgB64(raw)); break;
				case LIST: sendServer(raw); break;
				case PRIVMSG: sendServer(raw); break;
				case LOGOUT: logout(); break;
			}
		}
	}

	@Override
	public void run() {
		try {
			listenNetwork();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    public static void main( String[] args ) throws InterruptedException, IOException {
	    new client.TheClient("localhost", 1234);
    }
}

package client;

import opencard.core.service.*;
import opencard.core.terminal.*;
import opencard.core.util.*;
import opencard.opt.util.*;

import java.io.*;
import java.net.Socket;
import java.util.Locale;
import java.util.Scanner;

import java.io.IOException;


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

	// secureApp.Server command
	private static final int CONNECTED = 3;
	private static final int ALREADYCONNECTED = 4;
	private static final int REGISTERED = 5;
	private static final int ERR_REGISTERED = 6;
	private static final int DISCONNECTED = 7;
	private static final int USERLIMITREACHED = 8;
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


    public TheClient(String host, int port) throws IOException {
		//initStream(host, port);
		//authentication();
		//start();
		//listenConsole();

	    try {
		    SmartCard.start();
		    System.out.print( "Smartcard inserted?... " ); 
		    
		    CardRequest cr = new CardRequest (CardRequest.ANYCARD,null,null); 
		    
		    SmartCard sm = SmartCard.waitForCard (cr);
		   
		    if (sm != null) {
			    System.out.println ("got a SmartCard object!\n");
		    } else
			    System.out.println( "did not get a SmartCard object!\n" );
		   
		    this.initNewCard( sm ); 
		    
		    SmartCard.shutdown();
	   
	    } catch( Exception e ) {
		    System.out.println( "TheClient error: " + e.getMessage() );
	    }
	    java.lang.System.exit(0) ;
    }

    private ResponseAPDU sendAPDU(CommandAPDU cmd) {
	    return sendAPDU(cmd, true);
    }

    private ResponseAPDU sendAPDU( CommandAPDU cmd, boolean display ) {
	    ResponseAPDU result = null;
	    try {
		result = this.servClient.sendCommandAPDU( cmd );
		if(display)
			displayAPDU(cmd, result);
	    } catch( Exception e ) {
           	 System.out.println( "Exception caught in sendAPDU: " + e.getMessage() );
           	 java.lang.System.exit( -1 );
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


    private void initNewCard( SmartCard card ) {
	if( card != null )
		System.out.println( "Smartcard inserted\n" );
	else {
		System.out.println( "Did not get a smartcard" );
		System.exit( -1 );
	}

	System.out.println( "ATR: " + HexString.hexify( card.getCardID().getATR() ) + "\n");


	try {
		this.servClient = (PassThruCardService)card.getCardService( PassThruCardService.class, true );
	} catch( Exception e ) {
		System.out.println( e.getMessage() );
	}

	System.out.println("Applet selecting...");
	if( !this.selectApplet() ) {
		System.out.println( "Wrong card, no applet to select!\n" );
		System.exit( 1 );
		return;
	} else 
		System.out.println( "Applet selected\n" );
       
	    byte[] cmd_ = {0,0,0,0};
            CommandAPDU cmd = new CommandAPDU( cmd_ );
	    System.out.println("Sending blank command APDU, nothing expected back");
            ResponseAPDU resp = this.sendAPDU( cmd, DISPLAY );
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
	/*********/

	private boolean isUserconnected(String raw){
		String[] splitRaw = raw.split(" ");
		System.out.println("couuucou + " + splitRaw.length);
		if (splitRaw.length == 3) {
			sendServer("<SYSTEM> [SENDFILE]: " + ISUSERCONNECTED + " " + splitRaw[1].trim() + " " + splitRaw[2].trim());
			return true;
		}else {
			displayConsole("<SYSTEM> [SENDFILE]: Bad arguments");
			return false;
		}
	}

	/* Features */
	private void sendFile(String raw) throws IOException {
		if(!checkReceiverState){
			this.checkReceiverState = isUserconnected(raw);
		} else {
			if (this.isReceiverConnected) {
				String[] splitRaw = raw.split(" ");
				System.out.println("coucou + " + splitRaw.length);
				if (splitRaw.length == 8) {
					File f = new File("./src/Client/Files/" + splitRaw[7].trim());
					if (!f.exists()) {
						displayConsole("<SYSTEM> [SENDFILE]: File doesn't exist");
					} else {
						sendServer("/sendfile " + splitRaw[5] + " " + splitRaw[7]);

						FileInputStream fin = new FileInputStream(f);
						int by = 0;
						while ((by = fin.read()) != -1) {
							sendServer(String.valueOf(by));
						}

						sendServer("<SYSTEM> [SENDFILE]: " + SENDFILESTOP);
					}

					this.checkReceiverState = false;
					this.isReceiverConnected = false;
				} else
					displayConsole("<SYSTEM> [SENDFILE]: Bad arguments");
			}
		}
	}

	private synchronized void retrieveFile(String raw) throws IOException {
		String[] splitRaw = raw.split(" ");
		if(raw.startsWith("<SYSTEM> [SENDFILE]: SENDFILESTART")){
			this.fout = new FileOutputStream("./src/Client/Files/retrieved_" + splitRaw[4]);
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

	private void authentication() throws IOException {
		while(this.inNetwork.hasNextLine()) {
			String raw = this.inNetwork.nextLine().trim();
			displayConsole(raw);
			if(raw.startsWith("<SYSTEM> Connected as:"))
				return;
			else if (raw.startsWith("<SYSTEM> Registration Successful") || raw.startsWith("<SYSTEM> Username or password is incorrect") || raw.startsWith("<SYSTEM> User already connected")){
				closeConsole();
				closeNetwork();
				return;
			}
			if (raw.startsWith("<SYSTEM> Enter your username") || raw.startsWith("Enter password:") || raw.startsWith("Confirm password:")){
				sendServer(this.inConsole.nextLine().trim());
			}
		}
	}

	/*********/

	private int serverParser(String text){
		if(text.startsWith("<SYSTEM> Connected as:"))
			return CONNECTED;
		else if(text.startsWith("<SYSTEM> Registration Successful"))
			return REGISTERED;
		else if(text.startsWith("<SYSTEM> Disconnecting..."))
			return DISCONNECTED;
		else if(text.startsWith("<SYSTEM> Username or password is incorrect"))
			return ERR_REGISTERED;
		else if(text.startsWith("<SYSTEM> User connected limit reached"))
			return USERLIMITREACHED;
		else if(text.startsWith("<SYSTEM> User already connected"))
			return ALREADYCONNECTED;
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
				case CONNECTED:
					this.isClientConnected = true;
					break;
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
				case REGISTERED: case ERR_REGISTERED: case DISCONNECTED: case USERLIMITREACHED: case ALREADYCONNECTED:
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
		else
			return MSG;
	}

	private void listenConsole() throws IOException {
		while(this.inConsole.hasNextLine()){
			String raw = this.inConsole.nextLine().trim();
			switch (commandParser(raw)) {
				case SENDFILE:
					sendFile(raw);
					break;
				case MSG:
					sendServer(raw);
					break;
				case LOGOUT:
					logout();
					break;
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

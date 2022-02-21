package applet;




import javax.sound.sampled.DataLine;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.*;




public class TheApplet extends Applet {

	private final static byte CLA_TEST                    = (byte)0x90;
	private final static byte INS_GENERATE_RSA_KEY        = (byte)0xF6;
	private final static byte INS_RSA_ENCRYPT             = (byte)0xA0;
	private final static byte INS_RSA_DECRYPT             = (byte)0xA2;
	private final static byte INS_GET_PUBLIC_RSA_KEY      = (byte)0xFE;
	private final static byte INS_PUT_PUBLIC_RSA_KEY      = (byte)0xF4;

	// cipher instances
	private Cipher cRSA_NO_PAD;
	// key objects
	private KeyPair keyPair;
	private Key publicRSAKey, privateRSAKey;

	// cipher key length
	private short cipherRSAKeyLength;




	// RSA Keys section

	// n = modulus
	static final byte[] n = new byte[] {
		(byte)0x90,(byte)0x08,(byte)0x15,(byte)0x32,(byte)0xb3,(byte)0x6a,(byte)0x20,(byte)0x2f,
		(byte)0x40,(byte)0xa7,(byte)0xe8,(byte)0x02,(byte)0xac,(byte)0x5d,(byte)0xec,(byte)0x11,
		(byte)0x1d,(byte)0xfa,(byte)0xf0,(byte)0x6b,(byte)0x1c,(byte)0xb7,(byte)0xa8,(byte)0x39,
		(byte)0x19,(byte)0x50,(byte)0x9c,(byte)0x44,(byte)0xed,(byte)0xa9,(byte)0x51,(byte)0x01,
		(byte)0x0f,(byte)0x11,(byte)0xd6,(byte)0xa3,(byte)0x60,(byte)0xa7,(byte)0x7e,(byte)0x95,
		(byte)0xa2,(byte)0xfa,(byte)0xe0,(byte)0x8d,(byte)0x62,(byte)0x5b,(byte)0xf2,(byte)0x62,
		(byte)0xa2,(byte)0x64,(byte)0xfb,(byte)0x39,(byte)0xb0,(byte)0xf0,(byte)0x6f,(byte)0xa2,
		(byte)0x23,(byte)0xae,(byte)0xbc,(byte)0x5d,(byte)0xd0,(byte)0x1a,(byte)0x68,(byte)0x11,
		(byte)0xa7,(byte)0xc7,(byte)0x1b,(byte)0xda,(byte)0x17,(byte)0xc7,(byte)0x14,(byte)0xab,
		(byte)0x25,(byte)0x92,(byte)0xbf,(byte)0xcc,(byte)0x81,(byte)0x65,(byte)0x7a,(byte)0x08,
		(byte)0x90,(byte)0x59,(byte)0x7f,(byte)0xc4,(byte)0xf9,(byte)0x43,(byte)0x9c,(byte)0xaa,
		(byte)0xbe,(byte)0xe4,(byte)0xf8,(byte)0xfb,(byte)0x03,(byte)0x74,(byte)0x3d,(byte)0xfb,
		(byte)0x59,(byte)0x7a,(byte)0x56,(byte)0xa3,(byte)0x19,(byte)0x66,(byte)0x43,(byte)0x77,
		(byte)0xcc,(byte)0x5a,(byte)0xae,(byte)0x21,(byte)0xf5,(byte)0x20,(byte)0xa1,(byte)0x22,
		(byte)0x8f,(byte)0x3c,(byte)0xdf,(byte)0xd2,(byte)0x03,(byte)0xe9,(byte)0xc2,(byte)0x38,
		(byte)0xe7,(byte)0xd9,(byte)0x38,(byte)0xef,(byte)0x35,(byte)0x82,(byte)0x48,(byte)0xb7
	};

	// e = public exponent
	static final byte[] e = new byte[] {
		(byte)0x01,(byte)0x00,(byte)0x01
	};

	// d = private exponent
	static final byte[] d = new byte[] {
		(byte)0x69,(byte)0xdf,(byte)0x67,(byte)0x25,(byte)0xa3,(byte)0xb8,(byte)0x88,(byte)0xfb,
		(byte)0xf2,(byte)0xfc,(byte)0xf9,(byte)0x90,(byte)0xad,(byte)0x7f,(byte)0x44,(byte)0xbd,
		(byte)0xb8,(byte)0x59,(byte)0xf3,(byte)0x4b,(byte)0xe9,(byte)0x0a,(byte)0x1f,(byte)0x80,
		(byte)0x09,(byte)0x59,(byte)0xb5,(byte)0xe4,(byte)0xfd,(byte)0x06,(byte)0x0e,(byte)0xe3,
		(byte)0x46,(byte)0x5e,(byte)0x88,(byte)0x76,(byte)0x03,(byte)0xe0,(byte)0x5b,(byte)0x2e,
		(byte)0x47,(byte)0x65,(byte)0x3e,(byte)0x96,(byte)0xef,(byte)0x0c,(byte)0x43,(byte)0x79,
		(byte)0xb9,(byte)0x81,(byte)0x9d,(byte)0x21,(byte)0xe5,(byte)0x2c,(byte)0x78,(byte)0x02,
		(byte)0xa9,(byte)0x54,(byte)0x12,(byte)0x66,(byte)0xab,(byte)0x48,(byte)0x1d,(byte)0xe2,
		(byte)0x6e,(byte)0x1d,(byte)0x7d,(byte)0xb2,(byte)0xce,(byte)0x7a,(byte)0x3f,(byte)0xbb,
		(byte)0x34,(byte)0xf2,(byte)0x46,(byte)0x5f,(byte)0x73,(byte)0x7c,(byte)0xba,(byte)0xf8,
		(byte)0xc1,(byte)0x29,(byte)0x97,(byte)0x85,(byte)0x67,(byte)0xdf,(byte)0x82,(byte)0x87,
		(byte)0x89,(byte)0x61,(byte)0x42,(byte)0xcc,(byte)0x1d,(byte)0xcc,(byte)0x03,(byte)0xce,
		(byte)0x41,(byte)0x7d,(byte)0x8f,(byte)0x25,(byte)0xc1,(byte)0x61,(byte)0xfe,(byte)0x06,
		(byte)0x4f,(byte)0x1a,(byte)0xf2,(byte)0x48,(byte)0x55,(byte)0xd8,(byte)0x6e,(byte)0xc6,
		(byte)0x3f,(byte)0x6d,(byte)0xe1,(byte)0xce,(byte)0xa9,(byte)0x28,(byte)0x9e,(byte)0x03,
		(byte)0x2d,(byte)0x74,(byte)0x59,(byte)0x1c,(byte)0xdb,(byte)0x18,(byte)0xb3,(byte)0x41
	};

	protected TheApplet() {
		publicRSAKey = privateRSAKey = null;
		cRSA_NO_PAD = null;

		cipherRSAKeyLength = KeyBuilder.LENGTH_RSA_1024;
		// build RSA pattern keys
		publicRSAKey = KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, cipherRSAKeyLength, true);
		privateRSAKey = KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PRIVATE, cipherRSAKeyLength, false);
		// initialize RSA public key
		((RSAPublicKey)publicRSAKey).setModulus(n, (short)0, (short)(cipherRSAKeyLength/8));
		((RSAPublicKey)publicRSAKey).setExponent(e, (short)0, (short)e.length);
		// initialize RSA private key
		((RSAPrivateKey)privateRSAKey).setModulus(n, (short)0, (short)(cipherRSAKeyLength/8));
		((RSAPrivateKey)privateRSAKey).setExponent(d, (short)0, (short)(cipherRSAKeyLength/8));
		// get cipher RSA instance
		cRSA_NO_PAD = Cipher.getInstance((byte)0x0C, false );

		register();

	}


	public static void install(byte[] bArray, short bOffset, byte bLength) throws ISOException {
		new TheApplet();
	}


	private static short byteToShort(byte b) {
        return (short) (b & 0xff);
    }

    // for 2 consecutive bytes
    private static short byteArrayToShort(byte[] ba, short offset) {
        return (short) (((ba[offset] << 8)) | ((ba[(short) (offset + 1)] & 0xff)));
    }

    private static byte[] shortToByteArray(short s) {
        return new byte[] { (byte) ((s & (short) 0xff00) >> 8), (byte) (s & (short) 0x00ff) };
    }

	public void process(APDU apdu) throws ISOException {
		if (selectingApplet())
			return ;

		byte[] buffer = apdu.getBuffer();

		if (buffer[ISO7816.OFFSET_CLA] != CLA_TEST)
			ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);

		switch(buffer[ISO7816.OFFSET_INS]) {
			case INS_GENERATE_RSA_KEY: generateRSAKey(); break;
			case INS_RSA_ENCRYPT: RSAEncrypt(apdu); break;
			case INS_RSA_DECRYPT: RSADecrypt(apdu); break;
			case INS_GET_PUBLIC_RSA_KEY: getPublicRSAKey(apdu); break;
			case INS_PUT_PUBLIC_RSA_KEY: putPublicRSAKey(apdu); break;
			default: ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
		}
	}


	void generateRSAKey() {
		keyPair = new KeyPair(KeyPair.ALG_RSA, (short)publicRSAKey.getSize());
		keyPair.genKeyPair();
		publicRSAKey = keyPair.getPublic();
		privateRSAKey = keyPair.getPrivate();
	}


	// RSA Encrypt (with public key)
	void RSAEncrypt(APDU apdu) {
		apdu.setIncomingAndReceive();
		byte[] buffer = apdu.getBuffer();
		cRSA_NO_PAD.init(publicRSAKey, Cipher.MODE_ENCRYPT);
		short dataLength = (short)(byteToShort(buffer[ISO7816.OFFSET_LC]));
		cRSA_NO_PAD.doFinal(buffer, (short)5, dataLength, buffer, (short)0);
		apdu.setOutgoingAndSend((short)0, dataLength);
	}


	// RSA Decrypt (with private key)
	void RSADecrypt(APDU apdu) {
		apdu.setIncomingAndReceive();
		byte[] buffer = apdu.getBuffer();
		cRSA_NO_PAD.init(privateRSAKey, Cipher.MODE_DECRYPT);
		short dataLength = (short)(byteToShort(buffer[ISO7816.OFFSET_LC]));
		cRSA_NO_PAD.doFinal(buffer, (short)5, dataLength, buffer, (short)0);
		apdu.setOutgoingAndSend((short)0, dataLength);
	}


	void getPublicRSAKey(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		// get the element type and length
		byte keyElement = (byte)(buffer[ISO7816.OFFSET_P2] & 0xFF);
		// check correct type (modulus or exponent)
		if((keyElement != 0x00) && (keyElement != 0x01))
			ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
		// check elements request
		if(keyElement == 0) {
			// retrieve modulus
			buffer[0] = (byte)((RSAPublicKey)publicRSAKey).getModulus(buffer, (short)1);
		} else
			// retrieve exponent
			buffer[0] = (byte)((RSAPublicKey)publicRSAKey).getExponent(buffer, (short)1);
		// send the key element
		apdu.setOutgoingAndSend((short)0, (short)((buffer[0] & 0xFF) + 1));
	}


	void putPublicRSAKey(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		// get the element type and length
		byte keyElement = (byte)(buffer[ISO7816.OFFSET_P1] & 0xFF);
		short publicValueLength = (short)(buffer[ISO7816.OFFSET_LC] & 0xFF);
		// check correct type (modulus or exponent)
		if((keyElement != 0x00) && (keyElement != 0x01))
			ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
		// use data in
		apdu.setIncomingAndReceive();
		// initialize RSA public key
		// check elements length for modulus only because exponent is naturaly short
		if(keyElement == 0) {
			// loading modulus
			if(publicValueLength != (short)(cipherRSAKeyLength/8))
				ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
			// initialize modulus
			((RSAPublicKey)publicRSAKey).setModulus(buffer, (short)ISO7816.OFFSET_CDATA, (short)(buffer[ISO7816.OFFSET_LC] & 0xFF));
		} else
			// initialize exponent
			((RSAPublicKey)publicRSAKey).setExponent(buffer, (short)ISO7816.OFFSET_CDATA, (short)(buffer[ISO7816.OFFSET_LC] & 0xFF));
	}
}

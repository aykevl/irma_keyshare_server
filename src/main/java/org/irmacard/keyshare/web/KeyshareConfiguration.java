package org.irmacard.keyshare.web;

import com.google.gson.JsonSyntaxException;
import org.irmacard.api.common.util.GsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.SignatureAlgorithm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class KeyshareConfiguration {
	private static Logger logger = LoggerFactory.getLogger(KeyshareConfiguration.class);

	private static final String filename = "config.json";
	private static KeyshareConfiguration instance;

	private String server_name = "IRMATestCloud";
	private String human_readable_name;

	private String jwt_privatekey = "sk.der";
	private String jwt_publickey = "pk.der";

	private int pinExpiry = 900; // 15 minutes

	private String mail_user = "";
	private String mail_password = "";
	private String mail_host = "";
	private String mail_from = "";
	private int mail_port = 587;

	private String webclient_url = "";

	private String scheme_manager = "";
	private String email_issuer = "";
	private String email_credential = "";
	private String email_attribute = "";
	private String email_login_credential = "";
	private String email_login_attribute = "";

	private String login_email_subject = "Log in on the keyshare server";
	private String login_email_body = "Click on the link below to log in on the keyshare server.";
	private String register_email_subject = "Verify your email address";
	private String register_email_body = "To finish registering to the keyshare server, please click on the link below.";

	private int session_timeout = 30;
	private int rate_limit = 3;

	private String apiserver_publickey = "apiserver.der";

	private transient PrivateKey jwtPrivateKey;
	private transient PublicKey jwtPublicKey;

	public KeyshareConfiguration() {}

	/**
	 * Reloads the configuration from disk so that {@link #getInstance()} returns the updated version
	 */
	public static void load() {
		// TODO: GSon seems to always be lenient (i.e. allow comments in the JSon), even though
		// the documentation states that by default, it is not lenient. Why is this? Could change?
		try {
			String json = new String(getResource(filename));
			instance = GsonUtil.getGson().fromJson(json, KeyshareConfiguration.class);
		} catch (IOException |JsonSyntaxException e) {
			logger.warn("Could not load configuration file. Using default values (may not work!)");
			e.printStackTrace();
			instance = new KeyshareConfiguration();
		}
	}

	public static KeyshareConfiguration getInstance() {
		if (instance == null)
			load();

		return instance;
	}

	public int getPinExpiry() {
		return pinExpiry;
	}

	public void setPinExpiry(int pinExpiry) {
		this.pinExpiry = pinExpiry;
	}

	public String getMailUser() {
		return mail_user;
	}

	public String getMailPassword() {
		return mail_password;
	}

	public String getMailHost() {
		return mail_host;
	}

	public int getMailPort() {
		return mail_port;
	}

	public String getMailFrom() {
		return mail_from;
	}

	public String getWebclientUrl() {
		return webclient_url;
	}

	public boolean isHttpsEnabled() {
		return webclient_url.startsWith("https://");
	}

	public String getSchemeManager() {
		return scheme_manager;
	}

	public String getEmailIssuer() {
		return email_issuer;
	}

	public String getEmailCredential() {
		return email_credential;
	}

	public String getEmailAttribute() {
		return email_attribute;
	}

	public String getEmailLoginCredential() {
		return email_login_credential;
	}

	public String getEmailLoginAttribute() {
		return email_login_attribute;
	}

	public String getLoginEmailSubject() {
		return login_email_subject;
	}

	public String getLoginEmailBody() {
		return login_email_body;
	}

	public String getRegisterEmailSubject() {
		return register_email_subject;
	}

	public String getRegisterEmailBody() {
		return register_email_body;
	}

	public int getSessionTimeout() {
		return session_timeout;
	}

	public int getRateLimit() {
		return rate_limit;
	}

	private static PublicKey parsePublicKey(byte[] bytes) throws KeyManagementException {
		try {
			if (bytes == null || bytes.length == 0)
				throw new KeyManagementException("Could not read public key");

			X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);

			return KeyFactory.getInstance("RSA").generatePublic(spec);
		} catch (NoSuchAlgorithmException|InvalidKeySpecException e) {
			throw new KeyManagementException(e);
		}
	}

	public static PrivateKey parsePrivateKey(byte[] bytes) throws KeyManagementException {
		try {
			if (bytes == null || bytes.length == 0)
				throw new KeyManagementException("Could not read private key");

			PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);

			return KeyFactory.getInstance("RSA").generatePrivate(spec);
		} catch (NoSuchAlgorithmException|InvalidKeySpecException e) {
			throw new KeyManagementException(e);
		}
	}

	public static byte[] getResource(String filename) throws IOException {
		URL url = KeyshareConfiguration.class.getClassLoader().getResource(filename);
		if (url == null)
			throw new IOException("Could not load file " + filename);

		URLConnection urlCon = url.openConnection();
		urlCon.setUseCaches(false);
		return convertSteamToByteArray(urlCon.getInputStream(), 2048);
	}

	public static byte[] convertSteamToByteArray(InputStream stream, int size) throws IOException {
		byte[] buffer = new byte[size];
		ByteArrayOutputStream os = new ByteArrayOutputStream();

		int line;
		while ((line = stream.read(buffer)) != -1) {
			os.write(buffer, 0, line);
		}
		stream.close();

		os.flush();
		os.close();
		return os.toByteArray();
	}

	@Override
	public String toString() {
		return GsonUtil.getGson().toJson(this);
	}

	public PrivateKey getJwtPrivateKey() {
		if (jwtPrivateKey == null) {
			try {
				jwtPrivateKey = parsePrivateKey(getResource(jwt_privatekey));
			} catch (KeyManagementException|IOException e) {
				throw new RuntimeException(e);
			}
		}

		return jwtPrivateKey;
	}

	public String getServerName() {
		return server_name;
	}

	public String getHumanReadableName() {
		if (human_readable_name == null || human_readable_name.length() == 0)
			return server_name;
		else
			return human_readable_name;
	}

	public PublicKey getApiServerPublicKey() {
		try {
			return parsePublicKey(getResource(apiserver_publickey));
		} catch (KeyManagementException|IOException e) {
			throw new RuntimeException(e);
		}
	}

	public PublicKey getJwtPublicKey() {
		if (jwtPublicKey == null) {
			try {
				jwtPublicKey = parsePublicKey(getResource(jwt_publickey));
			} catch (KeyManagementException|IOException e) {
				throw new RuntimeException(e);
			}
		}

		return jwtPublicKey;
	}

	public SignatureAlgorithm getJwtAlgorithm() {
		return SignatureAlgorithm.RS256;
	}
}

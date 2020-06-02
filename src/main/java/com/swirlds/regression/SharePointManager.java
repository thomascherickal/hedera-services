/*
 * (c) 2016-2019 Swirlds, Inc.
 *
 * This software is the confidential and proprietary information of
 * Swirlds, Inc. ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Swirlds.
 *
 * SWIRLDS MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. SWIRLDS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 */

package com.swirlds.regression;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SharePointManager {

	private static final Logger log = LogManager.getLogger(SharePointManager.class);
	private static final Marker MARKER = MarkerManager.getMarker("REGRESSION_TESTS");
	private static final Marker ERROR = MarkerManager.getMarker("EXCEPTION");

	private final static String BASE_UPLOAD_FOLDER = "/sites/Engineering";
	private final static String ATF_FOLDER = "Shared%20Documents/General/ATF%20Nightly%20Results";
	private static final String RTFA_SEARCH_STRING = "rtFa=";
	private static final String FEDAUTH_SEARCH_STRING = "FedAuth=";
	private static final String FORM_DIGEST = BASE_UPLOAD_FOLDER + "/_api/contextinfo";
	private static final String LIST_FILES = BASE_UPLOAD_FOLDER + "/_api/web/lists?$select=ID,Title";
	private static final String BASE_SITE = "hederatest.sharepoint.com";
	private static final String BASE_SITE_URL = "https://" + BASE_SITE;

	private final String sts = "https://login.microsoftonline.com/extSTS.srf";
	private final String loginContextPath = "/_forms/default.aspx?wa=wsignin1.0";
	private final String sharepointContext = BASE_SITE_URL;
	private final String username = "atfnightly@hedera.com";
	private final String password = "fvi42OKhJrYCcXgS";
	private final String DOMAIN = "hederatest";
	private final String reqXML =
			"<?xml version=\"1.0\" encoding=\"utf-8\" ?>" +
					"<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:a=\"http://www.w3" +
					".org/2005/08/addressing\" xmlns:u=\"http://docs.oasis-open" +
					".org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">" +
					"<s:Header>" +
					"<a:Action s:mustUnderstand=\"1\">http://schemas.xmlsoap" +
					".org/ws/2005/02/trust/RST/Issue</a:Action>" +
					"<a:ReplyTo>" +
					"<a:Address>http://www.w3.org/2005/08/addressing/anonymous</a:Address>" +
					"</a:ReplyTo>" +
					"<a:To s:mustUnderstand=\"1\">https://login.microsoftonline.com/extSTS.srf</a:To>" +
					"<o:Security s:mustUnderstand=\"1\" xmlns:o=\"http://docs.oasis-open" +
					".org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">" +
					"<o:UsernameToken>" +
					"<o:Username>[username]</o:Username>" +
					"<o:Password>[password]</o:Password>" +
					"</o:UsernameToken>" +
					"</o:Security>" +
					"</s:Header>" +
					"<s:Body>" +
					"<t:RequestSecurityToken xmlns:t=\"http://schemas.xmlsoap.org/ws/2005/02/trust\">" +
					"<wsp:AppliesTo xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">" +
					"<a:EndpointReference>" +
					"<a:Address>[endpoint]</a:Address>" +
					"</a:EndpointReference>" +
					"</wsp:AppliesTo>" +
					"<t:KeyType>http://schemas.xmlsoap.org/ws/2005/05/identity/NoProofKey</t:KeyType>" +
					"<t:RequestType>http://schemas.xmlsoap.org/ws/2005/02/trust/Issue</t:RequestType>" +
					"<t:TokenType>urn:oasis:names:tc:SAML:1.0:assertion</t:TokenType>" +
					"</t:RequestSecurityToken>" +
					"</s:Body>" +
					"</s:Envelope>";

	private String token;
	private ArrayList<String> cookieValues;
	private String digestValue;

	private String generateSAML() {
		String saml = reqXML
				.replace("[username]", username);
		saml = saml.replace("[password]", password);
		saml = saml.replace("[endpoint]", sharepointContext + loginContextPath);
		return saml;
	}

	public void login() {
		try {
			token = requestToken();
			cookieValues = submitToken(token);
			digestValue = getFormDigest();

		} catch (Exception e) {
			log.error(ERROR, "unable to login to SharePoint", e);
		}
	}

	private String getFormDigest() {
		String formDigestResponce = post(FORM_DIGEST, null, false);
		log.info(MARKER, "raw Json: {} end", formDigestResponce);

		ObjectMapper mapper = new ObjectMapper();
		try {
			Object json = mapper.readValue(formDigestResponce, Object.class);

			JsonNode formDigestNode = mapper.readTree(formDigestResponce);
			String digestValue =
					formDigestNode.get("d").get("GetContextWebInformation").get("FormDigestValue").asText();
			log.info(MARKER, "digetValue: {}", digestValue);
			return digestValue;
		} catch (IOException e) {
			log.error(ERROR, "Could not read form digest value from SharePoint Return.", e);
		}
		return null;
	}

	private String post(String apiPath, String param, boolean isMerge) {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {

			// log.info(MARKER, "HTTP Post Address: {}", sharepointContext + apiPath);
			HttpPost postRequest = new HttpPost(sharepointContext + apiPath);
			postRequest.addHeader("Cookie", cookieValues.get(0) + ";" + cookieValues.get(1));
			postRequest.addHeader("accept", "application/json;odata=verbose");
			postRequest.addHeader("content-type", "application/json;odata=verbose");
			postRequest.addHeader("X-RequestDigest", digestValue);
			postRequest.addHeader("IF-MATCH", "*");
			if (isMerge) {
				postRequest.addHeader("X-HTTP-Method", "MERGE");
			}

			ArrayList<NameValuePair> nameValues = new ArrayList<>();
			if (param != null) {
				StringEntity input = new StringEntity(param, "UTF-8");
				input.setContentType("application/json");
				postRequest.setEntity(input);
			}

			HttpResponse response = httpClient.execute(postRequest);
			if (response.getStatusLine().getStatusCode() != 201 && response.getStatusLine().getStatusCode() != 200 && response.getStatusLine().getStatusCode() != 204) {
				log.error(ERROR, "Sharepoint connection failed: HTTP Error Code: {} responce: {} for api path {}",
						response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase(), apiPath);
			}
			if (response.getEntity() == null || response.getEntity().getContent() == null) {
				log.error(ERROR, "HTTP error responce was empty: api path: {}", apiPath);
				return null;
			} else {
				String returnString = IOUtils.toString(response.getEntity().getContent(), "utf-8");
				if (returnString.isEmpty()) {
					log.error(ERROR, "sharePoint responce was emptyapi path{}, params{}, digestValue {}", apiPath,
							param,
							digestValue);
				}
				return returnString;
			}
		} catch (IOException e) {
			/* catches: ClientProtocolException | IOException */
			log.error(ERROR, "Could not get share point to respond to api path{}, params{}, digestValue {}", apiPath,
					param, digestValue, e);
		} finally {
			try {
				httpClient.close();
			} catch (IOException ex) {
				log.error(ERROR, "Failed to close http client connection to SharePoint", ex);
			}
		}
		return null;
	}

	private String get(String apiPath, boolean isEngineering) {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			String baseFunctionalFolder = "";
			if (isEngineering) {
				baseFunctionalFolder = BASE_UPLOAD_FOLDER;
			}
			log.info(MARKER, "HTTP Post Address: {}", sharepointContext + baseFunctionalFolder + apiPath);
			HttpGet getRequest = new HttpGet(sharepointContext + apiPath);
			getRequest.addHeader("Cookie", cookieValues.get(0) + ";" + cookieValues.get(1));
			getRequest.addHeader("accept", "application/json;odata=verbose");

			HttpResponse response = httpClient.execute(getRequest);
			if (response.getStatusLine().getStatusCode() != 200 && response.getStatusLine().getStatusCode() != 204) {
				log.error(ERROR, "Sharepoint connection failed: HTTP Error Code: {} responce: {} for api path {}",
						response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase(), apiPath);
			}
			if (response.getEntity() == null || response.getEntity().getContent() == null) {
				log.error(ERROR, "HTTP error responce was empty: api path: {}", apiPath);
				return null;
			} else {
				log.info(MARKER, "length {}, is streaming: {}", response.getEntity().getContentLength(),
						response.getEntity().isStreaming());
				String returnString = IOUtils.toString(response.getEntity().getContent(), "utf-8");
				if (returnString.isEmpty()) {
					log.error(ERROR, "sharePoint responce was emptyapi path{}, params{}, digestValue {}", apiPath,
							digestValue);
				}
				return returnString;
			}
		} catch (IOException e) {
			/* catches: ClientProtocolException | IOException */
			log.error(ERROR, "Could not get share point to respond to api path{}, params{}, digestValue {}", apiPath,
					digestValue, e);
		} finally {
			try {
				httpClient.close();
			} catch (IOException ex) {
				log.error(ERROR, "Failed to close http client connection to SharePoint", ex);
			}
		}
		return null;
	}

	public String requestToken() throws XPathExpressionException, SAXException,
			ParserConfigurationException, IOException {

		String saml = generateSAML();

		URL u = new URL(sts);
		URLConnection uc = u.openConnection();
		HttpURLConnection connection = (HttpURLConnection) uc;

		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestMethod("POST");

		connection.addRequestProperty("Content-Type", "text/xml; charset=utf-8");

		OutputStream out = connection.getOutputStream();
		Writer wout = new OutputStreamWriter(out);
		wout.write(saml);

		wout.flush();
		wout.close();

		InputStream in = connection.getInputStream();
		int c;
		StringBuilder sb = new StringBuilder();
		while ((c = in.read()) != -1)
			sb.append((char) (c));
		in.close();
		String result = sb.toString();
		String token = extractToken(result);
		log.trace(MARKER, "Token = {}", token);
		return token;
	}

	private String extractToken(
			String result) throws SAXException, IOException, ParserConfigurationException, XPathExpressionException {

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();

		Document document = db.parse(new InputSource(new StringReader(result)));

		XPathFactory xpf = XPathFactory.newInstance();
		XPath xp = xpf.newXPath();
		String token = xp.evaluate("//BinarySecurityToken/text()", document.getDocumentElement());
		log.trace(MARKER, "Token == {}", token);
		return token;
	}

	private ArrayList<String> submitToken(String token) throws IOException {

		String url = sharepointContext + loginContextPath;
		URL u = new URL(url);
		URLConnection uc = u.openConnection();
		HttpURLConnection connection = (HttpURLConnection) uc;

		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestMethod("POST");
		connection.addRequestProperty("Accept", "application/x-www-form-urlencoded");
		connection.addRequestProperty("User-Agent",
				"Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Win64; x64; Trident/5.0)");
		connection.addRequestProperty("Content-Type", "text/xml; charset=utf-8");
		connection.setInstanceFollowRedirects(false);

		OutputStream out = connection.getOutputStream();
		Writer wout = new OutputStreamWriter(out);

		wout.write(token);

		wout.flush();
		wout.close();

		out.flush();
		out.close();

		String rtFa = null;
		String fedAuth = null;

		ArrayList<String> result = new ArrayList<>();

		Map<String, List<String>> headerFields = connection.getHeaderFields();
		List<String> cookieHeader = headerFields.get("Set-Cookie");
		if (cookieHeader != null) {
			for (String cookie : cookieHeader) {
				if (cookie.startsWith(RTFA_SEARCH_STRING)) {
					rtFa = RTFA_SEARCH_STRING + HttpCookie.parse(cookie).get(0).getValue();
				} else if (cookie.startsWith(FEDAUTH_SEARCH_STRING)) {
					fedAuth = FEDAUTH_SEARCH_STRING + HttpCookie.parse(cookie).get(0).getValue();
				}
			}
		}
		// rtFA should always be index 0, and fedAuth index 1
		result.add(rtFa);
		result.add(fedAuth);
		return result;

	}

	public void createFolders(String[] folderList) {
		String pathOfCompleteFolders = "";
		for (String folder : folderList) {
			pathOfCompleteFolders += "/" + folder;
			String folder_api_path = String.format(BASE_UPLOAD_FOLDER + "/_api/web/Folders");
			String folder_content = String.format(
					"{ '__metadata': { 'type': 'SP.Folder' }, 'ServerRelativeUrl': '%s%s'}", ATF_FOLDER,
					pathOfCompleteFolders);
			String folder_jsonResponse = post(folder_api_path, folder_content, false);
			//printJsonResponce(folder_jsonResponse);
		}
	}

	/**
	 * "AAA-SharepointUploadTest/anotherFolderDown/OKThisIs/ACrazy/amount/ofFolders/config.txt"
	 *
	 * @param uploadPath
	 * @return
	 */
	public String uploadFile(String uploadPath, File uploadFile) {
		/* split the file on either forward or back slashes and then build the folders in sharepoint before attempting
		to upload the file */
		String[] folders = uploadPath.split("[/\\\\]");
		String additionalPath = "";
		if (folders.length > 1) {
			String[] paths = Arrays.copyOf(folders, folders.length - 1);
			createFolders(paths);
			additionalPath = "/" + String.join("/", paths);
			uploadPath = folders[folders.length - 1];
		}

		try {
			String content = FileUtils.readFileToString(uploadFile, "utf-8");
			String apiPath = String.format(
					BASE_UPLOAD_FOLDER + "/_api/web/GetFolderByServerRelativeUrl('%s/')/Files/add(url='%s'," +
							"overwrite=true)",
					ATF_FOLDER + additionalPath, uploadPath);
			//log.trace(MARKER, "api path: {}", apiPath);
			String jsonResponce = post(apiPath, content, false);
			//log.trace(ERROR, "raw Responce: {}", jsonResponce);
			//printJsonResponce(jsonResponce);
		} catch (IOException | OutOfMemoryError e) {
			log.error(ERROR, "could not upload file: {}", uploadPath, e);
		}
		return null;
	}

	private void printJsonResponce(String jsonResponce) {

		ObjectMapper mapper = new ObjectMapper();
		try {
			Object json = mapper.readValue(jsonResponce, Object.class);

			String indented = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
			log.info(MARKER, "responce JSON:\n{}", indented);
		} catch (IOException e) {
			log.error(ERROR, "Could not print out SharePoint Responce.", e);
		}
	}
}


/**
* The LbDockerCAdvisor program tries to do a load-balancing
* on a docker pool cluster using the load metrics that the
* cAdvisor collector returns.
* @see <a href="https://github.com/google/cadvisor">cAdvisor</a>
*
* @author  Jose E. Nunez
* @version 0.0.1
* @since   2018-10-14
*/

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

// Apache HttpClient
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import org.javatuples.Triplet;

// JSON-simple
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


/**
* The main class which queries cAdvisor, processes the statistics, and gives
* the load-balancing recommendation.
*
* @author  Jose E. Nunez
* @version 0.0.1
* @since   2018-10-14
*/
public final class LbDockerCAdvisor {

  /**
   * The cAdvisor server address (hostname or IP address).
   */
  private final String srvCAdvisor;

  /**
   * The cAdvisor server port number.
   */
  private final int portCAdvisor;

  /**
   * An HTTP proxy through which to communicate with the cAdvisor HTTP server.
   */
  private final HttpHost httpProxy;


  /**
  * Constructor. Saves the basic values to construct the Apache HttpClient to
  * the cAdvisor server.
  *
  * @param hostCAdvisor The hostname name or IP address to the cAdvisor server
  * @param portNumCAdvisor The port number of the cAdvisor server
  * @param httpProxyToCAdvisor An HttpHost of the HTTP proxy to use
  *     (can be null)
  */
  public LbDockerCAdvisor(final String hostCAdvisor, final int portNumCAdvisor,
                          final HttpHost httpProxyToCAdvisor
  ) {
    this.srvCAdvisor = hostCAdvisor;
    this.portCAdvisor = portNumCAdvisor;
    this.httpProxy = httpProxyToCAdvisor;   // may be null: not to use one
  }


  /**
  * Creates an HTTP client, with the parameters given at the constructor of
  *     this class.
  *
  * @return the HTTP GET request to be sent to the HTTP server
  */
  protected CloseableHttpClient createHttpClient() {

    CloseableHttpClient httpClient = HttpClients.custom()
            .build();

    return httpClient;
  }


  /**
  * Creates an HTTP URI object that requests an API query to the cAdvisor
  *     server we have registered in this class.
  * @see #srvCAdvisor
  *
  * @param apiQueryCAdvisor the path to be requested to cAdvisor, like
  *                         "/api/v1.3/docker"
  * @return the URI to our cAdvisor server to request such API path
  */
  protected URI buildCAdvisorUrl(final String apiQueryCAdvisor) {

    URI result = null;

    try {
      URIBuilder builder = new URIBuilder()
                               .setScheme("http")
                               .setHost(srvCAdvisor)
                               .setPort(portCAdvisor)
                               .setPath(apiQueryCAdvisor);
      result =  builder.build();

    } catch (URISyntaxException e) {
      e.printStackTrace();
    }

    return result;
  }

  /**
  * Creates an HTTP GET method, which will be later sent to the [cAdvisor]
  *     HTTP server.
  *
  * @param uri the URI to be requested via GET
  * @param dumpRequestHeaders whether to print the GET request headers created
  * @return the HTTP GET request to be sent to the HTTP server
  * @throws UnsupportedEncodingException an unsupported HTTP encoding
  */
  protected HttpGet createHttpGetMethod(final URI uri,
                                        final boolean dumpRequestHeaders
  ) throws UnsupportedEncodingException {

    HttpGet httpGet = new HttpGet(uri);

    RequestConfig config = RequestConfig.custom()
            .setAuthenticationEnabled(false)
            .setProxy(httpProxy)
            .build();
    httpGet.setConfig(config);

    httpGet.addHeader("content-type", "application/json");

    /* No authentication to cAdvisor
    Base64 encoder = new Base64();
    String basicAuthCred = encoder.encodeAsString(
                              String.format("%s:%s", webServerUser,
                                                     webServerPass)
                                    .getBytes()
                           );
    httpGet.addHeader("Authorization", "Basic " + basicAuthCred);
    // System.err.println("Basic " + basicAuthCred);
    */

    if (dumpRequestHeaders) {
      for (Header hdr: httpGet.getAllHeaders()) {
        String name = hdr.getName();
        String value = hdr.getValue();
        if (value != null) {
          System.out.println(String.format("%s:%s", name, value));
        } else {
          System.out.println(String.format("%s:", name));
        }
      }
      System.out.println();
    }

    return httpGet;
  }


  /**
  * Dumps an HTTP response from the [cAdvisor] HTTP server to standard-output.
  *     This is mainly intended for debugging the communication to cAdvisor.
  *
  * @param httpResponse the HTTP response obtained from the HTTP server
  */
  protected void dumpHttpResponse(final CloseableHttpResponse httpResponse) {

    if (httpResponse == null) {
      return;
    }

    System.out.println(httpResponse.getStatusLine());

    for (Header hdr: httpResponse.getAllHeaders()) {
      String name = hdr.getName();
      String value = hdr.getValue();
      if (value != null) {
        System.out.println(String.format("%s:%s", name, value));
      } else {
        System.out.println(String.format("%s:", name));
      }
    }

    HttpEntity resEntity = httpResponse.getEntity();
    if (resEntity != null) {
      try {
        System.out.println(EntityUtils.toString(resEntity));
        EntityUtils.consume(resEntity);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    System.out.println();
  }


  /**
  * Makes an HTTP GET request to the [cAdvisor] HTTP server with the given
  *     URL.
  *
  * @param uri The URI to GET at the HTTP server
  * @param dumpRequestHeaders Whether to dump request/response headers or not
  * @return the HTTP Response answered by the HTTP server to that GET query
  */
  protected CloseableHttpResponse simpleHttpGetRequest(
                                         final URI uri,
                                         final boolean dumpRequestHeaders
  ) {

    CloseableHttpClient httpClient = createHttpClient();

    HttpGet httpGet = null;
    try {
      httpGet = createHttpGetMethod(uri, dumpRequestHeaders);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      return null;
    }

    try {

      CloseableHttpResponse webServerResponse = httpClient.execute(httpGet);
      if (dumpRequestHeaders) {
        dumpHttpResponse(webServerResponse);
      }

      return webServerResponse;

    } catch (ClientProtocolException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
  * Returns the string of the body of the HTTP Response from the [cAdvisor]
  *     HTTP server.
  *
  * @param response The HTTP response object that the HTTP server has answered
  * @return the String of the body in that HTTP response
  */
  protected String getResponseStringBody(final CloseableHttpResponse response) {
    HttpEntity entity = response.getEntity();
    if (entity != null) {
      try {
        String responseStr = EntityUtils.toString(entity, "UTF-8");
        return responseStr;
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return null;
  }

  /**
  * Get the machine metric statistics from cAdvisor, by
  *     GET-ing the cAdvisor's "/api/v1.3/machine" REST API.
  */
  protected void getMachineStats() {
    // false means: don't dump http headers nor response body for debugging
    CloseableHttpResponse respMachStats = simpleHttpGetRequest(
                                      buildCAdvisorUrl("/api/v1.3/machine"),
                                      false
                                    );

    String strMachStats = getResponseStringBody(respMachStats);

    if (strMachStats != null) {
      JSONParser parser = new JSONParser();
      Object obj = null;
      try {
        obj = parser.parse(strMachStats);
      } catch (ParseException e) {
        e.printStackTrace();
      }
      System.out.println("DEBUG: Parsed MachineStats\n" + obj);
    }
  }


  /**
  * Get the Docker metric statistics from cAdvisor, by
  *     GET-ing the cAdvisor's "/api/v1.3/docker" REST API.
  */
  protected void getDockerStats() {
    // false means: don't dump http headers nor response body for debugging
    CloseableHttpResponse respDockerStats = simpleHttpGetRequest(
                                      buildCAdvisorUrl("/api/v1.3/docker"),
                                      false
                                    );

    String respBody = getResponseStringBody(respDockerStats);

    if (respBody != null) {
      ConvertBodyFromCAdvisor converter =
          new ConvertBodyFromCAdvisor(respBody);

      String dockerId = converter.getDockerId();

      List<Triplet<Long, Float, Long>> statsCpuMem =
          converter.getCAdvisorCpuMemStats();
    }
  }

  /**
  * getCAdvisorStats(): calls all the methods which query cAdvisor and parse
  *     the metric results.
  */
  public void getCAdvisorStats() {
    getMachineStats();
    getDockerStats();
  }

  /**
  * Main function: program entry point.
  *
  * @param args The command-line arguments to this program. Ignored since this
  *     is an example.
  * @throws Exception some unexpected exception happened querying cAdvisor in
  *     this example.
  */
  public static void main(final String[] args) throws Exception {

    String hostCAdvisor = "localhost";
    final int defaultCAdvisorPort = 8080;

    // don't use an http-proxy to connect to cAdvisor
    HttpHost httpProxy = null;
    // HttpHost httpProxy = new HttpHost(webProxyName, webProxyPort,
    //                                   webProxyScheme);

    // create an Apache wrapper object to query the NodeJS fake json-server
    // listening at port 3000
    LbDockerCAdvisor req = new LbDockerCAdvisor(hostCAdvisor,
                                                defaultCAdvisorPort,
                                                httpProxy);

    req.getCAdvisorStats();
  }
}


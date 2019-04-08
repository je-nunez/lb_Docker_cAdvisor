
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
import java.util.ArrayList;
import java.util.HashMap;
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

// JSON-simple
import org.json.simple.JSONObject;
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
   * The relative weights of timed metrics from cAdvisor about a docker
   * container for their summarization into a single metric to return to the
   * load balancer.
   */
  private final ConfigRelativeWeightsMetrics weightsMetrics;

  /**
   * From which properties file this program should read the relative weights
   * of timed metrics from cAdvisor.
   * @see weightsMetrics
   */
  private final String fnPropertiesRelWeightsMetrics =
                                         "metric_weights.properties";

  /**
   * The memory of the last values of some timed metrics from cAdvisor that
   * are accumulative counters (per docker-container-id, which acts as the
   * hash key).
   */
  private final HashMap<String, MemoryLastValueAccumCounters>
      memPreviousStatValuesOfContainers;

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

    weightsMetrics = new ConfigRelativeWeightsMetrics();
    weightsMetrics.loadWeightsFromPropFile(fnPropertiesRelWeightsMetrics);

    memPreviousStatValuesOfContainers =
      new HashMap<String, MemoryLastValueAccumCounters>();
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
  protected Object getMachineStats() {
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
      return obj;
    } else {
      return null;
    }
  }


  /**
  * Get the Docker metric statistics from cAdvisor, by
  *     GET-ing the cAdvisor's "/api/v1.3/docker" REST API.
  */
  protected List<DockerContainerPlusStats> getDockerStats() {
    // false means: don't dump http headers nor response body for debugging
    CloseableHttpResponse respDockerStats = simpleHttpGetRequest(
                                      buildCAdvisorUrl("/api/v1.3/docker"),
                                      false
                                    );

    String respBody = getResponseStringBody(respDockerStats);

    if (respBody != null) {
      // System.out.println(respBody);

      ConvertDockerBodyFromCAdvisor converter =
          new ConvertDockerBodyFromCAdvisor(respBody);

      List<String> dockerIds = converter.getDockerId();
      List<Long> memLimits = converter.getCAdvisorMemLimit();

      // like zip()-ing both lists dockerIds and memLimits
      assert (dockerIds.size() == memLimits.size());

      ArrayList<DockerContainerPlusStats> dockerDescripts =
          new ArrayList<DockerContainerPlusStats>(dockerIds.size());

      for (int idx = 0; idx < dockerIds.size(); idx++) {
        String currDockerId = dockerIds.get(idx);
        List<LbCAdvisorInputStat> dockerStats =
            converter.getCAdvisorCpuMemStats(currDockerId);

        DockerContainerPlusStats dockerDescription =
            new DockerContainerPlusStats()
                 .dockerId(currDockerId)
                 .memLimit(memLimits.get(idx))
                 .dockerStats(dockerStats);

        dockerDescripts.add(idx, dockerDescription);
      }

      return dockerDescripts;
    } else {
      return null;
    }
  }

  /**
  * Get the simplified, overall load factor of a Docker container.
  * (Greater values of this simplified, overall measure means greater current
  * load in the Docker container, so the less probable, relatively, it should
  * be chosen for the next service requests.)
  *
  * @param dockerDescript the docker container and its statistics
  * @param machineMemCapacity the memory capacity of this machine
  * @param containerLastStatValues the last, previous values for some
  *                                accumulative stats for this container
  * @return a non-negative int value with the simplified, overall load factor
  *         of this Docker container (an int in "DISPLAY-HINT d-3" in IETF
  *         RFC 2579)
  */

  // the current version snmp4j doesn't seem to support CounterBasedGauge64 in
  // IETF RFC 2856, and this is why we need to return an int instead of
  // returning a long
  protected int overallLoadFactor(
                    final DockerContainerPlusStats dockerDescript,
                    long machineMemCapacity,
                    MemoryLastValueAccumCounters containerLastStatValues
  ) {
    // TODO:
    // We only take the average of the samples in the time-period returned by
    // cAdvisor. Probably a more powerful method could be using an ARIMA
    // [AutoRegressive Integrated Moving Average] estimate.
    // (E.g., using https://github.com/signaflo/java-timeseries#features)

    List<LbCAdvisorInputStat> dockerStats = dockerDescript.dockerStats();

    double avgCpuLoadAvg = 0.0;
    double avgMemUsage = 0.0;
    double accumRxDropped = 0.0;
    double accumIoTime = 0.0;
    double accumReadTime = 0.0;
    double accumWriteTime = 0.0;
    double accumWeightedIoTime = 0.0;

    avgCpuLoadAvg = dockerStats.stream()
                    .mapToDouble(LbCAdvisorInputStat::cpuLoadAvg).average()
                    .getAsDouble();

    avgMemUsage = dockerStats.stream()
                    .mapToLong(LbCAdvisorInputStat::memUsage).average()
                    .getAsDouble();
    // we need to normalize the avgMemUsage
    long minDockerMemCapacity = 1;
    if (machineMemCapacity > 0 && dockerDescript.memLimit() != null) {
      minDockerMemCapacity = Math.min(machineMemCapacity,
                                        dockerDescript.memLimit().longValue());
    } else if (machineMemCapacity > 0) {
      minDockerMemCapacity = machineMemCapacity;
    } else if (dockerDescript.memLimit() != null) {
      minDockerMemCapacity = dockerDescript.memLimit().longValue();
    }
    avgMemUsage /= (minDockerMemCapacity / 100.0);  // normalize to 100%

    LbCAdvisorInputStat latestStat = dockerStats.get(dockerStats.size() - 1);
    LbCAdvisorInputStat oldestStat = dockerStats.get(0);

    if (containerLastStatValues.lastRxDropped() == 0) {
      accumRxDropped = latestStat.rxDropped() - oldestStat.rxDropped();
    } else {
      accumRxDropped = latestStat.rxDropped()
                       - containerLastStatValues.lastRxDropped();
    }
    containerLastStatValues.lastRxDropped(latestStat.rxDropped());

    if (containerLastStatValues.lastIoTime() == 0) {
      accumIoTime = latestStat.ioTime() - oldestStat.ioTime();
    } else {
      accumIoTime = latestStat.ioTime() - containerLastStatValues.lastIoTime();
    }
    containerLastStatValues.lastIoTime(latestStat.ioTime());

    if (containerLastStatValues.lastReadTime() == 0) {
      accumReadTime = latestStat.readTime() - oldestStat.readTime();
    } else {
      accumReadTime = latestStat.readTime()
                      - containerLastStatValues.lastReadTime();
    }
    containerLastStatValues.lastReadTime(latestStat.readTime());

    if (containerLastStatValues.lastWriteTime() == 0) {
      accumWriteTime = latestStat.writeTime() - oldestStat.writeTime();
    } else {
      accumWriteTime = latestStat.writeTime()
                       - containerLastStatValues.lastWriteTime();
    }
    containerLastStatValues.lastWriteTime(latestStat.writeTime());

    if (containerLastStatValues.lastWeightedIoTime() == 0) {
      accumWeightedIoTime = latestStat.weightedIoTime()
                            - oldestStat.weightedIoTime();
    } else {
      accumWeightedIoTime = latestStat.weightedIoTime()
                            - containerLastStatValues.lastWeightedIoTime();
    }
    containerLastStatValues.lastWeightedIoTime(latestStat.weightedIoTime());


    double doubleVal = (
        weightsMetrics.rwCpuLoadAvg() * avgCpuLoadAvg

        + weightsMetrics.rwMemUsage() * avgMemUsage

        + weightsMetrics.rwRxDropped() * accumRxDropped

        + weightsMetrics.rwIoTime() * accumIoTime

        + weightsMetrics.rwReadTime() * accumReadTime

        + weightsMetrics.rwWriteTime() * accumWriteTime

        + weightsMetrics.rwWeightedIoTime() * accumWeightedIoTime
    );

    // convert the double value above to an int value in the format
    // "DISPLAY-HINT d-3"
    // Note: long values are not supported yet since the TEXTUAL-CONVENTION
    // CounterBasedGauge64 in IETF RFC 285 doesn't seem to be supported by
    // the current version of snmp4j

    int intVal = (int) (doubleVal * 1000);  // * 1000 = DISPLAY-HINT d-3
    if (intVal < 0) {
      System.err.println(
          String.format("WARN: The overall, summarized load factor for a "
                        + "Docker container returned a negative value: %d. "
                        + "Truncating it to zero (0) for Gauge32 "
                        + "in RFC 2578 doesn't support negatives.", intVal)
      );
      return 0;
    } else {
      return intVal;
    }
  }

  /**
  * getCAdvisorStats(): calls all the methods which query cAdvisor and parse
  *     the metric results.
  */
  public void getCAdvisorStats() {

    long machineMemCapacity = -1;
    Object machineStats = getMachineStats();
    if (machineStats != null) {
      System.out.println("DEBUG: Parsed MachineStats\n" + machineStats);
      if (machineStats instanceof JSONObject) {
        Object memCapacity =
            ((JSONObject) machineStats).get("memory_capacity");
        if (memCapacity != null && memCapacity instanceof Number) {
          machineMemCapacity = ((Number) memCapacity).longValue();
        }
      }
    }

    List<DockerContainerPlusStats> dockerDescripts = getDockerStats();
    if (dockerDescripts == null) {
      System.err.println("ERROR: Couldn't retrieve cAdvisor statistics\n");
    }

    List<LbCAdvisorResultStat> lbResultStats =
        new ArrayList<LbCAdvisorResultStat>(dockerDescripts.size());

    for (int idx = 0; idx < dockerDescripts.size(); idx++) {

      DockerContainerPlusStats dockerDescript = dockerDescripts.get(idx);
      assert (dockerDescript != null);

      System.out.println(dockerDescript);

      String currDockerId = dockerDescript.dockerId();

      // this overallLoadFactor() is the value used for load-balancing
      MemoryLastValueAccumCounters memPreviousStatValues =
          memPreviousStatValuesOfContainers.get(currDockerId);
      if (memPreviousStatValues == null) {
        // TODO: in the above condition, we need to take care as well of the
        //       case when the values of the previous-stats are found, but
        //       they are too old, in which case it could be wise to discard
        //       such old previous stat-values. Ie., the class
        //       MemoryLastValueAccumCounters must save as well the
        //       timestamp of when those previous stat-values where taken.
        memPreviousStatValues = new MemoryLastValueAccumCounters();
        memPreviousStatValuesOfContainers.put(currDockerId,
                                              memPreviousStatValues);
      }
      int currDockerLoadFactor =
          overallLoadFactor(dockerDescript, machineMemCapacity,
                            memPreviousStatValues);

      System.out.format("Overall load factor of container %s: %d\n",
                        currDockerId, currDockerLoadFactor);

      LbCAdvisorResultStat lbResultStat =
            new LbCAdvisorResultStat()
                 .dockerId(currDockerId)
                 .lbFinalStat(currDockerLoadFactor);

      lbResultStats.add(idx, lbResultStat);
    }

    // TODO: we need to pass the just calculated list of result metrics for
    //       the load balancer, in "lbResultStats", to the SNMP agent module
    //       (that is the one which exports those metrics to the load
    //       balancer).

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

    final int defaultCAdvisorPort = 8080;
    String hostCAdvisor = "localhost";
    if (args.length >= 1) {
      // the first argument is the address of CAdvisor
      hostCAdvisor = args[0];
    }

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


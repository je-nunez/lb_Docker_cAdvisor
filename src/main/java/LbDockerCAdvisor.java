
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

import org.apache.http.HttpHost;


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
  * Main function: program entry point.
  *
  * @param args The command-line arguments to this program. Ignored since this
  *     is an example.
  * @throws Exception some unexpected exception happened querying cAdvisor in
  *     this example.
  */
  public static void main(final String[] args) throws Exception {

    Thread.currentThread().setName("LbDockerCAdvisor: main thread");

    int portCAdvisor = 8080;
    String hostCAdvisor = "localhost";
    if (args.length >= 1) {
      // the first argument is the address of CAdvisor
      hostCAdvisor = args[0];
    }
    if (args.length >= 2) {
      // the second argument is the port number of CAdvisor
      portCAdvisor = Integer.parseInt(args[1]);
    }

    // don't use an http-proxy to connect to cAdvisor
    HttpHost httpProxy = null;
    // HttpHost httpProxy = new HttpHost(webProxyName, webProxyPort,
    //                                   webProxyScheme);

    // run a full cycle of queries to cAdvisor every 20 seconds:
    int delayBetweenQueryCyclesMillisec = 20 * 1000;

    BackendThreadQueryCAdvisor querycAdvisor =
        new BackendThreadQueryCAdvisor(hostCAdvisor,
                                 portCAdvisor,
                                 httpProxy,
                                 delayBetweenQueryCyclesMillisec);
    querycAdvisor.start();
    // TODO: create the SNMP front-end thread to answer queries from the
    // lb.
    querycAdvisor.join();
  }
}


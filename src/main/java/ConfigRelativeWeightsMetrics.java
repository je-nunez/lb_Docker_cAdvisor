
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;


/**
* The relative weights of timed metrics from cAdvisor for their summarization
* to the load balancer.
*
* @author  Jose E. Nunez
* @version 0.0.1
* @since   2019-03-31
*/
public class ConfigRelativeWeightsMetrics {

  /**
   * The relative weight of the CPU load average of the docker container
   * for the load balancer.
   *
   * @param rwCpuLoadAvg New value for the relative weight of the CPU load
   *                    average of the docker container for the load balancer.
   * @return The current value of the relative weight of the CPU load average
   *         of the docker container for the load balancer.
   */
  @Accessors(fluent = true)
  @Getter @Setter private float rwCpuLoadAvg = 0;

  /**
   * The relative weight of the Memory Usage of the docker container for the
   * load balancer.
   *
   * @param rwMemUsage New value for the relative weight of the Memory Usage
   *                    of the docker container for the load balancer.
   * @return The current value of the relative weight of the Memory Usage
   *         of the docker container for the load balancer.
   */
  @Accessors(fluent = true)
  @Getter @Setter private float rwMemUsage = 0;

  /**
   * The relative weight of the Network RX-Dropped of the docker container for
   * the load balancer.
   *
   * @param rwRxDropped New value for the relative weight of the Network RX-
   *                   Dropped of the docker container for the load balancer.
   * @return The current value of the relative weight of the Network RX-Dropped
   *         of the docker container for the load balancer.
   */
  @Accessors(fluent = true)
  @Getter @Setter private float rwRxDropped = 0;

  /**
   * The relative weight of the FS IOTime (millisecs) of the docker container
   * for the load balancer.
   *
   * @param rwIoTime New value for the relative weight of the FS IOTime
   *                   of the docker container for the load balancer.
   * @return The current value of the relative weight of the FS IOTime
   *         of the docker container for the load balancer.
   */
  @Accessors(fluent = true)
  @Getter @Setter private float rwIoTime = 0;

  /**
   * The relative weight of the FS ReadTime (millisecs) of the docker container
   * for the load balancer.
   *
   * @param rwReadTime New value for the relative weight of the FS ReadTime
   *                  of the docker container for the load balancer.
   * @return The current value of the relative weight of the FS ReadTime
   *         of the docker container for the load balancer.
   */
  @Accessors(fluent = true)
  @Getter @Setter private float rwReadTime = 0;

  /**
   * The relative weight of the FS WriteTime (millisecs) of the docker container
   * for the load balancer.
   *
   * @param rwWriteTime New value for the relative weight of the FS WriteTime
   *                   of the docker container for the load balancer.
   * @return The current value of the relative weight of the FS WriteTime
   *         of the docker container for the load balancer.
   */
  @Accessors(fluent = true)
  @Getter @Setter private float rwWriteTime = 0;

  /**
   * The relative weight of the FS Weighted-IO-Time (millisecs) of the docker
   * container for the load balancer.
   *
   * @param rwWeightedIoTime New value for the relative weight of the FS
   *                        Weighted-IO-Time of the docker container for the
   *                        load balancer.
   * @return The current value of the relative weight of the FS
   *         Weighted-IO-Time of the docker container for the load balancer.
   */
  @Accessors(fluent = true)
  @Getter @Setter private float rwWeightedIoTime = 0;


  /**
  * Load the relative weights of the cAdvisor docker metrics for the load
  * balancer from a property file.
  *
  * @param propFileName the filename of the property file
  */
  protected void loadWeightsFromPropFile(final String propFileName) {
    Properties prop = new Properties();
    // InputStream propF = null;

    try (InputStream propF = new FileInputStream(propFileName);) {

      prop.load(propF);

      rwCpuLoadAvg =
        Float.parseFloat(prop.getProperty("rwCpuLoadAvg", "0.8"));

      rwMemUsage =
        Float.parseFloat(prop.getProperty("rwMemUsage", "0.5"));

      rwRxDropped =
        Float.parseFloat(prop.getProperty("rwRxDropped", "0.8"));

      rwIoTime =
        Float.parseFloat(prop.getProperty("rwIoTime", "0.4"));

      rwReadTime =
        Float.parseFloat(prop.getProperty("rwReadTime", "0.2"));

      rwWriteTime =
        Float.parseFloat(prop.getProperty("rwWriteTime", "0.3"));

      rwWeightedIoTime =
        Float.parseFloat(prop.getProperty("rwWeightedIoTime", "0.2"));

    } catch (IOException
             | NullPointerException
             | NumberFormatException ex) {
      ex.printStackTrace();
    }
  }

}

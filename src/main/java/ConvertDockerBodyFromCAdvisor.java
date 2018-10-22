
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

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

// JSonPath
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.ReadContext;

import net.minidev.json.JSONArray;

import org.javatuples.Triplet;

/**
* Parse and convert the response body from cAdvisor REST query
* "/api/v1.3/docker".
*
* @author  Jose E. Nunez
* @version 0.0.1
* @since   2018-10-14
*/
public class ConvertDockerBodyFromCAdvisor {

  /**
  * the JsonPath ReadContext.
  */
  private ReadContext ctx;

  /**
  * Instance constructor.
  *
  * @param strDockerStats the string with the [JSON] body of the
  *                       /api/m.n/docker response from cAdvisor
  */
  public ConvertDockerBodyFromCAdvisor(final String strDockerStats) {
    // prepare JsonPath queries on the metrics returned by cAdvisor
    ctx = JsonPath.parse(strDockerStats);
  }

  /**
  * Get the Docker ID string.
  *
  * @return the DockerId
  */
  public String getDockerId() {
    try {
      Object result = ctx.read("$..['id']");   // "$..['id']"

      if (result instanceof JSONArray) {
        String dockerId = (String) ((JSONArray) result).get(0);
        return dockerId;
      }
    } catch (PathNotFoundException e) {
      e.printStackTrace();
    }
    return null;
  }


  /**
  * A generic function to converts a JSON array of elements to a Java list.
  *
  * @param <T> the type that each element in the Java list will be.
  * @param jsonFromCAdvisor the JSON array of elements.
  * @param convertElem the conversion function of one element into T.
  * @return the corresponding Java list Ts.
  */
  protected <T> List<T> convertFromCAdvisor(
                                          final Object jsonFromCAdvisor,
                                          final Function<Object, T> convertElem

  ) {
    List<T> results = new ArrayList<>();

    if (jsonFromCAdvisor instanceof JSONArray) {
      for (Object o: (JSONArray) jsonFromCAdvisor) {
        T result = convertElem.apply(o);
        if (result != null) {
          results.add(result);
        }
      }
    }

    return results;
  }

  /**
  * Converts the a cAdvisor timestamp (which should be a string) to epoch time
  *     (in seconds).
  *
  * @param vcAdvisorTStamp the timestamp of the stats returned by cAdvisor
  * @return the corresponding the corresponding epoch time (in seconds)
  */
  protected Long convertCAdvisorDate(final Object vcAdvisorTStamp) {
    if (vcAdvisorTStamp instanceof String) {
      try {
        // TODO: modify the ZonedDateTime.parse() so that it saves some of
        //       the milliseconds in the date-strings. (cAdvisor gives
        //       timestamps with nanoseconds, i.e., in vcAdvisorTStamp.)
        Long epoch = new Long(ZonedDateTime
                               .parse((String) vcAdvisorTStamp)
                               .toEpochSecond()
                             );
        return epoch;
      } catch (DateTimeParseException e) {
        e.printStackTrace();
        return null;
      }
    } else {
      System.err.println("ERROR: unknown time-stamp: " + vcAdvisorTStamp);
      return null;
    }
  }

  /**
  * Converts a CPU load-average stat returned by cAdvisor to a Float.
  *
  * @param vcAdvisorLoadAvg the CPU load-average stat returned by cAdvisor
  * @return the corresponding Float.
  */
  protected Float convertCAdvisorLoadAvg(final Object vcAdvisorLoadAvg) {
    if (vcAdvisorLoadAvg instanceof Number) {
      Float loadAvg = new Float(((Number) vcAdvisorLoadAvg).floatValue());
      return loadAvg;
    } else {
      return null;
    }
  }

  /**
  * Converts a memory usage stat returned by cAdvisor to a Long.
  *
  * @param vcAdvisorMemUsg the memory usage stat returned by cAdvisor
  * @return the corresponding Long.
  */
  protected Long convertCAdvisorMemUsage(final Object vcAdvisorMemUsg) {
    if (vcAdvisorMemUsg instanceof Number) {
      Long memUsage = new Long(((Number) vcAdvisorMemUsg).longValue());
      return memUsage;
    } else {
      return null;
    }
  }

  /**
  * Gets the Java list of all the stats timestamps returned by cAdvisor, as
  * Unix epochs.
  *
  * @return the Java list of all the stats epochs returned by cAdvisor.
  */
  protected List<Long> getCAdvisorTStamps() {
    try {
      Object samplesTimeStamps =
          ctx.read("$..['stats'].[*].['timestamp']");

      List<Long> statsTStamps =
          convertFromCAdvisor(samplesTimeStamps,
              obj -> convertCAdvisorDate(obj)
          );
      return statsTStamps;
    } catch (PathNotFoundException e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
  * Gets the Java list of all the CPU Load Averages returned by cAdvisor.
  *
  * @return the Java list of all the CPU Load Averages returned by cAdvisor.
  */
  protected List<Float> getCAdvisorLoadAvg() {
    try {
      // we prefer to use the 'cpu.load_average' stat, rather than
      // 'cpu.usage.total' stat, because the later is a LongInt with the
      // accumulated CPU usage since the container start-up, so we would
      // to have take the maximum CPU limit for this container, and
      // divide the delta of 'cpu.usage.total' by the max CPU limit for
      // this container. This is in essence the 'cpu.load_average' stat.
      Object samplesCpuLoadAvg =
          ctx.read("$..['stats'].[*].['cpu'].['load_average']");
      List<Float> statsLoadAvg =
          convertFromCAdvisor(samplesCpuLoadAvg,
              obj -> convertCAdvisorLoadAvg(obj)
          );
      return statsLoadAvg;
    } catch (PathNotFoundException e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
  * Gets the Java list of all the Memory Usages returned by cAdvisor.
  *
  * @return the Java list of all the Memory Usages returned by cAdvisor.
  */
  protected List<Long> getCAdvisorMemUsage() {
    try {
      Object samplesMemUsages =
          ctx.read("$..['stats'].[*].['memory'].['usage']");
      List<Long> statsMemUsage =
          convertFromCAdvisor(samplesMemUsages,
              obj -> convertCAdvisorMemUsage(obj)
          );
      return statsMemUsage;
    } catch (PathNotFoundException e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
  * Gets the Java list of all the CPU, Memory stats returned by cAdvisor,
  * together with their respective timestamp as a Unix epoch.
  *
  * @return the Java list of such timed-stats returned by cAdvisor.
  */
  public List<Triplet<Long, Float, Long>> getCAdvisorCpuMemStats() {

    List<Long> tmStamps = getCAdvisorTStamps();
    List<Float> cpuLoadAvgs = getCAdvisorLoadAvg();
    List<Long> memUsages = getCAdvisorMemUsage();

    int numTStamps = tmStamps.size();
    if (numTStamps != cpuLoadAvgs.size()
        || numTStamps != memUsages.size()) {
      return null;
    }

    List<Triplet<Long, Float, Long>> results = new ArrayList<>(numTStamps);

    for (int i = 0; i < numTStamps; i++) {
      Long tmStamp = tmStamps.get(i);
      Float cpuLoadAvg = cpuLoadAvgs.get(i);
      Long memUsage = memUsages.get(i);

      results.add(new Triplet<>(tmStamp, cpuLoadAvg, memUsage));
    }

    return results;
  }

}


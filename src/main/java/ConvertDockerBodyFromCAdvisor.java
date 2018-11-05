
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

// JSonPath
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.ReadContext;

import net.minidev.json.JSONArray;

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
  * the DateTimeFormatter to format timestamps returned by cAdvisor.
  */
  private DateTimeFormatter dateFormatter = null;

  /**
  * Instance constructor.
  *
  * @param strDockerStats the string with the [JSON] body of the
  *                       /api/m.n/docker response from cAdvisor
  */
  public ConvertDockerBodyFromCAdvisor(final String strDockerStats) {
    // prepare JsonPath queries on the metrics returned by cAdvisor
    ctx = JsonPath.parse(strDockerStats);

    dateFormatter = new DateTimeFormatterBuilder()
      .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
      .optionalStart()
      .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
      .optionalEnd()
      .appendOffset("+HH:mm", "Z")
      .toFormatter();
  }

  /**
  * Get the Docker ID strings returned by cAdvisor.
  *
  * @return the list of DockerId
  */
  public List<String> getDockerId() {
    try {
      Object result = ctx.read("$..['id']");   // "$..['id']"

      if (result instanceof JSONArray) {
        JSONArray jsonArray = ((JSONArray) result);
        ArrayList<String> dockerIds = new ArrayList<String>(jsonArray.size());
        for (int idx = 0; idx < jsonArray.size(); idx++) {
          Object element = jsonArray.get(idx);
          if (element instanceof String) {
            dockerIds.add(idx, (String) element);
          } else {
            dockerIds.add(idx, null);
          }
        }

        return dockerIds;
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
  * (in milliseconds).
  *
  * @param vcAdvisorTStamp the timestamp of the stats returned by cAdvisor
  * @return the corresponding the corresponding epoch time (in milliseconds)
  */
  protected Long convertCAdvisorDate(final Object vcAdvisorTStamp) {

    if (vcAdvisorTStamp instanceof String) {
      try {
        Long epoch = new Long(ZonedDateTime
                                .parse((String) vcAdvisorTStamp, dateFormatter)
                                .toInstant()
                                .toEpochMilli()
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
  * Converts a Json object containing a Number object to a Float object.
  *
  * @param jsonFloat an object representing a parsed Json Float.
  * @return the corresponding Float, or null if the argument is not a number.
  */
  protected Float convertJsonFloat(final Object jsonFloat) {
    if (jsonFloat instanceof Number) {
      Float javaFloat = new Float(((Number) jsonFloat).floatValue());
      return javaFloat;
    } else {
      return null;
    }
  }

  /**
  * Converts a Json object containing a Number object to a Long object.
  *
  * @param jsonLong an object representing a parsed Json Long.
  * @return the corresponding Long, or null if the argument is not a number.
  */
  protected Long convertJsonLong(final Object jsonLong) {
    if (jsonLong instanceof Number) {
      Long javaLong = new Long(((Number) jsonLong).longValue());
      return javaLong;
    } else {
      return null;
    }
  }

  /**
  * Sums a Json object containing a list according to the field fieldName
  * of elements in that list. (I.e., in: [{..., fieldName: value1, ...},
  * {..., fieldName: value2, ...},..., {..., fieldName: valueN, ...}]: adds
  * all the values of fieldName found in this list.)
  *
  * @param jsonList an object representing a parsed Json list (array).
  * @param fieldName the field name to sum up in the elements of jsonList.
  * @return the corresponding Long, or null if the argument is not a number.
  */
  protected Long sumSamplesSamePeriod(final Object jsonList,
                                      final String fieldName) {
    if (jsonList instanceof JSONArray) {
      long accum = 0;
      for (Object statsSameTimePeriod: (JSONArray) jsonList) {
        if (statsSameTimePeriod instanceof LinkedHashMap) {

          Object statsForThisFilesys =
              ((LinkedHashMap) statsSameTimePeriod).get(fieldName);

          if (statsForThisFilesys instanceof Number) {
            accum += ((Number) statsForThisFilesys).longValue();
          }
        }
      }
      return new Long(accum);
    }
    return null;
  }

  /**
  * Gets the Java list of all the stats timestamps returned by cAdvisor, as
  * Unix epochs (in milliseconds), for a Docker container-id.
  *
  * @param nameAdvisorChild the value of the top-level "/docker/container-id".
  * @return the Java list of all the stats epochs returned by cAdvisor.
  */
  protected List<Long> getCAdvisorTStamps(final String nameAdvisorChild) {
    try {
      Object samplesTimeStamps =
          ctx.read("$.['" + nameAdvisorChild
                   + "'].['stats'].[*].['timestamp']");

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
  * Gets the Java list of all the Docker containers' memory limits returned
  * by cAdvisor.
  *
  * @return the Java list of all the CPU Load Averages returned by cAdvisor.
  */
  protected List<Long> getCAdvisorMemLimit() {
    try {
      Object samplesMemLimit =
          ctx.read("$..['spec'].['memory'].['limit']");
      List<Long> statsMemLimit =
          convertFromCAdvisor(samplesMemLimit, obj -> convertJsonLong(obj));
      return statsMemLimit;
    } catch (PathNotFoundException e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
  * Gets the Java list of all the CPU Load Averages returned by cAdvisor for a
  * Docker container-id.
  *
  * @param nameAdvisorChild the value of the top-level "/docker/container-id".
  * @return the Java list of all the CPU Load Averages returned by cAdvisor.
  */
  protected List<Float> getCAdvisorLoadAvg(final String nameAdvisorChild) {
    try {
      // we prefer to use the 'cpu.load_average' stat, rather than
      // 'cpu.usage.total' stat, because the later is a LongInt with the
      // accumulated CPU usage since the container start-up, so we would
      // to have take the maximum CPU limit for this container, and
      // divide the delta of 'cpu.usage.total' by the max CPU limit for
      // this container. This is in essence the 'cpu.load_average' stat.
      Object samplesCpuLoadAvg =
          ctx.read("$.['" + nameAdvisorChild
                   + "'].['stats'].[*].['cpu'].['load_average']");
      List<Float> statsLoadAvg =
          convertFromCAdvisor(samplesCpuLoadAvg,
              obj -> convertJsonFloat(obj)
          );
      return statsLoadAvg;
    } catch (PathNotFoundException e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
  * Gets the Java list of all the Memory Usages returned by cAdvisor for a
  * Docker container-id.
  *
  * @param nameAdvisorChild the value of the top-level "/docker/container-id".
  * @return the Java list of all the Memory Usages returned by cAdvisor.
  */
  protected List<Long> getCAdvisorMemUsage(final String nameAdvisorChild) {
    try {
      Object samplesMemUsages =
          ctx.read("$.['" + nameAdvisorChild
                   + "'].['stats'].[*].['memory'].['usage']");
      List<Long> statsMemUsage =
          convertFromCAdvisor(samplesMemUsages, obj -> convertJsonLong(obj));
      return statsMemUsage;
    } catch (PathNotFoundException e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
  * Gets the Java list of all the Network RX-Dropped returned by cAdvisor for
  * a Docker container-id.
  *
  * @param nameAdvisorChild the value of the top-level "/docker/container-id".
  * @return the Java list of all the Network RX-Dropped returned by cAdvisor.
  */
  protected List<Long> getCAdvisorRxDropped(final String nameAdvisorChild) {
    try {
      Object samplesRxDropped =
          ctx.read("$.['" + nameAdvisorChild
                   + "'].['stats'].[*].['network'].['rx_dropped']");
      List<Long> statsRxDropped =
          convertFromCAdvisor(samplesRxDropped, obj -> convertJsonLong(obj));
      return statsRxDropped;
    } catch (PathNotFoundException e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
  * Gets the Java list of __the sum__ of all the Filesystems IO-Time returned
  * by cAdvisor for a Docker container-id (the sum is for all the filesystems
  * in a same timestamp sampled by cAdvisor).
  *
  * @param nameAdvisorChild the value of the top-level "/docker/container-id".
  * @return the Java list of the sum of all the filesystems' IO-Times.
  */
  protected List<Long> getCAdvisorIoTime(final String nameAdvisorChild) {
    try {
      Object samplesFilesystem =
          ctx.read("$.['" + nameAdvisorChild
                   + "'].['stats'].[*].['filesystem']");
      // The processing of the cAdvisor 'filesystem' stats is a little
      // different that for 'cpu', 'memory', etc., stats, because the latter
      // return scalars per each sample time, but 'filesystem' returns a list
      // of the stats of all the filesystems in the Docker container per each
      // sample time returned by cAdvisor.
      List<Long> statsIoTime =
          convertFromCAdvisor(samplesFilesystem,
              obj -> sumSamplesSamePeriod(obj, "io_time")
          );
      return statsIoTime;
    } catch (PathNotFoundException e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
  * Gets the Java list of __the sum__ of all the Filesystems Read-Time returned
  * by cAdvisor for a Docker container-id (the sum is for all the filesystems
  * in a same timestamp sampled by cAdvisor).
  *
  * @param nameAdvisorChild the value of the top-level "/docker/container-id".
  * @return the Java list of the sum of all the filesystems' Read-Times.
  */
  protected List<Long> getCAdvisorReadTime(final String nameAdvisorChild) {
    try {
      Object samplesFilesystem =
          ctx.read("$.['" + nameAdvisorChild
                   + "'].['stats'].[*].['filesystem']");
      // The processing of the cAdvisor 'filesystem' stats is a little
      // different that for 'cpu', 'memory', etc., stats, because the latter
      // return scalars per each sample time, but 'filesystem' returns a list
      // of the stats of all the filesystems in the Docker container per each
      // sample time returned by cAdvisor.
      List<Long> statsReadTime =
          convertFromCAdvisor(samplesFilesystem,
              obj -> sumSamplesSamePeriod(obj, "read_time")
          );
      return statsReadTime;
    } catch (PathNotFoundException e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
  * Gets the Java list of __the sum__ of all the Filesystems Write-Time returned
  * by cAdvisor for a Docker container-id (the sum is for all the filesystems
  * in a same timestamp sampled by cAdvisor).
  *
  * @param nameAdvisorChild the value of the top-level "/docker/container-id".
  * @return the Java list of the sum of all the filesystems' Write-Times.
  */
  protected List<Long> getCAdvisorWriteTime(final String nameAdvisorChild) {
    try {
      Object samplesFilesystem =
          ctx.read("$.['" + nameAdvisorChild
                   + "'].['stats'].[*].['filesystem']");
      // The processing of the cAdvisor 'filesystem' stats is a little
      // different that for 'cpu', 'memory', etc., stats, because the latter
      // return scalars per each sample time, but 'filesystem' returns a list
      // of the stats of all the filesystems in the Docker container per each
      // sample time returned by cAdvisor.
      List<Long> statsWriteTime =
          convertFromCAdvisor(samplesFilesystem,
              obj -> sumSamplesSamePeriod(obj, "write_time")
          );
      return statsWriteTime;
    } catch (PathNotFoundException e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
  * Gets the Java list of __the sum__ of all the Filesystems Weighted-IO-Time
  * returned by cAdvisor for a Docker container-id (the sum is for all the
  * filesystems in a same timestamp sampled by cAdvisor).
  *
  * @param nameAdvisorChild the value of the top-level "/docker/container-id".
  * @return the Java list of the sum of all the filesystems' Weighted-IO-Times.
  */
  protected List<Long> getCAdvisorWeightedIoTime(
                                    final String nameAdvisorChild
  ) {
    try {
      Object samplesFilesystem =
          ctx.read("$.['" + nameAdvisorChild
                   + "'].['stats'].[*].['filesystem']");
      // The processing of the cAdvisor 'filesystem' stats is a little
      // different that for 'cpu', 'memory', etc., stats, because the latter
      // return scalars per each sample time, but 'filesystem' returns a list
      // of the stats of all the filesystems in the Docker container per each
      // sample time returned by cAdvisor.
      List<Long> statsWeightedIoTime =
          convertFromCAdvisor(samplesFilesystem,
              obj -> sumSamplesSamePeriod(obj, "weighted_io_time")
          );
      return statsWeightedIoTime;
    } catch (PathNotFoundException e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
  * Gets an index in Java list, which can contain "null" as element, so that if
  * index in the list is a "null"-valued element, then returns instead a
  * default value.
  *
  * @param <T> This is the generic type parameter of the elements in the list.
  * @param list the list to index.
  * @param index the index to look in the list.
  * @param defaultVal the default value to return if the index in the list is
  *                   null-valued element.
  * @return the value of the list at that index, or the default value if the
  *         the index in the list is a null-valued element.
  */
  protected final <T> T getSafeListElement(final List<T> list, final int index,
                                     final T defaultVal) {
    if (list.get(index) != null) {
      return list.get(index);
    }
    return defaultVal;
  }

  /**
  * Gets the Java list of all measurement stats returned by cAdvisor for a
  * Docker container-id.
  *
  * @param dockerId the value of the Docker container-id.
  * @return the Java list of such timed-stats returned by cAdvisor.
  */
  public List<LbCAdvisorInputStat> getCAdvisorCpuMemStats(
                                                    final String dockerId
  ) {

    String nameAdvisorChild = "/docker/" + dockerId;

    List<Long> tmStamps = getCAdvisorTStamps(nameAdvisorChild);
    List<Float> cpuLoadAvgs = getCAdvisorLoadAvg(nameAdvisorChild);
    List<Long> memUsages = getCAdvisorMemUsage(nameAdvisorChild);
    List<Long> rxDroppeds = getCAdvisorRxDropped(nameAdvisorChild);
    List<Long> ioTimeTotals = getCAdvisorIoTime(nameAdvisorChild);
    List<Long> readTimeTotals = getCAdvisorReadTime(nameAdvisorChild);
    List<Long> writeTimeTotals = getCAdvisorWriteTime(nameAdvisorChild);
    List<Long> weigtedIoTimeTotals =
        getCAdvisorWeightedIoTime(nameAdvisorChild);

    int numTStamps = tmStamps.size();
    if (numTStamps != cpuLoadAvgs.size()
        || numTStamps != memUsages.size()
        || numTStamps != rxDroppeds.size()
        || numTStamps != ioTimeTotals.size()
        || numTStamps != readTimeTotals.size()
        || numTStamps != writeTimeTotals.size()
        || numTStamps != weigtedIoTimeTotals.size()) {
      return null;
    }

    List<LbCAdvisorInputStat> results = new ArrayList<>(numTStamps);

    Long defaultLongVal = new Long(0);
    Float defaultFloatVal = new Float(0);

    for (int i = 0; i < numTStamps; i++) {
      long tmStamp =
          getSafeListElement(tmStamps, i, defaultLongVal).longValue();

      float cpuLoadAvg =
          getSafeListElement(cpuLoadAvgs, i, defaultFloatVal).floatValue();

      long memUsage =
          getSafeListElement(memUsages, i, defaultLongVal).longValue();

      long rxDropped =
          getSafeListElement(rxDroppeds, i, defaultLongVal).longValue();

      long ioTime =
          getSafeListElement(ioTimeTotals, i, defaultLongVal).longValue();

      long readTime =
          getSafeListElement(readTimeTotals, i, defaultLongVal).longValue();

      long writeTime =
          getSafeListElement(writeTimeTotals, i, defaultLongVal).longValue();

      long weightedIoTime =
          getSafeListElement(weigtedIoTimeTotals, i, defaultLongVal)
            .longValue();

      results.add(new LbCAdvisorInputStat()
                         .epochTimeStampMilli(tmStamp)
                         .cpuLoadAvg(cpuLoadAvg)
                         .memUsage(memUsage)
                         .rxDropped(rxDropped)
                         .ioTime(ioTime)
                         .readTime(readTime)
                         .writeTime(writeTime)
                         .weightedIoTime(weightedIoTime)
      );
    }

    return results;
  }

}


import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
* Class that describes a docker container, its ID, its memory limit, and all
* its stats. (Note: the memory limit of a docker container is not properly
* a stat of the container, because it is not under the 'stats' subtree of the
* cAdvisor result.)
*
* @author  Jose E. Nunez
* @version 0.0.1
* @since   2019-04-02
*/
public class DockerContainerPlusStats {

  /**
   * The Id of this docker container.
   *
   * @param dockerId New Id value for this docker container.
   * @return The current value of the ID of this container.
   */
  @Accessors(fluent = true)
  @Getter @Setter private String dockerId = null;

  /**
   * The memory limit of this docker container.
   *
   * @param dockerId New memory limit for this docker container.
   * @return The current value of the memory limit of this container.
   */
  @Accessors(fluent = true)
  @Getter @Setter private Long memLimit = null;

  /**
   * The list of timed-statistics of this docker container returned
   * by cAdvisor (a time-series).
   *
   * @param dockerStats New time-series of stats of this docker container.
   * @return The current value of the time-series of stats of this container.
   */
  @Accessors(fluent = true)
  @Getter @Setter private List<LbCAdvisorInputStat> dockerStats = null;

  @Override
  public String toString() {

    StringBuilder result = new StringBuilder();

    result.append(String.format("%s %s\n", dockerId(), memLimit()));

    List<LbCAdvisorInputStat> stats = dockerStats();

    stats.forEach(stat -> {
      result.append(String.format("%d: %f %d %d %d %d %d %d\n",
                                  stat.epochTimeStampMilli(),
                                  // Values of the metrics sampled then:
                                  stat.cpuLoadAvg(), stat.memUsage(),
                                  stat.rxDropped(), stat.ioTime(),
                                  stat.readTime(), stat.writeTime(),
                                  stat.weightedIoTime()
                                 )
      );
    });

    return result.toString();
  }



}

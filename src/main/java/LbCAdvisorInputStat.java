
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;


/**
* The tuple of timed metrics from cAdvisor taken into consideration.
* See below for the ones chosen so far (other metrics may be added as well in
* the future.)
*
* @author  Jose E. Nunez
* @version 0.0.1
* @since   2018-10-21
*/
public class LbCAdvisorInputStat {

  /**
   * The timestamp of this statistic measure by cAdvisor in Unix epoch format
   * (in milliseconds).
   *
   * @param epochTimeStampMilli New value for the timestamp of this stat.
   * @return The current value of the timestamp of this stat measure.
   */
  @Accessors(fluent = true)
  @Getter @Setter private long epochTimeStampMilli = 0;

  /**
   * The CPU load average sampled by cAdvisor during this statistic measure.
   *
   * @param cpuLoadAvg New value for the CPU load average of this stat measure.
   * @return The current value of the CPU load average during this stat.
   */
  @Accessors(fluent = true)
  @Getter @Setter private float cpuLoadAvg = 0;

  /**
   * The Memory Usage sampled by cAdvisor during this statistic measure.
   *
   * @param cpuLoadAvg New value for the Memory Usage of this stat measure.
   * @return The current value of the Memory Usage during this stat.
   */
  @Accessors(fluent = true)
  @Getter @Setter private long memUsage = 0;

  /**
   * The Network RX-Dropped sampled by cAdvisor during this statistic measure.
   *
   * @param rxDropped New value for the Network RX-Dropped of this measure.
   * @return The current value of the Network RX-Dropped during this stat.
   */
  @Accessors(fluent = true)
  @Getter @Setter private long rxDropped = 0;

  /**
   * The Network RX-Bytes sampled by cAdvisor during this statistic measure.
   *
   * @param rxBytes New value for the Network RX-Bytes of this measure.
   * @return The current value of the Network RX-Bytes during this stat.
   */
  @Accessors(fluent = true)
  @Getter @Setter private long rxBytes = 0;

  /**
   * The Network RX-Packets sampled by cAdvisor during this statistic measure.
   *
   * @param rxPackets New value for the Network RX-Packets of this measure.
   * @return The current value of the Network RX-Packets during this stat.
   */
  @Accessors(fluent = true)
  @Getter @Setter private long rxPackets = 0;

  /**
   * The Network TX-Bytes sampled by cAdvisor during this statistic measure.
   *
   * @param txBytes New value for the Network TX-Bytes of this measure.
   * @return The current value of the Network TX-Bytes during this stat.
   */
  @Accessors(fluent = true)
  @Getter @Setter private long txBytes = 0;

  /**
   * The Network TX-Packets sampled by cAdvisor during this statistic measure.
   *
   * @param txPackets New value for the Network TX-Packets of this measure.
   * @return The current value of the Network TX-Packets during this stat.
   */
  @Accessors(fluent = true)
  @Getter @Setter private long txPackets = 0;

  /**
   * The FS IOTime (millisecs) sampled by cAdvisor during this statistic
   * measure. (cAdvisor takes this from /proc/diskstats in Linux.)
   *
   * @param ioTime New value for the FS IOTime (ms) of this measure.
   * @return The current value of the FS IOTime (ms) sampled at this stat
   */
  @Accessors(fluent = true)
  @Getter @Setter private long ioTime = 0;

  /**
   * The FS ReadTime (millisecs) sampled by cAdvisor during this statistic
   * measure. (cAdvisor takes this from /proc/diskstats in Linux.)
   *
   * @param readTime New value for the FS ReadTime (ms) of this measure.
   * @return The current value of the FS ReadTime (ms) sampled at this stat.
   */
  @Accessors(fluent = true)
  @Getter @Setter private long readTime = 0;

  /**
   * The FS WriteTime (millisecs) sampled by cAdvisor during this statistic
   * measure. (cAdvisor takes this from /proc/diskstats in Linux.)
   *
   * @param writeTime New value for the FS WriteTime (ms) of this measure.
   * @return The current value of the FS WriteTime (ms) sampled at this stat
   */
  @Accessors(fluent = true)
  @Getter @Setter private long writeTime = 0;

  /**
   * The FS Weighted-IO-Time (millisecs) sampled by cAdvisor during this
   * measure. (cAdvisor takes this from /proc/diskstats in Linux.)
   *
   * @param weightedIOTime New value for the FS Weighted-IO-Time (ms).
   * @return The current value of the FS Weighted-IO-Time (ms).
   */
  @Accessors(fluent = true)
  @Getter @Setter private long weightedIoTime = 0;

}


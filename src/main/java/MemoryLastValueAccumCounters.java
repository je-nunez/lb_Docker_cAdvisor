import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;


/**
* The memory of the last values of some timed metrics from cAdvisor that
* are accumulative counters (ie., the counter at the beginning of the timed
* sampling interval is not 0, but starts at some other non-decrementing
* value. For example, the value of "network.rxBytes" at the start of a
* sampling interval from cAdvisor needs not be 0 (and if there was a
* non-zero value for rxBytes at the end of the previous interval, then
* this value will be non-decrementing -- unless wrapping at Long.MAX_VALUE).
*
* @author  Jose E. Nunez
* @version 0.0.1
* @since   2019-03-31
*/
public class MemoryLastValueAccumCounters {

  @Accessors(fluent = true)
  @Getter @Setter private long lastRxDropped = 0;

  @Accessors(fluent = true)
  @Getter @Setter private long lastIoTime = 0;

  @Accessors(fluent = true)
  @Getter @Setter private long lastReadTime = 0;

  @Accessors(fluent = true)
  @Getter @Setter private long lastWriteTime = 0;

  @Accessors(fluent = true)
  @Getter @Setter private long lastWeightedIoTime = 0;

}


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;


/**
* The final [scalar] metric to return to the load-balancer about a Docker
* container.
*
* @author  Jose E. Nunez
* @version 0.0.1
* @since   2019-04-07
*/

public class LbCAdvisorResultStat {

  /**
   * The dockerId of the container.
   *
   * @param dockerId New value for the dockerId of this LB stat.
   * @return The current value of the dockerId of this LB stat.
   */
  @Accessors(fluent = true)
  @Getter @Setter private String dockerId = null;

  /**
   * The final metric stat about this docker container to be returned to the LB.
   *
   * @param lbFinalStat New value for the final stat about this container.
   * @return The current value of the final stat about this container.
   */
  @Accessors(fluent = true)
  @Getter @Setter private int lbFinalStat = -1;

}


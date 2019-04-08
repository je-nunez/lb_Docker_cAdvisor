
import java.util.List;

import javax.management.InvalidAttributeValueException;

import org.snmp4j.agent.BaseAgent;
import org.snmp4j.agent.DefaultMOServer;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.smi.Gauge32;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.SMIConstants;

public class MngdObjDockersLbFinalStat {


  private MOTableBuilder builder;
  private List<LbCAdvisorResultStat> lbResultStats;


  public MngdObjDockersLbFinalStat() {
    lbResultStats = null;
  }


  public void setLbResultStats(List<LbCAdvisorResultStat> newLbResultStats) {

    lbResultStats = newLbResultStats;

    builder = new MOTableBuilder(SnmpMOIdentifiers.TABLE_BASE_OID)
                    .addColumnType(SMIConstants.SYNTAX_OCTET_STRING,
                                   MOAccessImpl.ACCESS_READ_ONLY)
                    .addColumnType(SMIConstants.SYNTAX_GAUGE32,
                                   MOAccessImpl.ACCESS_READ_ONLY);

    for (LbCAdvisorResultStat dockerLbFinalStat: newLbResultStats) {
      builder.addRowValue(new OctetString(dockerLbFinalStat.dockerId()));
      builder.addRowValue(new Gauge32(dockerLbFinalStat.lbFinalStat()));
    }

    // registerMOs(snmpAgent);    // TODO
  }


  public void registerMOs(BaseAgent agent)
      throws DuplicateRegistrationException, InvalidAttributeValueException {
    DefaultMOServer server = agent.getServer();

    // unregister all
    server.unregister(builder.build(), null);

    // register it back again
    server.register(builder.build(), null);
  }

}


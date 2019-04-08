import org.snmp4j.smi.OID;

public final class SnmpMOIdentifiers {

  // a private MIB under .1.3.6.1.4.1.99999.1
  // (note: .1.3.6.1.4.1.99999 == enterprises)

  private static final String STR_TABLE_BASE_OID =
              ".1.3.6.1.4.1.99999.1";

  public static final OID TABLE_BASE_OID =
      new OID(STR_TABLE_BASE_OID);
  public static final OID CONTAINER_IDENTIFIER =
      new OID(STR_TABLE_BASE_OID + ".1");
  public static final OID CONTAINER_LB_METRIC_STAT =
      new OID(STR_TABLE_BASE_OID + ".2");

  private SnmpMOIdentifiers() {
    // This is a utility class, exporting above OIDs
  }
}


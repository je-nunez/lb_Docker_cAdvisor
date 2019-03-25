import org.snmp4j.smi.OID;

public final class SnmpMOIdentifiers {

  // a private MIB under .1.3.6.1.4.1.99999.1
  // (note: .1.3.6.1.4.1.99999 == enterprises)

  public static final OID CONTAINER_IDENTIFIER =
      new OID(".1.3.6.1.4.1.99999.1.1.1");
  public static final OID CONTAINER_EPOCH_TIME_STAMP_MILLI =
      new OID(".1.3.6.1.4.1.99999.1.2.1");
  public static final OID CONTAINER_CPU_LOAD_AVG =
      new OID(".1.3.6.1.4.1.99999.1.3.1");
  public static final OID CONTAINER_MEM_USAGE =
      new OID(".1.3.6.1.4.1.99999.1.4.1");
  public static final OID CONTAINER_RX_DROPPED =
      new OID(".1.3.6.1.4.1.99999.1.5.1");
  public static final OID CONTAINER_IO_TIME =
      new OID(".1.3.6.1.4.1.99999.1.6.1");
  public static final OID CONTAINER_READ_TIME =
      new OID(".1.3.6.1.4.1.99999.1.7.1");
  public static final OID CONTAINER_WRITE_TME =
      new OID(".1.3.6.1.4.1.99999.1.8.1");
  public static final OID CONTAINER_WEIGHTED_IO_TIME =
      new OID(".1.3.6.1.4.1.99999.1.9.1");

  private SnmpMOIdentifiers() {
    // This is a utility class, exporting above OIDs
  }
}


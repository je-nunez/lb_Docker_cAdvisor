
import java.io.File;
import java.io.IOException;

import org.snmp4j.TransportMapping;
import org.snmp4j.agent.BaseAgent;
import org.snmp4j.agent.CommandProcessor;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.MOGroup;
import org.snmp4j.agent.ManagedObject;
import org.snmp4j.agent.mo.snmp.RowStatus;
import org.snmp4j.agent.mo.snmp.SnmpCommunityMIB;
import org.snmp4j.agent.mo.snmp.SnmpCommunityMIB.SnmpCommunityEntryRow;
import org.snmp4j.agent.mo.snmp.SnmpNotificationMIB;
import org.snmp4j.agent.mo.snmp.SnmpTargetMIB;
import org.snmp4j.agent.mo.snmp.StorageType;
import org.snmp4j.agent.mo.snmp.VacmMIB;
import org.snmp4j.agent.security.MutableVACM;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.USM;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;
import org.snmp4j.transport.TransportMappings;


public class CAdvisorSnmpAgent extends BaseAgent {

  private String snmpCommunity = "public";
  private String address;

  public CAdvisorSnmpAgent(String address) throws IOException {
    super(new File("conf.agent"), new File("bootCounter.agent"),
        new CommandProcessor(new OctetString(MPv3.createLocalEngineID())));
    this.address = address;
  }

  @Override
  protected void registerManagedObjects() {
    // TODO
  }

  @Override
  protected void addNotificationTargets(SnmpTargetMIB targetMib,
      SnmpNotificationMIB notificationMib) {
  }

  @Override
  protected void addViews(VacmMIB vacm) {

    vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c,
        new OctetString(snmpCommunity),
        new OctetString("groupv1-2"),
        StorageType.nonVolatile);

    vacm.addAccess(new OctetString("groupv1-2"),
        new OctetString(snmpCommunity),
        SecurityModel.SECURITY_MODEL_ANY,
        SecurityLevel.NOAUTH_NOPRIV,
        MutableVACM.VACM_MATCH_EXACT,
        new OctetString("cAdvisorSnmpQueryView"),
        null,
        null,
        StorageType.nonVolatile);

    vacm.addViewTreeFamily(new OctetString("cAdvisorSnmpQueryView"),
        new OID("1.3"),              // OID
        new OctetString(),
        VacmMIB.vacmViewIncluded,
        StorageType.nonVolatile);
  }

  public void start() throws IOException {

    init();

    addShutdownHook();
    getServer().addContext(new OctetString(snmpCommunity));
    finishInit();
    run();
    sendColdStartNotification();
  }

  public void registerManagedObject(ManagedObject mo) {
    try {
      server.register(mo, null);
    } catch (DuplicateRegistrationException ex) {
      throw new RuntimeException(ex);
    }
  }

  public void unregisterManagedObject(MOGroup moGroup) {
    moGroup.unregisterMOs(server, getContext(moGroup));
  }

  protected void unregisterManagedObjects() {
    // unregister objects previously registered...
  }

  protected void addUsmUser(USM usm) {
  }

  protected void initTransportMappings() throws IOException {
    transportMappings = new TransportMapping<?>[1];
    Address udpAddress = GenericAddress.parse(address);
    TransportMapping<?> tm = TransportMappings.getInstance().createTransportMapping(udpAddress);
    transportMappings[0] = tm;
  }

  protected void addCommunities(SnmpCommunityMIB communityMib) {
    Variable[] com2sec = new Variable[] {
      new OctetString(snmpCommunity),
      new OctetString("public"),
      getAgent().getContextEngineID(),
      new OctetString(snmpCommunity),
      new OctetString(), // transport tag
      new Integer32(StorageType.nonVolatile),
      new Integer32(RowStatus.active)
    };

    SnmpCommunityEntryRow row =
        communityMib
        .getSnmpCommunityEntry()
        .createRow(new OctetString("public2public").toSubIndex(true), com2sec);
    communityMib.getSnmpCommunityEntry().addRow(row);
  }


}


import java.util.ArrayList;
import java.util.List;

import org.snmp4j.agent.MOAccess;
import org.snmp4j.agent.mo.DefaultMOMutableRow2PC;
import org.snmp4j.agent.mo.DefaultMOTable;
import org.snmp4j.agent.mo.MOColumn;
import org.snmp4j.agent.mo.MOMutableTableModel;
import org.snmp4j.agent.mo.MOTable;
import org.snmp4j.agent.mo.MOTableIndex;
import org.snmp4j.agent.mo.MOTableSubIndex;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.SMIConstants;
import org.snmp4j.smi.Variable;

public class MOTableBuilder {

  private MOTableSubIndex[] subIndexes =
      new MOTableSubIndex[] {new MOTableSubIndex(SMIConstants.SYNTAX_INTEGER)};

  private MOTableIndex indexDef = new MOTableIndex(subIndexes, false);

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private final List<MOColumn> columns = new ArrayList<MOColumn>();
  private final List<Variable[]> tableRows = new ArrayList<Variable[]>();

  private int currentRow = 0;
  private int currentCol = 0;

  private OID tableRootOid;

  private int colTypeCnt = 0;


  public MOTableBuilder(OID tableRootOid) {
    this.tableRootOid = tableRootOid;
  }

  /**
   * Adds all column types {@link MOColumn} to this table.
   * Important to understand that you must add all types here before
   * adding any row values
   *
   * @param syntax use {@link SMIConstants}
   * @param access maximum access supported by a managed object
   * @return this object, for fluent functional programming style
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public MOTableBuilder addColumnType(int syntax, MOAccess access) {
    colTypeCnt++;
    columns.add(new MOColumn(colTypeCnt, syntax, access));
    return this;
  }

  public MOTableBuilder addRowValue(Variable variable) {
    if (tableRows.size() == currentRow) {
      tableRows.add(new Variable[columns.size()]);
    }
    tableRows.get(currentRow)[currentCol] = variable;
    currentCol++;
    if (currentCol >= columns.size()) {
      currentRow++;
      currentCol = 0;
    }
    return this;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public MOTable build() {
    DefaultMOTable ifTable =
        new DefaultMOTable(tableRootOid, indexDef,
                           columns.toArray(new MOColumn[0]));

    MOMutableTableModel model = (MOMutableTableModel) ifTable.getModel();
    int i = 1;

    for (Variable[] variables: tableRows) {
      model.addRow(new DefaultMOMutableRow2PC(new OID(String.valueOf(i)), variables));
      i++;
    }
    ifTable.setVolatile(true);
    return ifTable;
  }
}


import com.github.rvesse.airline.annotations.Command
import com.google.api.services.sheets.v4.Sheets
import javax.inject.Inject

@Command(name = "getNamesWithoutAddresses")
class GetNamesWithoutAddresses : Runnable {
  @Inject
  var options = Options()

  override fun run() {
    val values = getSpreadsheetValues()
    println(getNamesWithoutAddresses(options.spreadsheetId, values))
  }

  private fun getSpreadsheetValues(): Sheets.Spreadsheets.Values = SheetsService().spreadsheets().Values()

  private fun getNamesWithoutAddresses(spreadsheetId: String, values: Sheets.Spreadsheets.Values): List<String> {
    val namedAddresses = getNamedAddresses(spreadsheetId, values)
    val addressesByName = namedAddresses
      .distinct()
      .associateBy { it.name }
    val names = getTransactionNames(spreadsheetId, values)
    val nameToAddressMap = names.map { it.to(addressesByName[it]?.address) }
    return nameToAddressMap
      .filter { it.second == null }
      .map { it.first }
  }

  private fun getTransactionNames(spreadsheetId: String, values: Sheets.Spreadsheets.Values): List<String> {
    val transactions = getTransactions(spreadsheetId, values)
    return transactions
      .map { it.name }
      .filterNotNull()
      .distinct()
  }
}


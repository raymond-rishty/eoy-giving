import com.github.rvesse.airline.annotations.Command
import com.google.api.services.sheets.v4.Sheets

@Command(name = "getNamesWithoutAddresses")
class GetNamesWithoutAddresses : Runnable {
  override fun run() {
    val service = SheetsService()
    val spreadsheetId = "1dOkYihYd1UuXP2f08tr1VA9TjGFDg1DwII1x1t8Ji1s"
    val values = service.spreadsheets().Values()
    println(getNamesWithoutAddresses(spreadsheetId, values))
  }

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


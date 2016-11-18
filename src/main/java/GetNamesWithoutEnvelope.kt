
import com.github.rvesse.airline.annotations.Command
import javax.inject.Inject

@Command(name = "getNamesWithoutEnvelope")
class GetNamesWithoutEnvelope : Runnable {
  @Inject
  var options = Options()

  override fun run() {
    val values = getSheetInfo()
    val transactions = getTransactions(options.spreadsheetId, values)
    println(    transactions.filter { it.envelope == null }.map { it.name }.distinct().joinToString())
  }
}
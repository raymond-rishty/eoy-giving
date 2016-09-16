import com.google.api.client.repackaged.com.google.common.base.CharMatcher
import com.google.api.services.sheets.v4.Sheets
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

fun main(args : Array<String>) {
  val service = SheetsService()

  val spreadsheetId = "1dOkYihYd1UuXP2f08tr1VA9TjGFDg1DwII1x1t8Ji1s"
  val values = service.spreadsheets().Values()

  val transactions = getTransactions(spreadsheetId, values)
  generateReports(transactions)
}

private fun generateReports(transactions: List<Transaction>) {
  val transactionsByName = transactions.groupBy { it.name }
  for ((key, value) in transactionsByName) {
    value
            .sortedBy { it.date }
            .forEach { println(it) }
  }
}

private fun getTransactions(spreadsheetId: String, values: Sheets.Spreadsheets.Values): List<Transaction> {
  val transactions = getEnvelopeTransactions(spreadsheetId, values) + getNamedTransactions(spreadsheetId, values)
  val envelopes = getEnvelopeAssignments(spreadsheetId, values)

  val envelopesByNumber = envelopes.associateBy { it.number }

  val resolvedTransactions = transactions.map { if (it.name == null) Transaction(date = it.date, envelope = it.envelope, name = envelopesByNumber[it.envelope]?.name, amount = it.amount) else it }
  return resolvedTransactions
}

@Suppress("UNCHECKED_CAST")
private fun getEnvelopeAssignments(spreadsheetId: String, values: Sheets.Spreadsheets.Values): List<Envelope> {
  val assignmentsSheet = values.get(spreadsheetId, "Assignments!A2:B").execute()
  val envelopes = (assignmentsSheet.values.toTypedArray()[2] as ArrayList<ArrayList<String>>)
          .map {
            Envelope(number = Integer.parseInt(it[0]), name = it[1])
          }
  return envelopes
}

@Suppress("UNCHECKED_CAST")
private fun getEnvelopeTransactions(spreadsheetId: String, values: Sheets.Spreadsheets.Values): List<Transaction> {
  val transactionSheet = values.get(spreadsheetId, "Envelopes!A2:C").execute()
  val pattern = DateTimeFormatter.ofPattern("M/d/yyyy")
  val charMatcher = CharMatcher.`is`('$').or(CharMatcher.anyOf("$,"))
  val transactions = (transactionSheet.values.toTypedArray()[2] as ArrayList<ArrayList<String>>)
          .filter { it.size == 3 }
          .map {
            Transaction(date = LocalDate.parse(it[0], pattern), envelope = Integer.parseInt(it[1]), amount = BigDecimal(charMatcher.removeFrom(it[2])).toDouble())
          }

  return transactions
}

@Suppress("UNCHECKED_CAST")
private fun getNamedTransactions(spreadsheetId: String, values: Sheets.Spreadsheets.Values): List<Transaction> {
  val transactionSheet = values.get(spreadsheetId, "Notebook!A2:C").execute()
  val pattern = DateTimeFormatter.ofPattern("M/d/yyyy")
  val charMatcher = CharMatcher.`is`('$').or(CharMatcher.anyOf("$,"))
  val transactions = (transactionSheet.values.toTypedArray()[2] as ArrayList<ArrayList<String>>)
          .filter { it.size == 3 }
          .map {
            Transaction(date = LocalDate.parse(it[0], pattern), name = it[1], amount = BigDecimal(charMatcher.removeFrom(it[2])).toDouble())
          }

  return transactions
}



import com.github.rvesse.airline.Cli.builder
import com.google.api.client.repackaged.com.google.common.base.CharMatcher
import com.google.api.services.sheets.v4.Sheets
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

fun main(args : Array<String>) {
  val cli = builder<Runnable>("eoy")
    .withCommands(listOf(GenerateReports::class.java, GetNamesWithoutAddresses::class.java))
    .withDefaultCommand(GenerateReports::class.java)
    .build()
  val command = cli.parse(*args)
  command.run()
/*  val service = SheetsService()

  val spreadsheetId = "1dOkYihYd1UuXP2f08tr1VA9TjGFDg1DwII1x1t8Ji1s"
  val values = service.spreadsheets().Values()

  val transactions = getTransactions(spreadsheetId, values)
//  val listOf = listOf(17, 160, 24, 4, 19, 144, 34, 21, 1, 161, 162, 2, 20, 6, 18, 5, 16, 143, 31, 39, 164, 159, 35)
//  val sum = transactions.filter { it.envelope in listOf }.filter{ it.date < LocalDate.of(2016, Month.MAY, 8) }.map { it.amount }.sum()
//  println(sum / 18)
//  println(sum * 17 / 18)
  //println(getNamesWithoutAddresses(spreadsheetId, values))
  generateReports(transactions)*/
}


fun getSheetInfo(): Pair<String, Sheets.Spreadsheets.Values> {
  val service = SheetsService()
  val spreadsheetId = "1dOkYihYd1UuXP2f08tr1VA9TjGFDg1DwII1x1t8Ji1s"
  val values = service.spreadsheets().Values()
  return spreadsheetId to values
}

@Suppress("UNCHECKED_CAST")
private fun getTransactionAddresses(spreadsheetId: String, values: Sheets.Spreadsheets.Values): List<NamedAddress> {
  val transactionSheet = values.get(spreadsheetId, "Notebook!A2:D").execute()

  val transactions = (transactionSheet.values.toTypedArray()[2] as ArrayList<ArrayList<String>>)
    .filter { it.size == 4 }
    .filterNot { it[3].isBlank() }
    .map {
      NamedAddress(name = it[1], address = it[3])
    }
    .filterNotNull()

  return transactions
}

@Suppress("UNCHECKED_CAST")
private fun getEnvelopeAddresses(spreadsheetId: String, values: Sheets.Spreadsheets.Values): List<NamedAddress> {
  val transactionSheet = values.get(spreadsheetId, "Assignments!A2:C").execute()

  val transactions = (transactionSheet.values.toTypedArray()[2] as ArrayList<ArrayList<String>>)
    .filter { it.size == 3 }
    .filterNot { it[2].isBlank() }
    .map {
      NamedAddress(name = it[1], address = it[2])
    }
    .filterNotNull()

  return transactions
}

fun getNamedAddresses(spreadsheetId: String, values: Sheets.Spreadsheets.Values): List<NamedAddress> {
  return getTransactionAddresses(spreadsheetId, values) + getEnvelopeAddresses(spreadsheetId, values)
}

fun getTransactions(spreadsheetId: String, values: Sheets.Spreadsheets.Values): List<Transaction> {
  val transactions = getEnvelopeTransactions(spreadsheetId, values) + getNamedTransactions(spreadsheetId, values)
  val envelopes = getEnvelopeAssignments(spreadsheetId, values)

  val envelopesByNumber = envelopes.associateBy { it.number }
  val envelopesByName = envelopes.associateBy { it.name.split(" ").last() }

  return transactions.map {
    if (it.name == null)
      Transaction(date = it.date, envelope = it.envelope, name = envelopesByNumber[it.envelope]?.name, amount = it.amount)
    else {
      val envelope = getEnvelopeNumber(envelopesByName, it.name)
      Transaction(date = it.date, envelope = envelope, name = envelopesByNumber[envelope]?.name ?: it.name, amount = it.amount)
    }
  }
}
private fun getEnvelopeNumber(envelopesByName: Map<String, Envelope>, name: String): Int? = (envelopesByName[name] ?: envelopesByName[name.split(" ").last()])?.number

@Suppress("UNCHECKED_CAST")
private fun getEnvelopeAssignments(spreadsheetId: String, values: Sheets.Spreadsheets.Values): List<Envelope> {
  val assignmentsSheet = values.get(spreadsheetId, "Assignments!A2:B").execute()
  val envelopes = (assignmentsSheet.values.toTypedArray()[2] as ArrayList<ArrayList<String>>)
    .map { Envelope(number = Integer.parseInt(it[0]), name = it[1]) }
  return envelopes
}

@Suppress("UNCHECKED_CAST")
private fun getEnvelopeTransactions(spreadsheetId: String, values: Sheets.Spreadsheets.Values): List<Transaction> {
  val transactionSheet = values.get(spreadsheetId, "Envelopes!A2:C").execute()
  val (pattern, charMatcher) = getPatterns()
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
  val (pattern, charMatcher) = getPatterns()
  val transactions = (transactionSheet.values.toTypedArray()[2] as ArrayList<ArrayList<String>>)
    .filter { it.size == 3 }
    .map {
      Transaction(date = LocalDate.parse(it[0], pattern), name = it[1], amount = BigDecimal(charMatcher.removeFrom(it[2])).toDouble())
    }

  return transactions
}

private fun getPatterns(): Pair<DateTimeFormatter, CharMatcher> {
  val pattern = DateTimeFormatter.ofPattern("M/d/yyyy")
  val charMatcher = CharMatcher.anyOf("$,")
  return Pair(pattern, charMatcher)
}
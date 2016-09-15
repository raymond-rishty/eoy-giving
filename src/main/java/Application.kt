import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.repackaged.com.google.common.base.CharMatcher
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.common.io.Resources
import com.google.common.primitives.Doubles
import java.io.IOException
import java.io.InputStreamReader
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.util.*

@Suppress("UNCHECKED_CAST")
        /**
 * Created by raymondrishty on 9/14/16.
 */
fun main(args : Array<String>) {

  val service = getSheetsService()

  // Prints the names and majors of students in a sample spreadsheet:
  // https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
  val spreadsheetId = "1dOkYihYd1UuXP2f08tr1VA9TjGFDg1DwII1x1t8Ji1s"
  val values = service.spreadsheets().Values()


  val transactions = getTransactions(spreadsheetId, values)
  val envelopes = getEnvelopeAssignments(spreadsheetId, values)

  val envelopesByNumber = envelopes.associateBy { it.number }

  val map = transactions.map { if (it.name == null) Transaction(date = it.date, envelope = it.envelope, name = envelopesByNumber[it.envelope]?.name, amount = it.amount) else it }

}

private fun getEnvelopeAssignments(spreadsheetId: String, values: Sheets.Spreadsheets.Values): List<Envelope> {
  val assignmentsSheet = values.get(spreadsheetId, "Assignments!A:B").execute()
  val envelopes = (assignmentsSheet.values.toTypedArray()[2] as ArrayList<ArrayList<String>>)
          .drop(1)
          .map {
            Envelope(number = Integer.parseInt(it[0]), name = it[1])
          }
  return envelopes
}

private fun getTransactions(spreadsheetId: String, values: Sheets.Spreadsheets.Values): List<Transaction> {
  val transactionSheet = values.get(spreadsheetId, "Envelopes!A:C").execute()
  val pattern = DateTimeFormatter.ofPattern("M/d/yyyy")
  val charMatcher = CharMatcher.`is`('$')
  val separatorCharMatcher = CharMatcher.`is`(',')
  val transactions = (transactionSheet.values.toTypedArray()[2] as ArrayList<ArrayList<String>>)
          .drop(1)
          .map {
            println(it)
            Transaction(date = LocalDate.parse(it[0], pattern), envelope = Integer.parseInt(it[1]), amount = BigDecimal(separatorCharMatcher.removeFrom(charMatcher.trimLeadingFrom(it[2]))).toDouble())
          }
  return transactions
}

/** Application name.  */
private val APPLICATION_NAME = "Google Sheets API Java Quickstart"

/** Directory to store user credentials for this application.  */
private val DATA_STORE_DIR = java.io.File(
        System.getProperty("user.home"), ".credentials/sheets.googleapis.com-java-quickstart")

/** Global instance of the [FileDataStoreFactory].  */
private var DATA_STORE_FACTORY: FileDataStoreFactory = FileDataStoreFactory(DATA_STORE_DIR)

/** Global instance of the JSON factory.  */
private val JSON_FACTORY = JacksonFactory.getDefaultInstance()

/** Global instance of the HTTP transport.  */
private var HTTP_TRANSPORT: HttpTransport = GoogleNetHttpTransport.newTrustedTransport()

/** Global instance of the scopes required by this quickstart.

 * If modifying these scopes, delete your previously saved credentials
 * at ~/.credentials/sheets.googleapis.com-java-quickstart
 */
private val SCOPES = Arrays.asList(SheetsScopes.SPREADSHEETS_READONLY)

/**
 * Creates an authorized Credential object.
 * @return an authorized Credential object.
 * *
 * @throws IOException
 */
@Throws(IOException::class)
fun authorize(): Credential {
  // Load client secrets.
  val `in` = Resources.getResource("client_secret.json").openStream()
  val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(`in`))

  // Build flow and trigger user authorization request.
  val flow = GoogleAuthorizationCodeFlow.Builder(
          HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES).setDataStoreFactory(DATA_STORE_FACTORY).setAccessType("offline").build()
  val credential = AuthorizationCodeInstalledApp(
          flow, LocalServerReceiver()).authorize("user")
  println("Credentials saved to " + DATA_STORE_DIR.absolutePath)
  return credential
}

/**
 * Build and return an authorized Sheets API client service.
 * @return an authorized Sheets API client service
 * *
 * @throws IOException
 */
@Throws(IOException::class)
fun getSheetsService(): Sheets {
  val credential = authorize()
  return Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build()
}
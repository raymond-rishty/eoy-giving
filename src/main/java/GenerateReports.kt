import com.github.rvesse.airline.annotations.Command
import com.google.common.io.Files
import com.google.common.io.Resources
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.runtime.RuntimeConstants
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader
import org.apache.velocity.util.ClassUtils
import java.io.File
import java.io.InputStream
import java.io.StringWriter
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@Command(name = "generateReports")
class GenerateReports : Runnable {
  @Inject
  var options = Options()

  private val velocityEngine: VelocityEngine by lazy {
    VelocityEngine().apply {
      setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath")
      setProperty("classpath.resource.loader.instance", object : ClasspathResourceLoader() {
        override fun getResourceStream(name: String?): InputStream? {
          try {
            return Resources.getResource(name).openStream()
          } catch (e: Exception) {
            return ClassUtils.getResourceAsStream(this.javaClass, name)
          }
        }
      })
    }
  }

  override fun run() {
    val values = getSheetInfo()
    val transactions = getTransactions(options.spreadsheetId, values)
    val namedAddresses = getNamedAddresses(options.spreadsheetId, values)
    generateReports(transactions, namedAddresses)
  }

  fun generateReports(transactions: List<Transaction>, namedAddresses: List<NamedAddress>) {
    val transactionsByName = transactions.groupBy { it.name }

    val map = transactionsByName
      .values
      .map { getReport(it, namedAddresses) }
      .filter { it.isEligible() }
      .map { it.toMarkdown() }
      .joinToString(separator = "\n")
    val reportsTex = StringWriter().apply {
      velocityEngine.getTemplate("reports.tex.vm")
        .merge(
          VelocityContext().apply {
            put("contents", map)
          },
          this)
    }.toString()
    Files.write(reportsTex, File("reports.tex"), Charsets.UTF_8)
    println(reportsTex)
  }

  private fun Report.isEligible(): Boolean = when {
    transactions.size >= 4 -> true
    transactions.sumByDouble { it.amount } > 250 -> true
    else -> false
  }

  private fun getReport(value: List<Transaction>, namedAddresses: List<NamedAddress>): Report {
    val transactionsByDate = value.groupBy { it.date }
    val transactionSums = transactionsByDate.keys.sorted()
      .map { Transaction(date = it, amount = transactionsByDate[it]!!.sumByDouble(Transaction::amount)) }
    val name = value[0].name as String
    return Report(
      namedAddress = NamedAddress(name, namedAddresses.firstOrNull { it.name == name }?.address ?: ""),
      transactions = transactionSums)
  }

  private fun Report.toMarkdown(): String = StringWriter().apply {
    velocityEngine.getTemplate("report.tex.vm")
      .merge(VelocityContext().apply {
        put("name", namedAddress.name.replace("&", "\\&"))
        put("address", namedAddress.address.replace("\n", "\\\\\n"))
        put("transactions", transactions)
        put("total", transactions.sumByDouble { it.amount })
        put("dateFormat", DateTimeFormatter.ofPattern("MM/dd/yyyy"))
      }, this)
  }.toString()
}
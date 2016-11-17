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

@Command(name = "generateReports")
class GenerateReports : Runnable {
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
    val (spreadsheetId, values) = getSheetInfo()
    val transactions = getTransactions(spreadsheetId, values)
    val namedAddresses = getNamedAddresses(spreadsheetId, values)
    generateReports(transactions, namedAddresses)
  }

  fun generateReports(transactions: List<Transaction>, namedAddresses: List<NamedAddress>) {
    val transactionsByName = transactions.groupBy { it.envelope }

    val map = transactionsByName
      .map { getReport(it.key, it.value, namedAddresses) }
      //.sortedByDescending { it.transactions.size }
      //.take(1)
      //.map { printReport(it.namedAddress, it.transactions) }
      .map { it.toMarkdown() }
      .joinToString(separator = "\n")
    val reportsTex = StringWriter().apply {
      velocityEngine.getTemplate("reports.tex.vm").merge(VelocityContext().apply { put("contents", map) }, this)
    }.toString()
    Files.write(reportsTex, File("reports.tex"), Charsets.UTF_8)
    println(reportsTex)
/*
    for ((key, value) in transactionsByName) {
      val (namedAddress, transactionSums) = getReport(key, value, namedAddresses)

      printReport(namedAddress, transactionSums)
      *//*

      value
        .sortedBy { it.date }
        .forEach { println("${it.date}\t$${it.amount}") }*//*
    }*/
  }

  private fun printReport(namedAddress: NamedAddress, transactionSums: List<Transaction>): String {
    val stringBuilder = StringBuilder()

    stringBuilder.appendln(namedAddress.name)
    stringBuilder.appendln(namedAddress.address)
    transactionSums
      .forEach { stringBuilder.appendln("${it.date}\t${it.amount.toCurrencyString()}") }

    stringBuilder.appendln(transactionSums.sumByDouble { it.amount }.toCurrencyString())
    return stringBuilder.toString()
  }

  private fun getReport(key: Int?, value: List<Transaction>, namedAddresses: List<NamedAddress>): Report {
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
      }, this)
  }.toString()
}
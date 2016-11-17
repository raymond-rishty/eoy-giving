
import java.time.LocalDate

/**
 * Created by raymondrishty on 9/14/16.
 */

data class Envelope(val number: Int, val name: String)

data class Transaction(val date: LocalDate, val envelope: Int? = null, val name: String? = null, val amount: Currency)

data class NamedAddress(val name: String, val address: String)

typealias Currency = Double

fun Currency.toCurrencyString() = String.format("$%.2f", this)

data class Report(val namedAddress: NamedAddress, val transactions: List<Transaction>)
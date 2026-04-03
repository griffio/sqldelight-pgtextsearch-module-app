package griffio

import app.cash.sqldelight.driver.jdbc.asJdbcDriver
import griffio.queries.Sample
import org.postgresql.ds.PGSimpleDataSource

private fun getSqlDriver() = PGSimpleDataSource().apply {
    setURL("jdbc:postgresql://localhost:6543/postgres")
    applicationName = "App Main"
    user = "postgres"
    password = "password"
}.asJdbcDriver()


fun main() {
    val driver = getSqlDriver()
    val sample = Sample(driver)
    println("--- top k")
    sample.documentQueries.topk("database system").executeAsList().forEach { println(it) }
    println("--- score")
    sample.documentQueries.score("database system").executeAsList().forEach { println(it) }
}

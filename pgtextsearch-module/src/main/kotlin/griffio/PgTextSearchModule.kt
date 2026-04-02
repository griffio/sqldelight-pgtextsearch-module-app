package griffio

import app.cash.sqldelight.dialect.api.DialectType
import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType
import app.cash.sqldelight.dialect.api.SqlDelightModule
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.postgresql.PostgreSqlTypeResolver
import app.cash.sqldelight.dialects.postgresql.grammar.PostgreSqlParser
import app.cash.sqldelight.dialects.postgresql.grammar.PostgreSqlParserUtil
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlTypeName
import com.intellij.lang.parser.GeneratedParserUtilBase.Parser
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import griffio.grammar.PgTextSearchParser
import griffio.grammar.PgTextSearchParserUtil
import griffio.grammar.PgTextSearchParserUtil.extension_expr
import griffio.grammar.PgTextSearchParserUtil.index_method
import griffio.grammar.PgTextSearchParserUtil.storage_parameters
import griffio.grammar.PgTextSearchParserUtil.type_name
import griffio.grammar.psi.PgTextSearchBm25QueryDataType
import griffio.grammar.psi.PgTextSearchExtensionExpr

class PgTextSearchModule : SqlDelightModule {
    override fun typeResolver(parentResolver: TypeResolver): TypeResolver = PgTextSearchTypeResolver(parentResolver)

    override fun setup() {
        PgTextSearchParserUtil.reset()
        PgTextSearchParserUtil.overridePostgreSqlParser()
        // As the grammar doesn't support inheritance - override type_name manually to try inherited type_name
        // Capture any existing overrides (e.g., from other PostgreSql Modules)
        val previousTypeName = PostgreSqlParserUtil.type_name
        val previousExtensionExpr = PostgreSqlParserUtil.extension_expr
        val previousIndexMethod = PostgreSqlParserUtil.index_method
        val previousStorageParameters = PostgreSqlParserUtil.storage_parameters
        // Uses previous parser rule (e.g bm25 -> vectorChord? -> postgresql) if another module exists otherwise use PostgreSqlParser
        PostgreSqlParserUtil.type_name = Parser { psiBuilder, i ->
            type_name?.parse(psiBuilder, i)
                    ?: PgTextSearchParser.type_name_real(psiBuilder, i)
                    || previousTypeName?.parse(psiBuilder, i)
                    ?: PostgreSqlParser.type_name_real(psiBuilder, i)
        }
        // etc
        PostgreSqlParserUtil.extension_expr = Parser { psiBuilder, i ->
            extension_expr?.parse(psiBuilder, i)
                    ?: PgTextSearchParser.extension_expr_real(psiBuilder, i)
                    || previousExtensionExpr?.parse(psiBuilder, i)
                    ?: PostgreSqlParser.extension_expr_real(psiBuilder, i)
        }
        // etc
        PostgreSqlParserUtil.index_method = Parser { psiBuilder, i ->
            index_method?.parse(psiBuilder, i)
                    ?: PgTextSearchParser.index_method_real(psiBuilder, i)
                    || previousIndexMethod?.parse(psiBuilder, i)
                    ?: PostgreSqlParser.index_method_real(psiBuilder, i)
        }
        // etc
        PostgreSqlParserUtil.storage_parameters = Parser { psiBuilder, i ->
            storage_parameters?.parse(psiBuilder, i)
                    ?: PgTextSearchParser.storage_parameters_real(psiBuilder, i)
                    || previousStorageParameters?.parse(psiBuilder, i)
                    ?: PostgreSqlParser.storage_parameters_real(psiBuilder, i)
        }
    }
}

enum class PgTextSearchSqlType(override val javaType: TypeName) : DialectType {
    BM25QUERY(STRING);

    override fun prepareStatementBinder(columnIndex: CodeBlock, value: CodeBlock): CodeBlock {
        return when (this) {
            BM25QUERY -> CodeBlock.of("bindString(%L, %L)\n", columnIndex, value)
        }
    }

    override fun cursorGetter(columnIndex: Int, cursorName: String): CodeBlock {
        return CodeBlock.of(
            when (this) {
                BM25QUERY -> "$cursorName.getString($columnIndex)"
            },
            javaType,
        )
    }
}

// Change to inheritance where some implementations may need to call `super` - not possible with delegation
// parentResolver is called to delegate to the next TypeResolver in the chain
private class PgTextSearchTypeResolver(private val parentResolver: TypeResolver) : PostgreSqlTypeResolver(parentResolver) {

    override fun definitionType(typeName: SqlTypeName): IntermediateType {
        return when (typeName) {
            is PgTextSearchBm25QueryDataType -> IntermediateType(PgTextSearchSqlType.BM25QUERY)
            else -> parentResolver.definitionType(typeName) // use parentResolver to use the module chain
        }
    }

    override fun resolvedType(expr: SqlExpr) : IntermediateType {
        return if (expr is PgTextSearchExtensionExpr && expr.scoreOperatorExpression != null)
            IntermediateType(PrimitiveType.REAL) else parentResolver.resolvedType(expr) // use parentResolver to use the module chain
    }

    override fun functionType(functionExpr: SqlFunctionExpr): IntermediateType? =
        when (functionExpr.functionName.text.lowercase()) {
            "to_bm25query" -> IntermediateType(PgTextSearchSqlType.BM25QUERY)
            else -> super.functionType(functionExpr) // postgresql.PostgreSqlTypeResolver.functionType calls parentResolver
        }
}

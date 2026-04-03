package griffio.grammar.mixins

import com.alecstrong.sql.psi.core.psi.SqlBinaryExpr
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.intellij.lang.ASTNode
import griffio.grammar.psi.PgTextSearchScoreOperatorExpression

/**
 * Used for <@> expressions to avoid bind errors
 */
internal abstract class PgTextSearchScoreOperatorMixin(node: ASTNode) :
    SqlCompositeElementImpl(node),
    SqlBinaryExpr,
    PgTextSearchScoreOperatorExpression {

    override fun getExprList(): List<SqlExpr> {
        return children.filterIsInstance<SqlExpr>()
    }
}

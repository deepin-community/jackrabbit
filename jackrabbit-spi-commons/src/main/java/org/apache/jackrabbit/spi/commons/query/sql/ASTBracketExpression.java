/* Generated By:JJTree: Do not edit this line. ASTBracketExpression.java */

package org.apache.jackrabbit.spi.commons.query.sql;

public class ASTBracketExpression extends SimpleNode {
  public ASTBracketExpression(int id) {
    super(id);
  }

  public ASTBracketExpression(JCRSQLParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(JCRSQLParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}

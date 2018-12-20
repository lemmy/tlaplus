// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.

package tlc2.tool;

import java.io.Serializable;

import tla2sany.semantic.OpDefNode;
import tla2sany.semantic.SemanticNode;
import tlc2.util.Context;
import util.UniqueString;

public final class Action implements ToolGlobals, Serializable {
  public static final UniqueString UNNAMED_ACTION = UniqueString.uniqueStringOf("UnnamedAction");

  /* A TLA+ action.   */

  /* Fields  */
  public final SemanticNode pred;     // Expression of the action
  public final Context con;           // Context of the action
  private final UniqueString actionName;
  private OpDefNode opDef = null;
  public CostModel cm = CostModel.DO_NOT_RECORD;

  /* Constructors */
  public Action(SemanticNode pred, Context con) {
	  this(pred, con, UNNAMED_ACTION);
  }

  public Action(SemanticNode pred, Context con, UniqueString actionName) {
	  this.pred = pred;
	  this.con = con;
	  this.actionName = actionName;
  }

  public Action(SemanticNode pred, Context con, OpDefNode opDef) {
	  this(pred, con, opDef.getName());
	  this.opDef = opDef;
  }

/* Returns a string representation of this action.  */
  public final String toString() {
    return "<Action " + pred.toString() + ">";
  }

  public final String getLocation() {
	  if (actionName != UNNAMED_ACTION && actionName != null && !"".equals(actionName.toString())) {
		  // If known, print the action name instead of the generic string "Action".
	      return "<" + actionName + " " +  pred.getLocation() + ">";
	  }
	  return "<Action " + pred.getLocation() + ">";
  }
  
  /**
   * @return The name of this action. Can be {@link Action#UNNAMED_ACTION}.
   */
  public final UniqueString getName() {
	  return actionName;
  }
  
  public final OpDefNode getOpDef() {
	  return this.opDef;
  }
}

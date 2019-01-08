// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Wed 12 Jul 2017 at 16:10:00 PST by ian morris nieves
//      modified on Mon 30 Apr 2007 at 13:21:03 PST by lamport
//      modified on Fri Aug 10 15:09:28 PDT 2001 by yuanyu

package tlc2.value;

import java.io.IOException;

import tlc2.tool.FingerprintException;
import util.Assert;

public class SetCupValue extends EnumerableValue implements Enumerable {
  public final Value set1;
  public final Value set2;
  protected SetEnumValue cupSet;

  /* Constructor */
  public SetCupValue(Value set1, Value set2) {
    this.set1 = set1;
    this.set2 = set2;
    this.cupSet = null;
  }

  public final byte getKind() { return SETCUPVALUE; }

  public final int compareTo(Object obj) {
    try {
      this.convertAndCache();
      return this.cupSet.compareTo(obj);
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

  public final boolean equals(Object obj) {
    try {
      this.convertAndCache();
      return this.cupSet.equals(obj);
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

  public final boolean member(Value elem) {
    try {
      return this.set1.member(elem) || this.set2.member(elem);
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

  public final boolean isFinite() {
    try {
      return this.set1.isFinite() && this.set2.isFinite();
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

  public final Value takeExcept(ValueExcept ex) {
    try {
      if (ex.idx < ex.path.length) {
        Assert.fail("Attempted to apply EXCEPT to the set " + ppr(this.toString()) + ".");
      }
      return ex.value;
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

  public final Value takeExcept(ValueExcept[] exs) {
    try {
      if (exs.length != 0) {
        Assert.fail("Attempted to apply EXCEPT to the set " + ppr(this.toString()) + ".");
      }
      return this;
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

  public final int size() {
    try {
      this.convertAndCache();
      return this.cupSet.size();
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

  public final boolean isNormalized() {
    try {
      return (this.cupSet != null &&
        this.cupSet != DummyEnum &&
        this.cupSet.isNormalized());
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

  public final Value normalize() {
    try {
      if (this.cupSet != null && this.cupSet != DummyEnum) {
        this.cupSet.normalize();
      }
      return this;
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

  @Override
  public final void deepNormalize() {
	    try {
      set1.deepNormalize();
      set2.deepNormalize();
      if (cupSet == null) {
        cupSet = DummyEnum;
      }
      else if (cupSet != DummyEnum) {
        cupSet.deepNormalize();
      }
	    }
	    catch (RuntimeException | OutOfMemoryError e) {
	      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
	      else { throw e; }
	    }
  }
  
  public final boolean isDefined() {
    try {
      return this.set1.isDefined() && this.set2.isDefined();
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

  public final Value deepCopy() { return this; }

  public final boolean assignable(Value val) {
    try {
      return this.equals(val);
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

	@Override
	public final void write(final ValueOutputStream vos) throws IOException {
		cupSet.write(vos);
	}

  /* The fingerprint methods */
  public final long fingerPrint(long fp) {
    try {
      this.convertAndCache();
      return this.cupSet.fingerPrint(fp);
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

  public final Value permute(MVPerm perm) {
    try {
      this.convertAndCache();
      return this.cupSet.permute(perm);
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

  private final void convertAndCache() {
    if (this.cupSet == null) {
      this.cupSet = this.toSetEnum();
    }
    else if (this.cupSet == DummyEnum) {
      SetEnumValue val = null;
      synchronized(this) {
        if (this.cupSet == DummyEnum) {
          val = this.toSetEnum();
          val.deepNormalize();
        }
      }
      synchronized(this) {
        if (this.cupSet == DummyEnum) {	this.cupSet = val; }
      }
    }
  }

  @Override
  public final SetEnumValue toSetEnum() {
      if (this.cupSet != null && this.cupSet != DummyEnum) {
        return this.cupSet;
      }
      ValueVec vals = new ValueVec();
      ValueEnumeration Enum = this.elements();
      Value elem;
      while ((elem = Enum.nextElement()) != null) {
        vals.addElement(elem);
      }
      return new SetEnumValue(vals, false);
  }

  /* String representation of the value. */
  public final StringBuffer toString(StringBuffer sb, int offset) {
    try {
      try {
        if (expand) {
          Value val = this.toSetEnum();
          return val.toString(sb, offset);
        }
      }
      catch (Throwable e) { /*SKIP*/ }

      sb = this.set1.toString(sb, offset);
      sb = sb.append(" \\cup ");
      sb = this.set2.toString(sb, offset);
      return sb;
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }
  }

  public final ValueEnumeration elements() {
    try {
      if (this.cupSet == null || this.cupSet == DummyEnum) {
        return new Enumerator();
      }
      return this.cupSet.elements();
    }
    catch (RuntimeException | OutOfMemoryError e) {
      if (hasSource()) { throw FingerprintException.getNewHead(this, e); }
      else { throw e; }
    }

  }

  final class Enumerator implements ValueEnumeration {
    ValueEnumeration enum1;
    ValueEnumeration enum2;

    public Enumerator() {
      if ((set1 instanceof Enumerable) &&
          (set2 instanceof Enumerable)) {
        this.enum1 = ((Enumerable)set1).elements();
        this.enum2 = ((Enumerable)set2).elements();
      }
      else {
        Assert.fail("Attempted to enumerate S \\cup T when S:\n" +
              ppr(set1.toString()) + "\nand T:\n" + ppr(set2.toString()) +
              "\nare not both enumerable");
      }
    }

    public final void reset() {
      this.enum1.reset();
      this.enum2.reset();
    }

    public final Value nextElement() {
      Value elem = this.enum1.nextElement();
      if (elem != null) return elem;
      elem = this.enum2.nextElement();
      return elem;
    }
  }

}

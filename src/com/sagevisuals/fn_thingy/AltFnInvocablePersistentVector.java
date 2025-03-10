/**
 *   Copyright (c) Rich Hickey. All rights reserved.
 *   The use and distribution terms for this software are covered by the
 *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 *   which can be found in the file epl-v10.html at the root of this distribution.
 *   By using this software in any fashion, you are agreeing to be bound by
 * 	 the terms of this license.
 *   You must not remove this notice, or any other, from this software.
 **/

/* rich Jul 5, 2007 */
/* Ajusted 2025 March 03 by Brad Losavio to add capability to change invocation function. */

package com.sagevisuals;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import clojure.lang.*;

public class AltFnInvocablePersistentVector extends APersistentVector implements IObj, IEditableCollection, IReduce, IKVReduce, IDrop{

    private static final long serialVersionUID = -7896022351281214157L;

    public static class Node implements Serializable {
	transient public final AtomicReference<Thread> edit;
	public final Object[] array;

	public Node(AtomicReference<Thread> edit, Object[] array){
	    this.edit = edit;
	    this.array = array;
	}

	Node(AtomicReference<Thread> edit){
	    this.edit = edit;
	    this.array = new Object[32];
	}
    }

    final static AtomicReference<Thread> NOEDIT = new AtomicReference<Thread>(null);
    public final static Node EMPTY_NODE = new Node(NOEDIT, new Object[32]);

    final int cnt;
    public final int shift;
    public final Node root;
    public final Object[] tail;
    final IPersistentMap _meta;
    public clojure.lang.Atom options;

    // Changed to a public function (and removed `final` modifier) so that
    // `options` map (contained in an Atom) may be dynamically passed.

    public static AltFnInvocablePersistentVector EMPTY(clojure.lang.Atom options) {
	return new AltFnInvocablePersistentVector(0, 5, EMPTY_NODE, new Object[]{}, options);
    }

    // anonymous class declaration extending AFn

    private static final IFn TRANSIENT_VECTOR_CONJ = new AFn() {
	    public Object invoke(Object coll, Object val) {
		return ((ITransientVector)coll).conj(val);
	    }
	    public Object invoke(Object coll) {
		return coll;
	    }
	};

    // The following `adopt()` method is problematic, because some external
    // caller might assume it exists, analagously to clojure.lang.PersistentVector,
    // but that external caller might not be aware of the additional requirement
    // of this class to pass the `options` atom to the constructor.
    //
    // However, I can see zero calls to this method from within this class, and
    // the Clojure code base does not appear to make any calls to *any* method
    // with that name. Furthermore, I can not find that this is a method
    // inherited of any Java lang class or any particular idiomatic usage.
    //
    // The `adopt()` method is, in fact, vestigal.
    //
    // The `adopt(Object [] items)` method was added on 2015 July 17 during
    // commit 36d6657 (https://github.com/clojure/clojure/commit/36d665793b43f62cfd22354aced4c6892088abd6#diff-6882a91e533bf76011bfc118f3bbd318d89c38acaaf8b5263ccae4f74b2c940d)
    // for version 1.8.0-RC1. The `adopt` method is called from the `vec()`
    // method of the ATuple abstraction, which extends the APersistentVector
    // abstraction.
    //
    // On 2015 July 26 during commit 8383026 (https://github.com/clojure/clojure/commit/838302612551ef6a50a8adbdb9708cb1362b0898),
    // also for version 1.8.0-RC1, that tuple implementation was "disabled" (as
    // described in the commit message), eliminating ATuple's call to
    // clojure.lang.PersistentVector's `adopt()` method.
    //
    // Since the method signature is both problematic and apparently un-used, I
    // will comment it out.

    /*
      static public AltFnInvocablePersistentVector adopt(Object [] items){
	  return new AltFnInvocablePersistentVector(items.length, 5, EMPTY_NODE, items);
      }
    */

    static public AltFnInvocablePersistentVector create(clojure.lang.Atom options, IReduceInit items) {
	TransientVector ret = EMPTY(options).asTransient();
	items.reduce(TRANSIENT_VECTOR_CONJ, ret);
	return ret.persistent();
    }

    static public AltFnInvocablePersistentVector create(clojure.lang.Atom options, ISeq items){
	Object[] arr = new Object[32];
	int i = 0;
	for(;items != null && i < 32; items = items.next())
	    arr[i++] = items.first();

	if(items != null) {  // >32, construct with array directly
	    AltFnInvocablePersistentVector start = new AltFnInvocablePersistentVector(32, 5, EMPTY_NODE, arr, options);
	    TransientVector ret = start.asTransient();
	    for (; items != null; items = items.next())
		ret = ret.conj(items.first());
	    return ret.persistent();
	} else if(i == 32) {   // exactly 32, skip copy
	    return new AltFnInvocablePersistentVector(32, 5, EMPTY_NODE, arr, options);
	} else {  // <32, copy to minimum array and construct
	    Object[] arr2 = new Object[i];
	    System.arraycopy(arr, 0, arr2, 0, i);
	    return new AltFnInvocablePersistentVector(i, 5, EMPTY_NODE, arr2, options);
	}
    }

    static public AltFnInvocablePersistentVector create(clojure.lang.Atom options, List list){
	int size = list.size();
	if (size <= 32)
	    return new AltFnInvocablePersistentVector(size, 5, AltFnInvocablePersistentVector.EMPTY_NODE, list.toArray(), options);

	TransientVector ret = EMPTY(options).asTransient();
	for(int i=0; i<size; i++)
	    ret = ret.conj(list.get(i));
	return ret.persistent();
    }

    static public AltFnInvocablePersistentVector create(clojure.lang.Atom options, Iterable items){
	// optimize common case
	if(items instanceof ArrayList)
	    return create(options, (ArrayList)items);

	Iterator iter = items.iterator();
	TransientVector ret = EMPTY(options).asTransient();
	while(iter.hasNext())
	    ret = ret.conj(iter.next());
	return ret.persistent();
    }

    static public AltFnInvocablePersistentVector create(clojure.lang.Atom options, Object... items){
	TransientVector ret = EMPTY(options).asTransient();
	for(Object item : items)
	    ret = ret.conj(item);
	return ret.persistent();
    }

    AltFnInvocablePersistentVector(int cnt, int shift, Node root, Object[] tail, clojure.lang.Atom options){
	this._meta = null;
	this.cnt = cnt;
	this.shift = shift;
	this.root = root;
	this.tail = tail;
	this.options = options;
    }


    AltFnInvocablePersistentVector(IPersistentMap meta, int cnt, int shift, Node root, Object[] tail, clojure.lang.Atom options){
	this._meta = meta;
	this.cnt = cnt;
	this.shift = shift;
	this.root = root;
	this.tail = tail;
	this.options = options;
    }

    public TransientVector asTransient(){
	return new TransientVector(this);
    }

    final int tailoff(){
	if(cnt < 32)
	    return 0;
	return ((cnt - 1) >>> 5) << 5;
    }

    public Object[] arrayFor(int i){
	if(i >= 0 && i < cnt)
	    {
		if(i >= tailoff())
		    return tail;
		Node node = root;
		for(int level = shift; level > 0; level -= 5)
		    node = (Node) node.array[(i >>> level) & 0x01f];
		return node.array;
	    }
	throw new IndexOutOfBoundsException();
    }

    public Object nth(int i){
	Object[] node = arrayFor(i);
	return node[i & 0x01f];
    }

    public Object nth(int i, Object notFound){
	if(i >= 0 && i < cnt)
	    return nth(i);
	return notFound;
    }

    public AltFnInvocablePersistentVector assocN(int i, Object val){
	if(i >= 0 && i < cnt)
	    {
		if(i >= tailoff())
		    {
			Object[] newTail = new Object[tail.length];
			System.arraycopy(tail, 0, newTail, 0, tail.length);
			newTail[i & 0x01f] = val;

			return new AltFnInvocablePersistentVector(meta(), cnt, shift, root, newTail, this.options);
		    }

		return new AltFnInvocablePersistentVector(meta(), cnt, shift, doAssoc(shift, root, i, val), tail, this.options);
	    }
	if(i == cnt)
	    return cons(val);
	throw new IndexOutOfBoundsException();
    }

    private static Node doAssoc(int level, Node node, int i, Object val){
	Node ret = new Node(node.edit,node.array.clone());
	if(level == 0)
	    {
		ret.array[i & 0x01f] = val;
	    }
	else
	    {
		int subidx = (i >>> level) & 0x01f;
		ret.array[subidx] = doAssoc(level - 5, (Node) node.array[subidx], i, val);
	    }
	return ret;
    }

    public int count(){
	return cnt;
    }

    public AltFnInvocablePersistentVector withMeta(IPersistentMap meta){
	if(meta() == meta)
	    return this;
	return new AltFnInvocablePersistentVector(meta, cnt, shift, root, tail, this.options);
    }

    public IPersistentMap meta(){
	return _meta;
    }


    public AltFnInvocablePersistentVector cons(Object val){
	//room in tail?
	//	if(tail.length < 32)
	if(cnt - tailoff() < 32)
	    {
		Object[] newTail = new Object[tail.length + 1];
		System.arraycopy(tail, 0, newTail, 0, tail.length);
		newTail[tail.length] = val;
		return new AltFnInvocablePersistentVector(meta(), cnt + 1, shift, root, newTail, this.options);
	    }
	//full tail, push into tree
	Node newroot;
	Node tailnode = new Node(root.edit,tail);
	int newshift = shift;
	//overflow root?
	if((cnt >>> 5) > (1 << shift))
	    {
		newroot = new Node(root.edit);
		newroot.array[0] = root;
		newroot.array[1] = newPath(root.edit,shift, tailnode);
		newshift += 5;
	    }
	else
	    newroot = pushTail(shift, root, tailnode);
	return new AltFnInvocablePersistentVector(meta(), cnt + 1, newshift, newroot, new Object[]{val}, this.options);
    }

    private Node pushTail(int level, Node parent, Node tailnode){
	//if parent is leaf, insert node,
	// else does it map to an existing child? -> nodeToInsert = pushNode one more level
	// else alloc new path
	//return  nodeToInsert placed in copy of parent
	int subidx = ((cnt - 1) >>> level) & 0x01f;
	Node ret = new Node(parent.edit, parent.array.clone());
	Node nodeToInsert;
	if(level == 5)
	    {
		nodeToInsert = tailnode;
	    }
	else
	    {
		Node child = (Node) parent.array[subidx];
		nodeToInsert = (child != null)?
		    pushTail(level-5,child, tailnode)
		    :newPath(root.edit,level-5, tailnode);
	    }
	ret.array[subidx] = nodeToInsert;
	return ret;
    }

    private static Node newPath(AtomicReference<Thread> edit,int level, Node node){
	if(level == 0)
	    return node;
	Node ret = new Node(edit);
	ret.array[0] = newPath(edit, level - 5, node);
	return ret;
    }

    public IChunkedSeq chunkedSeq(){
	if(count() == 0)
	    return null;
	return new ChunkedSeq(this,0,0, this.options);
    }

    public ISeq seq(){
	return chunkedSeq();
    }

    // @Override
    Iterator rangedIterator(final int start, final int end){
	return new Iterator(){
	    int i = start;
	    int base = i - (i%32);
	    Object[] array = (start < count())?arrayFor(i):null;

	    public boolean hasNext(){
		return i < end;
	    }

	    public Object next(){
		if(i < end) {
		    if(i-base == 32){
			array = arrayFor(i);
			base += 32;
		    }
		    return array[i++ & 0x01f];
		} else {
		    throw new NoSuchElementException();
		}
	    }

	    public void remove(){
		throw new UnsupportedOperationException();
	    }
	};
    }

    public Iterator iterator(){return rangedIterator(0,count());}

    // @Override
    Spliterator rangedSpliterator(final int start, final int end){
	return new Spliterator(){
	    int i = start;
	    int base = i - (i%32);
	    Object[] array = (start < count())?arrayFor(i):null;

	    @Override
	    public int characteristics() {
		return Spliterator.IMMUTABLE |   // persistent
		    Spliterator.ORDERED |    // know order
		    Spliterator.SIZED |      // know size
		    Spliterator.SUBSIZED;    // know size after split
	    }

	    @Override
	    public long estimateSize() {
		return end-i;
	    }

	    @Override
	    public long getExactSizeIfKnown() {
		return end-i;
	    }

	    @Override
	    public boolean tryAdvance(Consumer action) {
		if(i < end) {
		    if(i-base == 32){
			array = arrayFor(i);
			base += 32;
		    }
		    action.accept(array[i++ & 0x01f]);
		    return true;
		}
		return false;
	    }

	    @Override
	    public Spliterator trySplit() {
		int lo = i;
		int mid = (lo + end) >>> 1; // avoid overflow
		if(lo >= mid) {
		    return null;
		} else {
		    i = mid;
		    return rangedSpliterator(lo, mid);
		}
	    }

	    @Override
	    public void forEachRemaining(Consumer action) {
		int x=i;
		while(x<end){
		    Object[] array = arrayFor(x);
		    int remaining = end-x;
		    int offset = x & 0x01f;
		    int limit = Math.min(array.length, offset + remaining);
		    for(int j=offset; j<limit; ++j){
			action.accept(array[j]);
		    }
		    x+= (limit-offset);
		}
		i=end; // done
		this.array=null;
	    }
	};
    }

    public Spliterator spliterator(){return rangedSpliterator(0,count());}

    public Object reduce(IFn f){
	Object init;
	if (cnt > 0)
	    init = arrayFor(0)[0];
	else
	    return f.invoke();
	int step = 0;
	for(int i=0;i<cnt;i+=step){
	    Object[] array = arrayFor(i);
	    for(int j = (i==0)?1:0;j<array.length;++j){
		init = f.invoke(init,array[j]);
		if(RT.isReduced(init))
	            return ((IDeref)init).deref();
            }
	    step = array.length;
	}
	return init;
    }

    public Object reduce(IFn f, Object init){
	int step = 0;
	for(int i=0;i<cnt;i+=step){
	    Object[] array = arrayFor(i);
	    for(int j =0;j<array.length;++j){
		init = f.invoke(init,array[j]);
		if(RT.isReduced(init))
	            return ((IDeref)init).deref();
            }
	    step = array.length;
	}
	return init;
    }

    public Object kvreduce(IFn f, Object init){
	int step = 0;
	for(int i=0;i<cnt;i+=step){
	    Object[] array = arrayFor(i);
	    for(int j =0;j<array.length;++j){
		init = f.invoke(init,j+i,array[j]);
		if(RT.isReduced(init))
	            return ((IDeref)init).deref();
            }
	    step = array.length;
	}
	return init;
    }

    public Sequential drop(int n) {
	if(n < cnt) {
	    int offset = n%32;
	    return new ChunkedSeq(this, this.arrayFor(n), n-offset, offset, this.options);
	} else {
	    return null;
	}
    }

    @Override
    public String toString() {
	// Run-of-the-mill Clojure vectors are delimited by a preceeding left
	// square bracket char, [, and a trailing right bracket char, ]. To
	// distinguish this class, this instance methods trims the leading and
	// trailing brackets and concatenate the (presumably different)
	// delimiters specified in the `options` atom.

	// Note: This implementation assumes that the opening left bracket is
	// exactly the first char and the closing right bracket is exactly the
	// last char.

	// Note: This implementation uses the `.substring()` method, which is
	// O(n) in time and O(1) in space. For now, assume that `.toString()`
	// is not on a performance-critical path.

	// `options` is an instance field which is assigned a Clojure atom
	// during a call to the constructor. The atom contains a Clojure
	// hash-map with the following MapEntry elements:
	// :fn                a 2-arity Clojure function
	// :left-delimiter    a string
	// :right-delimiter   a string

	String base_string = super.toString();
	String trimmed_string = base_string.substring(1, base_string.length() - 1);

	clojure.lang.Keyword leftKeyword = clojure.lang.Keyword.intern("left-delimiter");
	clojure.lang.Keyword rightKeyword = clojure.lang.Keyword.intern("right-delimiter");

	clojure.lang.APersistentMap optionsMap = (clojure.lang.APersistentMap) this.options.deref();

	String left = (String) optionsMap.valAt(leftKeyword);
	String right = (String) optionsMap.valAt(rightKeyword);

	return left + trimmed_string + right;
    }

    // I would very much prefer to avoid the following duplicated methods by
    // overriding the superclass' `invoke` methods with a single varargs
    // declaration of the form
    //
    // public Object invoke(Object... args)
    //
    // However, clojure.lang.AFn explicitly defines each arity
    //
    // invoke()
    // invoke(Object arg1)
    // invoke(Object arg1, Object arg2)
    // invoke(Object arg1, Object arg2, Object arg3)
    // etc.
    // <up to twenty-one args>
    //
    // However, my understandinging of *The Java Language Specification*
    // §15.12.2 is that those methods with enumerated arities in the superclass
    // will take precedence over the varargs method in the subclass because
    // the enumerated methods in the superclass are more specific. Therefore,
    // the varargs method in the subclass will never be called.
    //
    // To override the enumerated-arity methods of the superclass, this subclass
    // must also define the `invoke` methods with explicit arities (i.e., not
    // varargs).

    @Override
    public Object invoke() {

	clojure.lang.APersistentMap optionsMap = (clojure.lang.APersistentMap) this.options.deref();
	clojure.lang.Keyword fnKeyword = clojure.lang.Keyword.intern("fn");
	clojure.lang.AFn fn = (clojure.lang.AFn) optionsMap.valAt(fnKeyword);

	return fn.applyTo(clojure.lang.ArraySeq.create(this));
    }

    @Override
    public Object invoke(Object arg0) {
	// Standard Clojure vectors perform an `nth`/`get` when in the function
	// postion of an S-expression followed by an integer. This instance
	// method makes the behavior dynamic. An instance of this class contains
	// `options`, a Clojure atom that wraps a hash-map. That hash-map holds
	// at least three key-vals, including `:fn`, which associates a 2-arity
	// clojure.lang.Fn, that consumes an instance as the first argument, and
	// any Object as the second argument.

	clojure.lang.APersistentMap optionsMap = (clojure.lang.APersistentMap) this.options.deref();
	clojure.lang.Keyword fnKeyword = clojure.lang.Keyword.intern("fn");
	clojure.lang.AFn fn = (clojure.lang.AFn) optionsMap.valAt(fnKeyword);

	return fn.applyTo(clojure.lang.ArraySeq.create(this, arg0));
    }

    @Override
    public Object invoke(Object arg0,
			 Object arg1) {

	clojure.lang.APersistentMap optionsMap = (clojure.lang.APersistentMap) this.options.deref();
	clojure.lang.Keyword fnKeyword = clojure.lang.Keyword.intern("fn");
	clojure.lang.AFn fn = (clojure.lang.AFn) optionsMap.valAt(fnKeyword);

	return fn.applyTo(clojure.lang.ArraySeq.create(this,
						       arg0,
						       arg1));
    }

    @Override
    public Object invoke(Object arg0,
			 Object arg1,
			 Object arg2) {

	clojure.lang.APersistentMap optionsMap = (clojure.lang.APersistentMap) this.options.deref();
	clojure.lang.Keyword fnKeyword = clojure.lang.Keyword.intern("fn");
	clojure.lang.AFn fn = (clojure.lang.AFn) optionsMap.valAt(fnKeyword);

	return fn.applyTo(clojure.lang.ArraySeq.create(this,
						       arg0,
						       arg1,
						       arg2));
    }

    @Override
    public Object invoke(Object arg0,
			 Object arg1,
			 Object arg2,
			 Object arg3) {

	clojure.lang.APersistentMap optionsMap = (clojure.lang.APersistentMap) this.options.deref();
	clojure.lang.Keyword fnKeyword = clojure.lang.Keyword.intern("fn");
	clojure.lang.AFn fn = (clojure.lang.AFn) optionsMap.valAt(fnKeyword);

	return fn.applyTo(clojure.lang.ArraySeq.create(this,
						       arg0,
						       arg1,
						       arg2,
						       arg3));
    }

    @Override
    public Object invoke(Object arg0,
			 Object arg1,
			 Object arg2,
			 Object arg3,
			 Object arg4) {

	clojure.lang.APersistentMap optionsMap = (clojure.lang.APersistentMap) this.options.deref();
	clojure.lang.Keyword fnKeyword = clojure.lang.Keyword.intern("fn");
	clojure.lang.AFn fn = (clojure.lang.AFn) optionsMap.valAt(fnKeyword);

	return fn.applyTo(clojure.lang.ArraySeq.create(this,
						       arg0,
						       arg1,
						       arg2,
						       arg3,
						       arg4));
    }

    @Override
    public Object invoke(Object arg0,
			 Object arg1,
			 Object arg2,
			 Object arg3,
			 Object arg4,
			 Object arg5) {

	clojure.lang.APersistentMap optionsMap = (clojure.lang.APersistentMap) this.options.deref();
	clojure.lang.Keyword fnKeyword = clojure.lang.Keyword.intern("fn");
	clojure.lang.AFn fn = (clojure.lang.AFn) optionsMap.valAt(fnKeyword);

	return fn.applyTo(clojure.lang.ArraySeq.create(this,
						       arg0,
						       arg1,
						       arg2,
						       arg3,
						       arg4,
						       arg5));
    }

    @Override
    public Object invoke(Object arg0,
			 Object arg1,
			 Object arg2,
			 Object arg3,
			 Object arg4,
			 Object arg5,
			 Object arg6) {

	clojure.lang.APersistentMap optionsMap = (clojure.lang.APersistentMap) this.options.deref();
	clojure.lang.Keyword fnKeyword = clojure.lang.Keyword.intern("fn");
	clojure.lang.AFn fn = (clojure.lang.AFn) optionsMap.valAt(fnKeyword);

	return fn.applyTo(clojure.lang.ArraySeq.create(this,
						       arg0,
						       arg1,
						       arg2,
						       arg3,
						       arg4,
						       arg5,
						       arg6));
    }
    
    @Override
    public Object invoke(Object arg0,
			 Object arg1,
			 Object arg2,
			 Object arg3,
			 Object arg4,
			 Object arg5,
			 Object arg6,
			 Object arg7) {

	clojure.lang.APersistentMap optionsMap = (clojure.lang.APersistentMap) this.options.deref();
	clojure.lang.Keyword fnKeyword = clojure.lang.Keyword.intern("fn");
	clojure.lang.AFn fn = (clojure.lang.AFn) optionsMap.valAt(fnKeyword);

	return fn.applyTo(clojure.lang.ArraySeq.create(this,
						       arg0,
						       arg1,
						       arg2,
						       arg3,
						       arg4,
						       arg5,
						       arg6,
						       arg7));
    }

    static public final class ChunkedSeq extends ASeq implements IChunkedSeq,Counted,IReduce,IDrop{

	public final AltFnInvocablePersistentVector vec;
	final Object[] node;
	final int i;
	public final int offset;
	public clojure.lang.Atom options;

	public ChunkedSeq(AltFnInvocablePersistentVector vec, int i, int offset, clojure.lang.Atom options){
	    this.vec = vec;
	    this.i = i;
	    this.offset = offset;
	    this.node = vec.arrayFor(i);
	    this.options = options;
	}

	ChunkedSeq(IPersistentMap meta, AltFnInvocablePersistentVector vec, Object[] node, int i, int offset, clojure.lang.Atom options){
	    super(meta);
	    this.vec = vec;
	    this.node = node;
	    this.i = i;
	    this.offset = offset;
	    this.options = options;
	}

	ChunkedSeq(AltFnInvocablePersistentVector vec, Object[] node, int i, int offset, clojure.lang.Atom options){
	    this.vec = vec;
	    this.node = node;
	    this.i = i;
	    this.offset = offset;
	    this.options = options;
	}

	public IChunk chunkedFirst() {
	    return new ArrayChunk(node, offset);
	}

	public ISeq chunkedNext(){
	    if(i + node.length < vec.cnt)
		return new ChunkedSeq(vec,i+ node.length,0, this.options);
	    return null;
	}

	public ISeq chunkedMore(){
	    ISeq s = chunkedNext();
	    if(s == null)
		return PersistentList.EMPTY;
	    return s;
	}

	public Obj withMeta(IPersistentMap meta){
	    /* 2025 March 03 Note: _meta is not public in Obj and cannot be
	       accessed from outside the clojure.lang package. The `if` clause
	       appears to be an optimization that dispenses with creating a new
	       ChunkedSeq if the metadata matches. If we skip the optimization
	       and create a new ChunkedSeq anyways, we don't need to access
	       _meta.
	    */
	    
	    //if(meta == this._meta)
	    //		return this;
	    return new ChunkedSeq(meta, vec, node, i, offset, this.options);
	}

	public Object first(){
	    return node[offset];
	}

	public ISeq next(){
	    if(offset + 1 < node.length)
		return new ChunkedSeq(vec, node, i, offset + 1, this.options);
	    return chunkedNext();
	}

	public int count(){
	    return vec.cnt - (i + offset);
	}

	public Iterator iterator() {
	    return vec.rangedIterator(i +offset, vec.cnt);
	}

	public Object reduce(IFn f) {
	    Object acc;
	    if (i +offset < vec.cnt)
		acc = node[offset];
	    else
		return f.invoke();

	    for(int j=offset+1;j<node.length;++j){
		acc = f.invoke(acc,node[j]);
		if(RT.isReduced(acc))
		    return ((IDeref)acc).deref();
	    }

	    int step = 0;
	    for(int ii = i +node.length; ii<vec.cnt; ii+=step){
		Object[] array = vec.arrayFor(ii);
		for(int j = 0;j<array.length;++j){
		    acc = f.invoke(acc,array[j]);
		    if(RT.isReduced(acc))
			return ((IDeref)acc).deref();
		}
		step = array.length;
	    }
	    return acc;
	}

	public Object reduce(IFn f, Object init) {
	    Object acc = init;
	    for(int j=offset;j<node.length;++j){
		acc = f.invoke(acc,node[j]);
		if(RT.isReduced(acc))
		    return ((IDeref)acc).deref();
	    }

	    int step = 0;
	    for(int ii = i +node.length; ii<vec.cnt; ii+=step){
		Object[] array = vec.arrayFor(ii);
		for(int j = 0;j<array.length;++j){
		    acc = f.invoke(acc,array[j]);
		    if(RT.isReduced(acc))
			return ((IDeref)acc).deref();
		}
		step = array.length;
	    }
	    return acc;
	}

	public Sequential drop(int n) {
	    int o = offset + n;
	    if(o < node.length) { // in current array
		return new ChunkedSeq(vec, node, i, o, this.options);
	    } else {
		int i = this.i +o;
		if(i < vec.cnt) { // in vec
		    int newOffset = i%32;
		    return new ChunkedSeq(vec, vec.arrayFor(i), i-newOffset, newOffset, this.options);
		} else {
		    return null;
		}
	    }
	}
    }

    public IPersistentCollection empty(){
	return EMPTY(this.options).withMeta(meta());
    }

    //private Node pushTail(int level, Node node, Object[] tailNode, Box expansion){
    //	Object newchild;
    //	if(level == 0)
    //		{
    //		newchild = tailNode;
    //		}
    //	else
    //		{
    //		newchild = pushTail(level - 5, (Object[]) arr[arr.length - 1], tailNode, expansion);
    //		if(expansion.val == null)
    //			{
    //			Object[] ret = arr.clone();
    //			ret[arr.length - 1] = newchild;
    //			return ret;
    //			}
    //		else
    //			newchild = expansion.val;
    //		}
    //	//expansion
    //	if(arr.length == 32)
    //		{
    //		expansion.val = new Object[]{newchild};
    //		return arr;
    //		}
    //	Object[] ret = new Object[arr.length + 1];
    //	System.arraycopy(arr, 0, ret, 0, arr.length);
    //	ret[arr.length] = newchild;
    //	expansion.val = null;
    //	return ret;
    //}

    public AltFnInvocablePersistentVector pop(){
	if(cnt == 0)
	    throw new IllegalStateException("Can't pop empty vector");
	if(cnt == 1)
	    return EMPTY(this.options).withMeta(meta());
	//if(tail.length > 1)
	if(cnt-tailoff() > 1)
	    {
		Object[] newTail = new Object[tail.length - 1];
		System.arraycopy(tail, 0, newTail, 0, newTail.length);
		return new AltFnInvocablePersistentVector(meta(), cnt - 1, shift, root, newTail, this.options);
	    }
	Object[] newtail = arrayFor(cnt - 2);

	Node newroot = popTail(shift, root);
	int newshift = shift;
	if(newroot == null)
	    {
		newroot = EMPTY_NODE;
	    }
	if(shift > 5 && newroot.array[1] == null)
	    {
		newroot = (Node) newroot.array[0];
		newshift -= 5;
	    }
	return new AltFnInvocablePersistentVector(meta(), cnt - 1, newshift, newroot, newtail, this.options);
    }

    private Node popTail(int level, Node node){
	int subidx = ((cnt-2) >>> level) & 0x01f;
	if(level > 5)
	    {
		Node newchild = popTail(level - 5, (Node) node.array[subidx]);
		if(newchild == null && subidx == 0)
		    return null;
		else
		    {
			Node ret = new Node(root.edit, node.array.clone());
			ret.array[subidx] = newchild;
			return ret;
		    }
	    }
	else if(subidx == 0)
	    return null;
	else
	    {
		Node ret = new Node(root.edit, node.array.clone());
		ret.array[subidx] = null;
		return ret;
	    }
    }

    static final class TransientVector extends AFn implements ITransientVector, ITransientAssociative2, Counted{
	volatile int cnt;
	volatile int shift;
	volatile Node root;
	volatile Object[] tail;
	public clojure.lang.Atom options;

	TransientVector(int cnt, int shift, Node root, Object[] tail, clojure.lang.Atom options){
	    this.cnt = cnt;
	    this.shift = shift;
	    this.root = root;
	    this.tail = tail;
	    this.options = options;
	}

	TransientVector(AltFnInvocablePersistentVector v){
	    this(v.cnt, v.shift, editableRoot(v.root), editableTail(v.tail), v.options);
	}

	public int count(){
	    ensureEditable();
	    return cnt;
	}

	Node ensureEditable(Node node){
	    if(node.edit == root.edit)
		return node;
	    return new Node(root.edit, node.array.clone());
	}

	void ensureEditable(){
	    if(root.edit.get() == null)
		throw new IllegalAccessError("Transient used after persistent! call");

	    //		root = editableRoot(root);
	    //		tail = editableTail(tail);
	}

	static Node editableRoot(Node node){
	    return new Node(new AtomicReference<Thread>(Thread.currentThread()), node.array.clone());
	}

	public AltFnInvocablePersistentVector persistent(){
	    ensureEditable();
	    //		Thread owner = root.edit.get();
	    //		if(owner != null && owner != Thread.currentThread())
	    //			{
	    //			throw new IllegalAccessError("Mutation release by non-owner thread");
	    //			}
	    root.edit.set(null);
	    Object[] trimmedTail = new Object[cnt-tailoff()];
	    System.arraycopy(tail,0,trimmedTail,0,trimmedTail.length);
	    return new AltFnInvocablePersistentVector(cnt, shift, root, trimmedTail, this.options);
	}

	static Object[] editableTail(Object[] tl){
	    Object[] ret = new Object[32];
	    System.arraycopy(tl,0,ret,0,tl.length);
	    return ret;
	}

	public TransientVector conj(Object val){
	    ensureEditable();
	    int i = cnt;
	    //room in tail?
	    if(i - tailoff() < 32)
		{
		    tail[i & 0x01f] = val;
		    ++cnt;
		    return this;
		}
	    //full tail, push into tree
	    Node newroot;
	    Node tailnode = new Node(root.edit, tail);
	    tail = new Object[32];
	    tail[0] = val;
	    int newshift = shift;
	    //overflow root?
	    if((cnt >>> 5) > (1 << shift))
		{
		    newroot = new Node(root.edit);
		    newroot.array[0] = root;
		    newroot.array[1] = newPath(root.edit,shift, tailnode);
		    newshift += 5;
		}
	    else
		newroot = pushTail(shift, root, tailnode);
	    root = newroot;
	    shift = newshift;
	    ++cnt;
	    return this;
	}

	private Node pushTail(int level, Node parent, Node tailnode){
	    //if parent is leaf, insert node,
	    // else does it map to an existing child? -> nodeToInsert = pushNode one more level
	    // else alloc new path
	    //return  nodeToInsert placed in parent
	    parent = ensureEditable(parent);
	    int subidx = ((cnt - 1) >>> level) & 0x01f;
	    Node ret = parent;
	    Node nodeToInsert;
	    if(level == 5)
		{
		    nodeToInsert = tailnode;
		}
	    else
		{
		    Node child = (Node) parent.array[subidx];
		    nodeToInsert = (child != null) ?
			pushTail(level - 5, child, tailnode)
			: newPath(root.edit, level - 5, tailnode);
		}
	    ret.array[subidx] = nodeToInsert;
	    return ret;
	}

	final private int tailoff(){
	    if(cnt < 32)
		return 0;
	    return ((cnt-1) >>> 5) << 5;
	}

	private Object[] arrayFor(int i){
	    if(i >= 0 && i < cnt)
		{
		    if(i >= tailoff())
			return tail;
		    Node node = root;
		    for(int level = shift; level > 0; level -= 5)
			node = (Node) node.array[(i >>> level) & 0x01f];
		    return node.array;
		}
	    throw new IndexOutOfBoundsException();
	}

	private Object[] editableArrayFor(int i){
	    if(i >= 0 && i < cnt)
		{
		    if(i >= tailoff())
			return tail;
		    Node node = root;
		    for(int level = shift; level > 0; level -= 5)
			node = ensureEditable((Node) node.array[(i >>> level) & 0x01f]);
		    return node.array;
		}
	    throw new IndexOutOfBoundsException();
	}

	public Object valAt(Object key){
	    //note - relies on ensureEditable in 2-arg valAt
	    return valAt(key, null);
	}

	public Object valAt(Object key, Object notFound){
	    ensureEditable();
	    if(Util.isInteger(key))
		{
		    int i = ((Number) key).intValue();
		    if(i >= 0 && i < cnt)
			return nth(i);
		}
	    return notFound;
	}

	private static final Object NOT_FOUND = new Object();
	public final boolean containsKey(Object key){
	    return valAt(key, NOT_FOUND) != NOT_FOUND;
	}

	public final IMapEntry entryAt(Object key){
	    Object v = valAt(key, NOT_FOUND);
	    if(v != NOT_FOUND)
		return MapEntry.create(key, v);
	    return null;
	}

	public Object invoke(Object arg1) {
	    //note - relies on ensureEditable in nth
	    if(Util.isInteger(arg1))
		return nth(((Number) arg1).intValue());
	    throw new IllegalArgumentException("Key must be integer");
	}

	public Object nth(int i){
	    ensureEditable();
	    Object[] node = arrayFor(i);
	    return node[i & 0x01f];
	}

	public Object nth(int i, Object notFound){
	    if(i >= 0 && i < count())
		return nth(i);
	    return notFound;
	}

	public TransientVector assocN(int i, Object val){
	    ensureEditable();
	    if(i >= 0 && i < cnt)
		{
		    if(i >= tailoff())
			{
			    tail[i & 0x01f] = val;
			    return this;
			}

		    root = doAssoc(shift, root, i, val);
		    return this;
		}
	    if(i == cnt)
		return conj(val);
	    throw new IndexOutOfBoundsException();
	}

	public TransientVector assoc(Object key, Object val){
	    //note - relies on ensureEditable in assocN
	    if(Util.isInteger(key))
		{
		    int i = ((Number) key).intValue();
		    return assocN(i, val);
		}
	    throw new IllegalArgumentException("Key must be integer");
	}

	private Node doAssoc(int level, Node node, int i, Object val){
	    node = ensureEditable(node);
	    Node ret = node;
	    if(level == 0)
		{
		    ret.array[i & 0x01f] = val;
		}
	    else
		{
		    int subidx = (i >>> level) & 0x01f;
		    ret.array[subidx] = doAssoc(level - 5, (Node) node.array[subidx], i, val);
		}
	    return ret;
	}

	public TransientVector pop(){
	    ensureEditable();
	    if(cnt == 0)
		throw new IllegalStateException("Can't pop empty vector");
	    if(cnt == 1)
		{
		    cnt = 0;
		    return this;
		}
	    int i = cnt - 1;
	    //pop in tail?
	    if((i & 0x01f) > 0)
		{
		    --cnt;
		    return this;
		}

	    Object[] newtail = editableArrayFor(cnt - 2);

	    Node newroot = popTail(shift, root);
	    int newshift = shift;
	    if(newroot == null)
		{
		    newroot = new Node(root.edit);
		}
	    if(shift > 5 && newroot.array[1] == null)
		{
		    newroot = ensureEditable((Node) newroot.array[0]);
		    newshift -= 5;
		}
	    root = newroot;
	    shift = newshift;
	    --cnt;
	    tail = newtail;
	    return this;
	}

	private Node popTail(int level, Node node){
	    node = ensureEditable(node);
	    int subidx = ((cnt - 2) >>> level) & 0x01f;
	    if(level > 5)
		{
		    Node newchild = popTail(level - 5, (Node) node.array[subidx]);
		    if(newchild == null && subidx == 0)
			return null;
		    else
			{
			    Node ret = node;
			    ret.array[subidx] = newchild;
			    return ret;
			}
		}
	    else if(subidx == 0)
		return null;
	    else
		{
		    Node ret = node;
		    ret.array[subidx] = null;
		    return ret;
		}
	}
    }
    /*
      static public void main(String[] args){
      if(args.length != 3)
      {
      System.err.println("Usage: AltFnInvocablePersistentVector size writes reads");
      return;
      }
      int size = Integer.parseInt(args[0]);
      int writes = Integer.parseInt(args[1]);
      int reads = Integer.parseInt(args[2]);
      //	Vector v = new Vector(size);
      ArrayList v = new ArrayList(size);
      //	v.setSize(size);
      //PersistentArray p = new PersistentArray(size);
      AltFnInvocablePersistentVector p = AltFnInvocablePersistentVector.EMPTY;
      //	MutableVector mp = p.mutable();

      for(int i = 0; i < size; i++)
      {
      v.add(i);
      //		v.set(i, i);
      //p = p.set(i, 0);
      p = p.cons(i);
      //		mp = mp.conj(i);
      }

      Random rand;

      rand = new Random(42);
      long tv = 0;
      System.out.println("ArrayList");
      long startTime = System.nanoTime();
      for(int i = 0; i < writes; i++)
      {
      v.set(rand.nextInt(size), i);
      }
      for(int i = 0; i < reads; i++)
      {
      tv += (Integer) v.get(rand.nextInt(size));
      }
      long estimatedTime = System.nanoTime() - startTime;
      System.out.println("time: " + estimatedTime / 1000000);
      System.out.println("AltFnInvocablePersistentVector");
      rand = new Random(42);
      startTime = System.nanoTime();
      long tp = 0;

      //	AltFnInvocablePersistentVector oldp = p;
      //Random rand2 = new Random(42);

      MutableVector mp = p.mutable();
      for(int i = 0; i < writes; i++)
      {
      //		p = p.assocN(rand.nextInt(size), i);
      mp = mp.assocN(rand.nextInt(size), i);
      //		mp = mp.assoc(rand.nextInt(size), i);
      //dummy set to force perverse branching
      //oldp =	oldp.assocN(rand2.nextInt(size), i);
      }
      for(int i = 0; i < reads; i++)
      {
      //		tp += (Integer) p.nth(rand.nextInt(size));
      tp += (Integer) mp.nth(rand.nextInt(size));
      }
      //	p = mp.immutable();
      //mp.cons(42);
      estimatedTime = System.nanoTime() - startTime;
      System.out.println("time: " + estimatedTime / 1000000);
      for(int i = 0; i < size / 2; i++)
      {
      mp = mp.pop();
      //		p = p.pop();
      v.remove(v.size() - 1);
      }
      p = (AltFnInvocablePersistentVector) mp.immutable();
      //mp.pop();  //should fail
      for(int i = 0; i < size / 2; i++)
      {
      tp += (Integer) p.nth(i);
      tv += (Integer) v.get(i);
      }
      System.out.println("Done: " + tv + ", " + tp);

      }
      //  */
}

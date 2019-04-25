package simpledb.index.hash;

import simpledb.query.Constant;
import simpledb.record.RID;
import java.util.ArrayList;

/**
 * A bucket containing a list of hashes (key, value) in a HashMap
 * can have elements removed or added, but has a maxSize limit
 */
public class Bucket {
    private int index, localDepth, maxSize;
    private ArrayList<BucketVal> contents;

    public Bucket(int index, int localDepth, int size) {
        this.index = index;
        this.localDepth = localDepth;
        this.maxSize = size;
        contents = new ArrayList<>();
    }

    public Bucket(Bucket bucket) {
        this.localDepth = bucket.localDepth;
        this.maxSize = bucket.maxSize;
        this.contents = bucket.contents;
    }

    public boolean isFull() {
        return contents.size() == maxSize;
    }

    public void insert(Constant dataval, RID rid) {
        contents.add(new BucketVal(dataval, rid));
    }

    public void delete(Constant dataval) {
        contents.remove(dataval);
    }

    public int getLocalDepth() {
        return localDepth;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public ArrayList<BucketVal> getContents() {
        return contents;
    }


//    public void setLocalDepth(int localDepth) {
//        this.localDepth = localDepth;
//    }
//
//    public void clearContents() {
//        contents.clear();
//    }
}

package simpledb.index.hash;

import simpledb.query.Constant;
import simpledb.record.RID;

public class BucketVal {
    private Constant dataval;
    private RID rid;

    public BucketVal(Constant dataval, RID rid) {
        this.dataval = dataval;
        this.rid = rid;
    }

    @Override
    public boolean equals(Object o) {
        BucketVal b = (BucketVal) o;
        return this.dataval.equals(b.getDataval());
    }

    public Constant getDataval() {
        return dataval;
    }

    public RID getRid() {
        return rid;
    }
}

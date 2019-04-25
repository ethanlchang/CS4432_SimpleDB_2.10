package simpledb.index.hash;

import simpledb.buffer.PageFormatter;
import simpledb.index.Index;
import simpledb.query.Constant;
import simpledb.query.TableScan;
import simpledb.record.RID;
import simpledb.record.Schema;
import simpledb.record.TableInfo;
import simpledb.tx.Transaction;
import java.util.ArrayList;
import static simpledb.file.Page.BLOCK_SIZE;


/**
 * An extensible hash implementation of the Index interface.
 *
 */
public class ExtensibleHashIndex implements Index {
	private String idxname;
	private Schema sch;
	private Transaction tx;
	private Constant searchkey = null;
	private TableScan ts = null;

	private static int MAX_BUCKET_SIZE; //number of records that can fit in a bucket
	private static int globalDepth, numBuckets;
	private static ArrayList<Bucket> directory;

	/**
	 * Opens an extensible hash index for the specified index.
	 *
	 * @param idxname the name of the index
	 * @param sch     the schema of the index records
	 * @param tx      the calling transaction
	 */
	public ExtensibleHashIndex(String idxname, Schema sch, Transaction tx) {
		this.idxname = idxname;
		this.sch = sch;
		this.tx = tx;

		TableInfo ti = new TableInfo(idxname + "0", sch);
		MAX_BUCKET_SIZE = 4;// BLOCK_SIZE / ti.recordLength();

		if (directory == null) {
			globalDepth = 1;
			numBuckets = (int) Math.pow(2, globalDepth);
			directory = new ArrayList<>();
			directory.add(new Bucket(0, globalDepth, MAX_BUCKET_SIZE));
			directory.add(new Bucket(1, globalDepth, MAX_BUCKET_SIZE));
			System.out.println("New Extensible Hash Index Created");
			System.out.println();
		}


		printTable();
	}

	/**
	 * Positions the index before the first index record
	 * having the specified search key.
	 * The method hashes the search key to determine the bucket,
	 * and then opens a table scan on the file
	 * corresponding to the bucket.
	 * The table scan for the previous bucket (if any) is closed.
	 *
	 * @see Index#beforeFirst(Constant)
	 */
	public void beforeFirst(Constant searchkey) {
		close();
		this.searchkey = searchkey;
		int bucket = getBucketIndex(this.searchkey);
		String tblname = idxname + bucket;
		TableInfo ti = new TableInfo(tblname, sch);
		ts = new TableScan(ti, tx);
		System.out.println("Inserting " + searchkey + " into bucket " + tblname +
				" (bucket " + bucket + ")");
		System.out.println();
	}

	/**
	 * Moves to the next record having the search key.
	 * The method loops through the table scan for the bucket,
	 * looking for a matching record, and returning false
	 * if there are no more such records.
	 *
	 * @see Index#next()
	 */
	public boolean next() {
		while (ts.next())
			if (ts.getVal("dataval").equals(searchkey))
				return true;
		return false;
	}

	/**
	 * Retrieves the dataRID from the current record
	 * in the table scan for the bucket.
	 *
	 * @see Index#getDataRid()
	 */
	public RID getDataRid() {
		int blknum = ts.getInt("block");
		int id = ts.getInt("id");
		return new RID(blknum, id);
	}

	/**
	 * Inserts a new record into the table scan for the bucket.
	 *
	 * @see Index#insert(Constant, RID)
	 */
	public void insert(Constant val, RID rid) {
		System.out.println("=======================================");
		int insertBucketIndex = getBucketIndex(val);
		Bucket insertToBucket = directory.get(insertBucketIndex);
		int oldLocalDepth, newLocalDepth, newBucketIndex;
		oldLocalDepth = insertToBucket.getLocalDepth();
		newLocalDepth = insertToBucket.getLocalDepth() + 1;
		newBucketIndex = insertBucketIndex + (int) Math.pow(2, oldLocalDepth);
		boolean splitting = false;

		if (insertToBucket.isFull()) {
			//if bucket is at global limit, double directory beforehand
			if (insertToBucket.getLocalDepth() == globalDepth
					|| newBucketIndex >= (int) Math.pow(2, globalDepth)) {
				System.out.println("Doubling number of buckets from " +
						(int) Math.pow(2, globalDepth)  + " to " + (int) Math.pow(2, globalDepth + 1));
				expandDirectory();
				//insertBucketIndex = getBucketIndex(val);
			}

			//now either way we can assume localDepth < globalDepth
			if (insertToBucket.getLocalDepth() < globalDepth) {
				System.out.println("Splitting bucket " + idxname + insertBucketIndex
						+ " (bucket " + insertBucketIndex + ")");
				//split the bucket into two
				Bucket temp1, temp2;
				temp1 = new Bucket(insertBucketIndex, newLocalDepth, MAX_BUCKET_SIZE);
				temp2 = new Bucket(newBucketIndex, newLocalDepth, MAX_BUCKET_SIZE);

				for (BucketVal b:
					 insertToBucket.getContents()) {
					Constant dataval = b.getDataval();
					int hash = getBucketIndex(dataval);

					if (hash % (int) Math.pow(2, globalDepth) == insertBucketIndex) {
						temp1.insert(b.getDataval(), b.getRid());
					}
					else {
						temp2.insert(b.getDataval(), b.getRid());
					}
				}

				directory.set(temp1.getIndex(), temp1);
				directory.set(temp2.getIndex(), temp2);

			}
			printTable();
			System.out.println("Finished splitting bucket " + insertBucketIndex +
					" into bucket " + insertBucketIndex + " and bucket " + newBucketIndex);
			splitting = true;
			insert(val, rid);
		}
		else {
			insertToBucket.insert(val, rid);
			System.out.println("=======================================");

		}

		if (!splitting) {
			beforeFirst(val);
			ts.insert();
			ts.setInt("block", rid.blockNumber());
			ts.setInt("id", rid.id());
			ts.setVal("dataval", val);
		}
	}

	/**
	 * Deletes the specified record from the table scan for
	 * the bucket.  The method starts at the beginning of the
	 * scan, and loops through the records until the
	 * specified record is found.
	 *
	 * @see Index#delete(Constant, RID)
	 */
	public void delete(Constant val, RID rid) {
		Bucket deleteFromBucket = directory.get(getBucketIndex(val));
		System.out.println("Deleting " + val + " from bucket " + idxname + getBucketIndex(val));
		deleteFromBucket.delete(val);

		beforeFirst(val);
		while (next())
			if (getDataRid().equals(rid)) {
				ts.delete();
				return;
			}
	}

	/**
	 * Closes the index by closing the current table scan.
	 *
	 * @see Index#close()
	 */
	public void close() {
		if (ts != null)
			ts.close();
	}

	private int getBucketIndex(Constant key) {
//		System.out.println("Num buckets: " + (int) Math.pow(2, globalDepth));
//		System.out.println("Key hashcode: " + Math.abs(key.hashCode())
//				% (int) Math.pow(2, globalDepth));
		return Math.abs(key.hashCode()) % (int) Math.pow(2, globalDepth);
	}

	/**
	 * Increase number of buckets (doubles them)
	 */
	private void expandDirectory() {
		System.out.println("Increasing Global Depth from " + globalDepth + " to " + (globalDepth + 1));
		globalDepth++;
		numBuckets = (int) Math.pow(2, globalDepth);
		//This effectively copies the "pointers" for the newly created buckets
		ArrayList<Bucket> newBuckets = directory;
		directory.addAll(newBuckets);
	}

	/**
	 * Prints out info for table
	 * global depth
	 * then the
	 */
	private void printTable() {
		System.out.println();
		System.out.println("New Global Table");
		System.out.println("Max Bucket Size = " + MAX_BUCKET_SIZE);
		System.out.println("Hash, LocalDepth, Filename, GlobalDepth");
		for (int i = 0; i < directory.size(); i++) {
			Bucket b = directory.get(i);
			if (i == 0) {
				System.out.println(i + " " + b.getLocalDepth() + " "
						+ idxname + b.getIndex() + " " + globalDepth);
			}
			else {
				System.out.println(i + " " + b.getLocalDepth() + " "
						+ idxname + b.getIndex() + " " + 0);
			}
		}
		System.out.println();
	}

	/**
	 * Returns the cost of searching an index file having the
	 * specified number of blocks.
	 *
	 * Assumes that the directory is not already in memory,
	 * so one I/O is needed to load in the directory
	 * and one I/O is needed to load in the data block
	 *
	 * @param numblocks the number of blocks of index records
	 * @param rpb the number of records per block
	 * @return the cost of traversing the index
	 */
	public static int searchCost(int numblocks, int rpb){
		return 2; //directory block + data block
	}

}

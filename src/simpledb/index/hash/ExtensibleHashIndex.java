package simpledb.index.hash;

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
 * Am extensible hash implementation of the Index interface.
 *
 */
public class ExtensibleHashIndex implements Index {
	private String idxname;
	private Schema sch;
	private Transaction tx;
	private Constant searchkey = null;
	private TableScan ts = null;

	private static int MAX_BUCKET_SIZE; //number of records that can fit in a bucket
	private int globalDepth, numBuckets;
	private ArrayList<Bucket> directory;

	/**
	 * Opens a hash index for the specified index.
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
		MAX_BUCKET_SIZE = BLOCK_SIZE / ti.recordLength();
		globalDepth = 1;
		numBuckets = (int) Math.pow(2, globalDepth);
		directory = new ArrayList<>();
		directory.add(new Bucket(0, globalDepth, MAX_BUCKET_SIZE));
		directory.add(new Bucket(1, globalDepth, MAX_BUCKET_SIZE));

		System.out.println("New Extensible Hash Index Created");
		System.out.println();
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
		System.out.println("Inserting " + searchkey + " into bucket " + tblname);
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
		int insertBucketIndex = getBucketIndex(val);
		Bucket insertToBucket = directory.get(insertBucketIndex);

		if (insertToBucket.isFull()) {
			//if bucket is at global limit, double directory beforehand
			if (insertToBucket.getLocalDepth() == globalDepth) {
				expandDirectory();
			}

			//now either way we can assume localDepth < globalDepth
			if (insertToBucket.getLocalDepth() < globalDepth) {
				System.out.println("Splitting bucket " + idxname + insertBucketIndex);
				//add in new value, then split the overfull bucket into two
				insertToBucket.insert(val, rid);
				Bucket temp1, temp2;
				temp1 = new Bucket(-1, insertToBucket.getLocalDepth() + 1, MAX_BUCKET_SIZE);
				temp2 = new Bucket(-1, insertToBucket.getLocalDepth() + 1, MAX_BUCKET_SIZE);
				for (BucketVal b:
					 insertToBucket.getContents()) {
					Constant dataval = b.getDataval();
					int hash = getBucketIndex(dataval);

					if (hash == hash % insertBucketIndex) {
						temp2.insert(b.getDataval(), b.getRid());
					}
					else {
						temp1.insert(b.getDataval(), b.getRid());
					}
				}

				//determine where to place both buckets
				ArrayList<Integer> replacementIndexes = new ArrayList<>();
				for (int i = 0; i < directory.size(); i++) {
					if (directory.get(i) == insertToBucket) {
						replacementIndexes.add(i);
					}
				}
				for (int i:
						replacementIndexes) {
					if (i == i % insertBucketIndex) {
						temp2.setIndex(i);
						directory.set(i, temp2);
					}
					else {
						temp1.setIndex(i);
						directory.set(i, temp1);
					}
				}
			}
			printTable();
		}
		else {
			insertToBucket.insert(val, rid);
		}

		beforeFirst(val);
		ts.insert();
		ts.setInt("block", rid.blockNumber());
		ts.setInt("id", rid.id());
		ts.setVal("dataval", val);
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
		return Math.abs(key.hashCode()) % numBuckets;
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

//		Bucket emptyBucket = new Bucket(globalDepth, MAX_BUCKET_SIZE);
//		for (int i = 0; i < Math.abs(numBuckets - directory.size()); i++) {
//			directory.add(emptyBucket);
//		}
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



//	/**
//	 * Positions the index before the first index record
//	 * having the specified search key.
//	 * The method hashes the search key to determine the bucket,
//	 * and then opens a table scan on the file
//	 * corresponding to the bucket.
//	 * The table scan for the previous bucket (if any) is closed.
//	 *
//	 * @see Index#beforeFirst(Constant)
//	 */
//	public void beforeFirst(Constant searchkey) {
//		close();
//		this.searchkey = searchkey;
//		int hashVal = this.searchkey.hashCode() % directory.length;
//		int counter = 0;
//		boolean hashValMatch = false;
//		do {
//			if (directory[counter].getHashVal() == hashVal) {
//				directory[counter].delete(rid);
//			}
//		} while (!hashValMatch);
//	}
//
//	/**
//	 * Moves to the next record having the search key.
//	 * The method loops through the table scan for the bucket,
//	 * looking for a matching record, and returning false
//	 * if there are no more such records.
//	 *
//	 * @see Index#next()
//	 */
//	public boolean next() {
//	}
//
//	/**
//	 * Retrieves the dataRID from the current record
//	 * in the table scan for the bucket.
//	 *
//	 * @see Index#getDataRid()
//	 */
//	public RID getDataRid() {
////		int blknum = ts.getInt("block");
////		int id = ts.getInt("id");
////		return new RID(blknum, id);
//	}
//
//	/**
//	 * Inserts a new record into the table scan for the bucket.
//	 *
//	 * @see Index#insert(Constant, RID)
//	 */
//	public void insert(Constant val, RID rid) {
//	}
//
//	/**
//	 * Deletes the specified record from the table scan for
//	 * the bucket.  The method starts at the beginning of the
//	 * scan, and loops through the records until the
//	 * specified record is found.
//	 *
//	 * @see Index#delete(Constant, RID)
//	 */
//	public void delete(Constant val, RID rid) {
//		beforeFirst(val);
//
//	}
//
//	/**
//	 * Closes the index by closing the current table scan.
//	 *
//	 * @see Index#close()
//	 */
//	public void close() {
//		if (ts != null)
//			ts.close();
//	}
//
//}
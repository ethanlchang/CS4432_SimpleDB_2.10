package simpledb.buffer;

import simpledb.file.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;


/**
 * Questions:
 * 2.1 is done?
 * How to hash a page? Is it supposed to be block(Blocks have IDs)?
 * (2.4 Flush) Doesn't flush already do this? and pins counter is already in buffer (If its there and working then youre all set)
 * Where exactly do we look at & print out buffer (whenever its adding a page? is there a way we should control startup so it knows its in testing?)
 */

/**
 * Manages the pinning and unpinning of buffers to blocks.
 * @author Edward Sciore
 *
 */
class BasicBufferMgr {
   private Buffer[] bufferpool;
   private ArrayList<Integer> availableFrames;
   private Map<Integer, Integer> blkLocations;
   private int numAvailable;
   private int clockOffset;
   
   /**
    * Creates a buffer manager having the specified number 
    * of buffer slots.
    * This constructor depends on both the {@link FileMgr} and
    * {@link simpledb.log.LogMgr LogMgr} objects 
    * that it gets from the class
    * {@link simpledb.server.SimpleDB}.
    * Those objects are created during system initialization.
    * Thus this constructor cannot be called until 
    * {@link simpledb.server.SimpleDB#initFileAndLogMgr(String)} or
    * is called first.
    *
    * CS4432-Project1:
    * Added availableFrames, which stores the indexes of
    * each Buffer that can have a frame inserted
    * Here availableframes is initialized with the
    * Buffers in bufferpool
    *
    * @param numbuffs the number of buffer slots to allocate
    */
   BasicBufferMgr(int numbuffs) {
      bufferpool = new Buffer[numbuffs];
      availableFrames = new ArrayList<Integer>();
      blkLocations = new HashMap<Integer, Integer>();
      numAvailable = numbuffs;
      clockOffset = 0;
      for (int i=0; i<numbuffs; i++) {
         availableFrames.add(i);
         bufferpool[i] = new Buffer(i);
      }
   }
   
   /**
    * Flushes the dirty buffers modified by the specified transaction.
    * @param txnum the transaction's id number
    */
   synchronized void flushAll(int txnum) {
      for (Buffer buff : bufferpool)
         if (buff.isModifiedBy(txnum)) {
            blkLocations.remove(buff.block().hashCode());
            buff.flush();
            blkLocations.put(buff.block().hashCode(), buff.getIndex());
         }

   }
   
   /**
    * Pins a buffer to the specified block. 
    * If there is already a buffer assigned to that block
    * then that buffer is used;  
    * otherwise, an unpinned buffer from the pool is chosen.
    * Returns a null value if there are no available buffers.
    *
    * CS4432-Project1:
    * when a buffer is pinned, its index is removed from availableFrames
    *
    * @param blk a reference to a disk block
    * @return the pinned buffer
    */
   synchronized Buffer pin(Block blk) {
      Buffer buff = findExistingBuffer(blk);  // Check if block is already in bufferpool
      if (buff == null) {                     // If it isnt in bufferpool
         buff = chooseUnpinnedBuffer();       // Find unpinned buffer to replace
         if (buff == null)                    // If no unpinned buffers
            return null;

         if (buff.block() != null) {
            blkLocations.remove(buff.block().hashCode());  // Remove old pairing
         }
         buff.assignToBlock(blk);             // But the block in the buffer
         blkLocations.put(blk.hashCode(), buff.getIndex());  // Store the pairing
      }
      // If not already pinned, pin
      if (!buff.isPinned()) {
         System.out.println(availableFrames);
         System.out.println("Removing buffer: " + buff.getIndex());
         availableFrames.remove(availableFrames.indexOf(buff.getIndex()));
         numAvailable--;
      }
      buff.pin();
      return buff;
   }
   
   /**
    * Allocates a new block in the specified file, and
    * pins a buffer to it. 
    * Returns null (without allocating the block) if 
    * there are no available buffers.
    *
    * CS4432-Project1:
    * when a buffer is pinned, its index is removed from availableFrames
    *
    * @param filename the name of the file
    * @param fmtr a pageformatter object, used to format the new block
    * @return the pinned buffer
    */
   synchronized Buffer pinNew(String filename, PageFormatter fmtr) {
      Buffer buff = chooseUnpinnedBuffer();
      if (buff == null)
         return null;
      if (buff.block() != null) {
         blkLocations.remove(buff.block().hashCode());  // Remove old pairing
      }
      buff.assignToNew(filename, fmtr);
      System.out.println(availableFrames);
      System.out.println("Removing buffer: " + buff.getIndex());
      blkLocations.put(buff.block().hashCode(), buff.getIndex());  // Store the pairing
      availableFrames.remove(availableFrames.indexOf(buff.getIndex()));
      numAvailable--;
      buff.pin();
      return buff;
   }
   
   /**
    * Unpins the specified buffer.
    *
    * CS4432-Project1:
    * when a buffer is unpinned, its index is added to availableFrames
    *
    * @param buff the buffer to be unpinned
    */
   synchronized void unpin(Buffer buff) {
      buff.unpin();
      if (!buff.isPinned()) {
         System.out.println("Adding buffer: " + buff.getIndex());
         availableFrames.add(buff.getIndex());
         numAvailable++;
      }
   }
   
   /**
    * Returns the number of available (i.e. unpinned) buffers.
    * @return the number of available buffers
    */
   int available() {
      return numAvailable;
   }

   /**
    * CS4432-Project1:
    * (2.2) checks to see if the given block exists in the bufferpool already
    * @param blk block its looking for
    * @return buffer the block is in, null if not in bufferpool
    */
   private Buffer findExistingBuffer(Block blk) {
      //System.out.println(this.toString());
      //System.out.println("Block Hash: " + blk.hashCode());
      // If block isnt in bufferpool
      if (blkLocations.containsKey(blk.hashCode())) {
         int bloc = blkLocations.get(blk.hashCode());
         Buffer buff = bufferpool[bloc];
         //System.out.println("REPEAT: Block hash: " + buff.block().hashCode() + ", Block Loc: " + buff.getIndex() + ", " + buff.block().equals(blk));
         if (buff.block().equals(blk))
            return buff;
      }
      return null;
   }

   /*
   // Old function we replaced above
   private Buffer findExistingBuffer(Block blk) {
      System.out.println("Block ID: " + blk.number());
      for (Buffer buff : bufferpool) {
         Block b = buff.block();
         if (b != null && b.equals(blk)){

            System.out.println("REPEAT: Block ID: " + buff.block().number() + ", Block Loc: " + buff.getIndex());
            return buff;
         }
      }
      return null;
   }
   //*/


   /**
    * CS4432-Project1:
    * Now checks if there are buffers available
    * if true, it returns the first buffer from
    * the availableFrames Arraylist
    */
   private Buffer chooseUnpinnedBuffer() {
      if (numAvailable > 0){
         // Put Replacement policy (2.3) in here
         // Loop through all unpinned buffers looking for LRU or null blocks
         while(numAvailable > 0){
            clockOffset = clockOffset % availableFrames.size();
            Buffer buff = bufferpool[availableFrames.get(clockOffset)];
            if (buff.getClockCounter() == 0){
               clockOffset++;
               return buff;
            }
            else{
               clockOffset++;
               buff.setClockCounter(chooseUnpinnedBuffer().getClockCounter()-1);
            }
         }
      }
      return null;
   }

   /**
    * CS4432-Project1:
    * Added toString() for testing
    * returns a string listing
    * the number of buffers
    * and the t0String() for each buffer
    */
   @Override
   public String toString() {
      String output = "BasicBufferMgr:\n"
              + "Number of Buffers: " + bufferpool.length + "\n"
              + "Buffers:\n\n";
      for (Buffer buff : bufferpool)
         output += "" + buff.toString() + "\n";
      return output;
   }
}

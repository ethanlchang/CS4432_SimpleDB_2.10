package simpledb.buffer;

import simpledb.file.*;

import java.util.ArrayList;


/**
 * Questions:
 * Emptyframe = one that is unpinned?
 * How to hash a page?
 * (2.4 Flush) Doesn't flush already do this?
 */

/**
 * Manages the pinning and unpinning of buffers to blocks.
 * @author Edward Sciore
 *
 */
class BasicBufferMgr {
   private Buffer[] bufferpool;
   private ArrayList<Integer> availableFrames;
   private int numAvailable;
   
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
      numAvailable = numbuffs;
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
         if (buff.isModifiedBy(txnum))
         buff.flush();
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
      Buffer buff = findExistingBuffer(blk);
      if (buff == null) {
         buff = chooseUnpinnedBuffer();
         if (buff == null)
            return null;
         buff.assignToBlock(blk);
      }
      if (!buff.isPinned())
         availableFrames.remove(buff.getIndex());
         numAvailable--;
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
      buff.assignToNew(filename, fmtr);
      availableFrames.remove(buff.getIndex());
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
      if (!buff.isPinned())
         availableFrames.add(buff.getIndex());
         numAvailable++;
   }

   synchronized int findPage(Page p){
      // Find it
      for (Buffer buff : bufferpool){
         if(buff.containsPage(p)){
            return buff.getIndex();
         }
      }
      return -1;
   }
   
   /**
    * Returns the number of available (i.e. unpinned) buffers.
    * @return the number of available buffers
    */
   int available() {
      return numAvailable;
   }
   
   private Buffer findExistingBuffer(Block blk) {
      for (Buffer buff : bufferpool) {
         Block b = buff.block();
         if (b != null && b.equals(blk))
            return buff;
      }
      return null;
   }

   /**
    * CS4432-Project1:
    * Now checks if there are buffers available
    * if true, it returns the first buffer from
    * the availableFrames Arraylist
    */
   private Buffer chooseUnpinnedBuffer() {
      if (numAvailable > 0){
         Buffer buff = bufferpool[availableFrames.get(0)];
         return buff;
      }
      return null;
   }

}

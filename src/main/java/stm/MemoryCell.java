/**
 * Project: STMv2
 * Package: stm
 * File: MemoryCell.java
 * 
 * @author sidmishraw
 *         Last modified: Dec 17, 2017 2:59:20 PM
 */
package stm;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.gson.Gson;

import stm.annotations.ReadLocking;
import stm.annotations.WriteLocking;

/**
 * The concrete implementation of the transactional variable. Internally it is a memory cell, a member of the memory --
 * held in the STM. Each memory cell has data of type T and an unique ID -- UUID.
 * 
 * <p>
 * MemoryCell implements the TVar interface to prevent the consumer from accessing its features directly. Furthermore,
 * it's package scoped so that it is only accessible internally.
 * 
 * @author sidmishraw
 *
 *         Qualified Name: stm.MemoryCell
 *
 */
class MemoryCell<T> implements TVar<T> {
    
    // # For synchronized access to the memory cell - very granular
    private ReentrantReadWriteLock memCellLock;
    // # For synchronized access to the memory cell - very granular
    
    /**
     * The identifier of the memory cell, it is a UUID of type 4
     */
    private UUID                   ID;
    
    /**
     * Data contained in the memory cell
     */
    private T                      data;
    
    /**
     * Constructs a new memory cell
     * 
     * @param data
     *            The data held in the memory cell
     */
    public MemoryCell(T data) {
        this.memCellLock = new ReentrantReadWriteLock();
        this.ID = UUID.randomUUID();
        this.data = data;
    }
    
    /**
     * Reads the data in the memory cell, only visible inside the stm package.
     * 
     * @InternalUsage
     * 
     * @return the copy of the data contained in the memory cell.
     */
    @ReadLocking
    T read() {
        T data = null;
        try {
            this.memCellLock.readLock().lock();
            data = this.data;
            return data;
        } finally {
            this.memCellLock.readLock().unlock();
        }
    }
    
    /**
     * Writes the data into the memory cell.
     * 
     * @InternalUsage
     * 
     * @param newData
     *            the new data to be written into the memory cell
     */
    @WriteLocking
    void write(T newData) {
        if (Objects.isNull(newData)) {
            return;
        }
        try {
            this.memCellLock.writeLock().lock();
            this.data = newData;
        } finally {
            this.memCellLock.writeLock().unlock();
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.ID == null) ? 0 : this.ID.hashCode());
        return result;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof MemoryCell)) {
            return false;
        }
        MemoryCell<?> other = (MemoryCell<?>) obj;
        if (this.ID == null) {
            if (other.ID != null) {
                return false;
            }
        } else if (!this.ID.equals(other.ID)) {
            return false;
        }
        return true;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}

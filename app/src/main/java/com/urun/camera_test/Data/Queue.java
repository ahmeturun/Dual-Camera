package com.urun.camera_test.Data;

import java.util.Collection;
import java.util.Iterator;

/**
 * Created by ahmet on 12/13/2016.
 */

public class Queue implements java.util.Queue {
    private class QueueNode{
        Object data;
        QueueNode nextQueueNode;

        public QueueNode(Object data) {
            this.data = data;
        }
    }

    QueueNode frontNode;
    QueueNode backNode;

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return frontNode == null;
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public Iterator iterator() {
        return null;
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public Object[] toArray(Object[] a) {
        return new Object[0];
    }

    @Override
    public boolean add(Object o) {
        QueueNode newNode = new QueueNode(o);
        if(frontNode==null){
            frontNode=newNode;
        }else{
            backNode.nextQueueNode = newNode;
        }
        backNode=newNode;
        return true;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean addAll(Collection c) {
        return false;
    }

    @Override
    public void clear() {

    }

    @Override
    public boolean retainAll(Collection c) {
        return false;
    }

    @Override
    public boolean removeAll(Collection c) {
        return false;
    }

    @Override
    public boolean containsAll(Collection c) {
        return false;
    }

    @Override
    public boolean offer(Object o) {
        return false;
    }

    @Override
    public Object remove() {
        return null;
    }

    @Override
    public Object poll() {
        if(!isEmpty()){
            Object returnValue = frontNode.data;
            frontNode = frontNode.nextQueueNode;
            return returnValue;
        }
        return null;
    }

    @Override
    public Object element() {
        return null;
    }

    @Override
    public Object peek() {
        return null;
    }
}

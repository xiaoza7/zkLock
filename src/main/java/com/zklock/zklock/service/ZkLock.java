package com.zklock.zklock.service;

import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * lmq
 */

public class ZkLock implements Lock {
    private ThreadLocal<ZooKeeper> zk =new ThreadLocal<>();

    private String LOCK_NAME = "/Lock";

    private ThreadLocal<String>CURRENT_NODE = new ThreadLocal<>();

    public void init()
    {
         if(zk.get()==null)
         {
             try {
                 zk.set(new ZooKeeper("localhost:2181", 3000, new Watcher() {
                     @Override
                     public void process(WatchedEvent watchedEvent) {

                     }
                 }));
             } catch (IOException e) {
                 e.printStackTrace();
             }
         }
    }

    public void lock()
    {
        init();
        if(tryLock())
        {
            System.out.println(Thread.currentThread().getName()+ "has already get the lock...........");
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    public boolean tryLock() {

        String nodeName = LOCK_NAME + "/zk_";
        try {
            CURRENT_NODE.set(zk.get().create(nodeName,new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL));
            List<String> list = zk.get().getChildren(LOCK_NAME,false);

            Collections.sort(list);

            String minIdNodeName = list.get(0);
            String currentNodeName = CURRENT_NODE.get().substring(CURRENT_NODE.get().lastIndexOf("/")+1);

            if(currentNodeName.equals(minIdNodeName))
            {
                //lock is self
                return  true;
            }else{

                int currentNodeIndex = list.indexOf(currentNodeName);

                String prevNodeName=list.get(currentNodeIndex-1);

                //if can not lock, just block the thread
                final CountDownLatch countDownLatch = new CountDownLatch(1);

                zk.get().exists(LOCK_NAME + "/" + prevNodeName, new Watcher() {
                    @Override
                    public void process(WatchedEvent watchedEvent) {
                        if(Event.EventType.NodeDeleted.equals(watchedEvent.getType()))
                        {
                            countDownLatch.countDown();
                            System.out.println(Thread.currentThread().getName()+ "is wakedup...");
                        }

                    }
                });

                System.out.println(Thread.currentThread().getName()+ "is blocked...");
                countDownLatch.await();
                return true;
            }


        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return false;
    }


    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

//     @Override
//    public void unlock() {
//
//    }

    @Override
    public Condition newCondition() {
        return null;
    }


    public void unlock()
    {
        try {
           zk.get().delete(CURRENT_NODE.get(),-1);
            CURRENT_NODE.set(null);
            zk.get().close();
            System.out.println(Thread.currentThread().getName()+ "has already released the lock...........");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }


}
